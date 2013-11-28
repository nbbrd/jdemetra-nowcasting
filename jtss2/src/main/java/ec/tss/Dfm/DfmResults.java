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
package ec.tss.Dfm;

import ec.tstoolkit.algorithm.IProcResults;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.mssf2.MFilteringResults;
import ec.tstoolkit.mssf2.MSmoothingResults;
import java.util.Map;

/**
 *
 * @author Jean Palate
 */
public class DfmResults implements IProcResults{
    
    private DynamicFactorModel model;
    // optimization (if any)
    Likelihood likelihood;
    Matrix hessian;
    double[] gradient;
    // smoothing/filtering
    MSmoothingResults smoothing;
    MFilteringResults filtering;

    @Override
    public boolean contains(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, Class> getDictionary() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T getData(String id, Class<T> tclass) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
