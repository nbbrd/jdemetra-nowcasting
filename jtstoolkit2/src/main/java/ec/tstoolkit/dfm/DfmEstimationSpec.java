/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import ec.tstoolkit.algorithm.IProcSpecification;
import ec.tstoolkit.information.InformationSet;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author palatej
 */
public class DfmEstimationSpec implements IProcSpecification, Cloneable {

    public static final String PC = "pc", PREEM = "preem", POSTEM = "postem", PROC = "proc";

    private PcSpec pc_ = new PcSpec();
    private EmSpec preEm_ = new EmSpec(), postEm_ = new EmSpec();
    private NumericalProcessingSpec proc_ = new NumericalProcessingSpec();

    // default
    public DfmEstimationSpec() {
        pc_.setEnabled(true);
        proc_.setEnabled(true);
    }
    
    public void disable(){
        pc_.setEnabled(false);
        preEm_.setEnabled(false);
        proc_.setEnabled(false);
        postEm_.setEnabled(false);
    }
    
    public boolean isEnabled(){
        return pc_.isEnabled() || preEm_.isEnabled() || proc_.isEnabled() || postEm_.isEnabled();
    }
    
    public boolean isDisabled(){
        return ! isEnabled();
    }

    public void setDefault() {
        pc_ = new PcSpec();
        preEm_ = new EmSpec();
        postEm_ = new EmSpec();
        proc_ = new NumericalProcessingSpec();
        pc_.setEnabled(true);
        proc_.setEnabled(true);
    }

    public PcSpec getPrincipalComponentsSpec() {
        return pc_;
    }

    public void setPrincipalComponentsSpec(PcSpec spec) {
        pc_ = spec;
    }

    public NumericalProcessingSpec getNumericalProcessingSpec() {
        return proc_;
    }

    public void setNumericalProcessingSpec(NumericalProcessingSpec spec) {
        proc_ = spec;
    }

    public EmSpec getPreEmSpec() {
        return preEm_;
    }

    public void setPreEmSpec(EmSpec em) {
        preEm_ = em;
    }

    public EmSpec getPostEmSpec() {
        return postEm_;
    }

    public void setPostEmSpec(EmSpec em) {
        postEm_ = em;
    }

    @Override
    public DfmEstimationSpec clone() {
        try {
            DfmEstimationSpec spec = (DfmEstimationSpec) super.clone();
            spec.pc_ = pc_.clone();
            spec.preEm_ = preEm_.clone();
            spec.postEm_ = postEm_.clone();
            spec.proc_ = proc_.clone();
            
            return spec;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof DfmEstimationSpec && equals((DfmEstimationSpec) obj));
    }
    
    public boolean equals(DfmEstimationSpec obj) {
        return obj.pc_.equals(pc_) && obj.preEm_.equals(preEm_) && 
                obj.postEm_.equals(postEm_) && obj.proc_.equals(proc_);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.pc_);
        hash = 17 * hash + Objects.hashCode(this.proc_);
        return hash;
    }

    @Override
    public InformationSet write(boolean verbose) {
        InformationSet info = new InformationSet();
        if (pc_.isEnabled() || verbose) {
            info.set(PC, pc_.write(verbose));
        }
        if (preEm_.isEnabled() || verbose) {
            info.set(PREEM, preEm_.write(verbose));
        }
        if (proc_.isEnabled() || verbose) {
            info.set(PROC, proc_.write(verbose));
        }
        if (postEm_.isEnabled() || verbose) {
            info.set(POSTEM, postEm_.write(verbose));
        }
        
        return info;
    }

    @Override
    public boolean read(InformationSet info) {
        InformationSet pc = info.getSubSet(PC);
        if (pc != null) {
            pc_.read(pc);
        } else {
            pc_.setEnabled(false);
        }
        InformationSet em = info.getSubSet(PREEM);
        if (em != null) {
            preEm_.read(em);
        } else {
            preEm_.setEnabled(false);
        }
        InformationSet proc = info.getSubSet(PROC);
        if (proc != null) {
            proc_.read(proc);
        } else {
            proc_.setEnabled(false);
        }
        em = info.getSubSet(POSTEM);
        if (em != null) {
            postEm_.read(em);
        } else {
            postEm_.setEnabled(false);
        }

        return true;
    }

    public static void fillDictionary(String prefix, Map<String, Class> dic) {
        PcSpec.fillDictionary(InformationSet.item(prefix, PC), dic);
        EmSpec.fillDictionary(InformationSet.item(prefix, PREEM), dic);
        NumericalProcessingSpec.fillDictionary(InformationSet.item(prefix, PROC), dic);
        EmSpec.fillDictionary(InformationSet.item(prefix, POSTEM), dic);
    }
}
