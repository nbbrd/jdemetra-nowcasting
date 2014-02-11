/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import data.Data;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.ssf2.ResidualsCumulator;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.tstoolkit.var.VarSpec;
import org.junit.Test;

/**
 *
 * @author deanton 
 */
public class DfmEMTest {

    // final: you cannot assign this name to another object
    static final DynamicFactorModel dmodel = new DynamicFactorModel(12, 3);
    static final int N = 500;
    static final boolean stressTest = false;

    static DfmInformationSet dfmdata;  // DAVID: static variables belong to the class and not any particular object in the class
    
    static  DfmEM em;
    
    public static void evaluate(final ResidualsCumulator rslts,
            final Likelihood ll) {
        int n = rslts.getObsCount();
        double ssqerr = rslts.getSsqErr(), ldet = rslts.getLogDeterminant();
        ll.set(ssqerr, ldet, n);
    }
    private static Matrix T, TVar, MVar, D, O, Z, M, dd, ddrnd;

    // why static method? 
    private static void loadData() {   // DAVID: use a TsData[] to create a dfmdata object

        TsData[] input = new TsData[dd.getRowsCount()];
        TsPeriod start = new TsPeriod(TsFrequency.Monthly, 1980, 0);
        for (int i = 0; i < input.length; ++i) {
            input[i] = new TsData(start, dd.row(i));
        }

        dfmdata = new DfmInformationSet(input); // input is TsData[]                    
        
    }

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
//                boolean log = M.get(i, 4) != 0, diff = M.get(i, 5) != 0;
//                if (log) {
//                    for (int k = 0; k < row.getLength(); ++k) {
//                        double w = row.get(k);
//                        if (DescriptiveStatistics.isFinite(w)) {
//                            row.set(k, Math.log(w));
//                        }
//                    }
//                }
//                if (diff) {
//                    double prev = row.get(0);
//                    for (int k = 1; k < row.getLength(); ++k) {
//                        double w = row.get(k);
//                        if (DescriptiveStatistics.isFinite(w)) {
//                            if (DescriptiveStatistics.isFinite(prev)) {
//                                row.set(k, w - prev);
//                            } else {
//                                row.set(k, Double.NaN);
//                            }
//                        }
//                        prev = w;
//                    }
//                }
//                double mm = M.get(i, 7), ee = M.get(i, 6);
                row.sub(m);
                row.mul(1 / e);
//                row.sub(mm);
//                row.mul(1 / ee);
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

    static {     //  static class; it will always "construct" whatever is inside
        loadDavidModel(); // first the model
        loadData();
    }

 //   public DfmEMTest() {
  //  }

    @Test
   public void testinitCalc() {
        DynamicFactorModel dmodelc = dmodel.clone();
        dmodelc.normalize();
        PcInitializer initializer = new PcInitializer();
        initializer.initialize(dmodelc, dfmdata);
        
   em = new DfmEM();  
   
  // em.initCalc(dmodel, dfmdata);
     //em.emstep(dmodel, dfmdata);
  // em.allcomponents();
  // em.emstep(dmodel,dfmdata) ;   
  // em.initCalc(dmodel, dfmdata);  // private, cannot be tested
 //  em.calc(dmodel,dfmdata);       // private, cannot be tested 
   em.initialize(dmodelc,dfmdata);  // public, can be tested
    }
    
       

}
