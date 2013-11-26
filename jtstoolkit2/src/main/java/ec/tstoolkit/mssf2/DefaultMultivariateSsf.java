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
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public abstract class DefaultMultivariateSsf extends AbstractMultivariateSsf {

    /**
     *
     */
    protected double[] m_H;
    /**
     *
     */
    protected Matrix m_Z;
    /**
     *
     */
    protected DataBlock m_tmp;
    /**
     *
     */
    protected Matrix m_T,
    /**
     *
     */
    m_S;
    /**
     *
     */
    protected Matrix m_Pf0,
    /**
     *
     */
    m_B0,
    /**
     *
     */
    m_Q,
    /**
     *
     */
    m_V,
    /**
     *
     */
    m_W;
    private int m_cur = -1;

    /**
     *
     * @param pos
     */
    protected void load(int pos) {
        if (pos == m_cur) {
            return;
        }
        m_cur = pos;
        loadT(pos, m_T);
        loadQ(pos, m_Q);
        if (m_S != null) {
            loadS(pos, m_S);
            m_V = SymmetricMatrix.quadraticFormT(m_Q, m_S);
        }
        loadZ(pos, m_Z);
        loadH(pos, m_H);
    }

    /**
     *
     */
    public DefaultMultivariateSsf() {
    }

    /**
     *
     * @param b
     */
    @Override
    public void diffuseConstraints(final SubMatrix b) {
        if (m_B0 != null) {
            b.copy(m_B0.subMatrix());
        }
    }

    /**
     *
     * @param pos
     * @param qm
     */
    @Override
    public void V(final int pos, final SubMatrix qm) {
        load(pos);
        Matrix SQS;
        if (hasS()) {
            if (!loadS(pos, m_S)) {
                return;
            } else {
                SQS = SymmetricMatrix.quadraticFormT(m_Q, m_S);
            }
        } else {
            SQS = m_Q;
        }
        qm.copy(SQS.subMatrix());
    }

    @Override
    public int getVarsCount() {
        return m_Z == null ? 0 : m_Z.getRowsCount();
    }

    /**
     *
     * @return
     */
    @Override
    public int getNonStationaryDim() {
        return m_B0 == null ? 0 : m_B0.getColumnsCount();
    }

    /**
     *
     * @return
     */
    @Override
    public int getStateDim() {
        return m_Z == null ? 0 : m_Z.getColumnsCount();
    }

    /**
     *
     * @return
     */
    @Override
    public int getTransitionResDim() {
        return m_Q == null ? 0 : m_Q.getRowsCount();
    }

    /**
     *
     * @return
     */
    @Override
    public boolean hasS() {
        return m_S != null;
    }

    /**
     *
     * @param pos
     * @return
     */
    @Override
    public boolean hasTransitionRes(final int pos) {
        return true;
    }

    /**
     *
     * @param dim
     * @param vardim
     * @param resdim
     * @param hasH
     */
    public void initialize(final int dim, final int vardim, final int resdim, final boolean hasH) {
        m_T = new Matrix(dim, dim);
        m_Z = new Matrix(vardim, dim);
        m_tmp = new DataBlock(dim);
        m_Q = new Matrix(resdim, resdim);
        if (hasH) {
            m_H = new double[vardim];
        }
        if (dim != resdim) {
            m_S = new Matrix(dim, resdim);
            m_W = new Matrix(dim, resdim);
        } else {
            m_W = new Matrix(dim, dim);
        }

    }

    /**
     *
     * @return
     */
    @Override
    public boolean isDiffuse() {
        return m_B0 != null;
    }

    /**
     *
     * @return
     */
    @Override
    public abstract boolean isMeasurementEquationTimeInvariant();

    /**
     *
     * @return
     */
    @Override
    public abstract boolean isTimeInvariant();

    /**
     *
     * @return
     */
    @Override
    public abstract boolean isTransitionEquationTimeInvariant();

    /**
     *
     * @return
     */
    @Override
    public abstract boolean isTransitionResidualTimeInvariant();

    /**
     *
     * @return
     */
    @Override
    public boolean isValid() {

        if (m_Z == null || m_T == null || m_Q == null) {
            return false;
        }
        int r = m_Z.getColumnsCount();
        if (r != m_T.getColumnsCount() || r != m_T.getRowsCount()) {
            return false;
        }
        if (m_Pf0 != null && m_Pf0.getRowsCount() != r) {
            return false;
        }
        return m_B0 == null || (m_B0.getRowsCount() == r && m_B0.getColumnsCount() <= m_B0
                .getRowsCount());

    }

    /**
     * L = T - K * Z
     *
     * @param pos
     * @param k
     * @param lm
     */
    @Override
    public void L(final int pos, final SubMatrix k, final SubMatrix lm) {
        load(pos);
        T(pos, lm);
        DataBlockIterator cols = lm.columns();
        DataBlock col = cols.getData();
        DataBlockIterator kcols = k.columns();
        DataBlock kcol = kcols.getData();
        do {
            kcols.begin();
            do {
                double z = -m_Z.get(kcols.getPosition(), cols.getPosition());
                col.addAY(z, kcol);

            } while (kcols.next());
        } while (cols.next());
    }

    /**
     *
     * @param pos
     * @param q
     * @return
     */
    protected abstract boolean loadQ(int pos, Matrix q);

    /**
     *
     * @param pos
     * @param t
     * @return
     */
    protected abstract boolean loadT(int pos, Matrix t);

    /**
     *
     * @param pos
     * @param w
     * @return
     */
    protected abstract boolean loadS(int pos, Matrix w);

    /**
     *
     * @param pos
     * @param h
     * @return
     */
    protected abstract boolean loadH(int pos, double[] h);

    /**
     *
     * @param pos
     * @param z
     * @return
     */
    protected abstract boolean loadZ(int pos, Matrix z);

    /**
     *
     * @param pf0
     */
    @Override
    public void Pf0(final SubMatrix pf0) {
        if (m_Pf0 != null) {
            pf0.copy(m_Pf0.subMatrix());
        }
    }

    /**
     *
     * @param pf0
     */
    @Override
    public void Pi0(final SubMatrix pf0) {
        if (m_B0 != null) {
            pf0.copy(SymmetricMatrix.XXt(m_B0).subMatrix());
        }
    }

    /**
     *
     * @param pos
     * @param qm
     */
    @Override
    public void Q(final int pos, final SubMatrix qm) {
        load(pos);
        qm.copy(m_Q.subMatrix());
    }

    /**
     *
     * @param pos
     * @param H
     */
    @Override
    public void H(final int pos, final SubMatrix H) {
        load(pos);
        H.diagonal().copyFrom(m_H, 0);
    }

    /**
     *
     * @param B0
     */
    public void setB0(final Matrix B0) {
        m_B0 = B0;
    }

    // Initialisation
    /**
     *
     * @param pf0
     */
    public void setPf0(final Matrix pf0) {
        m_Pf0 = pf0;
    }

    /**
     *
     * @param pos
     * @param tr
     */
    @Override
    public void T(final int pos, final SubMatrix tr) {
        load(pos);
        tr.copy(m_T.subMatrix());
    }

    /**
     *
     * @param pos
     * @param vm
     */
    @Override
    public void TVT(final int pos, final SubMatrix vm) {
        load(pos);
        vm.copy(SymmetricMatrix.quadraticFormT(vm, m_T.subMatrix())
                .subMatrix());
    }

    /**
     *
     * @param pos
     * @param x
     */
    @Override
    public void TX(final int pos, final DataBlock x) {
        load(pos);
        m_tmp.product(m_T.rows(), x);
        x.copy(m_tmp);
    }

    /**
     *
     * @param pos
     * @param v
     * @param w
     * @param vm
     * @param d
     */
    @Override
    public void VpZdZ(final int pos, int v, int w, final SubMatrix vm, final double d) {
        load(pos);
        int n = m_Z.getColumnsCount();
        for (int r = 0; r < n; ++r) {
            double zr = m_Z.get(v, r);
            if (zr != 0) {
                zr *= d;
                for (int c = 0; c <= r; ++c) {
                    double zc = m_Z.get(w, c);
                    if (zc != 0) {
                        double z = zr * zc;
                        vm.add(r, c, z);
                        if (r != c) {
                            vm.add(c, r, z);
                        }
                    }

                }
            }
        }
    }

    /**
     *
     * @param pos
     * @param s
     */
    @Override
    public void S(final int pos, final SubMatrix s) {
        load(pos);
        s.copy(m_S.subMatrix());
    }

    /**
     *
     * @param pos
     * @param v
     * @param x
     * @param d
     */
    @Override
    public void XpZd(final int pos, final int v, final DataBlock x, final double d) {
        load(pos);
        x.addAY(d, m_Z.row(v));
    }

    /**
     *
     * @param pos
     * @param x
     */
    @Override
    public void XT(final int pos, final DataBlock x) {
        load(pos);
        m_tmp.product(x, m_T.subMatrix().columns());
        x.copy(m_tmp);
    }

    /**
     *
     * @param pos
     * @param x
     */
    @Override
    public void Z(final int pos, final SubMatrix x) {
        load(pos);
        x.copy(m_Z.subMatrix());
    }

    /**
     *
     * @param pos
     * @param m
     */
    @Override
    public void ZM(final int pos, final SubMatrix m, final SubMatrix zm) {
        load(pos);
        zm.product(m_Z.subMatrix(), m);
    }

    /**
     *
     * @param pos
     * @param v
     * @param m
     * @param zm
     */
    @Override
    public void ZM(final int pos, final int v, final SubMatrix m, final DataBlock zm) {
        load(pos);
        zm.product(m_Z.row(v), m.columns());
    }

    /**
     *
     * @param pos
     * @param vm
     */
    @Override
    public void ZVZ(final int pos, final SubMatrix vm, final SubMatrix zvz) {
        load(pos);
        SymmetricMatrix.quadraticFormT(vm, m_Z.subMatrix(), zvz);
    }

    /**
     *
     * @param pos
     * @param v
     * @param x
     * @return
     */
    @Override
    public double ZX(final int pos, int v, final DataBlock x) {
        load(pos);
        return m_Z.row(v).dot(x);
    }

    @Override
    public void Z(int pos, int v, DataBlock z) {
        load(pos);
        z.copy(m_Z.row(v));
    }

    @Override
    public double ZVZ(int pos, int v, int w, SubMatrix vm) {
        load(pos);
        this.ZM(pos, v, vm, m_tmp);
        return m_tmp.dot(m_Z.row(w));
    }

    @Override
    public boolean hasH() {
        return m_H != null;
    }

    @Override
    public void addH(int pos, SubMatrix v) {
        load(pos);
        v.diagonal().add(new DataBlock(m_H));
    }

    @Override
    public void addV(int pos, SubMatrix p) {
        load(pos);
        p.add(m_V.subMatrix());
    }

    @Override
    public double H(int pos, int v) {
        load(pos);
        return m_H[v];

    }

    @Override
    public void R(int pos, SubMatrix r) {
        load(pos);
        r.diagonal().copyFrom(m_H, 0);
        r.diagonal().sqrt();
    }

    @Override
    public void W(int pos, SubMatrix w) {
        load(pos);
        w.copy(m_W.subMatrix());
    }

    /**
     *
     */
    protected void updateTransition() {
        Matrix q = m_Q.clone();
        SymmetricMatrix.lcholesky(q, 1e-9);
        if (m_S != null) {
            m_V = SymmetricMatrix.quadraticFormT(m_Q, m_S);
            m_W = m_S.times(q);
        } else {
            m_W = q;
            m_V = m_Q;
        }
    }
}
