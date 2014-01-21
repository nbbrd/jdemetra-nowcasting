/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import ec.tstoolkit.maths.realfunctions.IFunction;
import ec.tstoolkit.maths.realfunctions.IFunctionDerivatives;
import ec.tstoolkit.maths.realfunctions.IFunctionInstance;
import ec.tstoolkit.maths.realfunctions.IParametersDomain;
import ec.tstoolkit.ssf2.ResidualsCumulator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jean
 */
public class DfmEM2 implements IDfmInitializer {

    private IDfmInitializer initializer;
    private DfmProcessor processor = new DfmProcessor();
    private DynamicFactorModel dfm;
    private DfmInformationSet data;
    private Matrix M;
    private final EnumMap<DynamicFactorModel.MeasurementType, Map<Integer, DataBlock>> G = new EnumMap<>(DynamicFactorModel.MeasurementType.class);
    private final EnumMap<DynamicFactorModel.MeasurementType, Map<Integer, DataBlock>> G2 = new EnumMap<>(DynamicFactorModel.MeasurementType.class);
    private final Map<Integer, DataBlock> Efij = new HashMap<>();
    private int maxiter_ = 200, iter_;

    public DfmEM2(IDfmInitializer initializer) {
        this.initializer = initializer;
        processor.setCalcVariance(true);
    }

    private int size() {
        return data.getCurrentDomain().getLength();
    }

    private DataBlock ef(int i) {
        return processor.getSmoothingResults().getSmoothedStates().item(i);
    }

    private DataBlock vf(int i, int j) {
        return new DataBlock(processor.getSmoothingResults().componentCovar(i, j));
    }

    private DataBlock ef(int i, int j) {
        int idx = dfm.getBlockLength() * dfm.getFactorsCount() * j + i;
        DataBlock cur = Efij.get(idx);
        if (cur == null) {
            cur = vf(i, j);
            cur.addAXY(1, ef(i), ef(j));
            Efij.put(idx, cur);
        }
        return cur;
    }

    private void calcG() {
        G.clear();
        G2.clear();
        Efij.clear();
        int n = size();
        for (DynamicFactorModel.MeasurementDescriptor desc : dfm.getMeasurements()) {
            DynamicFactorModel.MeasurementType type = DynamicFactorModel.getMeasurementType(desc.type);
            if (!G.containsKey(type)) {
                int len = desc.type.getLength();
                DataBlock z = new DataBlock(len);
                desc.type.fill(z);
                HashMap<Integer, DataBlock> g = new HashMap<Integer, DataBlock>();
                HashMap<Integer, DataBlock> g2 = new HashMap<Integer, DataBlock>();
                for (int i = 0, j = 0; i < dfm.getFactorsCount(); ++i, j += dfm.getBlockLength()) {
                    DataBlock column = new DataBlock(n);
                    for (int k = 0; k < len; ++k) {
                             column.addAY(z.get(k), ef(j + k));
                     }
                    g.put(i, column);
                    for (int k = 0, l = 0; k < dfm.getFactorsCount(); ++k, l += dfm.getBlockLength()) {
                        DataBlock column2 = new DataBlock(n);
                        for (int pr = 0; pr < len; ++pr) {
                            double zr = z.get(pr);
                            if (zr != 0) {
                                for (int pc = 0; pc < len; ++pc) {
                                    double zc = z.get(pc);
                                    if (zc != 0) {
                                        column2.addAY(zr * zc, ef(j + pr, l + pc));
                                    }
                                }
                            }
                        }
                        g2.put(i + k * dfm.getFactorsCount(), column2);
                    }
                    G.put(type, g);
                    G2.put(type, g2);
                }
            }
        }
    }

    @Override
    public boolean initialize(DynamicFactorModel dfm, DfmInformationSet data
    ) {
        this.data = data;
        this.dfm = dfm;
        M = data.generateMatrix(null);
        if (initializer != null) {
            initializer.initialize(dfm, data);
        }
        iter_ = 0;
        do {
            EStep();
            MStep();
        } while (iter_++ < maxiter_);
        return true;
    }

