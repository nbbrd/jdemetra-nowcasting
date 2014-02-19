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

    public static DfmModelSpec getDemo() {
        DfmModelSpec result = newDfmModelSpec(4, 4);
        for (int i = 0; i < 10; i++) {
            result.getMeasurements().add(newMeasurementSpec("S" + i, 4));
        }
        return result;
    }

    public static DfmModelSpec newDfmModelSpec(int nvars, int nlags) {
        DfmModelSpec m = new DfmModelSpec();
        VarSpec vs = new VarSpec();
        vs.setSize(nvars, nlags);
        m.setVarSpec(vs);
        return m;
    }

    public static MeasurementSpec newMeasurementSpec(String name, int nvars) {
        MeasurementSpec ms = new MeasurementSpec();
        ms.setName(name);
        ms.setSeriesTransformations(null);
        ms.setFactorsTransformation(DynamicFactorModel.MeasurementType.L);
        Parameter[] params = new Parameter[nvars];
        for (int z = 0; z < params.length; z++) {
            params[z] = newParameter(false);
        }
        ms.setCoefficient(params);
        ms.setVariance(newParameter(false));
        return ms;
    }

    public static Parameter newParameter(boolean selected) {
        return new Parameter(0, selected ? ParameterType.Undefined : ParameterType.Fixed);
    }
}
