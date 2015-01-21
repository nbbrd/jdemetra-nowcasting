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
package ec.tstoolkit.dfm;

import ec.satoolkit.ISaSpecification;
import ec.tstoolkit.Parameter;
import ec.tstoolkit.ParameterType;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.algorithm.IProcSpecification;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.timeseries.simplets.TsData;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Jean Palate
 */
public class MeasurementSpec implements IProcSpecification, Cloneable {

    public static enum Transformation {

        Log,
        Sa,
        Diff1,
        DiffY
    }

    public static final String SERIESTRANSFORMATIONS = "stransformations",
            COEFF = "coeff", VAR = "var", FACTORTRANSFORMATION = "ftransformation",
            MEAN = "mean", STDEV = "stdev", DEF_NAME = "var";
    private static volatile int g_idx = 0;
    private Transformation[] transformations;
    private double mean = Double.NaN, stdev = Double.NaN;
    private Parameter[] coeff;
    private Parameter var;
    private DynamicFactorModel.MeasurementType type = DynamicFactorModel.MeasurementType.M;

    public MeasurementSpec() {
        this(0);
    }

    public MeasurementSpec(int nfac) {
        coeff = Parameter.create(nfac);
        transformations = new Transformation[0];
        var = new Parameter();
    }

    public void clear() {
        for (int i = 0; i < coeff.length; ++i) {
            if (!coeff[i].isFixed()) {
                coeff[i] = new Parameter();
            }
        }
        mean = Double.NaN;
        stdev = Double.NaN;
        var = new Parameter();
    }

    private static boolean isSpecified(Parameter p) {
        if (Parameter.isDefault(p)) {
            return false;
        }
        return p.getType() != ParameterType.Initial;
    }

    public boolean isSpecified() {
        if (!isSpecified(var)) {
            return false;
        }
        for (int i = 0; i < coeff.length; ++i) {
            if (!isSpecified(coeff[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean isDefined() {
        return Parameter.isDefined(var) && Parameter.isDefined(coeff);
    }

    public boolean setParameterType(ParameterType type) {
        if (Parameter.isDefault(var)) {
            return false;
        }
        var.setType(type);
        for (int i = 0; i < coeff.length; ++i) {
            if (Parameter.isDefault(coeff[i])) {
                return false;
            }
            if (!coeff[i].isFixed()) {
                coeff[i].setType(type);
            }
        }
        return true;
    }

    @Override
    public MeasurementSpec clone() {
        try {
            MeasurementSpec m = (MeasurementSpec) super.clone();
            if (coeff != null) {
                m.coeff = Parameter.clone(coeff);
            }
            if (var != null) {
                m.var = var.clone();
            }
            if (transformations != null) {
                m.transformations = transformations.clone();
            }
            return m;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public InformationSet write(boolean verbose) {
        InformationSet info = new InformationSet();
        if (!Parameter.isDefault(coeff)) {
            info.set(COEFF, coeff);
        }
        if (!Parameter.isDefault(var)) {
            info.set(VAR, var);
        }
        if (!Double.isNaN(mean)) {
            info.set(MEAN, mean);
        }
        if (!Double.isNaN(stdev)) {
            info.set(STDEV, stdev);
        }
        if (transformations != null) {
            String[] t = new String[transformations.length];
            for (int i = 0; i < t.length; ++i) {
                t[i] = transformations[i].name();
            }
            info.set(SERIESTRANSFORMATIONS, t);
        }
        info.set(FACTORTRANSFORMATION, type.name());
        return info;
    }

    @Override
    public boolean read(InformationSet info) {
        if (info == null) {
            return true;
        }
        Parameter[] c = info.get(COEFF, Parameter[].class);
        if (c != null) {
            coeff = c;
        }
        Parameter v = info.get(VAR, Parameter.class);
        if (v != null) {
            var = v;
        }
        Double d = info.get(MEAN, Double.class);
        if (d != null) {
            mean = d;
        }
        d = info.get(STDEV, Double.class);
        if (d != null) {
            stdev = d;
        }
        String[] tr = info.get(SERIESTRANSFORMATIONS, String[].class);
        if (tr != null) {
            transformations = new Transformation[tr.length];
            for (int i = 0; i < tr.length; ++i) {
                transformations[i] = Transformation.valueOf(tr[i]);
            }
        }
        String t = info.get(FACTORTRANSFORMATION, String.class);
        if (t == null) {
            return false;
        }
        switch (t) {
            case "L":
                type = DynamicFactorModel.MeasurementType.M;
                break;
            case "CD":
                type = DynamicFactorModel.MeasurementType.Q;
                break;
            case "C":
                type = DynamicFactorModel.MeasurementType.YoY;
                break;
            default:
                type = DynamicFactorModel.MeasurementType.valueOf(t);
        }
        return type != null;
    }

    /**
     * @return the coeff
     */
    public Parameter[] getCoefficients() {
        return coeff;
    }

    /**
     * @param coeff the coeff to set
     */
    public void setCoefficients(Parameter[] coeff) {
        this.coeff = coeff;
    }

    void setCoefficient(int j, Parameter c) {
        this.coeff[j] = c;
    }

    public Transformation[] getSeriesTransformations() {
        return transformations;
    }

    public void setSeriesTransformations(Transformation[] tr) {
        this.transformations = tr;
        this.mean = Double.NaN;
        this.stdev = Double.NaN;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getStdev() {
        return stdev;
    }

    public void setStdev(double e) {
        this.stdev = e;
    }

    /**
     * @return the var
     */
    public Parameter getVariance() {
        return var;
    }

    /**
     * @param var the var to set
     */
    public void setVariance(Parameter var) {
        this.var = var;
    }

    /**
     * @return the type
     */
    public DynamicFactorModel.MeasurementType getFactorsTransformation() {
        return type;
    }

    /**
     * @param type the transformation of the factors to set
     */
    public void setFactorsTransformation(DynamicFactorModel.MeasurementType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof MeasurementSpec && equals((MeasurementSpec) obj));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.var);
        hash = 17 * hash + Objects.hashCode(this.type);
        return hash;
    }

    public boolean equals(MeasurementSpec spec) {
        return type == spec.type && var.equals(spec.var) && Arrays.deepEquals(coeff, spec.coeff)
                && Arrays.equals(transformations, spec.transformations)
                && mean == spec.mean && stdev == spec.stdev;
    }

    public static void fillDictionary(String prefix, Map<String, Class> dic) {
        dic.put(InformationSet.item(prefix, SERIESTRANSFORMATIONS), String[].class);
        dic.put(InformationSet.item(prefix, FACTORTRANSFORMATION), String.class);
        dic.put(InformationSet.item(prefix, MEAN), Double.class);
        dic.put(InformationSet.item(prefix, STDEV), Double.class);
        dic.put(InformationSet.item(prefix, VAR), Parameter.class);
        dic.put(InformationSet.item(prefix, COEFF), Parameter[].class);
    }

}
