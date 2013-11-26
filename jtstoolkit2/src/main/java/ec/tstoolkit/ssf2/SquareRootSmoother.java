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
import ec.tstoolkit.eco.DiffuseLikelihood;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;

/**
 *
 * @author Jean Palate
 */
public class SquareRootSmoother {

    private DiffuseFilteringResults m_frslts;
    private DataBlock m_a;
    private SubMatrix m_P;
    private Matrix m_V;
    private DataBlock m_tmp;
    private SmoothingResults m_srslts;
    private ISsf m_ssf;
    private ISsfData m_data;
    private int m_pos, m_r;
    private DataBlock m_R, m_K;
    double m_v, m_f;
    Matrix m_N, m_Q;
    boolean m_bCalcVar = false, m_bMissing;
    double m_c, m_cvar;

    /**
     *
     */
    protected void clear() {
        m_data = null;
        m_R = null;
        m_K = null;
        m_N = null;
    }

    /**
     *
     * @return
     */
    public ISsf getSsf() {
        return m_ssf;
    }

    /**
     *
     */
    protected void initSmoother() {
        m_pos = m_data.getCount() - 1;
        m_r = m_ssf.getStateDim();
        if (m_data.hasData()) {
            m_R = new DataBlock(m_r);
        }
        m_K = new DataBlock(m_r);
        if (m_bCalcVar) {
            m_N = new Matrix(m_r, m_r);
            m_V = new Matrix(m_r, m_r);
            m_Q = new Matrix(m_r, m_r+1);
       }
        m_srslts.prepare(m_data.getCount(), m_r);
        if (m_data.hasData()) {
            m_a = new DataBlock(m_r);
            m_tmp = new DataBlock(m_r);
        }
    }

    /**
     *
     * @return
     */
    public boolean isCalcVar() {
        return m_bCalcVar;
    }

    /**
     * N = UU' where U is a lower triangular matrix
     * L'U = V <-> U'L = V'; so we have to apply ML on the transpose of U
     */
    protected void iterateN() {
        if (!m_bMissing && m_f != 0) {
            // N(t-1) = Z'(t)*Z(t)/f(t) + L'(t)*N(t)*L(t)
            Utilities.LtVL(m_ssf, m_pos, m_N.subMatrix(), m_K);
            m_ssf.VpZdZ(m_pos, m_N.subMatrix(), 1 / m_f);
        } else {
            // m_Nf = SymmetricMatrix.QuadraticForm(m_Nf, m_T);
            Utilities.TtVT(m_ssf, m_pos, m_N.subMatrix());
        }
        SymmetricMatrix.reinforeSymmetry(m_N);
    }

    /**
     *
     */
    protected void iterateR() {
        // R(t-1)=(v(t)/f(t)-R(t)*K(t))*Z(t)+R(t)*T(t)
        // R(t-1)=v(t)/f(t)*Z(t) + R(t)*L(t)
        if (!m_bMissing && m_f != 0) {
            Utilities.XL(m_ssf, m_pos, m_R, m_K);
            m_ssf.XpZd(m_pos, m_R, m_v / m_f);
//            m_c = m_v / m_ff - m_Rf.dot(m_Kf);
//            m_ssf.XT(m_pos, m_Rf);
//            m_ssf.XpZd(m_pos, m_Rf, m_c);
        } else {
            m_c = 0;
            m_ssf.XT(m_pos, m_R);
        }
    }

    /**
     *
     * @return
     */
    protected FilteredData getFilteredData() {
        return m_frslts.getFilteredData();
    }

    /**
     *
     * @return
     */
    public DiffuseFilteringResults getFilteringResults() {
        return m_frslts;
    }

    /**
     *
     * @return
     */
    protected VarianceFilter getVarianceFilter() {
        return m_frslts.getVarianceFilter();
    }

    /**
     *
     */
    protected void loadInfo() {
        m_bMissing = getVarianceFilter().isMissing(m_pos);
        m_f = getVarianceFilter().F(m_pos);
        if (!m_bMissing && m_f != 0) {
            m_v = getFilteredData().E(m_pos);
            m_K.copy(getVarianceFilter().C(m_pos));
            m_K.mul(1 / m_f);
        } else {
            m_v = 0;
        }
        m_P = m_frslts.getVarianceFilter().P(m_pos);
        if (m_a.getLength() != 0) {
            m_a.copy(m_frslts.getFilteredData().A(m_pos));
        }
    }

    /**
     *
     * @param value
     */
    public void setCalcVar(boolean value) {
        m_bCalcVar = value;
    }

    /**
     *
     * @param value
     */
    public void setSsf(ISsf value) {
        m_ssf = value;
        clear();
    }

    /**
     *
     */
    protected void iterateSmoother() {
        if (m_a.getLength() > 0) {
            iterateR();
            m_tmp.product(m_R, m_P.columns());
            m_a.add(m_tmp);
        }
        if (m_bCalcVar) {
            iterateN();
            SymmetricMatrix.quadraticForm(m_N.subMatrix(), m_P, m_V
                    .subMatrix());
            m_V.chs();
            m_V.subMatrix().add(m_P);
        }
        // a = a + r*P
    }

    /**
     *
     * @param data
     * @param frslts
     * @param rslts
     * @return
     */
    public boolean process(final ISsfData data,
            final DiffuseFilteringResults frslts, final SmoothingResults rslts) {
        clear();
        if (m_ssf == null) {
            return false;
        }
        m_data = data;
        m_frslts = frslts;
        m_srslts = rslts;
        m_srslts.setSaveP(m_bCalcVar);
        initSmoother();
        while (m_pos >= 0) {
            loadInfo();
            iterateSmoother();
            m_srslts.save(m_pos, m_a, m_V);
            --m_pos;
        }
        if (m_bCalcVar) {
            DiffuseLikelihood ll = new DiffuseLikelihood();
            LikelihoodEvaluation.evaluate(frslts, ll);
            double ser = ll.getSer();
            m_srslts.setStandardError(ser);
        }

        return true;
    }

    /**
     *
     * @param data
     * @param rslts
     * @return
     */
    public boolean process(final ISsfData data, final SmoothingResults rslts) {
        if (m_ssf == null) {
            return false;
        }
        DiffuseFilteringResults frslts = new DiffuseFilteringResults(true);
        frslts.getVarianceFilter().setSavingP(true);
        frslts.getFilteredData().setSavingA(data.hasData());
        Filter<ISsf> filter = new Filter<>();
        filter.setSsf(m_ssf);
        if (!filter.process(data, frslts)) {
            return false;
        }
        return process(data, frslts, rslts);
    }
}
