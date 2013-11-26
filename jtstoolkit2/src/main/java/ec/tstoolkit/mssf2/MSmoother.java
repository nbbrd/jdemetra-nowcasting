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
import ec.tstoolkit.eco.DiffuseLikelihood;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.LowerTriangularMatrix;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;

/**
 *
 * @author Jean Palate
 */
public class MSmoother {

    private boolean m_bvar = true;
    private DataBlock m_a;
    private SubMatrix m_P;
    private Matrix m_V;
    private IMSsf m_ssf;
    private IMSsfData m_data;
    private MFilteringResults m_frslts;
    private MSmoothingResults m_srslts;
    private int m_pos, m_n, m_v, m_stop;
    private DataBlock m_r, m_E;
    private SubMatrix m_K;
    private Matrix m_F;
    private Matrix m_N, m_L, m_T, m_Z, m_Zl;
    // 
    private DataBlock m_tmp, m_vtmp;
    private boolean[] m_missing;

    /**
     *
     */
    public MSmoother() {
    }

    /**
     *
     */
    private void clear() {
        m_a = null;
        m_V = null;
        m_P = null;
        m_r = null;
        m_F = null;
        m_ssf = null;
        m_T = null;
        m_L = null;
        m_N = null;
        m_Z = null;
        m_frslts = null;
        m_srslts = null;
        m_pos = 0;
        m_tmp = null;
        m_vtmp = null;
    }

    /**
     *
     * @return
     */
    public boolean isCalcVariance() {
        return m_bvar;
    }

    /**
     *
     * @param calc
     */
    public void setCalcVariance(boolean calc) {
        m_bvar = calc;
    }

    /**
     *
     * @param stop
     */
    public void setStopPosition(int stop) {
        m_stop = stop;
    }

    /**
     *
     * @return
     */
    public int getStopPosition() {
        return m_stop;
    }

    /**
     *
     */
    private void initSmoother() {
        m_pos = m_data.getCount() - 1;
        m_n = m_ssf.getStateDim();
        m_v = m_ssf.getVarsCount();
        m_r = new DataBlock(m_n);
        m_tmp = new DataBlock(m_n);
        m_vtmp = new DataBlock(m_v);
        m_missing=new boolean[m_v];
        m_F = new Matrix(m_v, m_v);
        m_E = new DataBlock(m_v);
        m_a = new DataBlock(m_n);
        m_Z = new Matrix(m_v, m_n);
        m_Zl = new Matrix(m_v, m_n);
        if (m_bvar) {
            m_V = new Matrix(m_n, m_n);
            m_N = new Matrix(m_n, m_n);
            m_L = new Matrix(m_n, m_n);
            m_T = new Matrix(m_n, m_n);
        }
        m_srslts.prepare(m_data.getCount(), m_n, m_v);
    }

    /**
     *
     */
    protected void iterateSmoother() {
        if (m_pos >= m_stop) {
            iterateR();
            m_tmp.product(m_r, m_P.columns());
            m_a.add(m_tmp);
            if (m_bvar) {
                iterateN();
                SymmetricMatrix.quadraticForm(m_N.subMatrix(), m_P, m_V.subMatrix());
                m_V.chs();
                m_V.subMatrix().add(m_P);
            }
        }
        // a = a + r*P
    }

    private void iterateR() {
        // R(t-1)=(v(t)/f(t)-R(t)*K(t))*Z(t)+R(t)*T(t)
        // R(t-1)=v(t)/f(t)*Z(t) + R(t)*L(t)
//        if (!m_bMissing && m_ff != 0) {
//            Utilities.XL(m_ssf, m_pos, m_Rf, m_Kf);
//            m_ssf.XpZd(m_pos, m_Rf, m_v / m_ff);
//        } else {
//            m_c = 0;
//            m_ssf.XT(m_pos, m_Rf);
//        }
        xL(m_r);
        LowerTriangularMatrix.lsolve(m_F, m_E, MFilter.Zero);
        for (int i = 0; i < m_v; ++i) {
            if (!m_data.isMissing(i, m_pos)) {
                m_ssf.XpZd(m_pos, i, m_r, m_E.get(i));
            }
        }
    }

