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

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockIterator;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.ElementaryTransformations;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;

/**
 * Mixed algorithm based on the diffuse initializer of Durbin-Koopman and on the
 * (square root) array filter of Kailath for the diffuse part. That solution
 * provides a much more stable estimate of the diffuse part.
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Preliminary)
public class DiffuseSquareRootInitializer implements ISsfInitializer<ISsf> {

    private ISsf m_ssf;
    private IDiffuseFilteringResults m_frslts;
    private int m_r;
    private boolean m_saveDiffuseVariance = true;
    private boolean m_qinit;
    Matrix X_;

    /**
     *
     */
    public DiffuseSquareRootInitializer() {
    }

    /**
     *
     * @return
     */
    public boolean isSavingDiffuseVariance() {
        return m_saveDiffuseVariance;
    }

    /**
     *
     * @param save
     */
    public void setSavingDiffuseVariance(boolean save) {
        m_saveDiffuseVariance = save;
    }

    /**
     *
     * @param pos
     * @param data
     * @param state
     */
    protected void EPredDiffuse(final int pos, final ISsfData data,
            final DiffuseState state) {
        // calc f and fi
        // fi = Z Pi Z' , f = Z P Z' + H
        // m_fi=m_Pi.quadraticForm(m_Z);
        // m_ff=m_Pf.quadraticForm(m_Z)+m_h;
        preArray(pos);
        state.fi = X_.row(0).ssq();
        if (state.fi < State.ZERO) {
            state.fi = 0;
            X_.row(0).set(0);
        }

        state.f = m_ssf.ZVZ(pos, state.P.subMatrix());
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
            ElementaryTransformations.givensTriangularize(X_.subMatrix());
            state.Ci.copy(X_.column(0).drop(1, 0));
            state.Ci.mul(X_.get(0, 0));
        }
    }


    private void initialize() {
        int resdim = m_ssf.getTransitionResDim();
        m_qinit = false;
         // load diffuse constraints
        int d = m_ssf.getNonStationaryDim();
        X_ = new Matrix(m_r + 1, d + 1);
        m_ssf.diffuseConstraints(X_.subMatrix(1, 1 + m_r, 1, 1 + d));
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
        initialize();
        int pos = 0, end = data.getCount();
        DiffuseState dstate = new DiffuseState(m_ssf.getStateDim(), data.hasData());
        initState(data, dstate);
        if (m_frslts != null) {
            m_frslts.prepareDiffuse(m_ssf, data);
        }
        if (m_ssf.isDiffuse()) {
            do {
                EPredDiffuse(pos, data, dstate);
                if (m_frslts != null) {
                    m_frslts.save(pos, dstate);
                }
                nextDiffuse(pos, data, dstate);
            } while (++pos < end && !isNull(X_.subMatrix(1, X_.getRowsCount(), 1, X_.getColumnsCount())));
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
    }

    private boolean isNull(final SubMatrix P) {
        return P.isNull(State.ZERO);
    }

    private void nextDiffuse(final int pos, final ISsfData data,
            final DiffuseState state) {
        if (state.isMissing() || (state.fi == 0 && state.f == 0)) {
            nextMissingDiffuse(pos, data, state);
        } else if (state.fi == 0) {
            nextDiffuse0(pos, data, state);
        } else if (state.f == 0) {
            nextDiffuse1(pos, data, state);
        } else {
            nextDiffuse2(pos, data, state);
        }
        if (m_saveDiffuseVariance) {
            SymmetricMatrix.XXt(X_.subMatrix(1, X_.getRowsCount(), 1, X_.getColumnsCount()), state.Pi.subMatrix());
        }
    }

    private void nextDiffuse0(final int pos, final ISsfData data,
            final DiffuseState state) {
        //state.f != 0
        // variance
        m_ssf.TVT(pos, state.P.subMatrix());
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

    private void nextDiffuse2(final int pos, final ISsfData data,
            final DiffuseState state) {
        //state.f != 0, state.fi != 0
        // calc f0, f1, f2
        double f1 = 1 / state.fi;
        double f2 = -state.f * f1 * f1;


        // P = T P T' - f2*(TMi)(TMi)'-f1(TMi*TMf' + TMf*TMi')+RQR'
        // m_Pf=m_Pf.quadraticForm(new TmpTranspose(m_T));
        m_ssf.TVT(pos, state.P.subMatrix());

        DataBlockIterator cols = state.P.columns();
        DataBlock col = cols.getData();
        int icol = 0;
        do {
            double c = f2 * state.Ci.get(icol) + f1 * state.C.get(icol);
            if (icol > 0) {
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
            if (icol > 0) {
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

    private void nextDiffuse1(final int pos, final ISsfData data,
            final DiffuseState state) {
        //state.f == 0, state.fi != 0 -> f2=0
        // calc f0, f1, f2
        double f1 = 1 / state.fi;


        // P = T P T' - f2*(TMi)(TMi)'-f1(TMi*TMf' + TMf*TMi')+RQR'
        // m_Pf=m_Pf.quadraticForm(new TmpTranspose(m_T));
        m_ssf.TVT(pos, state.P.subMatrix());

        DataBlockIterator cols = state.P.columns();
        DataBlock col = cols.getData();
        int icol = 0;
        do {
            double c = f1 * state.Ci.get(icol);
            if (icol > 0) {
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
        m_ssf.addV(pos, state.P.subMatrix());
        m_ssf.TX(pos, state.A);
    }

    // Array routines
    //     |0 Z X|
    // X = |     |
    //     |0 T X|
    private void preArray(final int pos) {
        int rmax = X_.getRowsCount(), cmax = X_.getColumnsCount();
        DataBlock X = X_.row(0).range(1, cmax);
        X.set(0);
        X_.column(0).set(0);
        SubMatrix Y = X_.subMatrix(1, rmax, 1, cmax);
        Utilities.ZM(m_ssf, pos, Y.columns(), X);
        DataBlockIterator lcols = Y.columns();
        DataBlock lcol = lcols.getData();
        do {
            m_ssf.TX(pos, lcol);
        } while (lcols.next());
    }

 }
