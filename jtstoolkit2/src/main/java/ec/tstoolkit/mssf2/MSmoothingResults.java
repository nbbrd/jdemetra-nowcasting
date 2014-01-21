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
import ec.tstoolkit.data.TableOfBoolean;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.MatrixStorage;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;

/**
 *
 * @author Jean Palate
 */
public class MSmoothingResults {

    private int m_v, m_n, m_d;
    private DataBlockStorage m_a, m_r;
    private MatrixStorage m_P, m_N;
    private boolean m_bP, m_bA = true, m_bR, m_bN;
    private int m_start;
    private double m_ser = 1;

    /**
     *
     */
    public MSmoothingResults() {
    }

    /**
     *
     * @param idx
     * @return
     */
    public DataBlock A(int idx) {
        return (m_a == null || idx < m_start) ? null : m_a.block(idx - m_start);
    }

    /**
     *
     * @return
     */
    public double getStandardError() {
        return m_ser;
    }

    /**
     *
     * @param value
     */
    public void setStandardError(double value) {
        m_ser = value;
    }

    private int check(DataBlock z) {
        int idx = -1;
        for (int i = 0; i < z.getLength(); ++i) {
            if (z.get(i) != 0) {
                if (idx != -1) {
                    return -1;
                } else {
                    idx = i;
                }
            }
        }
        return idx;
    }

    /**
     *
     */
    public void clear() {
        m_a = null;
        m_P = null;
    }

    /**
     *
     * @param idx
     * @return
     */
    public double[] component(int idx) {
        if (m_a == null) {
            return null;
        }
        double[] c = new double[m_n - m_start];
        m_a.item(idx).copyTo(c, 0);
        return c;
    }

    /**
     *
     * @param i
     * @param j
     * @return
     */
    public double[] componentCovar(int i, int j) {
        if (m_P == null) {
            return null;
        }
        double[] c = new double[m_n - m_start];
        double ser2 = m_ser * m_ser;
        for (int z = 0; z < m_n - m_start; ++z) {
            c[z] = m_P.matrix(z- m_start).get(i, j) * ser2;
        }
        return c;
    }

    /**
     *
     * @param idx
     * @return
     */
    public double[] componentStdev(int idx) {
        if (m_P == null) {
            return null;
        }
        double[] c = new double[m_n - m_start];
        for (int i = 0; i < m_n - m_start; ++i) {
            c[i] = Math.sqrt(m_P.matrix(i- m_start).get(idx, idx)) * m_ser;
        }
        return c;
    }

    /**
     *
     * @param idx
     * @return
     */
    public double[] componentVar(int idx) {
        if (m_P == null) {
            return null;
        }
        double[] c = new double[m_n - m_start];
        double ser2 = m_ser * m_ser;
        for (int i = 0; i < m_n - m_start; ++i) {
            c[i] = m_P.matrix(i- m_start).get(idx, idx) * ser2;
        }
        return c;
    }

    /**
     *
     * @return
     */
    public int getComponentsCount() {
        return m_d;
    }

    /**
     *
     * @return
     */
    public DataBlockStorage getSmoothedStates() {
        return m_a;
    }

    /**
     *
     * @return
     */
    public MatrixStorage getSmoothedStatesVariance() {
        return m_P;
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
     * @return
     */
    public int getSavingStart() {
        return m_start;
    }

    /**
     *
     * @param value
     */
    public void setSaveP(boolean value) {
        m_bP = value;
        clear();
    }

    /**
     *
     * @param start
     */
    public void setSavingStart(int start) {
        m_start = start;
        clear();
    }

    /**
     *
     * @param idx
     * @return
     */
    public SubMatrix P(int idx) {
        return (m_P == null || idx < m_start) ? null : m_P.matrix(idx-m_start);
    }

    /**
     *
     * @param n
     * @param d 
     * @param v  
     */
    public void prepare(int n, int d, int v) {
        int nz=n-m_start;
        m_n = n;
        m_d=d;
        m_v = v;
        clear();
        if (m_bA) {
            m_a = new DataBlockStorage(m_d, nz);
        }
        if (m_bP) {
            m_P = new MatrixStorage(m_d, nz);
        }
    }

    /**
     *
     * @param pos
     * @param a
     * @param p
     */
    public void save(final int pos, DataBlock a, Matrix p) {
        int np=pos-m_start;
        if (np<0)
            return;
        if (m_bA) {
            m_a.save(np, a);
        }
        if (m_bP && p != null) {
            m_P.save(np, p);
        }
    }

    /**
     *
     * @param value
     */
    /**
     *
     * @param z
     * @return
     */
    public double[] zcomponent(DataBlock z) {
        int iz = check(z);
        if (iz >= 0) {
            return component(iz);
        }
        if (m_a == null) {
            return null;
        }
        if (m_d != z.getLength()) {
            return null;
        }

        double[] c = new double[m_n - m_start];
        for (int i = 0; i < m_n - m_start; ++i) {
            c[i] = m_a.block(i).dot(z);
        }
        return c;
    }

    /**
     *
     * @param idx
     * @param z
     * @return
     */
    public double zcomponent(int idx, DataBlock z) {
        return m_a == null || idx < m_start ? Double.NaN : m_a.block(idx - m_start).dot(z);
    }

    /**
     *
     * @param z
     * @return
     */
    public double[] zvariance(DataBlock z) {
        if (m_P == null)
            return null;
        int iz = check(z);
        if (iz >= 0) {
            return componentVar(iz);
        }

        if (m_d != z.getLength()) {
            return null;
        }
        double[] var = new double[m_n - m_start];
        double ser2 = m_ser * m_ser;
        for (int i = 0; i < m_n - m_start; ++i) {
            var[i] = SymmetricMatrix.quadraticForm(m_P.matrix(i), z) * ser2;
        }
        return var;
    }

    /**
     *
     * @param idx
     * @param z
     * @return
     */
    public double zvariance(int idx, DataBlock z) {
        return m_P == null || idx < m_start ? Double.NaN : SymmetricMatrix.quadraticForm(m_P.matrix(idx), z) * m_ser * m_ser;
    }
}
