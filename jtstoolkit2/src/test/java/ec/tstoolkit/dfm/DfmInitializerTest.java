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
package ec.tstoolkit.dfm;

import data.Data;
import ec.tstoolkit.algorithm.IProcessingHook;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.dfm.DynamicFactorModel.TransitionDescriptor;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.MatrixException;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import ec.tstoolkit.maths.realfunctions.IFunctionInstance;
import ec.tstoolkit.maths.realfunctions.ISsqFunctionInstance;
import ec.tstoolkit.maths.realfunctions.ProxyMinimizer;
import ec.tstoolkit.maths.realfunctions.levmar.LevenbergMarquardtMethod;
import ec.tstoolkit.maths.realfunctions.riso.LbfgsMinimizer;
import ec.tstoolkit.mssf2.MSsfFunctionInstance;
import ec.tstoolkit.ssf2.ResidualsCumulator;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.tstoolkit.var.VarSpec;
import org.junit.Test;

/**
 *
 * @author Jean Palate
 */
public class DfmInitializerTest {

    static final DynamicFactorModel dmodel = new DynamicFactorModel(12, 3);
    static final int N = 500;
    static final boolean stressTest = false;

    public static void evaluate(final ResidualsCumulator rslts,
            final Likelihood ll) {
        int n = rslts.getObsCount();
        double ssqerr = rslts.getSsqErr(), ldet = rslts.getLogDeterminant();
        ll.set(ssqerr, ldet, n);
    }
    private static Matrix T, TVar, MVar, D, O, Z, M, dd, ddrnd;

    private static void loadDavidModel() {
        T = Data.readMatrix(DynamicFactorModelTest.class, "/transition.csv");
        //System.out.println(T);
        TVar = Data.readMatrix(DynamicFactorModelTest.class, "/tcovar.csv");
        //System.out.println(TVar);
        MVar = Data.readMatrix(DynamicFactorModelTest.class, "/mcovar.csv");
        //System.out.println(MVar);
        D = Data.readMatrix(DynamicFactorModelTest.class, "/data.csv");
        O = Data.readMatrix(DynamicFactorModelTest.class, "/original.csv");
        //System.out.println(D);
        Z = Data.readMatrix(DynamicFactorModelTest.class, "/loadings.csv");
        //System.out.println(Z);
        M = Data.readMatrix(DynamicFactorModelTest.class, "/model.csv");
        //System.out.println(M);

        //transition equation
        int nb = 3, nl = 4, c = 24;
        DynamicFactorModel.TransitionDescriptor tdesc = new DynamicFactorModel.TransitionDescriptor(nb, nl);
        for (int i = 0; i < nb; ++i) {
            DataBlock trow = T.row(i * 12).range(0, nl);
            DataBlock prow = tdesc.varParams.row(i).range(0, nl);
            for (int j = 0; j < nb; ++j) {
                prow.copy(trow);
                trow.move(12);
                prow.move(nl);
            }
        }
        for (int i = 0; i < nb; ++i) {
            DataBlock trow = TVar.row(i * 12).extract(0, nb, 12);
            DataBlock prow = tdesc.covar.row(i);
            prow.copy(trow);
        }
        dmodel.setTransition(tdesc);

        // measurement equation
        int nv = 0;
        for (int i = 0; i < M.getRowsCount(); ++i) {
            if (!M.row(i).range(0, 3).isZero()) {
                ++nv;
            }
        }
        dd = new Matrix(nv, O.getRowsCount() + 15);
        dd.set(Double.NaN);
        for (int i = 0, j = 0; i < M.getRowsCount(); ++i) {
            if (!M.row(i).range(0, 4).isZero()) {
                DataBlock row = dd.row(j);
                row.range(0, O.getRowsCount()).copy(O.column(j));
                DescriptiveStatistics stats = new DescriptiveStatistics(O.column(j));
                double m = stats.getAverage();
                double e = stats.getStdev();
                row.sub(m);
                row.mul(1 / e);
                double[] q = new double[3];
                for (int k = 0; k < 3; ++k) {
                    if (M.get(i, k + 1) == 1) {
                        q[k] = Z.get(j, k * 12);
                    } else {
                        q[k] = Double.NaN;
                    }
                }
                for (int l = 0; l < q.length; ++l) {
                }
                DynamicFactorModel.MeasurementDescriptor desc = new DynamicFactorModel.MeasurementDescriptor(
                        measurement((int) M.get(i, 0)), q, MVar.get(j, j));
                dmodel.addMeasurement(desc);
                ++j;
            }
        }
        ddrnd = dd.clone();
        ddrnd.randomize();
        dmodel.setInitialization(VarSpec.Initialization.SteadyState);
    }

