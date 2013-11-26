/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockIterator;
import ec.tstoolkit.maths.matrices.HouseholderR;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import ec.tstoolkit.mssf2.DefaultTimeInvariantMultivariateSsf;
import ec.tstoolkit.mssf2.IMSsf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author palatej
 */
public class DynamicFactorModel implements Cloneable {

    /**
     *
     */
    public static interface IMeasurement {

        /**
         *
         * @return
         */
        int getLength();

        /**
         *
         * @param z
         */
        void fill(DataBlock z);

        /**
         *
         * @param x
         * @return
         */
        double dot(DataBlock x);
    }

    private static class _L implements IMeasurement {

        static final _L ML = new _L();

        @Override
        public int getLength() {
            return 1;
        }

        @Override
        public void fill(DataBlock z) {
            z.set(0, 1);
        }

        @Override
        public double dot(DataBlock x) {
            return x.get(0);
        }
    }

    private static class _C implements IMeasurement {

        static final _C MC12 = new _C(12), MC4 = new _C(4);

        private _C(int l) {
            len = l;
        }
        private final int len;

        @Override
        public int getLength() {
            return len;
        }

        @Override
        public void fill(DataBlock z) {
            z.set(1);
        }

        @Override
        public double dot(DataBlock x) {
            return x.sum();
        }
    }

    private static class _CD implements IMeasurement {

        static final _CD MCD3 = new _CD(3);

        private _CD(int l) {
            len = l;
        }
        private final int len;

        @Override
        public int getLength() {
            return 2 * len - 1;
        }

        @Override
        public void fill(DataBlock z) {
            int n = (len << 1) - 1;
            for (int i = 1; i < len; ++i) {
                z.set(i - 1, i);
                z.set(n - i, i);
            }
            z.set(len - 1, len);
        }

        @Override
        public double dot(DataBlock x) {
            double r = 0;
            int n = (len << 1) - 1;
            for (int i = 1; i < len; ++i) {
                r += i * (x.get(i - 1) + x.get(n - i));
            }
            r += len * x.get(len - 1);
            return r;
        }
    }

    /**
     *
     */
    public static enum MeasurementType {

        // Level: z*(1 0 0 0 0 0 0 0 0 0 0 0)
        /**
         *
         */
        L,
        // Cumulated differences: z*(1 2 3 2 1 0 0 0 0 0 0 0)
        /**
         *
         */
        CD,
        // Cumul: z*(1 1 1 1 1 1 1 1 1 1 1 1)
        /**
         *
         */
        C;
    }

    /**
     * Represent the measurement: y(t) = coeff*Z*a(t) + e(t), e=N(0, var)
     */
    public static final class MeasurementDescriptor {

        /**
         *
         * @param type
         * @param factors
         * @param coeff
         * @param var
         */
        public MeasurementDescriptor(final IMeasurement type,
                final int[] factors, final double[] coeff, final double var) {
            this.type = type;
            this.coeff = coeff.clone();
            this.factors = factors.clone();
            this.var = var;
        }

        /**
         *
         * @param type
         * @param factor
         * @param coeff
         * @param var
         */
        public MeasurementDescriptor(final IMeasurement type,
                final int factor, final double coeff, final double var) {
            this.type = type;
            this.coeff = new double[]{coeff};
            this.factors = new int[]{factor};
            this.var = var;
        }
        /**
         *
         */
        public final IMeasurement type;
        /**
         *
         */
        public final int[] factors;
        /**
         *
         */
        public final double[] coeff;
        /**
         *
         */
        public double var;
    }

    /**
     *
     */
    public static final class TransitionDescriptor {

