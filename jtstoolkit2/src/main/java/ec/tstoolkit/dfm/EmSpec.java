/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import ec.tstoolkit.algorithm.IProcSpecification;
import ec.tstoolkit.information.InformationSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author palatej
 */
public class EmSpec implements IProcSpecification, Cloneable {

    public static final int DEF_VERSION = 2, DEF_MAXITER = 100;
    public static final String ENABLED = "enabled", VERSION = "version", MAXITER = "maxiter";

    private boolean enabled_;
    private int version_ = DEF_VERSION;
    private int maxIter_ = DEF_MAXITER;

    public void setEnabled(boolean use) {
        enabled_ = use;
    }

    public boolean isEnabled() {
        return enabled_;
    }

    public void setMaxIter(int iter) {
        maxIter_ = iter;
    }

    public int getMaxIter() {
        return maxIter_;
    }

    public void setVersion(int v) {
        version_ = v;
    }

    public int getVersion() {
        return version_;
    }

    @Override
    public EmSpec clone() {
        try {
            return (EmSpec) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public InformationSet write(boolean verbose) {
        InformationSet info = new InformationSet();
        info.set(ENABLED, enabled_);
        if (version_ != DEF_VERSION || verbose) {
            info.set(VERSION, version_);
        }
        if (maxIter_ != DEF_MAXITER || verbose) {
            info.set(MAXITER, maxIter_);
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
        Integer v = info.get(VERSION, Integer.class);
        if (v != null) {
            version_ = v;
        }
        Integer ni = info.get(MAXITER, Integer.class);
        if (ni != null) {
            maxIter_ = ni;
        }
        return true;
    }
    
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof EmSpec && equals((EmSpec) obj));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + (this.enabled_ ? 1 : 0);
        hash = 41 * hash + this.version_;
        hash = 41 * hash + this.maxIter_;
        return hash;
    }
    
    public boolean equals(EmSpec spec){
        return enabled_==spec.enabled_ && maxIter_ == spec.maxIter_ && version_ == spec.version_;
    }

    public static void fillDictionary(String prefix, Map<String, Class> dic) {
        dic.put(InformationSet.item(prefix, ENABLED), Boolean.class);
        dic.put(InformationSet.item(prefix, VERSION), Integer.class);
        dic.put(InformationSet.item(prefix, MAXITER), Integer.class);
    }
}
