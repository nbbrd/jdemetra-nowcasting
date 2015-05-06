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

import ec.satoolkit.DecompositionMode;
import ec.satoolkit.GenericSaProcessingFactory;
import ec.satoolkit.ISaSpecification;
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
import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.dfm.DefaultInitializer;
import ec.tstoolkit.dfm.DfmEM;
import ec.tstoolkit.dfm.DfmEM2;
import ec.tstoolkit.dfm.DfmEstimator;
import ec.tstoolkit.dfm.DfmModelSpec;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.dfm.EmSpec;
import ec.tstoolkit.dfm.IDfmInitializer;
import ec.tstoolkit.dfm.MeasurementSpec;
import ec.tstoolkit.dfm.NumericalProcessingSpec;
import ec.tstoolkit.dfm.PcInitializer;
import ec.tstoolkit.dfm.PcSpec;
import ec.tstoolkit.maths.realfunctions.IFunctionInstance;
import ec.tstoolkit.maths.realfunctions.ISsqFunctionInstance;
import ec.tstoolkit.maths.realfunctions.ProxyMinimizer;
import ec.tstoolkit.maths.realfunctions.levmar.LevenbergMarquardtMethod;
import ec.tstoolkit.maths.realfunctions.riso.LbfgsMinimizer;
import ec.tstoolkit.modelling.ModellingDictionary;
import ec.tstoolkit.modelling.SeriesInfo;
import ec.tstoolkit.modelling.arima.PreprocessingModel;
import ec.tstoolkit.mssf2.MSsfFunctionInstance;
import ec.tstoolkit.timeseries.Day;
import ec.tstoolkit.timeseries.PeriodSelectorType;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.information.TsInformationSet;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jean Palate
 */
