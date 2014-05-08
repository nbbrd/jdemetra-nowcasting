/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tss.dfm;

import data.Data;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.dfm.DfmEM;
import ec.tstoolkit.dfm.DfmEM2;
import ec.tstoolkit.dfm.DfmInformationSet;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.dfm.IDfmEstimator;
import ec.tstoolkit.dfm.PcInitializer;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.ssf2.ResidualsCumulator;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataTable;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.tstoolkit.var.VarSpec;
import org.junit.Test;

/**
 *
 * @author deanton
 */
public class DfmResultsTest {

    public DfmResultsTest() {
    }

    // final: you cannot assign this name to another object
    static final DynamicFactorModel dmodel = new DynamicFactorModel(12, 3);
    static final int N = 500;
    static final boolean stressTest = false;

    static DfmInformationSet dfmdata;  // DAVID: static variables belong to the class and not any particular object in the class
    static DfmResults results;
    static TsData[] factors;
  
    static DfmEM em;
    static DfmEM2 em2;
    static IDfmEstimator estimator_;

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
        T = Data.readMatrix(DfmResultsTest.class, "/transition.csv");
        //System.out.println(T);
        TVar = Data.readMatrix(DfmResultsTest.class, "/tcovar.csv");
        //System.out.println(TVar);
        MVar = Data.readMatrix(DfmResultsTest.class, "/mcovar.csv");
        //System.out.println(MVar);
        D = Data.readMatrix(DfmResultsTest.class, "/data.csv");
        O = Data.readMatrix(DfmResultsTest.class, "/original.csv");
        //System.out.println(D);
        Z = Data.readMatrix(DfmResultsTest.class, "/loadings.csv");
        //System.out.println(Z);
        M = Data.readMatrix(DfmResultsTest.class, "/model.csv");
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

    @Test
    public void testSomeMethod() {
        
        
        DynamicFactorModel dmodelc = dmodel.clone();
        dmodelc.normalize();dmodelc.setInitialization(VarSpec.Initialization.Zero);
        PcInitializer initializer = new PcInitializer();
       // initializer.initialize(dmodelc, dfmdata);        
         
       double [] angles = new double[10];
       angles[0] = Math.PI;
       angles[1] = Math.PI;
       angles[2] = Math.PI;
       angles[3] = Math.PI;
       angles[4] = Math.PI;
       angles[5] = Math.PI;
       angles[6] = Math.PI;
       angles[7] = Math.PI;
       angles[8] = Math.PI;
       angles[9] = Math.PI;              


       
       Rotation R = new Rotation(angles);
       Matrix rot=R.getRotation();
       Matrix check = rot.times(rot.transpose() );
       System.out.println(check);
       
      em = new DfmEM();     
      em.estimate(dmodelc,dfmdata);  // public, can be tested
     // em2 = new DfmEM2(null);     
   //   em2.initialize(dmodelc,dfmdata);  
      
        results = new DfmResults(dmodelc, dfmdata);
        
        
        factors = new TsData[dmodelc.getFactorsCount()];
        TsDataTable tablefactors=new TsDataTable();
        for (int count = 0;
                count < dmodelc.getFactorsCount();
                count += 1) {

            factors[count] = results.getFactor(count);
            tablefactors.insert(-1, factors[count]);
        }
      
        System.out.println(tablefactors);
        
        // TESTING SHOCKS
        TsData[] shocks = results.getShocks();
      
        TsDataTable tableshocks=new TsDataTable();
        
                for (int count = 0;
                count < dmodelc.getFactorsCount();
                count += 1) {

            tableshocks.insert(-1, shocks[count]);
        }
      
        System.out.println(tableshocks);
        
                
        // TESTING NOISE
        TsData[] noise = results.getNoise();
      
        TsDataTable tablenoise=new TsDataTable();
        
                for (int count = 0;
                count < dmodelc.getMeasurementsCount();
                count += 1) {
                  tablenoise.insert(-1, noise[count]);
                 }
      
        System.out.println(tablenoise);
        
        
          // TESTING SIGNAL
        TsData[] signal = results.getSignal();
      
        TsDataTable tableSignal=new TsDataTable();
        
                for (int count = 0;
                count < dmodelc.getMeasurementsCount();
                count += 1) {
                  tableSignal.insert(-1, signal[count]);
                 }
      
        System.out.println(tableSignal);
        
        
          // TESTING DATA
          TsData[] theData = results.getTheData();
      
          TsDataTable tableData=new TsDataTable();
        
                for (int count = 0;
                count < dmodelc.getMeasurementsCount();
                count += 1) {
                  tableData.insert(-1, theData[count]);
                 }
      
           System.out.println(tableData);
        
        
           TsData[][] shockDec = results.getShocksDecomposition();
      
           TsDataTable tableShockDec=new TsDataTable();
        
                for (int v = 0;  v < dmodelc.getMeasurementsCount();v += 1) {
                    for (int i = 0;  i < dmodelc.getFactorsCount()+2; i++){
                    tableShockDec.insert(-1, shockDec[i][v]);
                   }
                }
                
                    
              System.out.println("historical shoc decomposition");
     
                System.out.println(tableShockDec);
                
                
              int[] hor = new int[29];
              hor[0]=1;
              hor[1]=2;
              hor[2]=3;
              hor[3]=4;
              hor[4]=5;
              hor[5]=6;
              hor[6]=7;
              hor[7]=8;
              hor[8]=9;
              hor[9]=10;
              hor[10]=11;
              hor[11]=12;
              hor[12]=14;
              hor[13]=16;
              hor[14]=18;
              hor[15]=20;
              hor[16]=24;
              hor[17]=28;
              hor[18]=32;
              hor[19]=36;
              hor[20]=40;              
              hor[21]=48;
              hor[22]=60;
              hor[23]=72;
              hor[24]=84;
              hor[25]=96;
              hor[26]=120;
              hor[27]=240;
              hor[28]=1000;
              
      
              
//              Matrix[]  varianceDecomposition = new Matrix[hor.length]; 
                
             int pib   = 27;
             int shock = 1;
             Matrix varDecShock0 = results.getVarianceDecompositionShock(hor, 0);
             Matrix varDecIdx   = results.getVarianceDecompositionIdx(hor, pib);
             Matrix irf         = results.getIrfIdx(hor, pib);
                 
           //   Matrix varDecIdx= results.getVarianceDecompositionIdx(hor, pib);
                     
                 
              System.out.println("variance decomposition");
          
          System.out.println(varDecIdx);  // one variable (all shocks)
          //    System.out.println(varDecShock0); // all variables (one shock)
           //   System.out.println(irf);
            
            
            
            Matrix idCorr =results.getIdiosyncratic();
            
            System.out.println(idCorr) ;
                
           }
    
     
            
}

   
   