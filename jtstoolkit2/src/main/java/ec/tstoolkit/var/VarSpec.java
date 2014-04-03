/*
 * Copyright 2013 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package ec.tstoolkit.var;

import ec.tstoolkit.Parameter;
import ec.tstoolkit.algorithm.IProcSpecification;
import ec.tstoolkit.data.Table;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.information.InformationSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Specification for a VAR model
 *
 * @author Jean Palate
 */
public class VarSpec implements IProcSpecification, Cloneable {

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

    public static final String NVARS = "nvars", NLAGS = "nlags", VPARAMS = "vparams",
            NPARAMS = "nparams", INIT = "initialization";

    /**
     * Number of variables
     */
    private int nvars;
    /**
     * Number of lags in the auto-regressive polynomial
     */
    private int nlags;
    /**
     * Parameters of the auto-regressive polynomial P(B)v(t) = P1*v(t-1) +
     * P2*v(t-2)...+ Pnlags*v(t-nlags) The parameters are arranged as follows: -
     * columns [0, nvars[ contain P1 - ... - columns [(nlags-1)*nvars,
     * nlags*nvars[ contain Pnlags
     */
    private Table<Parameter> vparams;
    /**
     * Parameters of the noise disturbances. Only the lower triangular part of
     * the matrix should be used.
     */
    private Table<Parameter> nparams;

    private Initialization init_ = Initialization.Zero;

    public VarSpec() {
    }

    public void setSize(final int nvars, final int nlags) {
        this.nvars = nvars;
        this.nlags = nlags;
        vparams = new Table<>(nvars, nvars * nlags);
        nparams = new Table<>(nvars, nvars);
    }

    public int getEquationsCount() {
        return nparams.getRowsCount();
    }

    public int getLagsCount() {
        return vparams.getColumnsCount() / vparams.getRowsCount();
    }

    public Table<Parameter> getVarParams() {
        return vparams;
    }

    public Table<Parameter> getNoiseParams() {
        return nparams;
    }

    public Initialization getInitialization() {
        return init_;
    }

    public void setInitialization(Initialization init) {
        init_ = init;
    }

    @Override
    public VarSpec clone() {
        try {
            VarSpec spec = (VarSpec) super.clone();
            spec.nparams = new Table<>(nparams);
            spec.vparams = new Table<>(vparams);
            return spec;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public InformationSet write(boolean verbose) {
        InformationSet info = new InformationSet();
        info.add(NVARS, nvars);
        info.add(NLAGS, nlags);
        info.add(INIT, init_.name());
        Parameter[] p = new Parameter[vparams.size()];
        vparams.copyTo(p);
        if (!Parameter.isDefault(p)) {
            info.add(VPARAMS, p);
        }
        Parameter[] n = new Parameter[nparams.size()];
        nparams.copyTo(n);
        if (!Parameter.isDefault(n)) {
            Parameter[] q = new Parameter[nvars * (nvars + 1) / 2];
            for (int c = 0, i = 0; c < nvars; ++c) {
                for (int r = c; r <= nvars; ++r, ++i) {
                    q[i] = nparams.get(r, c);
                }
            }
            info.add(NPARAMS, q);
        }
        return info;
    }

    @Override
    public boolean read(InformationSet info) {
        if (info == null) {
            return true;
        }
        Integer nv = info.get(NVARS, Integer.class);
        Integer nl = info.get(NLAGS, Integer.class);
        if (nv == null || nl == null) {
            return false;
        }
        setSize(nv, nl);
        String init = info.get(INIT, String.class);
        if (init != null) {
            init_ = Initialization.valueOf(init);
        }
        Parameter[] s = info.get(VPARAMS, Parameter[].class);
        if (s != null && !vparams.copyFrom(s)) {
            return false;
        }

        s = info.get(NPARAMS, Parameter[].class);
        if (s != null) {
            if (s.length != nvars * (nvars + 1) / 2) {
                return false;
            }
            for (int c = 0, i = 0; c < nvars; ++c) {
                for (int r = c; r <= nvars; ++r, ++i) {
                    nparams.set(r, c, s[i]);
                }
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof VarSpec && equals((VarSpec) obj));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + this.nvars;
        hash = 79 * hash + this.nlags;
        hash = 79 * hash + Objects.hashCode(this.init_);
        return hash;
    }

    public boolean equals(VarSpec spec) {
        if (spec.nlags != nlags || spec.nvars != nvars || init_ != spec.init_) {
            return false;
        }
        return spec.nparams.deepEquals(nparams)
                && spec.vparams.deepEquals(vparams);
    }

    public static void fillDictionary(String prefix, Map<String, Class> dic) {
        dic.put(InformationSet.item(prefix, NVARS), Integer.class);
        dic.put(InformationSet.item(prefix, NLAGS), Integer.class);
        dic.put(InformationSet.item(prefix, INIT), String.class);
        dic.put(InformationSet.item(prefix, VPARAMS), Parameter[].class);
        dic.put(InformationSet.item(prefix, NPARAMS), Parameter[].class);
    }
}