    private static DynamicFactorModel.IMeasurement measurement(int i) {
        if (i == 1) {
            return DynamicFactorModel.measurement(DynamicFactorModel.MeasurementType.L);
        } else if (i == 2) {
            return DynamicFactorModel.measurement(DynamicFactorModel.MeasurementType.CD);
        } else {
            return DynamicFactorModel.measurement(DynamicFactorModel.MeasurementType.C);
        }
    }

    static {
        loadDavidModel();
    }

    public DfmInitializerTest() {
    }

    //@Test
    public void testInitialize() {
        TsData[] s = new TsData[dd.getRowsCount()];
        TsPeriod start = new TsPeriod(TsFrequency.Monthly, 1980, 0);
        for (int i = 0; i < s.length; ++i) {
            s[i] = new TsData(start, dd.row(i));
        }
        PcInitializer initializer = new PcInitializer();
        initializer.setEstimationDomain(s[0].getDomain().drop(120, 12));
        DynamicFactorModel model0 = dmodel.clone();
        initializer.initialize(model0, new DfmInformationSet(s));
//        for (int i = 0; i < dmodel.getTransition().nbloks; ++i) {
//            DataBlock factor=initializer.getPrincipalComponents(i).getFactor(0);
//            factor.sub(factor.sum()/factor.getLength());
//            factor.div(Math.sqrt(factor.ssq()/factor.getLength()));
//            factor.chs();
//            System.out.println(factor);
//        }
        System.out.println(model0.getTransition().covar);
        System.out.println(model0.getTransition().varParams);
        for (DynamicFactorModel.MeasurementDescriptor desc : model0.getMeasurements()) {
            for (int i = 0; i < desc.coeff.length; ++i) {
                System.out.print(desc.coeff[i]);
                System.out.print('\t');
            }
            System.out.println(desc.var);
        }
    }
    //@Test

    public void testSmoohter() {
        TsData[] s = new TsData[dd.getRowsCount()];
        for (int i = 0; i < s.length; ++i) {
            s[i] = new TsData(new TsPeriod(TsFrequency.Monthly, 1980, 0), dd.row(i));
        }
        DfmProcessor processor = new DfmProcessor();
        processor.setCalcVariance(false);
        processor.process(dmodel, new DfmInformationSet(s));
        for (int i = 0; i < dmodel.getFactorsCount(); ++i) {
            DataBlock cmp = new DataBlock(processor.getSmoothingResults().component(i * dmodel.getBlockLength()));
            cmp.sub(cmp.sum() / cmp.getLength());
            cmp.div(Math.sqrt(cmp.ssq() / cmp.getLength()));
            System.out.println(cmp);
        }
    }

    //@Test
    public void testEM() {
        TsData[] s = new TsData[dd.getRowsCount()];
        TsPeriod start = new TsPeriod(TsFrequency.Monthly, 1980, 0);
        for (int i = 0; i < s.length; ++i) {
            s[i] = new TsData(start, dd.row(i));
        }
        PcInitializer initializer = new PcInitializer();
//       initializer.setEstimationDomain(s[0].getDomain().drop(120, 12));
        DynamicFactorModel model0 = dmodel.clone();
        model0.normalize();
//        model0.setInitialization(DynamicFactorModel.Initialization.Zero);
//        DynamicFactorModel model1=model0.clone();
        DfmInformationSet dfmInformationSet = new DfmInformationSet(s);
//        DefaultInitializer initializer =new DefaultInitializer();
        initializer.initialize(model0, dfmInformationSet);
//        initializer.initialize(model0, dfmInformationSet);
//        for (int i = 0; i < dmodel.getTransition().nbloks; ++i) {
//            DataBlock factor=initializer.getPrincipalComponents(i).getFactor(0);
//            factor.sub(factor.sum()/factor.getLength());
//            factor.div(Math.sqrt(factor.ssq()/factor.getLength()));
//            factor.chs();
//            System.out.println(factor);
//        }
//        System.out.println(model0.getTransition().covar);
//        System.out.println(model0.getTransition().varParams);
//        for (DynamicFactorModel.MeasurementDescriptor desc : model0.getMeasurements()) {
//            for (int i = 0; i < desc.coeff.length; ++i) {
//                System.out.print(desc.coeff[i]);
//                System.out.print('\t');
//            }
//            System.out.println(desc.var);
//        }
//
//        DfmEM em = new DfmEM();
        DfmEM2 em = new DfmEM2(null);
        em.setMaxIter(10000);
        em.initialize(model0, dfmInformationSet);
//        System.out.println(model0.getTransition().covar);
//        System.out.println(model0.getTransition().varParams);
//        for (DynamicFactorModel.MeasurementDescriptor desc : model0.getMeasurements()) {
//            for (int i = 0; i < desc.coeff.length; ++i) {
//                System.out.print(desc.coeff[i]);
//                System.out.print('\t');
//            }
//            System.out.println(desc.var);
//        }
        DfmMonitor monitor = new DfmMonitor();
        DfmEstimator estimator = new DfmEstimator();
        estimator.setMaxIter(1000);
//        estimator.setMaxInitialIter(0);
//        estimator.setMaxNextIter(3);
        //monitor.setInitializer(initializer);
        //        monitor.setInitializer(new DefaultInitializer());
        monitor.setEstimator(estimator);
        //monitor.process(model0, s);
        em.setMaxIter(1000);
        em.initialize(model0, dfmInformationSet);

    }

