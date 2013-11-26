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
import ec.tstoolkit.maths.matrices.MatrixException;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;

/**
 *
 * @author Jean Palate
 */
public class ArrayFilter {

    private IArraySsf m_ssf;
    private IMSsfData m_data;
    private Matrix m_X;
    // m_r=state dim, m_v= measurement dim, m_e = transition res dim
    private int m_pos, m_end, m_r, m_v, m_e;
    private ArrayState m_state;

    /**
     *
     * @param ssf
     * @param data
     * @param rslts
     * @return
     */
    public boolean process(IArraySsf ssf, IMSsfData data, IArrayFilteringResults rslts) {
        m_ssf = ssf;
        m_data = data;
        if (!initFilter()) {
            return false;
        }
        rslts.prepare(ssf, data);
        do {
            preArray();
            if (!triangularize()) {
                return false;
            }
            // save results...
            mpred();
            rslts.save(m_pos, m_state);
            postArray();
        } while (++m_pos < m_end);
        return true;
    }

    private boolean initFilter() {
        clear();
        m_r = m_ssf.getStateDim();
        m_v = m_ssf.getVarsCount();
        m_e = m_ssf.getTransitionResDim();
        m_end = m_data.getCount();
        // initialise the working matrix
        m_X = new Matrix(m_r + m_v, m_r + m_v + m_e);
        // initialise the state
        m_state = new ArrayState(m_r, m_v);
        double[] a0 = m_data.getInitialState();
        if (a0 != null) {
            m_state.A.copyFrom(a0, 0);
        }
        Matrix P0 = new Matrix(m_r, m_r);
        m_ssf.Pf0(P0.subMatrix());
        try {
            SymmetricMatrix.lcholesky(P0, 1e-9);
            E().copy(P0.subMatrix());
            m_state.Pl.copy(P0);
            return true;
        } catch (MatrixException err) {
            return false;
        }
    }

    /**
     *
     */
    protected void clear() {
        m_pos = 0;
        m_state = null;
    }

    private boolean triangularize() {
        return ec.tstoolkit.maths.matrices.ElementaryTransformations.householderTriangularize(m_X.subMatrix());
        //return ec.tstoolkit.maths.matrices.ElementaryTransformations.givensTriangularize(m_X.subMatrix());
    }

    private void mpred() {
        m_state.F.subMatrix().copy(A());
        m_state.K.subMatrix().copy(D());
        m_state.E.set(0);
        for (int i = 0; i < m_v; ++i) {
            if (!m_data.isMissing(i, m_pos)) {
                double y = m_data.get(i, m_pos);
                m_state.E.set(i, y - m_ssf.ZX(m_pos, i, m_state.A));
            }
        }
        LowerTriangularMatrix.rsolve(m_state.F, m_state.E);
    }
    // X = |A B C|
    //     |D E F|

    private SubMatrix A() {
        return m_X.subMatrix(0, m_v, 0, m_v);
    }

    private SubMatrix B() {
        return m_X.subMatrix(0, m_v, m_v, m_v + m_r);
    }

    private SubMatrix C() {
        return m_X.subMatrix(0, m_v, m_v + m_r, m_v + m_r + m_e);
    }

    private SubMatrix D() {
        return m_X.subMatrix(m_v, m_v + m_r, 0, m_v);
    }

    private SubMatrix E() {
        return m_X.subMatrix(m_v, m_v + m_r, m_v, m_v + m_r);
    }

    private SubMatrix F() {
        return m_X.subMatrix(m_v, m_v + m_r, m_v + m_r, m_v + m_r + m_e);
    }

    // We start with 
    // X(t-1) = |. .       .|
    //          |. P*(t-1) .|
    // We set
    // X = |H*  ZP* 0 |
    //     |0   TP* Q*|
    private void preArray() {
        SubMatrix ma = A(), mb = B(), mc = C(), md = D(), me = E(), mf = F();
        ma.set(0);
        mb.set(0);
        mc.set(0);
        md.set(0);
        mf.set(0);
        m_ssf.R(m_pos, ma);
        // we take into account missing values: just set some rows/columns of
        // H, Z to zero...
        for (int i = 0; i < m_v; ++i) {
            if (m_data.isMissing(i, m_pos)) {
                ma.row(i).set(0);
            }
        }
        ZM(me, mb);
        m_ssf.TM(m_pos, me);
        m_ssf.W(m_pos, mf);
    }

    private void postArray() {
        m_state.Pl.subMatrix().copy(E());
        m_ssf.TX(m_pos, m_state.A);
        // update with K*e
        for (int i = 0; i < m_v; ++i) {
            if (!m_data.isMissing(i, m_pos)) {
                m_state.A.addAY(m_state.E.get(i), m_state.K.column(i));
            }
        }
    }

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
}
