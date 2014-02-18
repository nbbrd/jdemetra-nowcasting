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
package ec.tstoolkit.mssf2;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockIterator;
import ec.tstoolkit.maths.matrices.LowerTriangularMatrix;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;

/**
 *
 * @author Jean Palate
 */
public class MFilter {

    /**
     *
     */
    public static final double Zero = 1e-9;

    private MState m_state;
    private IMSsf m_ssf;
    private IMSsfData m_data;
    private int m_pos, m_end, m_r, m_e, m_v;

    /**
     *
     */
    public MFilter() {
    }

    /**
     * Computes zm = Z * M
     *
     * @param M
     * @param zm
     */
    private void ZM(SubMatrix M, SubMatrix zm) {
        DataBlockIterator mcols = M.columns();
        DataBlock mcol = mcols.getData();
        DataBlockIterator zrows = zm.rows();
        DataBlock zr = zrows.getData();
        int i = 0;
        do {
            mcols.begin();
            if (m_data.isMissing(i, m_pos)) {
                zr.set(0);
            } else {
                do {
                    zr.set(mcols.getPosition(), m_ssf.ZX(m_pos, i, mcol));
                } while (mcols.next());
            }
            ++i;
        } while (zrows.next());
    }

    private void cleanMissing(SubMatrix V) {
        for (int i = 0; i < m_v; ++i) {
            if (m_data.isMissing(i, m_pos)) {
                V.row(i).set(0);
                V.column(i).set(0);
            }
        }
    }

    /**
     *
     */
    public void mpred() {

        // K = TPZ'(ZPZ')^-1
        // computes (ZP)' in K'. Missing values are set to 0 
        // Z~v x r, P~r x r, K~r x v
        SubMatrix F = m_state.F.subMatrix(), K = m_state.K.subMatrix();
        F.set(0);
        K.set(0);
        ZM(m_state.P.subMatrix(), K.transpose());
        // computes ZPZ'; results in m_state.F
        ZM(K, F);
        SymmetricMatrix.reinforeSymmetry(m_state.F);
        m_ssf.addH(m_pos, F);
        cleanMissing(F);

        // m_state.F contains the Cholesky factor !!!
        SymmetricMatrix.lcholesky(m_state.F, 1e-9);

        // We put in K  TPZ'*(ZPZ'+H)^-1/2 = TPZ'* F^-1 = TPZ'*(LL')^-1/2 = TPZ'(L')^-1
        // K L' = TPZ' or L K' = ZPT'
        m_ssf.TM(m_pos, K);
        LowerTriangularMatrix.rsolve(m_state.F, K.transpose(), Zero);

        m_state.E.set(0);
        for (int i = 0; i < m_v; ++i) {
            if (!m_data.isMissing(i, m_pos)) {
                double y = m_data.get(i, m_pos);
                m_state.E.set(i, y - m_ssf.ZX(m_pos, i, m_state.A));
            }
        }
        LowerTriangularMatrix.rsolve(m_state.F, m_state.E, Zero);
    }

    /**
     *
     * @return
     */
    public MState getState() {
        return m_state;
    }

    private boolean initFilter() {
        m_pos = 0;
        m_r = m_ssf.getStateDim();
        m_v = m_ssf.getVarsCount();
        m_e = m_ssf.getTransitionResDim();
        m_end = m_data.getCount();
        m_pos = 0;
        return true;
    }

    private int initState() {
        m_state = new MState(m_r, m_v);
        double[] a0 = m_data.getInitialState();
        if (a0 != null) {
            m_state.A.copyFrom(a0, 0);
        }
        m_ssf.Pf0(m_state.P.subMatrix());
        m_ssf.TVT(m_pos, m_state.P.subMatrix());
        m_ssf.addV(m_pos, m_state.P.subMatrix());
        return 0;
    }

    /**
     *
     */
    public void next() {

        // P = TPT' - (TM)* F^-1 *(TM)' + RQR' --> Symmetric
        // TPZ'(LL')^-1 ZPT' =TPZ'L'^-1*L^-1*ZPT'
        // A = Ta + (TM)* F^-1 * v
        m_ssf.TVT(m_pos, m_state.P.subMatrix());

        for (int i = 0; i < m_v; ++i) {
            if (!m_data.isMissing(i, m_pos)) {
                //for (int j = 0; j < m_v; ++j) {
                //if (!m_data.isMissing(j, m_pos)) {
                update(m_state.P, m_state.K.column(i));//, m_state.K.column(i));
                //}
                //}
            }
        }
        m_ssf.addV(m_pos, m_state.P.subMatrix());

        //a(t+1)=Ta(t)+(TPZ)F^-1* v=Ta(t)+(TPZ)(LL')^-1* v
        // = Ta(t)+(TPZ)(L')^-1* L^-1*v =  Ta(t)+K*L^-1*v
        // U = L^-1*E or LU=E
        m_ssf.TX(m_pos, m_state.A);

        for (int i = 0; i < m_v; ++i) {
            if (!m_data.isMissing(i, m_pos)) {
                m_state.A.addAY(m_state.E.get(i), m_state.K.column(i));
            }
        }
    }

    /**
     *
     * @param ssf
     * @param data
     * @param rslts
     * @return
     */
    public boolean process(final IMSsf ssf, final IMSsfData data, final IMFilteringResults rslts) {
        m_ssf = ssf;
        m_data = data;
        if (!initFilter()) {
            return false;
        }
        m_pos = initState();
        if (m_pos < 0) {
            return false;
        }
        if (rslts != null) {
            rslts.prepare(m_ssf, m_data);
        }
        if (m_pos < m_end) {
            do {
                mpred();
                if (rslts != null) {
                    rslts.save(m_pos, m_state);
                }
                next();
            } while (++m_pos < m_end);
        }
        if (rslts != null) {
            rslts.close();
        }
        return true;
    }

    // P -= c*r
    private void update(Matrix P, DataBlock c) {//, DataBlock r) {
//        DataBlockIterator cols = P.columns();
//        DataBlock col = cols.getData();
//        do {
//            double a = r.get(cols.getPosition());
//            col.addAY(-a, c);
//        } while (cols.next());
        P.addXaXt(-1, c);
    }
}
