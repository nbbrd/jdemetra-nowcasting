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
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.dfm.DfmEstimator.IEstimationHook;
import ec.tstoolkit.dfm.DynamicFactorModel.TransitionDescriptor;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.Matrix;
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
import org.junit.Test;

/**
 *
 * @author Jean Palate
 */
public class DfmMonitorTest {

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
                DynamicFactorModel.MeasurementDescriptor desc = new DynamicFactorModel.MeasurementDescriptor(
                        measurement((int) M.get(i, 0)), q, MVar.get(j, j));
                dmodel.addMeasurement(desc);
                ++j;
            }
        }
        ddrnd = dd.clone();
        ddrnd.randomize();
        dmodel.setInitialization(DynamicFactorModel.Initialization.Zero);
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
        DfmMonitor monitor = new DfmMonitor();
        monitor.process(dmodel, s);
        double[] component = monitor.getSmoothingResults().component(0);

        Likelihood ll = new Likelihood();
        evaluate(monitor.getFilteringResults(), ll);
        System.out.println(ll.getLogLikelihood());
        DynamicFactorModel nmodel = dmodel.clone();
        nmodel.normalize();
        monitor.process(nmodel, s);
        double[] ncomponent = monitor.getSmoothingResults().component(0);

        Likelihood nll = new Likelihood();
        evaluate(monitor.getFilteringResults(), nll);
        System.out.println(nll.getLogLikelihood());

//        for (int i=0; i<ncomponent.length; ++i){
//            System.out.print(component[i]);
//            System.out.print('\t');
//            System.out.println(ncomponent[i]);
//            
//        }
    }

    @Test
    public void testDavidModel() {

        final TsData[] s = new TsData[dd.getRowsCount()];
        TsPeriod start = new TsPeriod(TsFrequency.Monthly, 1980, 0);
        for (int i = 0; i < s.length; ++i) {
            s[i] = new TsData(start, dd.row(i));
        }
        DynamicFactorModel dmodelc = dmodel.clone();
        dmodelc.normalize();
        System.out.println(new DfmMapping(dmodelc).parameters());
//        dmodelc.setTransition(new TransitionDescriptor(3, 2));
//        dmodelc = dmodelc.compactFactors(0, 2);
        DfmMonitor monitor = new DfmMonitor();
        PcInitializer initializer = new PcInitializer();
//        initializer.setEstimationDomain(s[0].getDomain().drop(120,12));
        DfmEstimator estimator = new DfmEstimator(new IEstimationHook() {
            int i = 0;

            @Override
            public boolean next(DynamicFactorModel current, Likelihood ll) {
                System.out.print(++i);
                System.out.print('\t');
                System.out.print(ll.getLogLikelihood());
                System.out.print('\t');
                System.out.println(new DfmMapping(current).parameters());
                return true;
            }
        });
 //       estimator.setMaxIter(100);
 //       estimator.setMaxInitialIter(2);
        monitor.setInitializer(initializer);
        //        monitor.setInitializer(new DefaultInitializer());
        monitor.setEstimator(estimator);
        monitor.process(dmodelc, s);
        dmodelc.normalize();
        for (int i = 0; i < dmodelc.getFactorsCount(); ++i) {
            DataBlock cmp = new DataBlock(monitor.getSmoothingResults().component(i * dmodelc.getBlockLength()));
            cmp.sub(cmp.sum() / cmp.getLength());
            cmp.div(Math.sqrt(cmp.ssq() / cmp.getLength()));
            System.out.println(cmp);
        }
        System.out.println(estimator.getGradient());
        IReadDataBlock map = new DfmMapping2(dmodelc).map(dmodelc);
        System.out.println(map);
    }
}
