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

import ec.satoolkit.GenericSaProcessingFactory;
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
import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.dfm.DefaultInitializer;
import ec.tstoolkit.dfm.DfmEM;
import ec.tstoolkit.dfm.DfmEM2;
import ec.tstoolkit.dfm.DfmEstimator;
import ec.tstoolkit.dfm.DfmInformationSet;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.dfm.EmSpec;
import ec.tstoolkit.dfm.IDfmInitializer;
import ec.tstoolkit.dfm.MeasurementSpec;
import ec.tstoolkit.dfm.NumericalProcessingSpec;
import ec.tstoolkit.dfm.PcInitializer;
import ec.tstoolkit.dfm.PcSpec;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.modelling.ModellingDictionary;
import ec.tstoolkit.timeseries.PeriodSelectorType;
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

    public static final String INPUTC = "inputc", DFM = "dfm", PC = "pc", PREEM = "preEM", POSTEM = "postEM", PROC = "numproc";

    public static final AlgorithmDescriptor DESCRIPTOR = new AlgorithmDescriptor("Nowcasting", "DynamicFactorModel", "1.0");
    public static final DfmProcessor instance = new DfmProcessor();

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
        addInitialStep(spec, processing);
        addPcStep(spec, context, processing);
        addPreEmStep(spec, context, processing);
        addProcStep(spec, context, processing);
        addPostEmStep(spec, context, processing);
        addFinalStep(spec, context, processing);
        return processing;
    }

    private static void addInitialStep(DfmSpec spec, SequentialProcessing processing) {
        processing.add(createInitialStep(spec));
    }

    private static void addPcStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getEstimationSpec() == null) {
            return;
        }
        PcSpec pc = spec.getEstimationSpec().getPrincipalComponentsSpec();
        if (pc != null && pc.isEnabled()) {
            processing.add(createPcStep(pc));
        }
    }

    private static void addPreEmStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getEstimationSpec() == null) {
            return;
        }
        EmSpec em = spec.getEstimationSpec().getPreEmSpec();
        if (em != null && em.isEnabled()) {
            processing.add(createEmStep(em, PREEM));
        }
    }

    private static void addProcStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getEstimationSpec() == null) {
            return;
        }
        NumericalProcessingSpec proc = spec.getEstimationSpec().getNumericalProcessingSpec();
        if (proc != null && proc.isEnabled()) {
            processing.add(createProcStep(proc));
        }
    }

    private static void addPostEmStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getEstimationSpec() == null) {
            return;
        }
        EmSpec em = spec.getEstimationSpec().getPostEmSpec();
        if (em != null && em.isEnabled()) {
            processing.add(createEmStep(em, POSTEM));
        }
    }

    private static void addFinalStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        processing.add(createFinalStep(spec));
    }

    private static IProcessingNode<TsVariables> createInitialStep(final DfmSpec spec) {
        return new IProcessingNode<TsVariables>() {

            @Override
            public String getName() {
                return INPUTC;
            }

            @Override
            public String getPrefix() {
                return INPUTC;
            }

            @Override
            public IProcessing.Status process(TsVariables input, Map<String, IProcResults> results, InformationSet info) {
                List<MeasurementSpec> measurements = spec.getModelSpec().getMeasurements();
                TsData[] sc = new TsData[measurements.size()];
                int k = 0;
                for (MeasurementSpec ms : measurements) {
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
                                case Diff1:
                                    s = s.delta(1);
                                    break;
                                case DiffY:
                                    s = s.delta(s.getFrequency().intValue());
                                    break;
                                case Sa:
                                    CompositeResults sarslts = SaManager.instance.process(spec.getSaSpec(), s);
                                    if (sarslts == null) {
                                        return IProcessing.Status.Invalid;
                                    }
                                    IProcResults finals = sarslts.get(GenericSaProcessingFactory.FINAL);
                                    if (finals == null) {
                                        return IProcessing.Status.Invalid;
                                    }
                                    s = finals.getData(ModellingDictionary.SA, TsData.class);
                                    break;
                            }
                            if (s == null) {
                                return IProcessing.Status.Invalid;
                            }
                        }
                    }
                    double m = ms.getMean(), e = ms.getStdev();
                    if (Double.isNaN(m) || Double.isNaN(e)) {
                        DescriptiveStatistics stats = new DescriptiveStatistics(s);
                        m = stats.getAverage();
                        e = stats.getStdev();
                    }
                    s.getValues().sub(m);
                    s.getValues().div(e);
                    sc[k++] = s;
                }
                MultiTsData inputc = new MultiTsData("var", sc);
                results.put(INPUTC, inputc);
                DfmResults start=new DfmResults(spec.getModelSpec().build(), new DfmInformationSet(sc));
                  new DefaultInitializer().initialize(start.getModel(), start.getInput());
              results.put(DFM, start);
                return IProcessing.Status.Valid;
            }
        };
    }

    private static IProcessingNode createEmStep(final EmSpec spec, final String name) {
        return new IProcessingNode<TsVariables>() {

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getPrefix() {
                return name;
            }

            @Override
            public IProcessing.Status process(TsVariables input, Map<String, IProcResults> results, InformationSet info) {
                DfmResults rslts = (DfmResults) results.get(DFM);
                if (rslts == null) {
                    return IProcessing.Status.Unprocessed;
                }
                IDfmInitializer initializer;
                if (spec.getVersion() == EmSpec.DEF_VERSION) {
                    DfmEM2 em = new DfmEM2(null);
                    em.setMaxIter(spec.getMaxIter());
                    initializer = em;
                } else {
                    DfmEM em = new DfmEM();
                    em.setMaxIter(spec.getMaxIter());
                    initializer = em;
                }
                if (!initializer.initialize(rslts.getModel(), rslts.getInput())) {
                    return IProcessing.Status.Invalid;
                }
                return IProcessing.Status.Valid;
            }
        };
    }

    private static IProcessingNode createProcStep(final NumericalProcessingSpec spec) {
        return new IProcessingNode<TsVariables>() {

            @Override
            public String getName() {
                return PROC;
            }

            @Override
            public String getPrefix() {
                return PROC;
            }

            @Override
            public IProcessing.Status process(TsVariables input, Map<String, IProcResults> results, InformationSet info) {
                DfmResults rslts = (DfmResults) results.get(DFM);
                if (rslts == null) {
                    return IProcessing.Status.Unprocessed;
                }
                DfmEstimator estimator = new DfmEstimator();
                estimator.setMaxIter(spec.getMaxIter());
                estimator.setMaxInitialIter(spec.getMaxInitialIter());
                estimator.setMaxIntermediateIter(spec.getMaxIntermediateIter());
                if (!estimator.estimate(rslts.getModel(), rslts.getInput())) {
                    return IProcessing.Status.Invalid;
                }
                return IProcessing.Status.Valid;
            }
        };
    }

    private static IProcessingNode createPcStep(final PcSpec spec) {
        return new IProcessingNode<TsVariables>() {

            @Override
            public String getName() {
                return PC;
            }

            @Override
            public String getPrefix() {
                return PC;
            }

            @Override
            public IProcessing.Status process(TsVariables input, Map<String, IProcResults> results, InformationSet info) {
                DfmResults rslts = (DfmResults) results.get(DFM);
                if (rslts == null) {
                    return IProcessing.Status.Unprocessed;
                }
                PcInitializer initializer = new PcInitializer();
                if (spec.getSpan().getType() != PeriodSelectorType.All) {
                    TsDomain cur = rslts.getInput().getCurrentDomain();
                    cur = cur.select(spec.getSpan());
                    initializer.setEstimationDomain(cur);
                } else {
                    initializer.setNonMissingThreshold(spec.getMinPartNonMissingSeries());
                }
                if (!initializer.initialize(rslts.getModel(), rslts.getInput())) {
                    return IProcessing.Status.Invalid;
                }
                return IProcessing.Status.Valid;
            }
        };
    }

    private static IProcessingNode<TsVariables> createFinalStep(final DfmSpec spec) {
        return new IProcessingNode<TsVariables>() {

            @Override
            public String getName() {
                return DFM;
            }

            @Override
            public String getPrefix() {
                return DFM;
            }

            @Override
            public IProcessing.Status process(TsVariables input, Map<String, IProcResults> results, InformationSet info) {
                return IProcessing.Status.Valid;
            }
        };
    }

}
