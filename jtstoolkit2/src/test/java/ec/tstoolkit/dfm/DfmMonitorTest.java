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
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DescriptiveStatistics;
import static ec.tstoolkit.dfm.DynamicFactorModelTest.dmodel;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.realfunctions.IFunctionInstance;
import ec.tstoolkit.maths.realfunctions.ISsqFunctionDerivatives;
import ec.tstoolkit.maths.realfunctions.ISsqFunctionInstance;
import ec.tstoolkit.maths.realfunctions.minpack.IEstimationProblem;
import ec.tstoolkit.maths.realfunctions.minpack.ILmHook;
import ec.tstoolkit.maths.realfunctions.minpack.SsqEstimationProblem;
import ec.tstoolkit.mssf2.IMSsfData;
import ec.tstoolkit.mssf2.MSsfAlgorithm;
import ec.tstoolkit.mssf2.MSsfFunction;
import ec.tstoolkit.mssf2.MSsfFunctionInstance;
import ec.tstoolkit.mssf2.MultivariateSsfData;
import ec.tstoolkit.ssf2.ResidualsCumulator;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jean Palate
 */
public class DfmMonitorTest {

    static final DynamicFactorModel dmodel = new DynamicFactorModel(12);
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
                int ni = 0;
                for (int k = 1; k < 4; ++k) {
                    if (M.get(i, k) == 1) {
                        ++ni;
                    }
                }
                int[] f = new int[ni];
                for (int k = 1, l = 0; k < 4; ++k) {
                    if (M.get(i, k) == 1) {
                        f[l++] = k - 1;
                    }
                }
                double[] q = new double[ni];
                for (int l = 0; l < q.length; ++l) {
                    q[l] = Z.get(j, l * 12);
                }
                DynamicFactorModel.MeasurementDescriptor desc = new DynamicFactorModel.MeasurementDescriptor(
                        measurement((int) M.get(i, 0)), f, q, MVar.get(j, j));
                dmodel.addMeasurement(desc);
                ++j;
            }
        }
        ddrnd = dd.clone();
        ddrnd.randomize();
        dmodel.setInitialization(DynamicFactorModel.Initialization.SteadyState);
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

    public DfmMonitorTest() {
    }

    //@Test
    public void testNormalize() {
        TsData[] s = new TsData[dd.getRowsCount()];
        TsPeriod start = new TsPeriod(TsFrequency.Monthly, 1980, 0);
        for (int i = 0; i < s.length; ++i) {
            s[i] = new TsData(start, dd.row(i));
        }
        DfmMonitor monitor = new DfmMonitor(dmodel);
        monitor.setCalcVariance(false);
        monitor.setNormalizingVariance(false);
        monitor.process(s);
        double[] component = monitor.getSmoothingResults().component(0);

        Likelihood ll = new Likelihood();
        evaluate(monitor.getFilteringResults(), ll);
        System.out.println(ll.getLogLikelihood());
        DfmMonitor nmonitor = new DfmMonitor(dmodel.normalize());
        nmonitor.setCalcVariance(false);
        nmonitor.setNormalizingVariance(false);
        nmonitor.process(s);
        double[] ncomponent = nmonitor.getSmoothingResults().component(0);

        Likelihood nll = new Likelihood();
        evaluate(nmonitor.getFilteringResults(), nll);
        System.out.println(nll.getLogLikelihood());

//        for (int i=0; i<ncomponent.length; ++i){
//            System.out.print(component[i]);
//            System.out.print('\t');
//            System.out.println(ncomponent[i]);
//            
//        }
    }

    @Test
    public void testGradient() {

        IMSsfData data = new MultivariateSsfData(dd.subMatrix(), null);
        MSsfAlgorithm algorithm = new MSsfAlgorithm();
        //nmodel = dmodel.normalize();
        TsData[] s = new TsData[dd.getRowsCount()];
        TsPeriod start = new TsPeriod(TsFrequency.Monthly, 1980, 0);
        for (int i = 0; i < s.length; ++i) {
            s[i] = new TsData(start, dd.row(i));
        }
        DfmInitializer initializer = new DfmInitializer();
        initializer.initialize(dmodel, s, s[0].getDomain().drop(120, 12));
//        DynamicFactorModel nmodel = dmodel.normalize();
        DynamicFactorModel nmodel = initializer.getInitialModel();
        DfmMapping mapping = new DfmMapping(nmodel);
        MSsfFunction fn = new MSsfFunction(data, mapping, algorithm);
        DataBlock def = mapping.getDefault();
        MSsfFunctionInstance pt = fn.ssqEvaluate(mapping.map(nmodel.ssfRepresentation()));
        System.out.println(new DataBlock(pt.getParameters()));
        //         MSsfFunctionInstance pt = fn.ssqEvaluate(def);
        System.out.println(pt.getLikelihood().getLogLikelihood());
        Optimizer opt = new Optimizer();
        // ec.tstoolkit.maths.realfunctions.minpack.LevenbergMarquardtMinimizer opt
        //        = new ec.tstoolkit.maths.realfunctions.minpack.LevenbergMarquardtMinimizer();
        //opt.setHook(new LmHook());
        for (int i = 0; i < 10; ++i) {
            nmodel = ((DynamicFactorModel.Ssf) pt.ssf).getModel();
            mapping = new DfmMapping(nmodel, false, true);
            fn = new MSsfFunction(data, mapping, algorithm);
            opt.setMaxIter(i == 0 ? 10 : 3);
            opt.minimize(fn, mapping.map(nmodel.ssfRepresentation()));
            pt = (MSsfFunctionInstance) opt.getResult();
            nmodel = ((DynamicFactorModel.Ssf) pt.ssf).getModel();
            mapping = new DfmMapping(nmodel, true, false);
            fn = new MSsfFunction(data, mapping, algorithm);
            opt.setMaxIter(3);
            opt.minimize(fn, mapping.map(pt.ssf));
            pt = (MSsfFunctionInstance) opt.getResult();
            nmodel = ((DynamicFactorModel.Ssf) pt.ssf).getModel();
            mapping = new DfmMapping(nmodel, false, false);
            fn = new MSsfFunction(data, mapping, algorithm);
            boolean ok = opt.minimize(fn, mapping.map(pt.ssf));
            pt = (MSsfFunctionInstance) opt.getResult();
//            if (ok)
//                break;
        }
        nmodel = ((DynamicFactorModel.Ssf) pt.ssf).getModel();
        pt = fn.ssqEvaluate(mapping.map(nmodel.ssfRepresentation()));
        System.out.println(pt.getLikelihood().getLogLikelihood());
        nmodel = nmodel.normalize();
        mapping = new DfmMapping(nmodel, false, false);
        fn = new MSsfFunction(data, mapping, algorithm);
        pt = fn.ssqEvaluate(mapping.map(nmodel.ssfRepresentation()));
        System.out.println(pt.getLikelihood().getLogLikelihood());
        System.out.println(new DataBlock(opt.getGradient()).nrm2());
        System.out.println(new DataBlock(pt.getParameters()));
        DfmMonitor nmonitor = new DfmMonitor(nmodel);
        nmonitor.setCalcVariance(false);
        nmonitor.setNormalizingVariance(false);
        nmonitor.process(s);

        for (int i = 0; i < nmodel.getTransition().nbloks; ++i) {
            DataBlock cmp = new DataBlock(nmonitor.getSmoothingResults().component(i * nmodel.getBlockLength()));
            cmp.sub(cmp.sum() / cmp.getLength());
            cmp.div(Math.sqrt(cmp.ssq() / cmp.getLength()));
            System.out.println(cmp);
        }
    }

    //  @Test
    public void testBfgs() {

        IMSsfData data = new MultivariateSsfData(dd.subMatrix(), null);
        MSsfAlgorithm algorithm = new MSsfAlgorithm();
        algorithm.useSsq(false);
        DynamicFactorModel nmodel = dmodel.normalize();
        DfmMapping mapping = new DfmMapping(nmodel);
        MSsfFunction fn = new MSsfFunction(data, mapping, algorithm);

        DataBlock def = mapping.getDefault();
        MSsfFunctionInstance evaluate = fn.ssqEvaluate(mapping.map(nmodel.ssfRepresentation()));
        //      MSsfFunctionInstance evaluate = fn.ssqEvaluate(def);
        //       System.out.println(evaluate.getLikelihood().getLogLikelihood());
        //       Optimizer opt = new Optimizer();
        BfgsMinimizer opt = new BfgsMinimizer();
        opt.minimize(fn, evaluate);
        evaluate = (MSsfFunctionInstance) opt.getResult();
        System.out.println(evaluate.getLikelihood().getLogLikelihood());
        System.out.println(new DataBlock(opt.getGradient()).nrm2());
        System.out.println(new DataBlock(opt.getResult().getParameters()));
    }
}

class Optimizer extends ec.tstoolkit.maths.realfunctions.levmar.LevenbergMarquardtMethod {

    @Override
    protected boolean iterate() {
        boolean rslt = super.iterate();
        MSsfFunctionInstance evaluate = (MSsfFunctionInstance) getResult();
        System.out.println(evaluate.getLikelihood().getLogLikelihood());
        return rslt;

    }
}

class BfgsMinimizer extends ec.tstoolkit.maths.realfunctions.riso.LbfgsMinimizer {

    @Override
    protected boolean next() {
        boolean rslt = super.next();
        System.out.println(getObjective());
        return rslt;

    }
}

class LmHook implements ILmHook {

    @Override
    public void hook(IEstimationProblem problem, boolean success) {
        if (success) {
            SsqEstimationProblem sp = (SsqEstimationProblem) problem;
            MSsfFunctionInstance evaluate = (MSsfFunctionInstance) sp.getResult();
            System.out.println(evaluate.getSsqE());//.getLikelihood().getLogLikelihood());
        }
    }
}
