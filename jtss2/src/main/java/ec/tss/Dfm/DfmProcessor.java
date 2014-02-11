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

import ec.satoolkit.ISaSpecification;
import ec.tss.sa.SaManager;
import ec.tstoolkit.dfm.DfmModelSpec;
import ec.tstoolkit.algorithm.AlgorithmDescriptor;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.algorithm.IProcResults;
import ec.tstoolkit.algorithm.IProcSpecification;
import ec.tstoolkit.algorithm.IProcessing;
import ec.tstoolkit.algorithm.IProcessingFactory;
import ec.tstoolkit.algorithm.IProcessingNode;
import ec.tstoolkit.algorithm.MultiTsData;
import ec.tstoolkit.algorithm.ProcessingContext;
import ec.tstoolkit.algorithm.SequentialProcessing;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.dfm.MeasurementSpec;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.modelling.ModellingDictionary;
import ec.tstoolkit.timeseries.regression.ITsVariable;
import ec.tstoolkit.timeseries.regression.TsVariable;
import ec.tstoolkit.timeseries.regression.TsVariables;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jean Palate
 */
public class DfmProcessor implements IProcessingFactory<DfmSpec, TsVariables, CompositeResults> {

    public static final String STRANSFORM = "seriestransformations";

    public static final AlgorithmDescriptor DESCRIPTOR = new AlgorithmDescriptor("Nowcasting", "DynamicFactorModel", "1.0");

    @Override
    public void dispose() {
    }

    @Override
    public AlgorithmDescriptor getInformation() {
        return DESCRIPTOR;
    }

    @Override
    public boolean canHandle(IProcSpecification spec) {
        return spec instanceof DfmSpec;
    }

    @Override
    public IProcessing<TsVariables, CompositeResults> generateProcessing(DfmSpec spec, ProcessingContext context) {
        return create(spec, context);
    }

    @Override
    public Map<String, Class> getSpecificationDictionary(Class<DfmSpec> specClass) {
        HashMap<String, Class> dic = new HashMap<>();
        DfmSpec.fillDictionary(null, dic);
        return dic;
    }

    @Override
    public Map<String, Class> getOutputDictionary() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static SequentialProcessing<TsVariables> create(DfmSpec spec, ProcessingContext context) {
        SequentialProcessing processing = new SequentialProcessing();
        addInitialStep(spec.getModelSpec().getMeasurements(), context, processing);
        addPcStep(spec, context, processing);
        addPreEmStep(spec, context, processing);
        addProcStep(spec, context, processing);
        addPostEmStep(spec, context, processing);
        addFinalStep(spec, context, processing);
        return processing;
    }

    private static void addInitialStep(List<MeasurementSpec> measurements, ProcessingContext context, SequentialProcessing processing) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void addPcStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void addPreEmStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void addProcStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
    }

    private static void addPostEmStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
    }

    private static void addFinalStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
    }

    protected static IProcessingNode<TsVariables> createInitialStep(final TsVariables vars, final ISaSpecification sa, final List<MeasurementSpec> spec) {
        return new IProcessingNode<TsVariables>() {

            @Override
            public String getName() {
                return STRANSFORM;
            }

            @Override
            public String getPrefix() {
                return STRANSFORM;
            }

            @Override
            public IProcessing.Status process(TsVariables input, Map<String, IProcResults> results, InformationSet info) {
                TsData[] sc = new TsData[spec.size()];
                int k = 0;
                for (MeasurementSpec ms : spec) {
                    ITsVariable var = input.get(ms.getName());
                    if (var == null || var.getDim() > 1) {
                        return IProcessing.Status.Invalid;
                    }
                    TsDomain curdom = var.getDefinitionDomain();
                    if (curdom == null) {
                        return IProcessing.Status.Invalid;
                    }
                    DataBlock data = new DataBlock(curdom.getLength());
                    var.data(curdom, Collections.singletonList(data), 0);
                    TsData s = new TsData(curdom.getStart(), data.getData(), false);
                    MeasurementSpec.Transformation[] st = ms.getSeriesTransformations();
                    if (st != null) {
                        for (int i = 0; i < st.length; ++i) {
                            switch (st[i]) {
                                case Log:
                                    s = s.log();
                                    break;
                                case Diff:
                                    s = s.delta(1);
                                    break;
                                case Sa:
                                    s = SaManager.instance.process(sa, s).searchData(results, ModellingDictionary.SA, TsData.class);
                                    break;
                            }
                            if (s == null) {
                                return IProcessing.Status.Invalid;
                            }
                        }
                    }
                    sc[k++] = s;
                }
                MultiTsData inputc = new MultiTsData("var", sc);
                results.put(STRANSFORM, inputc);
                return IProcessing.Status.Valid;
            }
        };
    }

}
