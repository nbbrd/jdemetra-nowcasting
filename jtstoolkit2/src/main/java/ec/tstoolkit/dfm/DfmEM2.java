/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import ec.tstoolkit.algorithm.IProcessingHook;
import ec.tstoolkit.algorithm.ProcessingHookProvider;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.data.LogSign;
import ec.tstoolkit.data.Table;
import ec.tstoolkit.eco.Likelihood;
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
import ec.tstoolkit.maths.realfunctions.riso.LbfgsMinimizer;
import ec.tstoolkit.mssf2.MFilter;
import ec.tstoolkit.mssf2.MPredictionErrorDecomposition;
import ec.tstoolkit.mssf2.MultivariateSsfData;
import ec.tstoolkit.ssf2.ResidualsCumulator;
import ec.tstoolkit.var.VarSpec;
import java.util.EnumMap;

/**
 *
 * @author Jean
 */
public class DfmEM2 extends ProcessingHookProvider<DfmEM2, DynamicFactorModel> implements IDfmInitializer {

    private IDfmInitializer initializer;
    private DfmProcessor processor = new DfmProcessor();
    private DynamicFactorModel dfm;
    private DfmInformationSet data;
    private Matrix M;
    private final EnumMap<DynamicFactorModel.MeasurementType, DataBlock[]> G = new EnumMap<>(DynamicFactorModel.MeasurementType.class);
    private final EnumMap<DynamicFactorModel.MeasurementType, Table<DataBlock>> G2 = new EnumMap<>(DynamicFactorModel.MeasurementType.class);
    private DataBlock Efij[];
    private int maxiter_ = 1000, iter_;
    private boolean all_ = true, correctStart_ = true;
    private int modelSize;
    private int dataSize;
    private double ll_;
    private int numiter_ = 50;

    public DfmEM2(IDfmInitializer initializer) {
        this.initializer = initializer;
        processor.setCalcVariance(true);
    }

    public double getFinalLogLikelihood() {
        return ll_;
    }

    public boolean isEstimatingAll() {
        return all_;
    }

    public void setEstimateVar(boolean var) {
        all_ = var;
    }

    public boolean isCorrectingInitialVariance() {
        return correctStart_;
    }

    public void setCorrectingInitialVariance(boolean correct) {
        correctStart_ = correct;
    }

    public int getMaxIter() {
        return maxiter_;
    }

    public void setMaxIter(int i) {
        maxiter_ = i;
    }

    public int getMaxNumericIter() {
        return numiter_;
    }

    public void setMaxNumericIter(int i) {
        numiter_ = i;
    }

    private DataBlock ef(int i) {
        return processor.getSmoothingResults().getSmoothedStates().item(i);
    }

    private DataBlock vf(int i, int j) {
        return new DataBlock(processor.getSmoothingResults().componentCovar(i, j));
    }

    private DataBlock ef(int i, int j) {
        if (i > j) {
            return ef(j, i);
        }
        int idx = i + modelSize * j;
        DataBlock cur = Efij[idx];
        if (cur == null) {
            cur = vf(i, j);
            cur.addAXY(1, ef(i), ef(j));
            Efij[idx] = cur;
        }
        return cur;
    }

    private void calcG() {
        G.clear();
        G2.clear();
        for (int i = 0; i < Efij.length; ++i) {
            Efij[i] = null;
        }
        for (DynamicFactorModel.MeasurementDescriptor desc : dfm.getMeasurements()) {
            DynamicFactorModel.MeasurementType type = DynamicFactorModel.
                    getMeasurementType(desc.type);
            if (!G.containsKey(type)) {
                int len = desc.type.getLength();
                DataBlock z = new DataBlock(len);
                desc.type.fill(z);
                int nf = dfm.getFactorsCount();
                DataBlock[] g = new DataBlock[nf];
                Table<DataBlock> g2 = new Table<>(nf, nf);
                int nb = dfm.getBlockLength();
                for (int i = 0, j = 0; i < nf; ++i, j += nb) {
                    DataBlock column = new DataBlock(dataSize);
                    for (int k = 0; k < len; ++k) {
                        column.addAY(z.get(k), ef(j + k));
                    }
                    g[i] = column;
                }
                for (int i = 0, j = 0; i < nf; ++i, j += nb) {
                    for (int k = 0, l = 0; k <= i; ++k, l += nb) {
                        DataBlock column2 = new DataBlock(dataSize);
                        for (int pr = 0; pr < len; ++pr) {
                            double zr = z.get(pr);
                            if (zr != 0) {
                                for (int pc = 0; pc < len; ++pc) {
                                    double zc = z.get(pc);
                                    if (zc != 0) {
                                        column2.addAY(zr * zc, vf(j + pr, l + pc));
                                    }
                                }
                            }
                        }
                        column2.addAXY(1, g[i], g[k]);
                        g2.set(i, k, column2);
                        if (i != k) {
                            g2.set(k, i, column2);
                        }

                    }
                }

                G.put(type, g);
                G2.put(type, g2);
            }
        }
    }