    private void iterateN() {
//            // N(t-1) = Z'(t)*F^-1*Z(t) + L'(t)*N(t)*L(t)
        LtXL(m_N.subMatrix());
        DataBlockIterator zrows = m_Z.rows(), zlrows = m_Zl.rows();
        DataBlock zrow = zrows.getData(), zlrow = zlrows.getData();
        do {
            if (!m_data.isMissing(zrows.getPosition(), m_pos)) {
                zlrow.copy(zrow);
            } else {
                zlrow.set(0);
            }

        } while (zrows.next() && zlrows.next());
        // R^-1*Z = Zl <-> R*Zl = Z
        LowerTriangularMatrix.rsolve(m_F, m_Zl.subMatrix(), MFilter.Zero);
        for (int i = 0; i < m_v; ++i) {
            if (!m_missing[i]) {
                m_N.addXaXt(1, m_Zl.row(i));
            }
        }
    }

    private void loadInfo() {
        m_F.subMatrix().copy(m_frslts.getVarianceFilter().F(m_pos));
        m_E.copy(m_frslts.getFilteredData().E(m_pos));
        m_K = m_frslts.getVarianceFilter().K(m_pos);
        m_P = m_frslts.getVarianceFilter().P(m_pos);
        if (m_a.getLength() != 0) {
            m_a.copy(m_frslts.getFilteredData().A(m_pos));
        }
        for (int i = 0; i < m_v; ++i) {
            m_missing[i]=m_data.isMissing(i, m_pos);
        }
    }

    private void loadModelInfo() {
        m_ssf.Z(m_pos, m_Z.subMatrix());
    }

    /**
     *
     * @param ssf
     * @param data
     * @param frslts
     * @param rslts
     * @return
     */
    public boolean process(final IMSsf ssf, final IMSsfData data,
            final MFilteringResults frslts, final MSmoothingResults rslts) {
        clear();
        m_ssf = ssf;
        m_data = data;
        m_frslts = frslts;
        m_srslts = rslts;
        m_srslts.setSaveP(m_bvar);
        initSmoother();
        if (m_ssf.isTimeInvariant()) {
            loadModelInfo();
        }
        while (m_pos >= m_stop) {
            if (!m_ssf.isTimeInvariant()) {
                loadModelInfo();
            }
            loadInfo();
            iterateSmoother();
            m_srslts.save(m_pos, m_a, m_V);
            --m_pos;
        }
        if (m_bvar) {
            Likelihood ll = new Likelihood();
            frslts.evaluate(ll);
            double ser = ll.getSer();
            m_srslts.setStandardError(ser);
        }

        return true;
    }

    /**
     *
     * @param ssf
     * @param data
     * @param rslts
     * @return
     */
    public boolean process(final IMSsf ssf, final IMSsfData data, final MSmoothingResults rslts) {
        m_ssf = ssf;
        m_data = data;
        MFilteringResults frslts = new MFilteringResults();
        frslts.getVarianceFilter().setSavingK(true);
        frslts.getVarianceFilter().setSavingP(true);
        MFilter filter = new MFilter();
        if (!filter.process(ssf, data, frslts)) {
            return false;
        }
        return process(ssf, data, frslts, rslts);
    }

    /**
     * Compute x*(T-KZ)=xT-(xK * Z)=xT-(xQ*R^-1 * Z)
     *
     * @param x
     */
    private void xL(DataBlock x) {
        // xQ
        m_vtmp.set(0);
        for (int i = 0; i < m_v; ++i) {
            if (!m_missing[i]) {
                m_vtmp.set(i, x.dot(m_K.column(i)));
            }
        }
        // xQ*R^-1=Y <=> xQ=YR
        LowerTriangularMatrix.lsolve(m_F, m_vtmp, MFilter.Zero);
        m_ssf.XT(m_pos, x);
        for (int i = 0; i < m_v; ++i) {
            if (!m_missing[i]) {
                m_ssf.XpZd(m_pos, i, x, -m_vtmp.get(i));
            }
        }
    }

    private void XL(DataBlockIterator X) {
        DataBlock x = X.getData();
        do {
            xL(x);
        } while (X.next());
    }

    private void LtXL(SubMatrix X) {
        XL(X.columns());
        XL(X.rows());
    }

    /**
     *
     * @return
     */
    public MFilteringResults getFilteringResults() {
        return m_frslts; //To change body of generated methods, choose Tools | Templates.
    }
}