    @Test
    public void testEM2() {
        TsData[] s = new TsData[dd.getRowsCount()];
        TsPeriod start = new TsPeriod(TsFrequency.Monthly, 1980, 0);
        for (int i = 0; i < s.length; ++i) {
            s[i] = new TsData(start, dd.row(i));
        }
        DynamicFactorModel model0 = dmodel.clone();
        model0.normalize();
 //       model0.setTransition(new TransitionDescriptor(3, 1));
        DfmInformationSet dfmInformationSet = new DfmInformationSet(s);

        //DefaultInitializer initializer = new DefaultInitializer();
        PcInitializer initializer = new PcInitializer();
 //       initializer.initialize(model0, dfmInformationSet);

        DynamicFactorModel model1 = model0.clone();
        DfmMonitor monitor = new DfmMonitor();
//        LbfgsMinimizer lbfgs=new LbfgsMinimizer();
//        IProcessingHook<LbfgsMinimizer, IFunctionInstance> hook=new IProcessingHook<LbfgsMinimizer, IFunctionInstance>() {
//
//            @Override
//            public void process(IProcessingHook.HookInformation<LbfgsMinimizer, IFunctionInstance> info, boolean cancancel) {
//                MSsfFunctionInstance pt=(MSsfFunctionInstance) info.information;
//                System.out.println(pt.getLikelihood().getLogLikelihood());
//            }
//        };
//        lbfgs.register(hook);
//        DfmEstimator estimator = new DfmEstimator(lbfgs);
        LevenbergMarquardtMethod lm = new LevenbergMarquardtMethod();
        IProcessingHook<LevenbergMarquardtMethod, ISsqFunctionInstance> hook = new IProcessingHook<LevenbergMarquardtMethod, ISsqFunctionInstance>() {

            @Override
            public void process(IProcessingHook.HookInformation<LevenbergMarquardtMethod, ISsqFunctionInstance> info, boolean cancancel) {
                MSsfFunctionInstance pt = (MSsfFunctionInstance) info.information;
                DynamicFactorModel model = ((DynamicFactorModel.Ssf) pt.ssf).getModel();
                System.out.print(info.message);
                System.out.print('\t');
                System.out.print(pt.getLikelihood().getLogLikelihood());
                System.out.print('\t');
                System.out.println(new DfmMapping(model).parameters());
            }
        };
        lm.register(hook);
        DfmEstimator estimator = new DfmEstimator(new ProxyMinimizer(lm));
//        model0.setInitialization(VarSpec.Initialization.Zero);
//        Matrix v0=new Matrix(36, 36);
//        v0.diagonal().set(1e3);
//        model0.setInitialCovariance(v0);
        monitor.setEstimator(estimator);
               estimator.setMaxInitialIter(0);
        estimator.setMixedMethod(false);
//        estimator.setUsingBlockIterations(false);
        estimator.setMaxIter(500);
        monitor.process(model0, s);
        System.out.println("******************");
        System.out.println(new DfmMapping(model0).parameters());
        System.out.println("******************");
        System.out.println(estimator.getGradient());
        System.out.println("******************");
        Matrix H = estimator.getHessian();
        try {
            H = SymmetricMatrix.inverse(H);
            DataBlock stde = new DataBlock(H.diagonal());
            stde.sqrt();
            System.out.println(stde);
        } catch (MatrixException err) {

        }
        //DfmEM em = new DfmEM();
        DfmEM2 em = new DfmEM2(null);
        //em.setCorrectingInitialVariance(false);
        em.setMaxIter(500);
        IProcessingHook<DfmEM2, DynamicFactorModel> emhook = new IProcessingHook<DfmEM2, DynamicFactorModel>() {

            @Override
            public void process(IProcessingHook.HookInformation<DfmEM2, DynamicFactorModel> info, boolean cancancel) {
                System.out.print(info.message);
                System.out.print('\t');
                System.out.print(info.source.getFinalLogLikelihood());
                System.out.print('\t');
                System.out.println(new DfmMapping(info.information).parameters());
            }
        };
        em.register(emhook);
        //em.initialize(model0, dfmInformationSet);

    }
}