        /**
         *
         * @param nblocks Number of blocks
         * @param nlags Number of lags in the VAR model
         */
        public TransitionDescriptor(int nblocks, int nlags) {
            varParams = new Matrix(nblocks, nblocks * nlags);
            covar = new Matrix(nblocks, nblocks);
            this.nbloks = nblocks;
            this.nlags = nlags;
        }
        /**
         *
         */
        public final int nbloks,
                /**
                 *
                 */
                nlags;
        /**
         *
         */
        public final Matrix varParams;
        /**
         *
         */
        public final Matrix covar;
    }

    /**
     *
     * @param type
     * @return
     */
    public static IMeasurement measurement(final MeasurementType type) {
        switch (type) {
            case C:
                return _C.MC12;
            case CD:
                return _CD.MCD3;
            case L:
                return _L.ML;
            default:
                return null;
        }
    }

    /**
     *
     * @param len
     * @param type
     * @return
     */
    public static IMeasurement measurement(final int len, final MeasurementType type) {
        switch (type) {
            case C:
                if (len == 12) {
                    return _C.MC12;
                } else if (len == 4) {
                    return _C.MC4;
                } else {
                    return new _C(len);
                }
            case CD:
                if (len == 3) {
                    return _CD.MCD3;
                } else {
                    return new _CD(len);
                }
            case L:
                return _L.ML;
            default:
                return null;
        }
    }

    /**
     *
     */
    public static enum Initialization {

        /**
         *
         */
        Zero,
        /**
         *
         */
        SteadyState,
        /**
         *
         */
        UserDefined
    }
    private int c_;
    private TransitionDescriptor tdesc_;
    private List<MeasurementDescriptor> mdesc_ = new ArrayList<>();
    private Initialization init_ = Initialization.Zero;
    private Matrix V0_;

    /**
     *
     * @param c
     */
    public DynamicFactorModel(int c) {
        c_ = c;
    }

    /**
     * Rescale the model so that the variances of the transition shocks are
     * equal to 1. The methods divides the factors by the standard deviation of
     * the corresponding transition shock and updates the different coefficients
     * accordingly
     *
     * @return
     */
    public DynamicFactorModel normalize() {
        DynamicFactorModel n = clone();
        // scaling factors
        int nb = tdesc_.nbloks, nl = tdesc_.nlags;
        double[] w = new double[nb];
        tdesc_.covar.diagonal().copyTo(w, 0);
        for (int i = 0; i < nb; ++i) {
            w[i] = Math.sqrt(w[i]);
        }
        // covar
        for (int i = 0; i < nb; ++i) {
            if (w[i] != 0) {
                n.tdesc_.covar.set(i, i, 1);
                for (int j = 0; j < i; ++j) {
                    if (w[j] != 0) {
                        n.tdesc_.covar.mul(i, j, 1 / (w[i] * w[j]));
                    }
                }
            }
        }
        SymmetricMatrix.fromLower(n.tdesc_.covar);
        // varParams
        for (int i = 0; i < nb; ++i) {
            if (w[i] != 0) {
                DataBlock range = n.tdesc_.varParams.row(i).range(0, nl);
                for (int j = 0; j < nb; ++j) {
                    if (w[j] != 0 && i != j) {
                        range.mul(w[j] / w[i]);
                    }
                    range.move(nl);
                }
            }
        }
        // loadings
        for (MeasurementDescriptor desc : n.mdesc_) {
            for (int i = 0; i < desc.factors.length; ++i) {
                desc.coeff[i] *= w[desc.factors[i]];
            }
        }
        return n;
    }

