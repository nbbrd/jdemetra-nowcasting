/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.data.LogSign;
import ec.tstoolkit.data.Table;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.HouseholderR;
import ec.tstoolkit.maths.matrices.LowerTriangularMatrix;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.MatrixException;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import ec.tstoolkit.maths.realfunctions.DefaultDomain;
import ec.tstoolkit.maths.realfunctions.IFunction;
import ec.tstoolkit.maths.realfunctions.IFunctionDerivatives;
import ec.tstoolkit.maths.realfunctions.IFunctionInstance;
import ec.tstoolkit.maths.realfunctions.IParametersDomain;
import ec.tstoolkit.maths.realfunctions.NumericalDerivatives;
import ec.tstoolkit.maths.realfunctions.bfgs.Bfgs;
import ec.tstoolkit.maths.realfunctions.riso.LbfgsMinimizer;
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
    private int maxiter_ = 1000, iter_;
    private boolean all_ = true;

    public DfmEM2(IDfmInitializer initializer) {
        this.initializer = initializer;
        processor.setCalcVariance(true);
    }

    public boolean isEstimatingAll() {
        return all_;
    }

    public void setEstimateVar(boolean var) {
        all_ = var;
    }

    public int GetMaxIter() {
        return maxiter_;
    }

    public void setMaxIter(int i) {
        maxiter_ = i;
    }

    int size() {
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
            DynamicFactorModel.MeasurementType type = DynamicFactorModel.
                    getMeasurementType(desc.type);
            if (!G.containsKey(type)) {
                int len = desc.type.getLength();
                DataBlock z = new DataBlock(len);
                desc.type.fill(z);
                HashMap<Integer, DataBlock> g = new HashMap<Integer, DataBlock>();
                HashMap<Integer, DataBlock> g2 = new HashMap<Integer, DataBlock>();
                for (int i = 0, j = 0; i < dfm.getFactorsCount(); ++i, j
                        += dfm.getBlockLength()) {
                    DataBlock column = new DataBlock(n);
                    for (int k = 0; k < len; ++k) {
                        column.addAY(z.get(k), ef(j + k));
                    }
                    g.put(i, column);
                    for (int k = 0, l = 0; k < dfm.getFactorsCount(); ++k, l
                            += dfm.getBlockLength()) {
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
        processor.getSmoothingResults().setStandardError(1);
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
        if (all_) {
            mvar();
        }
    }

    private void mloadings() {
        // maximise loading
        int i = 0;
        for (DynamicFactorModel.MeasurementDescriptor mdesc
                : dfm.getMeasurements()) {
            int n = size();
            DynamicFactorModel.MeasurementType type = DynamicFactorModel.
                    getMeasurementType(mdesc.type);
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
                            double gj = g.get(j).get(z);
                            ee -= 2 * cj * gj * yz;
                            for (int k = 0; k < mdesc.coeff.length; ++k) {
                                double ck = mdesc.coeff[k];
                                if (!Double.isNaN(ck)) {
                                    ee += g2.get(j + k * dfm.getFactorsCount())
                                            .get(z) * cj * ck;
                                }
                            }
                        }
                    }
                }
            }
            mdesc.var = ee / nobs;
        }
    }

    private void mvar() {
        Bfgs bfgs = new Bfgs();
        bfgs.setMaxIter(10);
        LL2 fn = new LL2();
        bfgs.minimize(fn, fn.current());
        LL2.Instance ofn = (LL2.Instance) bfgs.getResult();
        if (ofn != null) {
            dfm.getTransition().varParams.copy(ofn.V);
            dfm.getTransition().covar.copy(SymmetricMatrix.XXt(ofn.lQ));
        }
//        LL2 fn = new LL2();
//        LL2.Instance ofn = fn.current();
//        // search the descent direction
//        IFunctionDerivatives derivatives = fn.getDerivatives(ofn);
//        DataBlock gradient = new DataBlock(derivatives.getGradient());
//        DataBlock parameters = new DataBlock(ofn.getParameters());
//        double step =1 / gradient.nrm2();
//        double f0 = ofn.getValue(), f1;
//        int i = 0;
//        do {
//            step /= 2;
//            DataBlock x = parameters.deepClone();
//            x.addAY(-step, gradient);
//            ofn = fn.evaluate(x);
//            f1 = ofn.getValue();
//        } while (i++ < 20 && f0 <= f1);
//
//        if (i < 20) {
//            dfm.getTransition().varParams.copy(ofn.V);
//            dfm.getTransition().covar.copy(SymmetricMatrix.XXt(ofn.lQ));
//        }else
//             System.out.println();
    }

    // Real function corresponding to the second part of the likelihood
    class LL2 implements IFunction {

        private final Table<Matrix> allK;
        private final Matrix lQ0;

        LL2() {
            lQ0 = dfm.getTransition().covar.clone();
            SymmetricMatrix.lcholesky(lQ0);
            // computes  results independent of the VAR parameters: K(i,j) = sum(f(i,j), t)
            int p = 1 + dfm.getTransition().nlags;
            allK = new Table<>(p, p);
            for (int i = 0; i < p; ++i) {
                allK.set(i, i, calcK(i, i));
                for (int j = 0; j < i; ++j) {
                    Matrix m = calcK(i, j);
                    allK.set(i, j, m);
                    allK.set(j, i, m.transpose());
                }
            }
        }

        Instance current() {
            return new Instance();
        }

        Matrix K(int i, int j) {
            return allK.get(i, j);
        }

        /**
         * computes sum(t|f(i+k*len, j+l*len)
         *
         * @param i The first lag
         * @param j The second lag
         * @return
         */
        private Matrix calcK(int i, int j) {
            int n = dfm.getFactorsCount();
            int len = dfm.getBlockLength();
            Matrix K = new Matrix(n, n);
            for (int k = 0; k < n; ++k) {
                for (int l = 0; l < n; ++l) {
                    K.set(k, l, ef(i + k * len, j + l * len).sum());
                }
            }
            return K;
        }

        @Override
        public Instance evaluate(IReadDataBlock parameters) {
            return new Instance(parameters);
        }

        @Override
        public IFunctionDerivatives getDerivatives(IFunctionInstance point) {
            return new NumericalDerivatives(this, point, true, true);
        }

        @Override
        public IParametersDomain getDomain() {
            int nf = dfm.getFactorsCount();
            int nl = dfm.getTransition().nlags;
            return new DefaultDomain(nf * nf * nl + nf * (nf + 1) / 2, 1e-6);
        }

        public class Instance implements IFunctionInstance {

            private final Matrix lQ, V; // lQ = cholesky factor of Q
            private final DataBlock p;
            private final double val;
            private final Matrix lv0;
            private final Matrix[] LA;

            public Instance(IReadDataBlock p) {
                this.p = new DataBlock(p);
                int nf = dfm.getFactorsCount();
                int nl = dfm.getTransition().nlags;
                V = new Matrix(nf, nf * nl);
                int vlen = V.internalStorage().length;
                this.p.range(0, vlen).copyTo(V.internalStorage(), 0);
                this.lQ = new Matrix(nf, nf);
                for (int i = 0, j = vlen; i < nf; j += nf - i, i++) {
                    lQ.column(i).drop(i, 0).copy(this.p.range(j, j + nf - i));
                }
                lv0 = calclv0();
                LA = calcLA();
                val = calc();
            }

            public Instance() {
                this.V = dfm.getTransition().varParams.clone();
                int nf = dfm.getFactorsCount();
                this.lQ = lQ0.clone();
                int vlen = V.internalStorage().length;
                p = new DataBlock(vlen + nf * (nf + 1) / 2);
                p.range(0, vlen).copyFrom(V.internalStorage(), 0);
                for (int i = 0, j = vlen; i < nf; j += nf - i, i++) {
                    p.range(j, j + nf - i).copy(lQ.column(i).drop(i, 0));
                }
                lv0 = calclv0();
                LA = calcLA();
                val = calc();
            }

            private double calc() {
                double v0 = calcdetv0();
                double v1 = calcssq0();
                double v2 = calcdetq();
                double v3 = calcssq();
                //return (v0 + v1 + v2 + v3) / (data.getCurrentDomain()
                //        .getLength() * dfm.getFactorsCount() * dfm.getTransition().nlags);
                return (v2 + v3) / (data.getCurrentDomain().getLength() * 
                        dfm.getFactorsCount() * dfm.getTransition().nlags);
                // return v2;
            }

            private Matrix A(int i) {
                int nl = dfm.getTransition().nlags;
                int nf = dfm.getFactorsCount();
                if (i == 0) {
                    return Matrix.identity(nf);
                } else {
                    Matrix a = new Matrix(nf, nf);
                    for (int j = 0; j < nf; ++j) {
                        a.column(j).copy(V.column(i - 1 + j * nl));
                    }
                    a.chs();
                    return a;
                }
            }

            /**
             * AQA = A'(i)*Q^-1*A(j) = A"(i)*L'^-1*L^-1*A
             *
             * @param i
             * @param j
             * @return
             */
            @Override
            public IReadDataBlock getParameters() {
                return p;
            }

            @Override
            public double getValue() {
                return val; //To change body of generated methods, choose Tools | Templates.
            }

            private double calcssq0() {
                if (lv0 == null) {
                    return Double.MAX_VALUE / 4;
                }
                // computes f0*f0 x V^-1
                Matrix lower = lv0.clone();
                lower = LowerTriangularMatrix.inverse(lower);
                Matrix iv0 = SymmetricMatrix.XtX(lower);
                int nl = dfm.getTransition().nlags;
                int nf = dfm.getFactorsCount();
                int len = dfm.getBlockLength();
                double ssq0 = 0;
                SubMatrix P = processor.getSmoothingResults().P(0);
                for (int i = 0; i < nf; ++i) {
                    for (int j = 1; j < nl; ++j) {
                        for (int k = 1; k < nl; ++k) {
                            double ejk = P.get(i * len + j, i * len + k);
                            ssq0 += iv0.get(i * nl + j - 1, i * nl + k - 1) * ejk;
                        }
                    }
                }
                return ssq0;
            }

            private double calcdetv0() {
                if (lv0 == null) {
                    return Double.MAX_VALUE / 4;
                }
                LogSign sumLog = lv0.diagonal().sumLog();
                return 2 * sumLog.value;
            }

            private double calcdetq() {
                if (lQ == null) {
                    return Double.MAX_VALUE / 4;
                }
                LogSign sumLog = lQ.diagonal().sumLog();
                return data.getCurrentDomain().getLength() * 2 * sumLog.value;
            }

            private double calcssq() {
                int nl = 1 + dfm.getTransition().nlags;
                int nf = dfm.getFactorsCount();
                double ssq = 0;
                for (int i = 0; i < nl; ++i) {
                    for (int j = 0; j < nl; ++j) {
                        Matrix aqa = new Matrix(nf, nf);
                        aqa.subMatrix().product(LA[i].subMatrix().transpose(),
                                LA[j].subMatrix());
                        ssq += K(i, j).dot(aqa);
                    }
                }
                return ssq;
            }

            private Matrix calclv0() {
                // compute the initial covar. We could reuse the code of DynamicFactorModel
                int nl = dfm.getTransition().nlags;
                int nf = dfm.getFactorsCount();
                // We have to solve the steady state equation:
                // V = T V T' + Q
                // We consider the nlag*nb, nlag*nb sub-system
                Matrix q = SymmetricMatrix.XXt(lQ);
                int n = nf * nl;
                Matrix cov = new Matrix(n, n);
                int np = (n * (n + 1)) / 2;
                Matrix M = new Matrix(np, np);
                double[] b = new double[np];
                // fill the matrix
                for (int c = 0, i = 0; c < n; ++c) {
                    for (int r = c; r < n; ++r, ++i) {
                        M.set(i, i, 1);
                        if (r % nl == 0 && c % nl == 0) {
                            b[i] = q.get(r / nl, c / nl);
                        }
                        for (int k = 0; k < n; ++k) {
                            for (int l = 0; l < n; ++l) {
                                double zr = 0, zc = 0;
                                if (r % nl == 0) {
                                    zr = V.get(r / nl, l);
                                } else if (r == l + 1) {
                                    zr = 1;
                                }
                                if (c % nl == 0) {
                                    zc = V.get(c / nl, k);
                                } else if (c == k + 1) {
                                    zc = 1;
                                }
                                double z = zr * zc;
                                if (z != 0) {
                                    int p = l <= k ? pos(k, l, n) : pos(l, k, n);
                                    M.add(i, p, -z);
                                }
                            }
                        }
                    }
                }
                HouseholderR hous = new HouseholderR(false);
                hous.decompose(M);
                double[] solve = hous.solve(b);
                for (int i = 0, j = 0; i < n; i++) {
                    cov.column(i).drop(i, 0).copyFrom(solve, j);
                    j += n - i;
                }
                SymmetricMatrix.fromLower(cov);
                try {
                    SymmetricMatrix.lcholesky(cov);
                    return cov;
                } catch (MatrixException err) {
                    return null;
                }
            }

            private Matrix[] calcLA() {
                Matrix[] M = new Matrix[1 + dfm.getTransition().nlags];
                for (int i = 0; i < M.length; ++i) {
                    Matrix ai = A(i);
                    // L^-1*A = B or  L B = A
                    LowerTriangularMatrix.rsolve(lQ, ai.subMatrix());
                    M[i] = ai;
                }
                return M;
            }
        }
    }

    private static int pos(int r, int c, int n) {
        return r + c * (2 * n - c - 1) / 2;
    }
}