    private void EStep() {
        if (!processor.process(dfm, data)) {
            return;
        }
        Likelihood ll = new Likelihood();
        evaluate(processor.getFilteringResults(), ll);
        System.out.println(ll.getLogLikelihood());
//        processor.getSmoothingResults().setStandardError(1);
        calcG();
    }

    public static void evaluate(final ResidualsCumulator rslts,
            final Likelihood ll) {
        int n = rslts.getObsCount();
        double ssqerr = rslts.getSsqErr(), ldet = rslts.getLogDeterminant();
        ll.set(ssqerr, ldet, n);
    }

    private void MStep() {
        mloadings();
    }

    private void mloadings() {
        // maximise loading
        int i = 0;
        for (DynamicFactorModel.MeasurementDescriptor mdesc : dfm.getMeasurements()) {
            int n = size();
            DynamicFactorModel.MeasurementType type = DynamicFactorModel.getMeasurementType(mdesc.type);
            Map<Integer, DataBlock> g = G.get(type);
            Map<Integer, DataBlock> g2 = G2.get(type);
            DataBlock y = M.column(i++);
            int nobs = 0;
            for (int k = 0; k < n; ++k) {
                if (!Double.isNaN(y.get(k))) {
                    ++nobs;
                }
            }
            double[] gy = new double[mdesc.getUsedFactorsCount()];
            Matrix G2 = new Matrix(gy.length, gy.length);
            for (int j = 0, u = 0; j < mdesc.coeff.length; ++j) {
                if (!Double.isNaN(mdesc.coeff[j])) {
                    DataBlock gj = g.get(j);
                    for (int z = 0; z < n; ++z) {
                        double yz = y.get(z);
                        if (!Double.isNaN(yz)) {
                            gy[u] += gj.get(z) * yz;
                        }
                    }
                    for (int k = 0, v = 0; k <= j; ++k) {
                        if (!Double.isNaN(mdesc.coeff[k])) {
                            DataBlock g2jk = g2.get(j + k * dfm.getFactorsCount());
                            for (int z = 0; z < n; ++z) {
                                double yz = y.get(z);
                                if (!Double.isNaN(yz)) {
                                    G2.add(u, v, g2jk.get(z));
                                }
                            }
                            ++v;
                        }
                    }
                    ++u;
                }
            }
            SymmetricMatrix.fromLower(G2);
            // C = G/GG or C * GG = G 
            SymmetricMatrix.solve(G2, new DataBlock(gy), false);
            for (int j = 0, u = 0; j < mdesc.coeff.length; ++j) {
                if (!Double.isNaN(mdesc.coeff[j])) {
                    mdesc.coeff[j] = gy[u++];
                }
            }
            double ee = 0;
            for (int z = 0; z < n; ++z) {
                double yz = y.get(z);
                if (!Double.isNaN(yz)) {
                    ee += yz * yz;
                    for (int j = 0; j < mdesc.coeff.length; ++j) {
                        double cj = mdesc.coeff[j];
                        if (!Double.isNaN(cj)) {
                            double gk = g.get(j).get(z);
                            ee -= 2 * cj * gk * yz;
                            for (int k = 0; k < mdesc.coeff.length; ++k) {
                                double ck = mdesc.coeff[k];
                                if (!Double.isNaN(ck)) {
                                    ee += g2.get(j + k * dfm.getFactorsCount()).get(z) * cj * ck;
                                }
                            }
                        }
                    }
                }
            }
            mdesc.var = ee / nobs;
        }
    }
    
//    // Real function corresponding to the second part of the likelihood
//    public class LL2 implements IFunction{
//
//        @Override
//        public IFunctionInstance evaluate(IReadDataBlock parameters) {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//
//        @Override
//        public IFunctionDerivatives getDerivatives(IFunctionInstance point) {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//
//        @Override
//        public IParametersDomain getDomain() {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//    }
//    
//    public class LL2Instance implements IFunctionInstance {
//
//        @Override
//        public IReadDataBlock getParameters() {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//
//        @Override
//        public double getValue() {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//         }
//    
//    }
  }