    @Override
    public DynamicFactorModel clone() {
        try {
            DynamicFactorModel m = (DynamicFactorModel) super.clone();
            TransitionDescriptor td = new TransitionDescriptor(tdesc_.nbloks, tdesc_.nlags);
            td.covar.copy(tdesc_.covar);
            td.varParams.copy(tdesc_.varParams);
            m.tdesc_ = td;
            m.mdesc_ = new ArrayList<>();
            for (MeasurementDescriptor md : mdesc_) {
                m.mdesc_.add(new MeasurementDescriptor(
                        md.type, md.factors.clone(), md.coeff.clone(), md.var));
            }
            return m;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    public DynamicFactorModel compactFactor(int from, int to) {
        if (from < 0 || to < from || to >= tdesc_.nbloks) {
            return null;
        }
        if (to == from) {
            return clone();
        }
        int nc = to - from;
        DynamicFactorModel m = new DynamicFactorModel(c_);
        TransitionDescriptor td = new TransitionDescriptor(tdesc_.nbloks - nc, tdesc_.nlags);
        m.tdesc_ = td;
        m.tdesc_.covar.diagonal().set(1);
        for (MeasurementDescriptor md : mdesc_) {
            int nf=0;
            boolean ok=false;
            for (int i=0; i<md.factors.length; ++i){
                if (md.factors[i]< from || md.factors[i]>to)
                    ++nf;
                else if(!ok && md.factors[i]>= from && md.factors[i]<= to){
                    ok=true;
                    ++nf;
                } 
            }
            int[] fc=new int[nf];
            nf=0;
            ok=false;
            for (int i=0; i<md.factors.length; ++i){
                if (md.factors[i]< from)
                    fc[nf++]=md.factors[i];
                else if (md.factors[i]>to)
                    fc[nf++]=md.factors[i]-nc;
                else if(!ok && md.factors[i]>= from && md.factors[i]<= to){
                    ok=true;
                    fc[nf++]=from;
                } 
            }
            
            m.mdesc_.add(new MeasurementDescriptor(
                    md.type, fc, new double[nf], 1));
        }
        
        return m;
    }

    /**
     *
     * @return
     */
    public int getBlockLength() {
        return c_;
    }

    /**
     *
     * @param c
     */
    public void setBlockLength(int c) {
        c_ = c;
    }

    /**
     *
     * @param desc
     */
    public void setTransition(TransitionDescriptor desc) {
        tdesc_ = desc;
    }

    /**
     *
     * @return
     */
    public TransitionDescriptor getTransition() {
        return tdesc_;
    }

    /**
     *
     * @return
     */
    public List<MeasurementDescriptor> getMeasurements() {
        return Collections.unmodifiableList(mdesc_);
    }

    /**
     *
     * @param desc
     */
    public void addMeasurement(MeasurementDescriptor desc) {
        mdesc_.add(desc);
    }

    /**
     *
     * @return
     */
    public IMSsf ssfRepresentation() {
        return new Ssf(init_, V0_);
    }

    /**
     *
     * @return
     */
    public int getMeasurementsCount() {
        return mdesc_.size();
    }

    /**
     *
     * @param init
     */
    public void setInitialization(Initialization init) {
        init_ = init;
        if (init_ != Initialization.UserDefined) {
            V0_ = null;
        }
    }

    /**
     *
     * @return
     */
    public Initialization getInitialization() {
        return init_;
    }

    /**
     *
     * @param v0
     */
    public void setInitialCovariance(Matrix v0) {
        V0_ = v0.clone();
        init_ = Initialization.UserDefined;
    }

    class Ssf extends DefaultTimeInvariantMultivariateSsf {

        public DynamicFactorModel getModel() {
            return DynamicFactorModel.this;
        }
        private final DataBlock ttmp, xtmp;

        private Ssf(Initialization init, Matrix V0) {
            int nl = tdesc_.nlags, nb = tdesc_.nbloks;
            int mdim = nb * c_, vdim = mdesc_.size();
            this.initialize(mdim, vdim, nb, true);
            ttmp = new DataBlock(nb);
            xtmp = new DataBlock(mdim);
            // Measurement
            for (int i = 0; i < vdim; ++i) {
                MeasurementDescriptor zdesc = mdesc_.get(i);
                m_H[i] = zdesc.var;
                DataBlock z = m_Z.row(i);
                for (int j = 0; j < zdesc.factors.length; ++j) {
                    IMeasurement m = zdesc.type;
                    int start = c_ * zdesc.factors[j];
                    DataBlock cur = z.range(start, start + m.getLength());
                    m.fill(cur);
                    cur.mul(zdesc.coeff[j]);
                }
            }
            // Transition
            // T, S
            for (int i = 0, r = 0; i < nb; ++i, r += c_) {
                m_S.set(r, i, 1);
                for (int j = 0, c = 0; j < nb; ++j, c += c_) {
                    SubMatrix B = m_T.subMatrix(r, r + c_, c, c + c_);
                    if (i == j) {
                        B.subDiagonal(-1).set(1);
                    }
                    B.row(0).range(0, nl).
                            copy(tdesc_.varParams.row(i).range(j * nl, (j + 1) * nl));
                }
            }
            // Q
            m_Q.copy(tdesc_.covar);
            updateTransition();
            // initial covariance
            switch (init_) {
                case SteadyState:
                    m_Pf0 = getInitialVariance();
                    break;
                case UserDefined:
                    m_Pf0 = V0;
                    break;
                default:
                    m_Pf0 = m_V;
            }
        }

        @Override
        public void TX(int pos, DataBlock x) {
            int nb = tdesc_.nbloks, nl = tdesc_.nlags;
            // compute first the next item
            for (int i = 0; i < nb; ++i) {
                double r = 0;
                DataBlock p = tdesc_.varParams.row(i).range(0, nl);
                DataBlock xb = x.range(0, nl);
                for (int j = 0; j < nb; ++j) {
                    if (j != 0) {
                        p.move(nl);
                        xb.move(c_);
                    }
                    r += p.dot(xb);
                }
                ttmp.set(i, r);
            }
            x.fshift(DataBlock.ShiftOption.Zero);
            x.extract(0, -1, c_).copy(ttmp);
        }

        // TODO: improvement should not be too difficult (process by block)
        @Override
        public void TVT(final int pos, final SubMatrix vm) {
            // usual solution
            DataBlockIterator cols = vm.columns();
            DataBlock col = cols.getData();
            do {
                TX(pos, col);
            } while (cols.next());

            DataBlockIterator rows = vm.rows();
            DataBlock row = rows.getData();
            do {
                TX(pos, row);
            } while (rows.next());
            SymmetricMatrix.reinforeSymmetry(vm);
        }

        @Override
        public void addV(final int pos, final SubMatrix v) {
            int nb = tdesc_.nbloks;
            for (int i = 0; i < nb; ++i) {
                DataBlock cv = v.column(i * c_).extract(0, nb, c_);
                cv.add(tdesc_.covar.column(i));
            }
        }

        @Override
        public double ZX(final int pos, int v, final DataBlock x) {
            MeasurementDescriptor zdesc = mdesc_.get(v);
            double r = 0;
            for (int j = 0; j < zdesc.factors.length; ++j) {
                IMeasurement m = zdesc.type;
                int start = c_ * zdesc.factors[j];
                DataBlock cur = x.range(start, start + m.getLength());
                r += zdesc.coeff[j] * m.dot(cur);
            }
            return r;
        }

        @Override
        public void ZM(final int pos, final SubMatrix m, final SubMatrix zm) {
            DataBlockIterator rows = zm.rows();
            DataBlock row = rows.getData();
            do {
                ZM(pos, rows.getPosition(), m, row);
            } while (rows.next());
        }

        @Override
        public void ZM(final int pos, final int v, final SubMatrix M, final DataBlock zm) {
            MeasurementDescriptor zdesc = mdesc_.get(v);
            zm.set(0);
            for (int j = 0; j < zdesc.factors.length; ++j) {
                IMeasurement m = zdesc.type;
                int start = c_ * zdesc.factors[j];
                for (int c = 0; c < M.getColumnsCount(); ++c) {
                    DataBlock cur = M.column(c).range(start, start + m.getLength());
                    zm.add(c, zdesc.coeff[j] * m.dot(cur));
                }
            }
//           DataBlockIterator cols = m.columns();
//            DataBlock col = cols.getData();
//            do {
//                zm.set(cols.getPosition(), ZX(pos, v, col));
//            } while (cols.next());
        }

        @Override
        public void XT(int pos, DataBlock x) {
            // put the results in xtmp;
            int nb = tdesc_.nbloks, nl = tdesc_.nlags;
            for (int i = 0, k = 0, l = 0; i < nb; ++i) {
                for (int j = 0; j < nl; ++j, ++k) {
                    double r = x.get(k + 1);
                    r += tdesc_.varParams.column(l++).dot(x.extract(0, nb, c_));
                    xtmp.set(k, r);
                }
                for (int j = nl; j < c_ - 1; ++j, ++k) {
                    xtmp.set(k, x.get(k + 1));
                }
                xtmp.set(k++, 0);
            }
            x.copy(xtmp);
        }

        private Matrix getInitialVariance() {
            int nb = tdesc_.nbloks, nl = tdesc_.nlags;
            // We have to solve the steady state equation:
            // V = T V T' + Q
            // We consider the nlag*nb, nlag*nb sub-system

            int n = nb * nl;
            Matrix cov = new Matrix(n, n);
            int np = (n * (n + 1)) / 2;
            Matrix M = new Matrix(np, np);
            double[] b = new double[np];
            // fill the matrix
            for (int c = 0, i = 0; c < n; ++c) {
                for (int r = c; r < n; ++r, ++i) {
                    M.set(i, i, 1);
                    if (r % nl == 0 && c % nl == 0) {
                        b[i] = tdesc_.covar.get(r / nl, c / nl);
                    }
                    for (int k = 0; k < n; ++k) {
                        for (int l = 0; l < n; ++l) {
                            double zr = 0, zc = 0;
                            if (r % nl == 0) {
                                zr = tdesc_.varParams.get(r / nl, l);
                            } else if (r == l + 1) {
                                zr = 1;
                            }
                            if (c % nl == 0) {
                                zc = tdesc_.varParams.get(c / nl, k);
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
            Matrix fullCov = new Matrix(getStateDim(), getStateDim());
            for (int r = 0; r < nb; ++r) {
                for (int c = 0; c < nb; ++c) {
                    fullCov.subMatrix(r * c_, r * c_ + nl, c * c_, c * c_ + nl).copy(cov.subMatrix(r * nl, (r + 1) * nl, c * nl, (c + 1) * nl));
                }
            }
            for (int i = nl; i < c_; ++i) {
                TVT(0, fullCov.subMatrix());
                addV(0, fullCov.subMatrix());
            }
            return fullCov;
        }

        private Matrix getInitialVariance2() {
            int nb = tdesc_.nbloks, nl = tdesc_.nlags;
            // We have to solve the steady state equation:
            // V = T V T' + Q
            // first implementation, completely unoptimized
            int n = getStateDim();
            Matrix cov = new Matrix(n, n);
            Matrix t = new Matrix(n, n);
            Matrix q = new Matrix(n, n);
            T(0, t.subMatrix());
            V(0, q.subMatrix());
            int np = (n * (n + 1)) / 2;
            Matrix M = new Matrix(np, np);
            double[] b = new double[np];
            // fill the matrix
            for (int c = 0, i = 0; c < n; ++c) {
                for (int r = c; r < n; ++r, ++i) {
                    M.set(i, i, 1);
                    b[i] = q.get(r, c);
                    for (int k = 0; k < n; ++k) {
                        for (int l = 0; l < n; ++l) {
                            double z = t.get(r, l) * t.get(c, k);
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
            return cov;
        }
    }

    private static int pos(int r, int c, int n) {
//        if (r<c)
//            return c + r * (2 * n - r - 1) / 2;
//        else
        return r + c * (2 * n - c - 1) / 2;
    }
}
