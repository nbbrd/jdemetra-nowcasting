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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author palatej
 */
public class DynamicFactorModel implements Cloneable {

    /**
     * The IMeasurement interface represents the behaviour of a measurement
     * equation on a given factor (and its lags).
     */
    public static interface IMeasurement {

        /**
         * The number of lags (nlags) implied by the measurement
         *
         * @return Lags in [t to t-nlags[ are used by the measurement in t
         */
        int getLength();

        /**
         * Fills the polynomial of the measurement (without the actual
         * coefficient) Typical values are: 1 [0 ... 0] 1 1 ... 1 1 2 3 2 1 [0
         * 0... 0]
         *
         * @param z The buffer that will contain the polynomial. The length of
         * the buffer is defined by "getLength()".
         */
        void fill(DataBlock z);

        /**
         * Computes the product of the polynomial (z) with a block of data
         *
         * @param x The block of data. The length of the data is defined by
         * "getLength()".
         * @return returns z * x
         */
        double dot(DataBlock x);
    }

    /**
     * Z = 1 [0 ... 0]
     */
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

    /**
     * Z = 1 1 ... 1 (len times)
     */
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

    /**
     * Z = 1 2 3 2 1 [ 0 ... 0] for len = 3 (from monthly growth to quarterly
     * growth) Z = 1 2 1 for len = 2 Z = 1 2 3 4 3 2 1 for len = 4 ...
     */
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

