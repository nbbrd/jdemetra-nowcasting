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

import ec.tstoolkit.design.Development;

/**
 * 
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class PredictionErrorDecomposition extends ResidualsCumulator implements
	IFilteringResults, IFastFilteringResults {

    private double[] m_res;

    private boolean m_bres;

    /**
     * 
     * @param bres
     */
    public PredictionErrorDecomposition(final boolean bres)
    {
	m_bres = bres;
    }

    private void checkSize(final int pos) {
	if (m_res != null && pos < m_res.length)
	    return;
	double[] tmp = new double[ec.tstoolkit.data.DataBlockStorage
		.calcSize(pos + 1)];
	if (m_res != null)
	    System.arraycopy(m_res, 0, tmp, 0, m_res.length);
	m_res = tmp;
    }

    /**
     * 
     */
    public void close()
    {
    }

    /**
     * 
     * @return
     */
    public boolean hasResiduals()
    {
	return m_bres;
    }

    /**
     * 
     * @param ssf
     * @param data
     */
    public void prepare(final ISsf ssf, final ISsfData data)
    {
	clear();
	if (m_bres) {
	    m_res = new double[data.getCount()];
	    for (int i = 0; i < m_res.length; ++i)
		m_res[i] = Double.NaN;
	} else
	    m_res = null;
    }

    /**
     * 
     * @param bClean
     * @return
     */
    public double[] residuals(boolean bClean)
    {
	if (m_res == null)
	    return null;
	if (!bClean)
	    return m_res;
	else {
	    int n = 0;
	    for (int i = 0; i < m_res.length; ++i)
		if (!Double.isNaN(m_res[i]))
		    ++n;
	    if (n == m_res.length)
		return m_res;
	    else {
		double[] res = new double[n];
		for (int i = 0, j = 0; i < m_res.length; ++i)
		    if (!Double.isNaN(m_res[i]))
			res[j++] = m_res[i];
		return res;
	    }
	}
    }

     /**
     * 
     * @param t
     * @param state
     */
    @Override
    public void save(final int t, final FastState state)
    {
	add(state.e, state.f);
	if (m_bres) {
	    checkSize(t);
	    m_res[t] = state.e / Math.sqrt(state.f);
	}
    }

    /**
     * 
     * @param t
     * @param state
     */
    @Override
    public void save(final int t, final State state)
    {
	if (!state.isMissing()) {
	    add(state.e, state.f);
	    if (m_bres) {
		checkSize(t);
		m_res[t] = state.e / Math.sqrt(state.f);
	    }
	}
    }

    /**
     * 
     * @param value
     */
    public void setResiduals(final boolean value)
    {
	m_bres = value;
    }
}
