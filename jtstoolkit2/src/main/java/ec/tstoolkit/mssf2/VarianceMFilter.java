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

import ec.tstoolkit.maths.matrices.MatrixStorage;
import ec.tstoolkit.data.DataBlockStorage;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.SubMatrix;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class VarianceMFilter implements IMFilteringResults {

    IMSsf m_ssf;
    MatrixStorage m_P;
    MatrixStorage m_K;
    MatrixStorage m_F;
    int m_n, m_dim;
    int m_start;
    boolean m_open, m_bP, m_bK;

    /**
     *
     */
    public VarianceMFilter() {
    }

    /**
     *
     * @param t
     * @return
     */
    public SubMatrix K(final int t) {
        return (m_K == null || t < m_start) ? null : m_K.matrix(t - m_start);
    }

    /**
     *
     * @param n
     */
    protected void checkSize(final int n) {
        if (m_n >= n) {
            return;
        }
        m_n = n;
    }

    /**
     *
     */
    public void clear() {
        m_n = 0;
        m_dim = 0;
        m_P = null;
        m_K = null;
        m_F = null;
    }

    /**
     *
     */
    @Override
    public void close() {
        m_open = false;
    }

    /**
     *
     * @param t
     * @return
     */
    public SubMatrix F(final int t) {
        return t < m_start ? null : m_F.matrix(t - m_start);
    }

    /**
     *
     * @return
     */
    public int getSize() {
        return m_n;
    }

    /**
     *
     * @return
     */
    public int getStateDim() {
        return m_dim;
    }

    /**
     *
     * @param ssf
     * @param n
     */
    protected void init(final IMSsf ssf, final int n) {
        clear();
        m_open = true;
        m_ssf = ssf;

        m_dim = ssf.getStateDim();
        int nvars = ssf.getVarsCount();
        m_n = n;
        m_F = new MatrixStorage(nvars, n - m_start);

        if (m_bK) {
            m_K = new MatrixStorage(m_dim, nvars, n - m_start);
        }
        if (m_bP) {
            m_P = new MatrixStorage(m_dim, n - m_start);
        }
    }

    /**
     *
     * @return
     */
    public boolean isOpen() {
        return m_open;
    }

    /**
     *
     * @return
     */
    public boolean isSavingK() {
        return m_bK;
    }

    /**
     *
     * @return
     */
    public boolean isSavingP() {
        return m_bP;
    }

    /**
     *
     * @param t
     * @return
     */
    public SubMatrix P(final int t) {
        if (m_P == null || t < m_start) {
            return null;
        } else {
            return m_P.matrix(t - m_start);
        }
    }

    /**
     *
     * @param ssf
     * @param data
     */
    @Override
    public void prepare(final IMSsf ssf, final IMSsfData data) {
        if (!m_open) {
            init(ssf, data.count(0));
        } else {
            checkSize(data.count(0));
        }
    }

    /**
     *
     * @param t
     * @param state
     */
    @Override
    public void save(final int t, final MState state) {
        int st = t - m_start;
        if (st < 0) {
            return;
        }
        m_F.save(st, state.F);
        if (m_K != null) {
            m_K.save(st, state.K);
        }
        if (m_P != null) {
            m_P.save(st, state.P);
        }
    }

    /**
     *
     * @param value
     */
    public void setSavingK(final boolean value) {
        m_bK = value;
        m_K = null;
    }

    /**
     *
     * @param value
     */
    public void setSavingP(final boolean value) {
        m_bP = value;
        m_P = null;
    }

    /**
     *
     * @return
     */
    public int getStartSaving() {
        return m_start;
    }

    /**
     *
     * @param p
     */
    public void setStartSaving(int p) {
        m_start = p;
        m_K = null;
        m_P = null;
    }
}
