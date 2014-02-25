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

import ec.nbdemetra.ui.properties.NodePropertySetBuilder;
import ec.tstoolkit.dfm.DfmEstimationSpec;
import ec.tstoolkit.dfm.NumericalProcessingSpec;
import ec.tstoolkit.var.VarSpec;
import java.lang.reflect.InvocationTargetException;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;

/**
 *
 * @author Philippe Charles
 */
final class DfmSheets {

    private DfmSheets() {
        // static class
    }

    public static Sheet onDfmEstimationSpec(DfmEstimationSpec spec) {
        Sheet result = new Sheet();

        NodePropertySetBuilder b = new NodePropertySetBuilder();

        b.reset("Principal components");
        b.withBoolean()
                .select(spec.getPrincipalComponentsSpec(), "enabled")
                .display("Enabled")
                .add();
        b.with(String.class)
                .select("span", "TsPeriodSelector")
                .display("Estimation span")
                .add();
        b.with(String.class)
                .select("ns", "double ]0,1]")
                .display("Data availability (min %)")
                .add();
        result.put(b.build());

        b.reset("Preliminary EM");
        b.withBoolean()
                .select(spec.getPreEmSpec(), "enabled")
                .display("Enabled")
                .add();
        b.withInt()
                .select(spec.getPreEmSpec(), "version")
                .display("Version")
                .min(1).max(2)
                .add();
        b.withInt()
                .select(spec.getPreEmSpec(), "maxIter")
                .display("Max iterations")
                .min(1)
                .add();
        b.withInt()
                .select(spec.getPreEmSpec(), "maxNumIter")
                .display("Max numerical iterations")
                .min(1)
                .add();
        result.put(b.build());

        b.reset("Numerical optimization");
        b.withBoolean()
                .select(spec.getNumericalProcessingSpec(), "enabled")
                .display("Enabled")
                .add();
        b.withInt()
                .select(spec.getNumericalProcessingSpec(), "maxIter")
                .display("Max number iterations")
                .min(1)
                .add();
        b.withInt()
                .select(spec.getNumericalProcessingSpec(), "maxInitialIter")
                .display("Simplified model iterations")
                .min(0)
                .add();
        b.withBoolean()
                .select(spec.getNumericalProcessingSpec(), "blockIterations")
                .display("Iterations by blocks")
                .add();
        b.withBoolean()
                .select(spec.getNumericalProcessingSpec(), "mixedEstimation")
                .display("Mixed estimation")
                .add();
        b.withEnum(NumericalProcessingSpec.Method.class)
                .select(spec.getNumericalProcessingSpec(), "method")
                .display("Optimization method")
                .add();
        b.withDouble()
                .select(spec.getNumericalProcessingSpec(), "precision")
                .display("Precision")
                .min(.1)
                .add();
        result.put(b.build());

        b.reset("Final EM");
        b.withBoolean()
                .select(spec.getPostEmSpec(), "enabled")
                .display("Enabled")
                .add();
        b.withInt()
                .select(spec.getPostEmSpec(), "version")
                .display("Version")
                .min(1).max(2)
                .add();
        b.withInt()
                .select(spec.getPostEmSpec(), "maxIter")
                .display("Max iterations")
                .min(1)
                .add();
        b.withInt()
                .select(spec.getPostEmSpec(), "maxNumIter")
                .display("Max numerical iterations")
                .min(1)
                .add();
        result.put(b.build());

        return result;
    }

    public static Sheet onVarSpec(final VarSpec spec) {
        Sheet result = new Sheet();

        NodePropertySetBuilder b = new NodePropertySetBuilder();
        b.withInt()
                .select(new PropertySupport.ReadWrite<Integer>("nvars", Integer.class, "Equations count", null) {
                    @Override
                    public Integer getValue() throws IllegalAccessException, InvocationTargetException {
                        return spec.getEquationsCount();
                    }

                    @Override
                    public void setValue(Integer val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                        spec.setSize(val, spec.getLagsCount());
                    }
                })
                .min(1)
                .add();
        b.withInt()
                .select(new PropertySupport.ReadWrite<Integer>("nlags", Integer.class, "Lags count", null) {
                    @Override
                    public Integer getValue() throws IllegalAccessException, InvocationTargetException {
                        return spec.getLagsCount();
                    }

                    @Override
                    public void setValue(Integer val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                        spec.setSize(spec.getEquationsCount(), val);
                    }
                })
                .min(1)
                .add();
        result.put(b.build());

        return result;
    }
}
