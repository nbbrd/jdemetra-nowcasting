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

import ec.tstoolkit.design.Development;
import ec.tstoolkit.eco.DefaultLikelihoodEvaluation;
import ec.tstoolkit.eco.Likelihood;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public abstract class AbstractMSsfAlgorithm {

   public static void evaluate(final MPredictionErrorDecomposition rslts,
            final Likelihood ll) {
        int n = rslts.getObsCount();
        double ssqerr = rslts.getSsqErr(), ldet = rslts.getLogDeterminant();
        ll.set(ssqerr, ldet, n);
        ll.setRes(rslts.allResiduals());
    }
    
   private boolean m_ssq = false, m_ml = true;

    /**
     * 
     */
    public AbstractMSsfAlgorithm()
    {
    }

    /**
     *
     * @param dped
     * @return
     */
    protected DefaultLikelihoodEvaluation<Likelihood> calcLikelihood(
	    final MPredictionErrorDecomposition dped) {
	Likelihood cll = new Likelihood();
	evaluate(dped, cll);
	DefaultLikelihoodEvaluation<Likelihood> ll = new DefaultLikelihoodEvaluation<>(
		cll);
	ll.useLogLikelihood(!m_ssq);
	ll.useML(m_ml);
	return ll;
    }

     /**
     * 
     * @return
     */
    public boolean isUsingML()
    {
	return m_ml;
    }

    /**
     * 
     * @return
     */
    public boolean isUsingSsq()
    {
	return m_ssq;
    }

    /**
     * 
     * @param value
     */
    public void useML(final boolean value)
    {
	m_ml = value;
    }

    /**
     * 
     * @param value
     */
    public void useSsq(final boolean value)
    {
	m_ssq = value;
    }
}
