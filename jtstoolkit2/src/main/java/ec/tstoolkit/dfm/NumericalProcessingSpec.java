/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import ec.tstoolkit.algorithm.IProcSpecification;
import ec.tstoolkit.information.InformationSet;
import java.util.Map;

/**
 *
 * @author palatej
 */
public class NumericalProcessingSpec implements IProcSpecification, Cloneable {

    public static final int DEF_VERSION = 2, DEF_MAXITER = 1000, DEF_MAXSITER = 15,
            DEF_NITER = 5;
    public static final Boolean DEF_BLOCK = true;
    public static final String ENABLED = "enabled", MAXITER = "maxiter", MAXSITER = "maxsiter", NITER = "niter", BLOCKITER = "blockiter", EPS = "eps";
    public static final double DEF_EPS = 1e-9;
    private boolean enabled_;
    private int maxiter_ = DEF_MAXITER, maxsiter_ = DEF_MAXSITER, niter_ = DEF_NITER;
    private boolean block_ = DEF_BLOCK;
    private double eps_ = DEF_EPS;

    public void setEnabled(boolean use) {
        enabled_ = use;
    }

    public boolean isEnabled() {
        return enabled_;
    }

    public void setMaxIter(int iter) {
        maxiter_ = iter;
    }

    public int getMaxIter() {
        return maxiter_;
    }

    public void setMaxInitialIter(int iter) {
        maxsiter_ = iter;
    }

    public int getMaxInitialIter() {
        return maxsiter_;
    }

    public void setMaxIntermediateIter(int iter) {
        niter_ = iter;
    }

    public int getMaxIntermediateIter() {
        return niter_;
    }
    
    public boolean isBlockIterations(){
        return block_;
    }
    
    public void setBlockIterations(boolean b){
        block_=b;
    }

    @Override
    public NumericalProcessingSpec clone() {
        try {
            return (NumericalProcessingSpec) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public InformationSet write(boolean verbose) {
        InformationSet info = new InformationSet();
        info.set(ENABLED, enabled_);
        if (block_ != DEF_BLOCK || verbose) {
            info.set(BLOCKITER, block_);
        }
        if (eps_ != DEF_EPS || verbose) {
            info.set(EPS, eps_);
        }
        if (maxiter_ != DEF_MAXITER || verbose) {
            info.set(MAXITER, maxiter_);
        }
        if (maxsiter_ != DEF_MAXSITER || verbose) {
            info.set(MAXSITER, maxsiter_);
        }
        if (niter_ != DEF_NITER || verbose) {
            info.set(NITER, niter_);
        }
        return info;
    }

    @Override
    public boolean read(InformationSet info) {
        if (info == null) {
            return true;
        }
        Boolean enabled = info.get(ENABLED, Boolean.class);
        if (enabled != null) {
            enabled_ = enabled;
        }
        Boolean block = info.get(BLOCKITER, Boolean.class);
        if (block != null) {
            block_ = block;
        }
        Integer ni = info.get(MAXITER, Integer.class);
        if (ni != null) {
            maxiter_ = ni;
        }
        ni = info.get(MAXSITER, Integer.class);
        if (ni != null) {
            maxsiter_ = ni;
        }
        ni = info.get(NITER, Integer.class);
        if (ni != null) {
            niter_ = ni;
        }
        Double eps = info.get(EPS, Double.class);
        if (eps != null) {
            eps_ = eps;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof NumericalProcessingSpec && equals((NumericalProcessingSpec) obj));
    }

    public boolean equals(NumericalProcessingSpec obj) {
        return obj.enabled_ == enabled_ && obj.block_ == block_ && obj.eps_ == eps_
                && obj.maxiter_ == maxiter_ && obj.maxsiter_ == obj.maxsiter_ && obj.niter_ == niter_;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.enabled_ ? 1 : 0);
        hash = 17 * hash + this.maxiter_;
        hash = 17 * hash + this.maxsiter_;
        hash = 17 * hash + this.niter_;
        hash = 17 * hash + (int) (Double.doubleToLongBits(this.eps_) ^ (Double.doubleToLongBits(this.eps_) >>> 32));
        return hash;
    }

    public static void fillDictionary(String prefix, Map<String, Class> dic) {
        dic.put(InformationSet.item(prefix, ENABLED), Boolean.class);
        dic.put(InformationSet.item(prefix, MAXITER), Integer.class);
        dic.put(InformationSet.item(prefix, MAXSITER), Integer.class);
        dic.put(InformationSet.item(prefix, NITER), Integer.class);
        dic.put(InformationSet.item(prefix, BLOCKITER), Boolean.class);
        dic.put(InformationSet.item(prefix, EPS), Double.class);
    }
}
