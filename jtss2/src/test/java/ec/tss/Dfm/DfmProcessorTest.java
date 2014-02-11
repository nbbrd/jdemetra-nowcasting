/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tss.Dfm;

import ec.tss.sa.SaManager;
import ec.tss.sa.processors.TramoSeatsProcessor;
import ec.tss.sa.processors.X13Processor;
import ec.tstoolkit.Parameter;
import ec.tstoolkit.ParameterType;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.algorithm.IProcessing;
import ec.tstoolkit.algorithm.ProcessingContext;
import ec.tstoolkit.dfm.DfmEstimationSpec;
import ec.tstoolkit.dfm.DfmModelSpec;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.dfm.MeasurementSpec;
import ec.tstoolkit.dfm.MeasurementSpec.Transformation;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.regression.TsVariable;
import ec.tstoolkit.timeseries.regression.TsVariables;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.var.VarSpec;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author palatej
 */
public class DfmProcessorTest {

    static final TsVariables vars;
    static final DfmSpec spec;

    static {
        SaManager.instance.add(new TramoSeatsProcessor());
        SaManager.instance.add(new X13Processor());
        vars = new TsVariables();
        spec = new DfmSpec();
        DfmEstimationSpec espec = new DfmEstimationSpec();
        spec.setEstimationSpec(espec);
//        espec.getPrincipalComponentsSpec().setEnabled(false);
        espec.getPreEmSpec().setEnabled(true);
        espec.getPreEmSpec().setMaxIter(100);
//        espec.getPostEmSpec().setEnabled(true);
//        espec.getPostEmSpec().setMaxIter(10);
        espec.getNumericalProcessingSpec().setMaxInitialIter(0);
//        espec.getNumericalProcessingSpec().setEnabled(false);
        espec.getNumericalProcessingSpec().setMaxIter(1000);
        vars.set("m1", new TsVariable(data.Data.M1));
        vars.set("m2", new TsVariable(data.Data.M2));
        vars.set("m3", new TsVariable(data.Data.M3.changeFrequency(TsFrequency.Quarterly, TsAggregationType.Sum, true)));
        DfmModelSpec mspec = new DfmModelSpec();
        VarSpec vspec = new VarSpec();
        vspec.setSize(1, 11);
        MeasurementSpec mvspec = new MeasurementSpec();
        mvspec.setName("m1");
        mvspec.setSeriesTransformations(new Transformation[]{Transformation.Sa, Transformation.Diff1});
        mvspec.setCoefficient(new Parameter[]{new Parameter()});
        mvspec.setFactorsTransformation(DynamicFactorModel.MeasurementType.C);
        mspec.getMeasurements().add(mvspec);

        mvspec = new MeasurementSpec();
        mvspec.setName("m2");
        mvspec.setSeriesTransformations(new Transformation[]{Transformation.Sa, Transformation.Diff1});
        mvspec.setCoefficient(new Parameter[]{new Parameter()});
        mvspec.setFactorsTransformation(DynamicFactorModel.MeasurementType.C);
        mspec.getMeasurements().add(mvspec);

        mvspec = new MeasurementSpec();
        mvspec.setName("m3");
        mvspec.setSeriesTransformations(new Transformation[]{Transformation.Sa, Transformation.Diff1});
        mvspec.setCoefficient(new Parameter[]{new Parameter()});
        mvspec.setFactorsTransformation(DynamicFactorModel.MeasurementType.C);
        mspec.getMeasurements().add(mvspec);

        //vspec.setInitialization(VarSpec.Initialization.Zero);
        mspec.setVarSpec(vspec);
        spec.setModelSpec(mspec);

    }

    public DfmProcessorTest() {
    }

    //@Test
    public void testInitialStep() {

        ProcessingContext context = new ProcessingContext();
        IProcessing<TsVariables, CompositeResults> proc = DfmProcessor.instance.generateProcessing(spec, context);
        CompositeResults rslts = proc.process(vars);
        TsData v1 = rslts.getData("inputc.var1", TsData.class);
        TsData v2 = rslts.getData("inputc.var2", TsData.class);
        TsData v3 = rslts.getData("inputc.var3", TsData.class);
        assertTrue(v1 != null && v2 != null && v3 != null);
    }

    @Test
    public void testPcStep() {

        IProcessing<TsVariables, CompositeResults> proc = DfmProcessor.instance.generateProcessing(spec, null);
        CompositeResults rslts = proc.process(vars);
        DfmResults dfm = rslts.get(DfmProcessor.DFM, DfmResults.class);
        System.out.println(dfm.getModel());
    }

}
