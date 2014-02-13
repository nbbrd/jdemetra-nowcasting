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
package ec.tss.Dfm;

import ec.satoolkit.GenericSaProcessingFactory;
import ec.tss.sa.SaManager;
import ec.tstoolkit.algorithm.AlgorithmDescriptor;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.algorithm.IProcResults;
import ec.tstoolkit.algorithm.IProcSpecification;
import ec.tstoolkit.algorithm.IProcessing;
import ec.tstoolkit.algorithm.IProcessingFactory;
import ec.tstoolkit.algorithm.IProcessingHook;
import ec.tstoolkit.algorithm.IProcessingNode;
import ec.tstoolkit.algorithm.MultiTsData;
import ec.tstoolkit.algorithm.ProcessingContext;
import ec.tstoolkit.algorithm.ProcessingHookProvider;
import ec.tstoolkit.algorithm.SequentialProcessing;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.dfm.DefaultInitializer;
import ec.tstoolkit.dfm.DfmEM;
import ec.tstoolkit.dfm.DfmEM2;
import ec.tstoolkit.dfm.DfmEstimator;
import ec.tstoolkit.dfm.DfmInformationSet;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.dfm.EmSpec;
import ec.tstoolkit.dfm.IDfmInitializer;
import ec.tstoolkit.dfm.MeasurementSpec;
import ec.tstoolkit.dfm.NumericalProcessingSpec;
import ec.tstoolkit.dfm.PcInitializer;
import ec.tstoolkit.dfm.PcSpec;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.maths.realfunctions.IFunctionInstance;
import ec.tstoolkit.maths.realfunctions.ISsqFunctionInstance;
import ec.tstoolkit.maths.realfunctions.ProxyMinimizer;
import ec.tstoolkit.maths.realfunctions.levmar.LevenbergMarquardtMethod;
import ec.tstoolkit.maths.realfunctions.riso.LbfgsMinimizer;
import ec.tstoolkit.modelling.ModellingDictionary;
import ec.tstoolkit.mssf2.MSsfFunctionInstance;
import ec.tstoolkit.timeseries.PeriodSelectorType;
import ec.tstoolkit.timeseries.regression.ITsVariable;
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
public class DfmProcessingFactory extends ProcessingHookProvider<IProcessingNode, DfmProcessingFactory.EstimationInfo> implements IProcessingFactory<DfmSpec, TsVariables, CompositeResults> {

    public static class EstimationInfo {

        public EstimationInfo(DynamicFactorModel model, double loglikelihood) {
            this.model = model;
            this.loglikelihood = loglikelihood;
        }

        public final DynamicFactorModel model;
        public final double loglikelihood;
    }

    public static final String INPUTC = "inputc", DFM = "dfm", PC = "pc", PREEM = "preEM", POSTEM = "postEM", PROC = "numproc";

    public static final AlgorithmDescriptor DESCRIPTOR = new AlgorithmDescriptor("Nowcasting", "DynamicFactorModel", "1.0");
    public static final DfmProcessingFactory instance = new DfmProcessingFactory();

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

    private SequentialProcessing<TsVariables> create(DfmSpec spec, ProcessingContext context) {
        SequentialProcessing processing = new SequentialProcessing();
        addInitialStep(spec, processing);
        addPcStep(spec, context, processing);
        addPreEmStep(spec, context, processing);
        addProcStep(spec, context, processing);
        addPostEmStep(spec, context, processing);
        addFinalStep(spec, context, processing);
        return processing;
    }

    private void addInitialStep(DfmSpec spec, SequentialProcessing processing) {
        processing.add(createInitialStep(spec));
    }

