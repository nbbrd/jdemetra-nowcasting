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

import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.MatrixStorage;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockStorage;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;

/**
 * 
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class SmoothingResults {

    private int m_r, m_n;

    private DataBlockStorage m_a;

    private MatrixStorage m_P;

    private boolean m_bP = false, m_bA = true;

    private double m_ser=1, m_ser2=1;

    /**
     * 
     */
    public SmoothingResults()
    {
    }

    /**
     * 
     * @param hasData
     * @param hasVar
     */
    public SmoothingResults(boolean hasData, boolean hasVar)
    {
	m_bA = hasData;
	m_bP = hasVar;
    }

    /**
     * 
     * @param idx
     * @return
     */
    public DataBlock A(int idx)
    {
	return m_a.block(idx);
    }

    /**
     *
     * @return
     */
    public double getStandardError(){
        return m_ser;
    }
           
    /**
     *
     * @param value
     */
    public void setStandardError(double value){
        m_ser=value;
        m_ser2=value*value;
    }

    private int check(DataBlock z) {
	int idx = -1;
	for (int i = 0; i < z.getLength(); ++i)
	    if (z.get(i) != 0)
		if (idx != -1)
		    return -1;
		else
		    idx = i;
	return idx;
    }

    /**
     * 
     */
    public void clear()
    {
	m_a = null;
	m_P = null;
    }

    /**
     * 
     * @param idx
     * @return
     */
    public double[] component(int idx)
    {
	if (m_a == null)
	    return null;
	double[] c = new double[m_n];
	m_a.item(idx).copyTo(c, 0);
	return c;
    }

    /**
     * 
     * @param i
     * @param j
     * @return
     */
    public double[] componentCovar(int i, int j)
    {
	if (m_P == null)
	    return null;
	double[] c = new double[m_n];
	for (int z = 0; z < m_n; ++z)
	    c[z] = m_P.matrix(z).get(i, j)*m_ser2;
	return c;
    }

    /**
     * 
     * @param idx
     * @return
     */
    public double[] componentStdev(int idx)
    {
	if (m_P == null)
	    return null;
	double[] c = new double[m_n];
	for (int i = 0; i < m_n; ++i)
	    c[i] = Math.sqrt(m_P.matrix(i).get(idx, idx))*m_ser;
	return c;
    }

    /**
     * 
     * @param idx
     * @return
     */
    public double[] componentVar(int idx)
    {
	if (m_P == null)
	    return null;
	double[] c = new double[m_n];
	for (int i = 0; i < m_n; ++i)
	    c[i] = m_P.matrix(i).get(idx, idx)*m_ser2;
	return c;
    }

    /**
     * 
     * @return
     */
    public int getComponentsCount()
    {
	return m_r;
    }

     /**
     * 
     * @return
     */
    public DataBlockStorage getSmoothedStates()
    {
	return m_a;
    }

    /**
     * 
     * @return
     */
    public boolean isSavingA()
    {
	return m_bA;
    }

    /**
     * 
     * @return
     */
    public boolean isSavingP()
    {
	return m_bP;
    }

    /**
     * 
     * @param idx
     * @return
     */
    public SubMatrix P(int idx)
    {
	return m_P.matrix(idx);
    }

    /**
     * 
     * @param n
     * @param r
     */
    public void prepare(int n, int r)
    {
	m_n = n;
	m_r = r;
	clear();
	if (m_bA)
	    m_a = new DataBlockStorage(m_r, m_n);
	if (m_bP)
	    m_P = new MatrixStorage(m_r, m_n);
    }

    /**
     * 
     * @param pos
     * @param a
     * @param p
     */
    public void save(int pos, DataBlock a, Matrix p)
    {
	if (m_bA)
	    m_a.save(pos, a);
	if (m_bP && p != null)
	    m_P.save(pos, p);
    }

    /**
     * 
     * @param value
     */
    public void setSaveA(boolean value)
    {
	m_bA = value;
	clear();
    }

    /**
     * 
     * @param value
     */
    public void setSaveP(boolean value)
    {
	m_bP = value;
	clear();
    }

    /**
     * 
     * @param z
     * @return
     */
    public double[] zcomponent(DataBlock z)
    {
	int iz = check(z);
	if (iz >= 0)
	    return component(iz);
	if (m_a == null)
	    return null;
	if (m_r != z.getLength())
	    return null;

	double[] c = new double[m_n];
	for (int i = 0; i < m_n; ++i)
	    c[i] = m_a.block(i).dot(z);
	return c;
    }

    /**
     * 
     * @param idx
     * @param z
     * @return
     */
    public double zcomponent(int idx, DataBlock z)
    {
	return m_a.block(idx).dot(z);
    }

    /**
     * 
     * @param z
     * @return
     */
    public double[] zvariance(DataBlock z)
    {
	int iz = check(z);
	if (iz >= 0)
	    return componentVar(iz);

	if (m_r != z.getLength())
	    return null;
	double[] var = new double[m_n];
	for (int i = 0; i < m_n; ++i)
	    var[i] = SymmetricMatrix.quadraticForm(m_P.matrix(i), z)*m_ser2;
	return var;
    }

    /**
     * 
     * @param idx
     * @param z
     * @return
     */
    public double zvariance(int idx, DataBlock z)
    {
	return SymmetricMatrix.quadraticForm(m_P.matrix(idx), z)*m_ser2;
    }
}
