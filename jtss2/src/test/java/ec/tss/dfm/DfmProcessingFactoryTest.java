/*
 * Copyright 2014 National Bank of Belgium
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
package ec.tss.dfm;

import ec.tss.sa.SaManager;
import ec.tss.sa.processors.TramoSeatsProcessor;
import ec.tss.sa.processors.X13Processor;
import ec.tstoolkit.Parameter;
import ec.tstoolkit.ParameterType;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.algorithm.IProcessing;
import ec.tstoolkit.algorithm.IProcessingHook;
import ec.tstoolkit.algorithm.IProcessingNode;
import ec.tstoolkit.algorithm.ProcessingContext;
import ec.tstoolkit.dfm.DfmEstimationSpec;
import ec.tstoolkit.dfm.DfmModelSpec;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.dfm.MeasurementSpec;
import ec.tstoolkit.dfm.MeasurementSpec.Transformation;
import ec.tstoolkit.dfm.NumericalProcessingSpec;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.regression.TsVariable;
import ec.tstoolkit.timeseries.regression.TsVariables;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.var.VarSpec;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author palatej
 */
public class DfmProcessingFactoryTest {

    static final List<TsData>vars;
    static final DfmSpec spec;

    static {
        SaManager.instance.add(new TramoSeatsProcessor());
        SaManager.instance.add(new X13Processor());
        vars = new ArrayList<>();
        spec = new DfmSpec();
        DfmEstimationSpec espec = new DfmEstimationSpec();
        spec.setEstimationSpec(espec);
        espec.getPrincipalComponentsSpec().setEnabled(false);
        espec.getPreEmSpec().setEnabled(true);
        espec.getPreEmSpec().setMaxIter(20);
//        espec.getPostEmSpec().setEnabled(true);
//        espec.getPostEmSpec().setMaxIter(10);
        espec.getNumericalProcessingSpec().setMaxInitialIter(0);
//       espec.getNumericalProcessingSpec().setMethod(NumericalProcessingSpec.Method.Lbfgs);
//        espec.getNumericalProcessingSpec().setEnabled(false);
        espec.getNumericalProcessingSpec().setMaxIter(500);
        vars.add(data.Data.M1);
        vars.add(data.Data.M2);
        vars.add(data.Data.M3.changeFrequency(TsFrequency.Quarterly, TsAggregationType.Sum, true));
        DfmModelSpec mspec = new DfmModelSpec();
        VarSpec vspec = new VarSpec();
        vspec.setSize(1, 1);
        MeasurementSpec mvspec = new MeasurementSpec();
        mvspec.setSeriesTransformations(new Transformation[]{Transformation.Sa, Transformation.Diff1});
        mvspec.setCoefficients(new Parameter[]{new Parameter()});
        mvspec.setFactorsTransformation(DynamicFactorModel.MeasurementType.M);
        mspec.getMeasurements().add(mvspec);

        mvspec = new MeasurementSpec();
        mvspec.setSeriesTransformations(new Transformation[]{Transformation.Sa, Transformation.Diff1});
        mvspec.setCoefficients(new Parameter[]{new Parameter()});
        mvspec.setFactorsTransformation(DynamicFactorModel.MeasurementType.M);
        mspec.getMeasurements().add(mvspec);

        mvspec = new MeasurementSpec();
         mvspec.setSeriesTransformations(new Transformation[]{Transformation.Sa, Transformation.Diff1});
        mvspec.setCoefficients(new Parameter[]{new Parameter()});
        mvspec.setFactorsTransformation(DynamicFactorModel.MeasurementType.Q);
        mspec.getMeasurements().add(mvspec);

        //vspec.setInitialization(VarSpec.Initialization.Zero);
        mspec.setVarSpec(vspec);
        spec.setModelSpec(mspec);

    }

    public DfmProcessingFactoryTest() {
    }

    //@Test
    public void testInitialStep() {

        ProcessingContext context = new ProcessingContext();
        IProcessing<TsData[], CompositeResults> proc = DfmProcessingFactory.instance.generateProcessing(spec, context);
        CompositeResults rslts = proc.process(vars.toArray(new TsData[vars.size()]));
        TsData v1 = rslts.getData("inputc.var1", TsData.class);
        TsData v2 = rslts.getData("inputc.var2", TsData.class);
        TsData v3 = rslts.getData("inputc.var3", TsData.class);
        assertTrue(v1 != null && v2 != null && v3 != null);
    }

    @Test
    public void testPcStep() {
        IProcessingHook<IProcessingNode, DfmProcessingFactory.EstimationInfo> hook = new IProcessingHook<IProcessingNode, DfmProcessingFactory.EstimationInfo>() {

            @Override
            public void process(IProcessingHook.HookInformation<IProcessingNode, DfmProcessingFactory.EstimationInfo> info, boolean cancancel) {
                System.out.print(info.source.getName() + '\t');
                System.out.print(info.message + '\t');
                System.out.println(info.information.loglikelihood);
               
            }
        };
        DfmProcessingFactory.instance.register(hook);

        IProcessing<TsData[], CompositeResults> proc = DfmProcessingFactory.instance.generateProcessing(spec, null);
        CompositeResults rslts = proc.process(vars.toArray(new TsData[vars.size()]));
        DfmResults dfm = rslts.get(DfmProcessingFactory.DFM, DfmResults.class);
        System.out.println(dfm.getModel());
        DfmProcessingFactory.instance.unregister(hook);
    }

}