    @Override
    public boolean initialize(DynamicFactorModel rdfm, DfmInformationSet data) {
        this.data = data;
        if (rdfm.getBlockLength()==rdfm.getTransition().nlags){
            dfm=rdfm.clone();
            dfm.setBlockLength(rdfm.getBlockLength()+1);
        }else
            this.dfm=rdfm;
        modelSize = dfm.getBlockLength() * dfm.getFactorsCount();
        dataSize = data.getCurrentDomain().getLength();
        Efij = new DataBlock[modelSize * modelSize];
        M = data.generateMatrix(null);
        if (initializer != null) {
            initializer.initialize(dfm, data);
        }
        iter_ = 0;
        ll_ = 0;
        filter(true);
        while (iter_++ < maxiter_) {
            if (!EStep()) {
                break;
            }
            MStep();
        }

        // finishing
        dfm.normalize();
        if (rdfm != dfm){
            rdfm.copy(dfm);
        }
        filter(false);
        return true;
    }

    private void filter(boolean adjust) {
        try {
            MFilter filter = new MFilter();
            MPredictionErrorDecomposition results = new MPredictionErrorDecomposition(true);
            filter.process(dfm.ssfRepresentation(), new MultivariateSsfData(data.generateMatrix(null).subMatrix().transpose(), null), results);
            Likelihood ll = new Likelihood();
            evaluate(results, ll);
            ll_ = ll.getLogLikelihood();
            if (adjust) {
                dfm.rescaleVariances(ll.getSigma());
                dfm.normalize();
            }
        } catch (RuntimeException err) {
        }
    }

