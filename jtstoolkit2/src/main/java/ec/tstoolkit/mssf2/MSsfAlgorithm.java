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
public class MSsfAlgorithm extends AbstractMSsfAlgorithm
	implements IMSsfAlgorithm {

    /**
     * 
     */
    public MSsfAlgorithm()
    {
    }

     /**
     *
     * @param ssf
     * @param data
     * @return
     */
    @Override
    public DefaultLikelihoodEvaluation<Likelihood> evaluate(IMSsf ssf, IMSsfData data) {
	MFilter filter = new MFilter();
	MPredictionErrorDecomposition pred = new MPredictionErrorDecomposition(
		true);
	if (filter.process(ssf, data, pred))
	    return calcLikelihood(pred);
	else
	    return null;
    }
}
