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
import ec.tstoolkit.information.InformationSet;
import java.util.Arrays;
import java.util.Map;

/**
 * Specification for a VAR model
 *
 * @author Jean Palate
 */
public class VarSpec implements IProcSpecification, Cloneable {

    public static final String NVARS = "nvars", NLAGS = "nlags", VPARAMS = "vparams",
            NPARAMS = "nparams";

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
        Parameter[] p = vparams.storage();
        if (!Parameter.isDefault(p)) {
            info.add(VPARAMS, p);
        }
        if (!Parameter.isDefault(nparams.storage())) {
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
            return false;
        }
        Integer nv = info.get(NVARS, Integer.class);
        Integer nl = info.get(NLAGS, Integer.class);
        if (nv == null || nl == null) {
            return false;
        }
        setSize(nv, nl);
        Parameter[] s = info.get(VPARAMS, Parameter[].class);
        if (s != null) {
            Parameter[] t = vparams.storage();
            if (t.length != s.length) {
                return false;
            }
            for (int i = 0; i < t.length; ++i) {
                t[i] = s[i];
            }
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
        int hash = 5;
        hash = 41 * hash + this.nvars;
        hash = 41 * hash + this.nlags;
        return hash;
    }

    public boolean equals(VarSpec spec) {
        if (spec.nlags != nlags || spec.nvars != nvars) {
            return false;
        }
        return Arrays.deepEquals(spec.nparams.storage(), nparams.storage())
                && Arrays.deepEquals(spec.vparams.storage(), vparams.storage());
    }
    
     public static void fillDictionary(String prefix, Map<String, Class> dic) {
        dic.put(InformationSet.item(prefix, NVARS), Integer.class);
        dic.put(InformationSet.item(prefix, NLAGS), Integer.class);
        dic.put(InformationSet.item(prefix, VPARAMS), Parameter[].class);
        dic.put(InformationSet.item(prefix, NPARAMS), Parameter[].class);
     }
}