    private boolean EStep() {
        if (!processor.process(dfm, data)) {
            return false;
        }
        Likelihood ll = new Likelihood();
        evaluate(processor.getFilteringResults(), ll);
        ll_ = ll.getLogLikelihood();
        IProcessingHook.HookInformation<DfmEM2, DynamicFactorModel> hinfo
                = new IProcessingHook.HookInformation<>(this, dfm);
        this.processHooks(hinfo, all_);
        if (hinfo.cancel) {
            return false;
        }
        calcG();
        return true;
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
            DynamicFactorModel.MeasurementType type = DynamicFactorModel.
                    getMeasurementType(mdesc.type);
            DataBlock[] g = G.get(type);
            Table<DataBlock> g2 = G2.get(type);
            DataBlock y = M.column(i++);
            int nobs = 0;
            for (int k = 0; k < dataSize; ++k) {
                if (!Double.isNaN(y.get(k))) {
                    ++nobs;
                }
            }
            double[] gy = new double[mdesc.getUsedFactorsCount()];
            Matrix G2 = new Matrix(gy.length, gy.length);
            for (int j = 0, u = 0; j < mdesc.coeff.length; ++j) {
                if (!Double.isNaN(mdesc.coeff[j])) {
                    DataBlock gj = g[j];
                    for (int z = 0; z < dataSize; ++z) {
                        double yz = y.get(z);
                        if (!Double.isNaN(yz)) {
                            gy[u] += gj.get(z) * yz;
                        }
                    }
                    for (int k = 0, v = 0; k <= j; ++k) {
                        if (!Double.isNaN(mdesc.coeff[k])) {
                            double x = 0;
                            DataBlock g2jk = g2.get(j, k);
                            for (int z = 0; z < dataSize; ++z) {
                                double yz = y.get(z);
                                if (!Double.isNaN(yz)) {
                                    x += g2jk.get(z);
                                }
                            }
                            G2.set(u, v, x);
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
            for (int z = 0; z < dataSize; ++z) {
                double yz = y.get(z);
                if (!Double.isNaN(yz)) {
                    ee += yz * yz;
                    for (int j = 0; j < mdesc.coeff.length; ++j) {
                        double cj = mdesc.coeff[j];
                        if (!Double.isNaN(cj)) {
                            double gj = g[j].get(z);
                            ee -= 2 * cj * gj * yz;
                            for (int k = 0; k < mdesc.coeff.length; ++k) {
                                double ck = mdesc.coeff[k];
                                if (!Double.isNaN(ck)) {
                                    ee += g2.get(j, k).get(z) * cj * ck;
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
        // analytical optimization 
        int nl = dfm.getTransition().nlags;
        int nf = dfm.getFactorsCount();
        int blen = dfm.getBlockLength();
        int n = nf * nl;
        Matrix f = new Matrix(nf, n);
        Matrix f2 = new Matrix(n, n);
        // fill the matrices
        for (int i = 0; i < nf; ++i) {
            for (int j = 0; j < nl; ++j) {
                for (int k = 0; k < nf; ++k) {
                    double x = ef(i * blen, k * blen + j + 1).sum();
                    f.set(i, j * nf + k, x);
                }
            }
        }
        for (int i = 1, r = 0; i <= nl; ++i) {
            for (int k = 0; k < nf; ++k, ++r) {
                for (int j = 1, c = 0; j <= nl; ++j) {
                    for (int l = 0; l < nf; ++l, ++c) {
                        double x = ef(k * blen + i, l * blen + j).sum();
                        f2.set(r, c, x);
                    }
                }
            }
        }
        Matrix A;
        // A = f/f2 <-> Af2 = f
        try {
            A = f.clone();
            SymmetricMatrix.lsolve(f2, A.subMatrix(), false);
        } catch (MatrixException err) {
            // should never happen
            A = Matrix.lsolve(f2.subMatrix(), f.subMatrix());
        }
        // copy f in dfm...
        Matrix tmp = dfm.getTransition().varParams;
        for (int i = 0, k = 0; i < nl; ++i) {
            for (int j = 0; j < nf; ++j) {
                tmp.column(j * nl + i).copy(A.column(k++));
            }
        }
        // Q = 1/T * (E(f0,f0) - A * f')
        Matrix Q = dfm.getTransition().covar;
        for (int i = 0; i < nf; ++i) {
            for (int j = 0; j <= i; ++j) {
                Q.set(i, j, ef(i * blen, j * blen).sum());
            }
        }
        SymmetricMatrix.fromLower(Q);
        Matrix Y = new Matrix(nf, nf);
        Y.subMatrix().product(A.subMatrix(), f.subMatrix().transpose());
        Q.sub(Y);
        Q.mul(1.0 / dataSize);

        if (correctStart_ && dfm.getInitialization() == VarSpec.Initialization.SteadyState) {
            LbfgsMinimizer bfgs = new LbfgsMinimizer();
            //bfgs.setLineSearch(new SimpleLineSearch());
            bfgs.setMaxIter(numiter_);
            LL2 fn = new LL2();
            LL2.Instance cur = fn.current();
            bfgs.minimize(fn, cur);
            LL2.Instance ofn = (LL2.Instance) bfgs.getResult();
            if (ofn != null && cur.getValue() > ofn.getValue()) {
                dfm.getTransition().varParams.copy(ofn.V);
                dfm.getTransition().covar.copy(ofn.Q);
            }
        }
    }

    // Real function corresponding to the second part of the likelihood
    class LL2 implements IFunction {

        private final Table<Matrix> allK;
        private final Matrix K0;

        LL2() {
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
//          int n = dfm.getBlockLength(), nc = n-1, nf = dfm.getFactorsCount();
            int n = dfm.getBlockLength(), nc = dfm.getTransition().nlags, nf = dfm.getFactorsCount();
            K0 = new Matrix(nc * nf, nc * nf);
//           int del = 1;
            int del = n - nc;
            for (int i = 0; i < nf; ++i) {
                for (int k = 0; k < nc; ++k) {
                    for (int j = 0; j < nf; ++j) {
                        for (int l = 0; l < nc; ++l) {
                            double v = ef(i * n + k + del, j * n + l + del).get(0);
                            K0.set(i * nc + k, j * nc + l, v);
                        }
                    }
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
                    double s = ef(i + k * len, j + l * len).sum();
//                    // add first ef...
                    for (int u = 1; u < len - dfm.getTransition().nlags; ++u) {
                        s += ef(i + u + k * len, j + u + l * len).get(0);
                    }
                    K.set(k, l, s);
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

            private final Matrix Q, lQ, V; // lQ = cholesky factor of Q
            private final DataBlock p;
            private final double val;
            private final Matrix lv0;
            private final Matrix[] LA;
            private double v0, v1, v2, v3;

            public Instance(IReadDataBlock p) {
                this.p = new DataBlock(p);
                int nf = dfm.getFactorsCount();
                int nl = dfm.getTransition().nlags;
                V = new Matrix(nf, nf * nl);
                Q = new Matrix(nf, nf * nl);
                int vlen = V.internalStorage().length;
                this.p.range(0, vlen).copyTo(V.internalStorage(), 0);
                for (int i = 0, j = vlen; i < nf; j += nf - i, i++) {
                    Q.column(i).drop(i, 0).copy(this.p.range(j, j + nf - i));
                }
                SymmetricMatrix.fromLower(Q);
                lQ = Q.clone();
                SymmetricMatrix.lcholesky(lQ);
                lv0 = calclv0();
                LA = calcLA();
                val = calc();
            }

            public Instance() {
                this.V = dfm.getTransition().varParams;
                int nf = dfm.getFactorsCount();
                this.Q = dfm.getTransition().covar;
                int vlen = V.internalStorage().length;
                p = new DataBlock(vlen + nf * (nf + 1) / 2);
                p.range(0, vlen).copyFrom(V.internalStorage(), 0);
                for (int i = 0, j = vlen; i < nf; j += nf - i, i++) {
                    p.range(j, j + nf - i).copy(Q.column(i).drop(i, 0));
                }
                this.lQ = Q.clone();
                SymmetricMatrix.lcholesky(lQ);
                lv0 = calclv0();
                LA = calcLA();
                val = calc();
            }

            private double calc() {
                v0 = calcdetv0();
                v1 = calcssq0();
                v2 = calcdetq();
                v3 = calcssq();
                return v0 + v1 + v2 + v3;
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

            @Override
            public IReadDataBlock getParameters() {
                return p;
            }

            @Override
            public double getValue() {
                return val; //To change body of generated methods, choose Tools | Templates.
            }

            private double calcssq0() {
                // computes f0*f0 x V^-1 
                // V^-1 = (LL')^-1 = L'^-1*L^-1
                Matrix lower = LowerTriangularMatrix.inverse(lv0);
                Matrix iv0 = SymmetricMatrix.XtX(lower);
                return iv0.dot(K0);
            }

            private double calcdetv0() {
                LogSign sumLog = lv0.diagonal().sumLog();
                if (!sumLog.pos) {
                    throw new DfmException();
                }
                return 2 * sumLog.value;
            }

            private double calcdetq() {
                LogSign sumLog = lQ.diagonal().sumLog();
                if (!sumLog.pos) {
                    throw new DfmException();
                }
                return (dataSize + dfm.getBlockLength() - dfm.getTransition().nlags - 1) * 2 * sumLog.value;
//               return dataSize * 2 * sumLog.value;
            }

            private double calcssq() {
                int nl = 1 + dfm.getTransition().nlags;
                int nf = dfm.getFactorsCount();
                double ssq = 0;
                Matrix aqa = new Matrix(nf, nf);
                for (int i = 0; i < nl; ++i) {
                    aqa.subMatrix().product(LA[i].subMatrix().transpose(),
                            LA[i].subMatrix());
                    ssq += K(i, i).dot(aqa);
                    for (int j = 0; j < i; ++j) {
                        aqa.subMatrix().product(LA[i].subMatrix().transpose(),
                                LA[j].subMatrix());
                        ssq += 2 * K(i, j).dot(aqa);
                    }
                }
                return ssq;
            }

            private Matrix calclv0() {
                if (dfm.getInitialization() == VarSpec.Initialization.Zero) {
                    return null;
                }
                // compute the initial covar. We reuse the code of DynamicFactorModel
                DynamicFactorModel tmp = dfm.clone();
                tmp.clearMeasurements();
                tmp.setBlockLength(dfm.getTransition().nlags);
//                tmp.setBlockLength(dfm.getBlockLength()-1);
                tmp.getTransition().varParams.copy(V);
                tmp.getTransition().covar.copy(Q);
                try {
                    int n = tmp.getFactorsCount() * tmp.getBlockLength();
                    Matrix cov = new Matrix(n, n);
                    tmp.ssfRepresentation().Pf0(cov.subMatrix());
                    SymmetricMatrix.lcholesky(cov);
                    return cov;
                } catch (MatrixException err) {
                    throw new DfmException();
                }

            }

            /**
             * AQA = A'(i)*Q^-1*A(j) = A"(i)*L'^-1*L^-1*A We compute here L^-1*A
             */
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

}