    private void addPcStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getEstimationSpec() == null) {
            return;
        }
        PcSpec pc = spec.getEstimationSpec().getPrincipalComponentsSpec();
        if (pc != null && pc.isEnabled()) {
            processing.add(createPcStep(pc));
        }
    }

    private void addPreEmStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getEstimationSpec() == null) {
            return;
        }
        EmSpec em = spec.getEstimationSpec().getPreEmSpec();
        if (em != null && em.isEnabled()) {
            processing.add(createEmStep(em, false));
        }
    }

    private void addProcStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getEstimationSpec() == null) {
            return;
        }
        NumericalProcessingSpec proc = spec.getEstimationSpec().getNumericalProcessingSpec();
        if (proc != null && proc.isEnabled()) {
            processing.add(createProcStep(proc));
        }
    }

    private void addPostEmStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getEstimationSpec() == null) {
            return;
        }
        EmSpec em = spec.getEstimationSpec().getPostEmSpec();
        if (em != null && em.isEnabled()) {
            processing.add(createEmStep(em, true));
        }
    }

    private void addFinalStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        processing.add(createFinalStep(spec));
    }

    private IProcessingNode<TsVariables> createInitialStep(final DfmSpec spec) {
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
                DfmResults start = new DfmResults(spec.getModelSpec().build(), new DfmInformationSet(sc));
                new DefaultInitializer().initialize(start.getModel(), start.getInput());
                results.put(DFM, start);
                return IProcessing.Status.Valid;
            }
        };
    }

    private IProcessingNode createEmStep(final EmSpec spec, final boolean end) {
        return new EmNode(spec, end);
    }

    private IProcessingNode createProcStep(final NumericalProcessingSpec spec) {
        return new ProcNode(spec);
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

    private IProcessingNode<TsVariables> createFinalStep(final DfmSpec spec) {
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

    private class EmNode implements IProcessingNode<TsVariables> {

        private final boolean end;
        private final EmSpec spec;

        private EmNode(EmSpec spec, boolean end) {
            this.end = end;
            this.spec = spec;
        }

        @Override
        public String getName() {
            return end ? POSTEM : PREEM;
        }

        @Override
        public String getPrefix() {
            return end ? POSTEM : PREEM;
        }
        IProcessingHook hook;

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
                em.setCorrectingInitialVariance(end);
                initializer = em;
                if (DfmProcessingFactory.this.hasHooks()) {
                    hook = new IProcessingHook<DfmEM2, DynamicFactorModel>() {

                        @Override
                        public void process(IProcessingHook.HookInformation<DfmEM2, DynamicFactorModel> info, boolean cancancel) {
                            EstimationInfo einfo = new EstimationInfo(info.information, info.source.getFinalLogLikelihood());
                            HookInformation<IProcessingNode, EstimationInfo> hinfo = new HookInformation<>((IProcessingNode) EmNode.this, einfo);
                            hinfo.message = info.message;
                            DfmProcessingFactory.instance.processHooks(hinfo, cancancel);
                            if (hinfo.cancel) {
                                info.cancel = true;
                            }
                        }
                    };
                    em.register(hook);
                }
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

    private class ProcNode implements IProcessingNode<TsVariables> {

        private final NumericalProcessingSpec spec;

        private ProcNode(NumericalProcessingSpec spec) {
            this.spec = spec;
        }

        @Override
        public String getName() {
            return PROC;
        }

        @Override
        public String getPrefix() {
            return PROC;
        }
        IProcessingHook hook;

        @Override
        public IProcessing.Status process(TsVariables input, Map<String, IProcResults> results, InformationSet info) {
            DfmResults rslts = (DfmResults) results.get(DFM);
            if (rslts == null) {
                return IProcessing.Status.Unprocessed;
            }
            DfmEstimator estimator;
            if (spec.getMethod() == NumericalProcessingSpec.Method.LevenbergMarquardt) {
                LevenbergMarquardtMethod lm = new LevenbergMarquardtMethod();
                if (DfmProcessingFactory.this.hasHooks()) {
                    hook = new IProcessingHook<LevenbergMarquardtMethod, ISsqFunctionInstance>() {

                        @Override
                        public void process(IProcessingHook.HookInformation<LevenbergMarquardtMethod, ISsqFunctionInstance> info, boolean cancancel) {
                            MSsfFunctionInstance pt = (MSsfFunctionInstance) info.information;
                            DynamicFactorModel model = ((DynamicFactorModel.Ssf) pt.ssf).getModel();
                            EstimationInfo einfo = new EstimationInfo(model, pt.getLikelihood().getLogLikelihood());
                            HookInformation<IProcessingNode, EstimationInfo> hinfo = new HookInformation<>((IProcessingNode) ProcNode.this, einfo);
                            hinfo.message = info.message;
                            DfmProcessingFactory.instance.processHooks(hinfo, cancancel);
                            if (hinfo.cancel) {
                                info.cancel = true;
                            }
                        }
                    };
                    lm.register(hook);

                }
                estimator = new DfmEstimator(new ProxyMinimizer(lm));
            } else {
                LbfgsMinimizer lbfgs = new LbfgsMinimizer();
                if (DfmProcessingFactory.this.hasHooks()) {
                    hook = new IProcessingHook<LbfgsMinimizer, IFunctionInstance>() {

                        @Override
                        public void process(IProcessingHook.HookInformation<LbfgsMinimizer, IFunctionInstance> info, boolean cancancel) {
                            MSsfFunctionInstance pt = (MSsfFunctionInstance) info.information;
                            DynamicFactorModel model = ((DynamicFactorModel.Ssf) pt.ssf).getModel();
                            EstimationInfo einfo = new EstimationInfo(model, pt.getLikelihood().getLogLikelihood());
                            HookInformation<IProcessingNode, EstimationInfo> hinfo = new HookInformation<>((IProcessingNode) ProcNode.this, einfo);
                            hinfo.message = info.message;
                            DfmProcessingFactory.instance.processHooks(hinfo, cancancel);
                            if (hinfo.cancel) {
                                info.cancel = true;
                            }
                        }
                    };
                    lbfgs.register(hook);

                }
                estimator = new DfmEstimator(lbfgs);

            }
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
