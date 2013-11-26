/*
 * Copyright 2013 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package ec.tstoolkit.ssf2;

import ec.tstoolkit.data.SubArrayOfInt;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockIterator;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class DurbinKoopmanInitializer implements ISsfInitializer<ISsf> {
    
    private ISsf m_ssf;
    private IDiffuseFilteringResults m_frslts;
    private int m_r;
    private double m_norm = 0;
    private boolean m_qinit;
    int[] m_idxR;
    Matrix m_Q, m_WQW;
    Matrix m_W;

    /**
     *
     */
    public DurbinKoopmanInitializer() {
    }

    /**
     *
     * @param pos
     * @param data
     * @param state
     */
    protected void epredDiffuse(final int pos, final ISsfData data,
            final DiffuseState state) {
        // calc f and fi
        // fi = Z Pi Z' , f = Z P Z' + H
        // m_fi=m_Pi.quadraticForm(m_Z);
        // m_ff=m_Pf.quadraticForm(m_Z)+m_h;
        state.fi = m_ssf.ZVZ(pos, state.Pi.subMatrix());
        if (Math.abs(state.fi) < BaseState.EPS) {
            state.fi = 0;
        }
        state.f = m_ssf.ZVZ(pos, state.P.subMatrix()) + m_ssf.H(pos);
        if (Math.abs(state.f) / m_norm < BaseState.EPS) {
            state.f = 0;
        }
        if (data.hasData()) {
            double y = data.get(pos);
            if (Double.isNaN(y)) {
                state.e = Double.NaN;
                return;
            } else {
                state.e = y - m_ssf.ZX(pos, state.A);
            }
        }
        
        Utilities.ZM(m_ssf, pos, state.P.columns(), state.C);
        m_ssf.TX(pos, state.C);
        if (state.fi != 0) {
            Utilities.ZM(m_ssf, pos, state.Pi.columns(), state.Ci);
            m_ssf.TX(pos, state.Ci);
        }
    }

    /**
     *
     * @param ssf
     * @param data
     * @param state
     * @param rslts
     * @return
     */
    @Override
    public int initialize(final ISsf ssf, final ISsfData data,
            final State state, final IFilteringResults rslts) {
        m_ssf = ssf;
        m_r = ssf.getStateDim();
        if (rslts instanceof IDiffuseFilteringResults) {
            m_frslts = (IDiffuseFilteringResults) rslts;
        } else {
            m_frslts = null;
        }
        int pos = 0, end = data.getCount();
        DiffuseState dstate = new DiffuseState(m_ssf.getStateDim(), data.hasData());
        initState(data, dstate);
        if (m_frslts != null) {
            m_frslts.prepareDiffuse(m_ssf, data);
        }
        if (m_ssf.isDiffuse()) {
            do {
                epredDiffuse(pos, data, dstate);
                if (m_frslts != null) {
                    m_frslts.save(pos, dstate);
                }
                nextDiffuse(pos, data, dstate);
            } while (++pos < end && !isNull(dstate.Pi));
        }
        if (m_frslts != null) {
            m_frslts.closeDiffuse();
        }
        state.P = dstate.P;
        state.C = dstate.C;
        state.A = dstate.A;
        state.e = dstate.e;
        state.f = dstate.f;
        return pos;
    }
    
    private void initState(final ISsfData data, final DiffuseState state) {
        double[] a0 = data.getInitialState();
        if (a0 != null) {
            state.A.copyFrom(a0, 0);
        }
        m_ssf.Pf0(state.P.subMatrix());
        m_ssf.Pi0(state.Pi.subMatrix());
        m_norm = state.Pi.nrm2();
    }
    
    private boolean isNull(final Matrix P) {
        return P.isZero(BaseState.EPS * m_norm);
    }
    
    private void nextDiffuse(final int pos, final ISsfData data,
            final DiffuseState state) {
        if (state.isMissing()) {
            nextMissingDiffuse(pos, data, state);
        } else if (state.fi == 0) {
            nextDiffuse0(pos, data, state);
        } else {
            nextDiffuse1(pos, data, state);
        }
    }
    
    private void nextDiffuse0(final int pos, final ISsfData data,
            final DiffuseState state) {
        // variance
        m_ssf.TVT(pos, state.P.subMatrix());
        m_ssf.TVT(pos, state.Pi.subMatrix());

//        for (int i = 0; i < m_r; ++i)
//        {
//            double c = -state.C.get(i) / state.f;
//            if (c != 0)
//                for (int j = 0; j <= i; ++j)
//                {
//                    double cc = c * state.C.get(j);
//                    if (cc != 0)
//                        state.P.add(i, j, cc);
//                }
//        }
//        SymmetricMatrix.fromLower(state.P);
        DataBlockIterator cols = state.P.columns();
        DataBlock col = cols.getData();
        int icol = 0;
        do {
            double c = -state.C.get(icol) / state.f;
            if (pos > 0) {
                col.drop(icol, 0).addAY(c, state.C.drop(icol, 0));
            } else {
                col.addAY(c, state.C);
            }
            ++icol;
        } while (cols.next());
        SymmetricMatrix.fromLower(state.P);
        
        m_ssf.addV(pos, state.P.subMatrix());

        // state
        // a0 = Ta0 + f1*TMi*v0. Reuse Mf as temporary buffer
        if (data.hasData()) {
            m_ssf.TX(pos, state.A);
            // prod(n, m_T, m_a0, m_tmp);
            double c = state.e / state.f;
//            for (int i = 0; i < m_r; ++i)
//                state.A.set(i, state.A.get(i) + state.C.get(i) * c);
            state.A.addAY(c, state.C);
        }
    }
    
    private void nextDiffuse1(final int pos, final ISsfData data,
            final DiffuseState state) {
        // calc f0, f1, f2
        double f1 = 1 / state.fi;
        double f2 = -state.f * f1 * f1;

        // Pi = T Pi T' - f1* (TMi)(TMi)'
        m_ssf.TVT(pos, state.Pi.subMatrix());
        DataBlockIterator cols = state.Pi.columns();
        DataBlock col = cols.getData();
        int icol = 0;
        do {
            double c = -state.Ci.get(icol) * f1;
            if (pos > 0) {
                col.drop(icol, 0).addAY(c, state.Ci.drop(icol, 0));
            } else {
                col.addAY(c, state.Ci);
            }
            ++icol;
        } while (cols.next());
        SymmetricMatrix.fromLower(state.Pi);

        // P = T P T' - f2*(TMi)(TMi)'-f1(TMi*TMf' + TMf*TMi')+RQR'
        // m_Pf=m_Pf.quadraticForm(new TmpTranspose(m_T));
        m_ssf.TVT(pos, state.P.subMatrix());
        
        cols = state.P.columns();
        col = cols.getData();
        icol = 0;
        do {
            double c = f2 * state.Ci.get(icol) + f1 * state.C.get(icol);
            if (pos > 0) {
                col.drop(icol, 0).addAY(-c, state.Ci.drop(icol, 0));
            } else {
                col.addAY(-c, state.Ci);
            }
            ++icol;
        } while (cols.next());
        cols.begin();
        icol = 0;
        do {
            double c = f1 * state.Ci.get(icol);
            if (pos > 0) {
                col.drop(icol, 0).addAY(-c, state.C.drop(icol, 0));
            } else {
                col.addAY(-c, state.C);
            }
            ++icol;
        } while (cols.next());
        SymmetricMatrix.fromLower(state.P);
        // Add RQR'
        m_ssf.addV(pos, state.P.subMatrix());
        
        if (data.hasData()) {
            // a0 = Ta0 + f1*TMi*v0. Reuse Mf as temporary buffer
            m_ssf.TX(pos, state.A);
            // prod(n, m_T, m_a0, m_tmp);
            double q = f1 * state.e;
            state.A.addAY(q, state.Ci);
        }
    }
    
    private void nextMissingDiffuse(final int pos, final ISsfData data,
            final DiffuseState state) {
        // variance
        m_ssf.TVT(pos, state.P.subMatrix());
        m_ssf.TVT(pos, state.Pi.subMatrix());
        m_ssf.addV(pos, state.P.subMatrix());
        m_ssf.TX(pos, state.A);
    }
}