public class DfmProcessingFactory extends ProcessingHookProvider<IProcessingNode, DfmProcessingFactory.EstimationInfo>
        implements IProcessingFactory<DfmSpec, TsData[], CompositeResults> {

    public static class EstimationInfo {

        public EstimationInfo(DynamicFactorModel model, double loglikelihood) {
            this.model = model;
            this.loglikelihood = loglikelihood;
        }

        public final DynamicFactorModel model;
        public final double loglikelihood;
    }

    public static final String INPUTC = "inputc", DFM = "dfm", PC = "pc", PREEM = "preEM", POSTEM = "postEM", PROC = "numproc", FINALC = "finalc";

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
    public IProcessing<TsData[], CompositeResults> generateProcessing(DfmSpec spec, ProcessingContext context) {
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

    private SequentialProcessing<TsData[]> create(DfmSpec spec, ProcessingContext context) {
        SequentialProcessing processing = new SequentialProcessing();
        addInitialStep(spec, processing);
        addPcStep(spec, context, processing);
        addPreEmStep(spec, context, processing);
        addProcStep(spec, context, processing);
        addPostEmStep(spec, context, processing);
        addModelStep(spec, context, processing);
        addFinalStep(spec, context, processing);
        return processing;
    }

    private void addInitialStep(DfmSpec spec, SequentialProcessing processing) {
        processing.add(createInitialStep(spec));
    }

    private void addPcStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getModelSpec().isSpecified() || !spec.getEstimationSpec().isEnabled()) {
            return;
        }
        PcSpec pc = spec.getEstimationSpec().getPrincipalComponentsSpec();
        if (pc != null && pc.isEnabled()) {
            processing.add(createPcStep(pc));
        }
    }

    private void addPreEmStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getModelSpec().isSpecified() || !spec.getEstimationSpec().isEnabled()) {
            return;
        }
        EmSpec em = spec.getEstimationSpec().getPreEmSpec();
        if (em != null && em.isEnabled()) {
            processing.add(createEmStep(em, false));
        }
    }

    private void addProcStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getModelSpec().isSpecified() || !spec.getEstimationSpec().isEnabled()) {
            return;
        }
        NumericalProcessingSpec proc = spec.getEstimationSpec().getNumericalProcessingSpec();
        if (proc != null && proc.isEnabled()) {
            processing.add(createProcStep(proc));
        }
    }

    private void addPostEmStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        if (spec.getModelSpec().isSpecified() || !spec.getEstimationSpec().isEnabled()) {
            return;
        }
        EmSpec em = spec.getEstimationSpec().getPostEmSpec();
        if (em != null && em.isEnabled()) {
            processing.add(createEmStep(em, true));
        }
    }

    private void addModelStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        processing.add(createModelStep(spec));
    }

    private IProcessingNode<TsData[]> createModelStep(final DfmSpec spec) {
        return new IProcessingNode<TsData[]>() {

            @Override
            public String getName() {
                return DFM;
            }

            @Override
            public String getPrefix() {
                return DFM;
            }

            @Override
            public IProcessing.Status process(TsData[] input, Map<String, IProcResults> results) {
                return IProcessing.Status.Valid;
            }

        };
    }

    private void addFinalStep(DfmSpec spec, ProcessingContext context, SequentialProcessing processing) {
        processing.add(createFinalStep(spec));
    }

    private IProcessingNode<TsData[]> createInitialStep(final DfmSpec spec) {
        return new IProcessingNode<TsData[]>() {

            @Override
            public String getName() {
                return INPUTC;
            }

            @Override
            public String getPrefix() {
                return INPUTC;
            }

            @Override
            public IProcessing.Status process(TsData[] input, Map<String, IProcResults> results) {
                List<MeasurementSpec> measurements = spec.getModelSpec().getMeasurements();
                int n = input.length;
                if (n != measurements.size()) {
                    return IProcessing.Status.Invalid;
                }
                TsData[] trs = new TsData[n];
                TsData[] sc = new TsData[n];
                DfmSeriesDescriptor[] desc = new DfmSeriesDescriptor[n];
                int k = 0;
                for (MeasurementSpec ms : measurements) {
                    desc[k] = new DfmSeriesDescriptor(k);
                    TsData s = input[k].clone();
                    if (s == null) {
                        return IProcessing.Status.Invalid;
                    }
                    TsData[] stack = transform(s, ms.getSeriesTransformations(), spec.getSaSpec());
                    s = stack[stack.length - 1];
                    if (s == null) {
                        return IProcessing.Status.Invalid;
                    }

                    double m = ms.getMean(), e = ms.getStdev();
                    if (Double.isNaN(m) || Double.isNaN(e)) {
                        DescriptiveStatistics stats = new DescriptiveStatistics(s);
                        m = stats.getAverage();
                        e = stats.getStdev();
                    }
                    trs[k] = s.clone();
                    s.getValues().sub(m);
                    s.getValues().div(e);
                    desc[k].mean = m;
                    desc[k].stdev = e;
                    desc[k].transformations = ms.getSeriesTransformations();
                    sc[k++] = s;
                }
                MultiTsData inputc = new MultiTsData("var", trs);
                results.put(INPUTC, inputc);
                TsInformationSet dinfo = new TsInformationSet(sc);
                int fh = spec.getModelSpec().getForecastHorizon();
                if (fh < 0) {
                    fh = -fh * dinfo.getCurrentDomain().getFrequency().intValue();
                }
                if (fh > 0) {
                    TsPeriod last = dinfo.getCurrentDomain().getLast();
                    last.move(fh);
                    Day lastday = last.lastday();
                    dinfo = dinfo.extendTo(lastday);
                }
                DfmResults start = new DfmResults(spec.getModelSpec().build(), dinfo);
                start.setDescriptions(desc);
                if (!spec.getModelSpec().isDefined()) {
                    new DefaultInitializer().initialize(start.getModel(), start.getInput());
                }
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
        return new IProcessingNode<TsData[]>() {

            @Override
            public String getName() {
                return PC;
            }

            @Override
            public String getPrefix() {
                return PC;
            }

            @Override
            public IProcessing.Status process(TsData[] input, Map<String, IProcResults> results) {
                DfmResults rslts = (DfmResults) results.get(DFM);
                if (rslts == null) {
                    return IProcessing.Status.Unprocessed;
                }
                TsInformationSet actualData = rslts.getInput().actualData();
                PcInitializer initializer = new PcInitializer();
                if (spec.getSpan().getType() != PeriodSelectorType.All) {
                    TsDomain cur = actualData.getCurrentDomain();
                    cur = cur.select(spec.getSpan());
                    initializer.setEstimationDomain(cur);
                } else {
                    initializer.setNonMissingThreshold(spec.getMinPartNonMissingSeries());
                }
                if (!initializer.initialize(rslts.getModel(), actualData)) {
                    return IProcessing.Status.Invalid;
                }
                return IProcessing.Status.Valid;
            }
        };
    }

    private IProcessingNode<TsData[]> createFinalStep(final DfmSpec spec) {
        return new IProcessingNode<TsData[]>() {

            @Override
            public String getName() {
                return FINALC;
            }

            @Override
            public String getPrefix() {
                return FINALC;
            }

            @Override
            public IProcessing.Status process(TsData[] input, Map<String, IProcResults> results) {
                List<MeasurementSpec> measurements = spec.getModelSpec().getMeasurements();
                int n = input.length;
                if (n != measurements.size()) {
                    return IProcessing.Status.Invalid;
                }
                TsData[] trs = new TsData[n];
                DfmResults dfm = (DfmResults) results.get(DFM);
                for (int i = 0; i < n; ++i) {
                    TsData s = dfm.getData(DfmResults.SMOOTHED + (i + 1), TsData.class);
                    if (s != null) {
                        try {
                            s = s.changeFrequency(input[i].getFrequency(), TsAggregationType.Last, false);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            s = s.changeFrequency(input[i].getFrequency(), TsAggregationType.Last, false);
                        }
                        DfmSeriesDescriptor desc = dfm.getDescription(i);
                        TsData[] stack = untransform(input[i], s, desc.transformations, spec.getSaSpec());
                        trs[i] = stack[0];
                    }
                }
                MultiTsData finalc = new MultiTsData("var", trs);
                results.put(FINALC, finalc);

                return IProcessing.Status.Valid;
            }
        };
    }

    private class EmNode implements IProcessingNode<TsData[]> {

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
        public IProcessing.Status process(TsData[] input, Map<String, IProcResults> results) {
            DfmResults rslts = (DfmResults) results.get(DFM);
            if (rslts == null) {
                return IProcessing.Status.Unprocessed;
            }
            TsInformationSet actualData = rslts.getInput().actualData();
            IDfmInitializer initializer;
            if (spec.getVersion() == EmSpec.DEF_VERSION) {
                DfmEM2 em = new DfmEM2(null);
                em.setMaxIter(spec.getMaxIter());
                em.setCorrectingInitialVariance(end);
                em.setEpsilon(spec.getPrecision());
                initializer = em;
                if (DfmProcessingFactory.this.hasHooks()) {
                    hook = new IProcessingHook<DfmEM2, DynamicFactorModel>() {

                        @Override
                        public void process(IProcessingHook.HookInformation<DfmEM2, DynamicFactorModel> info, boolean cancancel) {
                            EstimationInfo einfo = new EstimationInfo(info.information, info.source.getFinalLogLikelihood());
                            HookInformation<IProcessingNode, EstimationInfo> hinfo = new HookInformation<>((IProcessingNode) EmNode.this, einfo);
                            hinfo.message = info.message;
                            DfmProcessingFactory.this.processHooks(hinfo, cancancel);
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
            if (!initializer.initialize(rslts.getModel(), actualData)) {
                return IProcessing.Status.Invalid;
            }
            return IProcessing.Status.Valid;
        }
    };

    private class ProcNode implements IProcessingNode<TsData[]> {

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
        public IProcessing.Status process(TsData[] input, Map<String, IProcResults> results) {
            DfmResults rslts = (DfmResults) results.get(DFM);
            if (rslts == null) {
                return IProcessing.Status.Unprocessed;
            }
            TsInformationSet actualData = rslts.getInput().actualData();
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
                            DfmProcessingFactory.this.processHooks(hinfo, cancancel);
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
                            DfmProcessingFactory.this.processHooks(hinfo, cancancel);
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
            estimator.setMixedMethod(spec.isMixedEstimation());
            estimator.setIndependentVarShocks(spec.isIndependentVarShocks());
            estimator.setUsingBlockIterations(spec.isBlockIterations());
            estimator.setPrecision(spec.getPrecision());
            if (!estimator.estimate(rslts.getModel(), actualData)) {
                return IProcessing.Status.Invalid;
            } else {
                rslts.setScore(estimator.getGradient());
                rslts.setObservedInformation(estimator.getHessian());
                rslts.setLikelihood(estimator.geLikelihood());
            }
            return IProcessing.Status.Valid;
        }
    };

    /**
     * Updates the spec with the given results
     *
     * @param spec The specification
     * @param rslts The results
     * @param disable
     */
    public static void update(DfmSpec spec, DfmResults rslts, boolean disable) {
        if (rslts == null) {
            return;
        }
        // estimation spec
        if (disable) {
            spec.getEstimationSpec().disable();
        }
        // model spec
        // transformations
        DynamicFactorModel model = rslts.getModel();
        DfmModelSpec mspec = spec.getModelSpec();
        mspec.copyParameters(model);
        int i = 0;
        // measurements        
        for (MeasurementSpec cur : mspec.getMeasurements()) {
            DfmSeriesDescriptor desc = rslts.getDescription(i++);
            cur.setMean(desc.mean);
            cur.setStdev(desc.stdev);
        }
    }

    public static TsData transform(TsData s, MeasurementSpec.Transformation tr, ISaSpecification spec) {
        if (s == null) {
            return null;
        }
        switch (tr) {
            case Log:
                return s.log();
            case Diff1:
                return s.delta(1);
            case DiffY:
                return s.delta(s.getFrequency().intValue());
            case Sa:
                CompositeResults sarslts = SaManager.instance.process(spec, s);
                if (sarslts == null) {
                    return null;
                }
                TsData sa = sarslts.getData(ModellingDictionary.SA_CMP, TsData.class);
                if (sa == null) {
                    return s.clone();
                } else {
                    return sa;
                }
        }
        return null;
    }

    public static TsData[] untransform(TsData original, TsData s, MeasurementSpec.Transformation[] tr, ISaSpecification spec) {
        if (tr == null || tr.length == 0) {
            return new TsData[]{s};
        }
        TsData[] us = new TsData[tr.length + 1];
        us[tr.length] = s;
        TsData[] stack = transform(original, tr, spec);
        for (int i = tr.length - 1; i >= 0; --i) {
            TsData cur = us[i + 1];
            TsData orig = stack[i];
            switch (tr[i]) {
                case Log:
                    cur = cur.exp();
                    break;
                case Diff1:
                    cur = undiff(orig, cur, 1);
                    break;
                case DiffY:
                    cur = undiff(orig, cur, orig.getFrequency().intValue());
                    break;
                case Sa:
                    cur = unsa(orig, cur, spec);
                    break;
            }
            us[i] = cur;
        }
        return us;
    }

    public static TsData[] transform(TsData s, MeasurementSpec.Transformation[] tr, ISaSpecification spec) {
        if (tr == null || tr.length == 0) {
            return new TsData[]{s.clone()};
        } else {
            TsData[] stack = new TsData[tr.length + 1];
            stack[0] = s.clone();
            for (int i = 0; i < tr.length; ++i) {
                stack[i + 1] = transform(stack[i], tr[i], spec);
            }
            return stack;
        }
    }

    private static TsData undiff(TsData orig, TsData cur, int del) {
        TsData s = orig.fittoDomain(cur.getDomain().extend(del, 0));
        int n = s.getLength();

        for (int i = 0; i < del; ++i) {
            // such the first non missing value of the original series
            int refpos = i;
            while (refpos < n && s.isMissing(refpos)) {
                refpos += del;
            }
            if (refpos >= n) {
                continue;
            }
            // fill after refpos
            for (int j = refpos, k = j + del; k < n; j = k, k += del) {
                if (s.isMissing(k)) {
                    s.set(k, s.get(j) + cur.get(j));
                }
            }
            // fill before refpos
            for (int j = refpos - del, k = refpos; j >= 0; k = j, j -= del) {
                s.set(j, s.get(k) - cur.get(j));
            }
        }
        return s;
    }

    private static TsData unsa(TsData orig, TsData cur, ISaSpecification spec) {
        if (orig.getDomain().equals(cur.getDomain())) {
            DescriptiveStatistics ds = new DescriptiveStatistics(orig);
            if (!ds.hasMissingValues()) {
                return orig.clone();
            }
        }
        CompositeResults sarslts = SaManager.instance.process(spec, orig);
        if (sarslts == null) {
            return cur;
        }
        TsData seas = sarslts.getData(ModellingDictionary.S_CMP, TsData.class);
        DecompositionMode mode = sarslts.getData(ModellingDictionary.MODE, DecompositionMode.class);
        if (seas == null) {
            return cur;
        }
        TsData fseas = sarslts.getData(ModellingDictionary.S_CMP + SeriesInfo.F_SUFFIX, TsData.class);
        if (fseas != null) {
            seas = seas.update(fseas);
        }
        TsData seasc = seas.fittoDomain(cur.getDomain());
        int n = cur.getLength();
        int freq = cur.getFrequency().intValue();
        // extends the seasonal component
        int start = cur.getDomain().search(seas.getStart());
        int end = cur.getDomain().search(seas.getEnd());
        if (end == -1) {
            end = n;
        }
        IReadDataBlock rextract = seas.rextract(0, freq);
        double[] sc = seasc.getValues().internalStorage();
        while (start >= freq) {
            start -= freq;
            rextract.copyTo(sc, start);
        }
        if (start > 0) {
            rextract.rextract(freq - start, start).copyTo(sc, 0);
        }
        rextract = seas.rextract(seas.getLength() - freq, freq);
        while (end + freq < n) {
            rextract.copyTo(sc, end);
            end += freq;
        }
        if (end < n) {
            rextract.rextract(0, n - end).copyTo(sc, end);
        }

        PreprocessingModel mdl = sarslts.get(GenericSaProcessingFactory.PREPROCESSING, PreprocessingModel.class);
        if (mdl != null) {
            TsData re = mdl.regressionEffect(seasc.getDomain());
            mdl.backTransform(re, true, true);
            if (mode == DecompositionMode.Multiplicative) {
                seasc = TsData.multiply(seasc, re);
            } else {
                seasc = TsData.add(seasc, re);
            }
        }
        TsData x;
        if (mode == DecompositionMode.Multiplicative) {
            x = TsData.multiply(seasc, cur);
        } else {
            x = TsData.add(seasc, cur);
        }
        int del = orig.getStart().minus(x.getStart());
        for (int i = 0; i < orig.getLength(); ++i) {
            if (!orig.isMissing(i)) {
                x.set(i + del, orig.get(i));
            }
        }

        return x;
    }

}
