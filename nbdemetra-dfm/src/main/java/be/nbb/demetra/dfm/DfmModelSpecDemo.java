/*
 * Copyright 2013 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
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
package be.nbb.demetra.dfm;

import ec.tstoolkit.Parameter;
import ec.tstoolkit.ParameterType;
import ec.tstoolkit.dfm.DfmModelSpec;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.dfm.MeasurementSpec;
import ec.tstoolkit.var.VarSpec;

/**
 *
 * @author Philippe Charles
 */
public class DfmModelSpecDemo {

    public static DfmModelSpec get() {
        DfmModelSpec m = new DfmModelSpec();
        VarSpec vs = new VarSpec();
        vs.setSize(4, 4);
        m.setVarSpec(vs);
        for (int i = 0; i < 10; i++) {
            MeasurementSpec ms = new MeasurementSpec();
            ms.setName("S" + i);
            Parameter[] params = new Parameter[4];
            for (int z = 0; z < params.length; z++) {
                params[z] = new Parameter();
            }
            ms.setCoefficient(params);
            ms.setVariance(new Parameter(0, ParameterType.Fixed));
            ms.setFactorsTransformation(DynamicFactorModel.MeasurementType.L);
            m.getMeasurements().add(ms);
        }
        return m;
    }
}