        /**
         * Level: z*(1 0 0 0 0 0 0 0 0 0 0 0)
         */
        L,
        /**
         * Cumulated differences: z*(1 2 3 2 1 0 0 0 0 0 0 0)
         */
        CD,
        /**
         * Cumul: z*(1 1 1 1 1 1 1 1 1 1 1 1)
         */
        C;
    }

    public static final class MeasurementStructure implements Comparable<MeasurementStructure> {

        public final MeasurementType type;
        public final boolean[] used;

        public MeasurementStructure(final MeasurementType type, final boolean[] used) {
            this.type = type;
            this.used = used;
        }

        @Override
        public int compareTo(MeasurementStructure o) {
            int cmp = type.compareTo(o.type);
            if (cmp != 0) {
                return cmp;
            } else { // DAVID: if the type is the same
                if (used.length < o.used.length) {
                    return -1;
                }
                if (used.length > o.used.length) {
                    return 1;
                }
                for (int i = 0; i < used.length; ++i) {
                    if (!used[i] && o.used[i]) {
                        return -1;
                    } else if (used[i] && !o.used[i]) {
                        return 1;
                    }
                }
                return 0;
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(type).append('[');
            if (used.length > 0) {
                builder.append(used[0] ? 1 : 0);
            }
            for (int i = 1; i < used.length; ++i) {
                builder.append(' ').append(used[i] ? 1 : 0);
            }
            builder.append(']');
            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MeasurementStructure) {
                MeasurementStructure m = (MeasurementStructure) o;
                return type == m.type && Arrays.equals(used, m.used);
            } else {
                return false;
            }

        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 37 * hash + Objects.hashCode(this.type);
            hash = 37 * hash + Arrays.hashCode(this.used);
            return hash;
        }
    }

    public static final class MeasurementLoads implements Comparable<MeasurementLoads> {

        public final boolean[] used;

        public MeasurementLoads(final boolean[] used) {
            this.used = used;
        }

        @Override
        public int compareTo(MeasurementLoads o) {
            if (used.length < o.used.length) {
                return -1;
            }
            if (used.length > o.used.length) {
                return 1;
            }
            for (int i = 0; i < used.length; ++i) {
                if (!used[i] && o.used[i]) {
                    return -1;
                } else if (used[i] && !o.used[i]) {
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            if (used.length > 0) {
                builder.append(used[0] ? 1 : 0);
            }
            for (int i = 1; i < used.length; ++i) {
                builder.append(' ').append(used[i] ? 1 : 0);
            }
            builder.append(']');
            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MeasurementLoads) {
                MeasurementLoads m = (MeasurementLoads) o;
                return Arrays.equals(used, m.used);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 41 * hash + Arrays.hashCode(this.used);
            return hash;
        }

    }

    /**
     * Represent the measurement: y(t) = coeff*Z*a(t) + e(t), e=N(0, var)
     */
    public static final class MeasurementDescriptor {

        /**
         * Creates a new measurement descriptor
         *
         * @param type Type of the measurement equation
         * @param coeff Coefficients (1 by factor). Unused factors are
         * identified by a "Double.NaN" coefficient.
         * @param var Variance of the measurement equation (>=0)
         */
        public MeasurementDescriptor(final IMeasurement type,
                final double[] coeff, final double var) {
            this.type = type;
            this.coeff = coeff.clone();
            this.var = var;
        }

        // WHY DO WE NEED THIS CONSTRUCTOR?
        public MeasurementDescriptor(final MeasurementStructure structure) {
            this.type = measurement(structure.type);
            this.coeff = new double[structure.used.length];
            for (int i = 0; i < coeff.length; ++i) {
                if (!structure.used[i]) {
                    coeff[i] = Double.NaN;
                }
            }
            this.var = 1;   // DAVID: why is equal to 1?  I think it must be initialized
        }
        
        
    
        
        /**
         * Type of the measurement equation
         */
        public final IMeasurement type;
        /**
         * Coefficients (1 by factor). Unused factors are identified by a
         * "Double.NaN" coefficient.
         */
        public final double[] coeff;
        /**
         * Variance of the measurement equation (>=0)
         */
        public double var; // DAVID: why not final?

   
                
        public boolean isUsed(int fac) {
            return !Double.isNaN(coeff[fac]);
        }

        public boolean[] getUsedFactors() {
            boolean[] used = new boolean[coeff.length]; // DAVID: I THINK THIS LINE SHOULD BE COMMENTED (OTHERWISE IT CREATES A NEW USED)
            for (int i = 0; i < used.length; ++i) {
                used[i] = !Double.isNaN(coeff[i]);
            }
            return used;
        }

       public int getUsedFactorsCount() {
           int n=0;
             for (int i = 0; i < coeff.length; ++i) {
                if (!Double.isNaN(coeff[i]))
                    ++n;
            }
            return n;
        }

       public MeasurementStructure getStructure() {
            return new MeasurementStructure(getMeasurementType(type), getUsedFactors());
        }

        public MeasurementLoads getLoads() {
            return new MeasurementLoads(getUsedFactors());
        }
    }

    /**
     * Description of the VAR part of the model
     */
    public static final class TransitionDescriptor {

        /**
         * Creates a new descriptor of the transition equation (VAR).
         *
         * @param nblocks Number of factors
         * @param nlags Number of lags in the VAR model
         */
        public TransitionDescriptor(int nblocks, int nlags) {
            varParams = new Matrix(nblocks, nblocks * nlags);
            covar = new Matrix(nblocks, nblocks);
            this.nlags = nlags;
        }
        /**
         * Number of lags
         */
        public final int nlags;
        /**
         * Parameters of the VAR equations The row i contains the coefficients
         * c(i,k) of fi(t): fi(t)= c(i,0)f0(t-1)+...+c(i,nlags-1)f0(t-nlags)+...
         * +c(i,k)fn(t-1)...+c(i,l)fn(t-nlags))
         */
        public final Matrix varParams;
        /**
         * Covariance matrix of the innovations
         */
        public final Matrix covar;
    }

    /**
     * Gets the default measurement for a given type.
     *
     * @param type The type of the measurement
     * @return For type C, returns (1...1) (length=12); for type CD, returns (1
     * 2 3 2 1); for type L, returns (1)
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

    public static MeasurementType getMeasurementType(final IMeasurement m) {
        if (m instanceof _C) {
            return MeasurementType.C;
        } else if (m instanceof _CD) {
            return MeasurementType.CD;
        } else if (m instanceof _L) {
            return MeasurementType.L;
        } else {
            return null;
        }
    }

    /**
     * Gets the measurement corresponding to given length and type.
     *
     * @param len The "length" of the measurement (see details on each
     * measurement type for further information)
     * @param type The type of the measurement
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
     * Initialisation of the model. Only the covariance of the state array is
     * considered in the model (the initial prediction errors are defined with
     * the data).
     */
    public static enum Initialization {

        /**
         * Zero initialisation. [A(-1)=0,] P(-1)=0 -> [A(0|-1)=0,] P(0|-1)=Q
         * (transition innovations)
         */
        Zero,
        /**
         * Steady state (or unconditional) initialisation, defined by V=TVT'+Q
         * [A(0|-1)=0,] V
         */
        SteadyState,
        /**
         * [A(0) and] P(0) are pre-specified
         */
        UserDefined
    }
    private int c_;
    private final int nf_;
    private TransitionDescriptor tdesc_;
    private List<MeasurementDescriptor> mdesc_ = new ArrayList<>();
    private Initialization init_ = Initialization.SteadyState;
    private Matrix V0_;

    /**
     * Creates a new dynamic factors model
     *
     * @param c The number of lags for each factors (in [t, t-c[) that has to be
     * integrated in the model
     * @param nf The number of factors
     */
    public DynamicFactorModel(int c, int nf) {
        c_ = c;
        nf_ = nf;
    }

    /**
     * Rescale the model so that the variances of the transition shocks are
     * equal to 1. The method divides each factor by the standard deviation of
     * the corresponding transition shock and updates the different coefficients
     * accordingly.
     */
    public void normalize() {
        // scaling factors
        int nl = tdesc_.nlags;
        double[] w = new double[nf_];
        tdesc_.covar.diagonal().copyTo(w, 0);
        for (int i = 0; i < nf_; ++i) {
            w[i] = Math.sqrt(w[i]);
        }
        // covar
        for (int i = 0; i < nf_; ++i) {
            if (w[i] != 0) {
                tdesc_.covar.set(i, i, 1);
                for (int j = 0; j < i; ++j) {
                    if (w[j] != 0) {
                        tdesc_.covar.mul(i, j, 1 / (w[i] * w[j]));
                    }
                }
            }
        }
        SymmetricMatrix.fromLower(tdesc_.covar);
        // varParams
        for (int i = 0; i < nf_; ++i) {
            if (w[i] != 0) {
                DataBlock range = tdesc_.varParams.row(i).range(0, nl);
                for (int j = 0; j < nf_; ++j) {
                    if (w[j] != 0 && i != j) {
                        range.mul(w[j] / w[i]);
                    }
                    range.move(nl);
                }
            }
        }
        // loadings
        double vmax = 0;
        for (MeasurementDescriptor desc : mdesc_) {
            if (desc.var > vmax) {
                vmax = desc.var;
            }
        }
        double emax = Math.sqrt(vmax);
        for (MeasurementDescriptor desc : mdesc_) {
            for (int i = 0; i < desc.coeff.length; ++i) {
                if (!Double.isNaN(desc.coeff[i])) {
                    desc.coeff[i] *= w[i] / emax;
                }
            }
            desc.var /= vmax;
        }
    }

    @Override
    public DynamicFactorModel clone() {
        try {
            DynamicFactorModel m = (DynamicFactorModel) super.clone();
            TransitionDescriptor td = new TransitionDescriptor(nf_, tdesc_.nlags);
            td.covar.copy(tdesc_.covar);
            td.varParams.copy(tdesc_.varParams);
            m.tdesc_ = td;
            m.mdesc_ = new ArrayList<>();
            for (MeasurementDescriptor md : mdesc_) {
                m.mdesc_.add(new MeasurementDescriptor(
                        md.type, md.coeff.clone(), md.var));
            }
            return m;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     * Copies the parameters of a given model in this object
     *
     * @param m The model being copied
     * @return True if the models have the same structure and can be copied
     * false otherwise. Models have the same structure means that they have: -
     * same VAR structure (number of factors, number of lags) - same number of
     * measurement equations
     */
    public boolean copy(DynamicFactorModel m) {
        if (nf_ != m.nf_
                || tdesc_.nlags != m.tdesc_.nlags
                || mdesc_.size() != m.mdesc_.size()) {
            return false;
        }
        tdesc_.covar.copy(m.tdesc_.covar);
        tdesc_.varParams.copy(m.tdesc_.varParams);
        for (int i = 0; i < mdesc_.size(); ++i) {
            MeasurementDescriptor s = m.mdesc_.get(i),
                    t = mdesc_.get(i);
            System.arraycopy(s.coeff, 0, t.coeff, 0, s.coeff.length);
            t.var = s.var;
        }
        return true;
    }

    /**
     * Compacts the factors of a given models
     *
     * @param from The first factor to merge
     * @param to The last factor (included) to merge
     * @return A new model is returned. It should be re-estimated.
     */
    public DynamicFactorModel compactFactors(int from, int to) {
        if (from < 0 || to < from || to >= nf_) {
            return null;
        }
        if (to == from) {
            return clone();
        }
        int nc = to - from;
        DynamicFactorModel m = new DynamicFactorModel(c_, nf_ - nc);
        TransitionDescriptor td = new TransitionDescriptor(nf_ - nc, tdesc_.nlags);
        m.tdesc_ = td;
        m.tdesc_.covar.diagonal().set(1);
        for (MeasurementDescriptor md : mdesc_) {
            double[] ncoeff = new double[nf_ - nc];
            for (int i = 0; i < from; ++i) {
                ncoeff[i] = md.coeff[i];
            }
            for (int i = to + 1; i < nf_; ++i) {
                ncoeff[i - nc] = md.coeff[i];
            }
            boolean used = false;
            for (int i = from; i <= to; ++i) {
                if (!Double.isNaN(md.coeff[i])) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                ncoeff[from] = Double.NaN;
            }
            m.mdesc_.add(new MeasurementDescriptor(
                    md.type, ncoeff, 1));
        }
        return m;
    }

    /**
     * The number of lags for each factor
     *
     * @return
     */
    public int getBlockLength() {
        return c_;
    }

    /**
     * The number of factors
     *
     * @return
     */
    public int getFactorsCount() {
        return nf_;
    }

    /**
     * Changes the number of lags of each factor that is included in the model
     *
     * @param c The size of each block of factors (lags in [t, t-c[ belong to
     * the model). c should larger or equal to the number of lags in the
     * transition equation.
     * @throws A DfmException is thrown when the model is invalid (see above)
     */
    public void setBlockLength(int c) throws DfmException {
        if (tdesc_ != null && c < tdesc_.nlags) {
            throw new DfmException(DfmException.INVALID_MODEL);
        }
        c_ = c;
    }

    /**
     * Sets a new descriptor for the transition equation (VAR model)
     *
     * @param desc The descriptor of the transition equation
     * @throws A DfmException is thrown when the model is invalid
     */
    public void setTransition(TransitionDescriptor desc) throws DfmException {
        if (desc.covar.getRowsCount() != nf_ || c_ < desc.nlags) {
            throw new DfmException(DfmException.INVALID_MODEL);
        }
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
    
    public void clearMeasurements(){
        mdesc_.clear();
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

        private boolean mused(MeasurementDescriptor m, int i) {
            double z = m.coeff[i];
            return z != 0 && !Double.isNaN(z);
        }

        public DynamicFactorModel getModel() {
            return DynamicFactorModel.this;
        }
        private final DataBlock ttmp, xtmp;

        private Ssf(Initialization init, Matrix V0) {
            int nl = tdesc_.nlags;
            int mdim = nf_ * c_, vdim = mdesc_.size();
            this.initialize(mdim, vdim, nf_, true);
            ttmp = new DataBlock(nf_);
            xtmp = new DataBlock(mdim);
            // Measurement
            for (int i = 0; i < vdim; ++i) {
                MeasurementDescriptor zdesc = mdesc_.get(i);
                m_H[i] = zdesc.var;
                DataBlock z = m_Z.row(i);
                for (int j = 0, start = 0; j < nf_; ++j, start += c_) {
                    if (mused(zdesc, j)) {
                        IMeasurement m = zdesc.type;
                        DataBlock cur = z.range(start, start + m.getLength());
                        m.fill(cur);
                        cur.mul(zdesc.coeff[j]);
                    }
                }
            }
            // Transition
            // T, S
            for (int i = 0, r = 0; i < nf_; ++i, r += c_) {
                m_S.set(r, i, 1);
                for (int j = 0, c = 0; j < nf_; ++j, c += c_) {
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
            int nl = tdesc_.nlags;
            // compute first the next item
            for (int i = 0; i < nf_; ++i) {
                double r = 0;
                DataBlock p = tdesc_.varParams.row(i).range(0, nl);
                DataBlock xb = x.range(0, nl);
                for (int j = 0; j < nf_; ++j) {
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
            for (int i = 0; i < nf_; ++i) {
                DataBlock cv = v.column(i * c_).extract(0, nf_, c_);
                cv.add(tdesc_.covar.column(i));
            }
        }

        @Override
        public double ZX(final int pos, int v, final DataBlock x) {
            MeasurementDescriptor zdesc = mdesc_.get(v);
            double r = 0;
            for (int j = 0, start = 0; j < nf_; ++j, start += c_) {
                if (mused(zdesc, j)) {
                    IMeasurement m = zdesc.type;
                    DataBlock cur = x.range(start, start + m.getLength());
                    r += zdesc.coeff[j] * m.dot(cur);
                }
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
            for (int j = 0, start = 0; j < nf_; ++j, start += c_) {
                if (mused(zdesc, j)) {
                    IMeasurement m = zdesc.type;
                    for (int c = 0; c < M.getColumnsCount(); ++c) {
                        DataBlock cur = M.column(c).range(start, start + m.getLength());
                        zm.add(c, zdesc.coeff[j] * m.dot(cur));
                    }
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
            int nl = tdesc_.nlags;
            for (int i = 0, k = 0, l = 0; i < nf_; ++i) {
                for (int j = 0; j < nl; ++j, ++k) {
                    double r = x.get(k + 1);
                    r += tdesc_.varParams.column(l++).dot(x.extract(0, nf_, c_));
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
            int nl = tdesc_.nlags;
            // We have to solve the steady state equation:
            // V = T V T' + Q
            // We consider the nlag*nb, nlag*nb sub-system

            int n = nf_ * nl;
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
            for (int r = 0; r < nf_; ++r) {
                for (int c = 0; c < nf_; ++c) {
                    fullCov.subMatrix(r * c_, r * c_ + nl, c * c_, c * c_ + nl).copy(cov.subMatrix(r * nl, (r + 1) * nl, c * nl, (c + 1) * nl));
                }
            }
            for (int i = nl; i < c_; ++i) {
                TVT(0, fullCov.subMatrix());
                addV(0, fullCov.subMatrix());
            }
            return fullCov;
        }
    }

    private static int pos(int r, int c, int n) {
//        if (r<c)
//            return c + r * (2 * n - r - 1) / 2;
//        else
        return r + c * (2 * n - c - 1) / 2;
    }
}
