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
import ec.tstoolkit.data.DataBlockStorage;
import ec.tstoolkit.design.Development;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class MFilteredData {

    DataBlockStorage m_A;
    DataBlockStorage m_e;
    private int m_n, m_start;

    /**
     *
     */
    public MFilteredData() {
    }

    /*
     * public bool IsMissing(int t) { return Double.IsNaN(m_e[t]); }
     */
    /**
     *
     * @param t
     * @return
     */
    public DataBlock A(final int t) {
        return t < m_start ? null : m_A.block(t - m_start);
    }
    
     /**
     * Method added by David 08-July-2014 (
     * @param idx
     * @return
     */
     public double[] component(int idx) {
        if (m_A == null) {
            return null;
        }
        double[] c = new double[m_n - m_start];
        m_A.item(idx).copyTo(c, 0);
        return c;
    }
     
    
    /**
     *
     * @param n
     */
    public void checkSize(int n) {
        m_e.resize(n);
        m_A.resize(n);
    }

    /**
     *
     */
    public void clear() {
        m_e = null;
        m_A = null;
        m_n = 0;
    }

    /**
     *
     */
    public void close() {
    }

    /**
     *
     * @param t
     * @return
     */
    public DataBlock E(final int t) {
        return t < m_start ? null : m_e.block(t - m_start);
    }

    /**
     *
     * @return
     */
    public int getCount() {
        return m_n;
    }

    /**
     *
     * @param dim
     * @param nvars 
     * @param n
     */
    public void init(final int dim, final int nvars, final int n) {
        clear();
        m_e = new DataBlockStorage(nvars, n - m_start);
        m_A = new DataBlockStorage(dim, n - m_start);
    }

    /**
     *
     * @param t
     * @param state
     */
    public void save(final int t, final BaseOrdinaryMState state) {
        int st = t - m_start;
        if (st < 0) {
            return;
        }
        m_e.save(st, state.E);
        m_A.save(st, state.A);
        if (t >= m_n) {
            ++m_n;
        }
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
        clear();
    }
}
