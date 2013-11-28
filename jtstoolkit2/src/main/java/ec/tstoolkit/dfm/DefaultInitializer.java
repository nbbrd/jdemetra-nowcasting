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

package ec.tstoolkit.dfm;

import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementDescriptor;

/**
 *
 * @author Jean Palate
 */
public class DefaultInitializer implements IDfmInitializer {

    @Override
    public boolean initialize(DynamicFactorModel dfm, DfmInformationSet data) {
        dfm.getTransition().covar.clear();
        dfm.getTransition().covar.diagonal().set(1);
        dfm.getTransition().varParams.clear();
        for (MeasurementDescriptor m : dfm.getMeasurements()){
            for (int i=0; i<m.coeff.length; ++i)
                m.coeff[i]=0;
            m.var=1;
        }
        return true;
    }
    
}
