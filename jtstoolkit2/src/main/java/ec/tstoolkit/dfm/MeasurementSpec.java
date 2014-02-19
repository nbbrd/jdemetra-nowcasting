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

import ec.tstoolkit.Parameter;
import ec.tstoolkit.algorithm.IProcSpecification;
import ec.tstoolkit.information.InformationSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Jean Palate
 */
public class MeasurementSpec implements IProcSpecification {

    public static enum Transformation {

        Log,
        Sa,
        Diff1,
        DiffY
    }

    public static final String NAME = "name", SERIESTRANSFORMATIONS = "stransformations", 
            COEFF = "coeff", VAR = "var", FACTORTRANSFORMATION = "ftransformation",
            MEAN="mean", STDEV="stdev", DEF_NAME="var";
    private static volatile int g_idx=0;
    private String name;
    private Transformation[] transformations;
    private double mean=Double.NaN, stdev=Double.NaN;
    private Parameter[] coeff;
    private Parameter var;
    private DynamicFactorModel.MeasurementType type= DynamicFactorModel.MeasurementType.L;
    
    public MeasurementSpec(int nfac){
        name=DEF_NAME+(++g_idx);
        coeff=Parameter.create(nfac);
        transformations=new Transformation[0];
        var=new Parameter();
    }
    
    public MeasurementSpec(){
        this(0);
    }

    @Override
    public MeasurementSpec clone() {
        try {
            MeasurementSpec m = (MeasurementSpec) super.clone();
            if (coeff != null) {
                m.coeff = coeff.clone();
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
        info.set(NAME, name);
        if (Parameter.isDefined(coeff)) {
            info.set(COEFF, coeff);
        }
        if (Parameter.isDefined(var)) {
            info.set(VAR, var);
        }
        if (! Double.isNaN(mean))
            info.set(MEAN, mean);
        if (! Double.isNaN(stdev))
            info.set(STDEV, stdev);
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
            return false;
        }
        name = info.get(NAME, String.class);
        coeff = info.get(COEFF, Parameter[].class);
        var = info.get(VAR, Parameter.class);
        Double d=info.get(MEAN, Double.class);
        if (d != null)
            mean=d;
        d=info.get(STDEV, Double.class);
        if (d != null)
            stdev=d;
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
        type = DynamicFactorModel.MeasurementType.valueOf(t);

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
    public void setCoefficient(Parameter[] coeff) {
        this.coeff = coeff;
    }

    public Transformation[] getSeriesTransformations() {
        return transformations;
    }

    public void setSeriesTransformations(Transformation[] tr) {
        this.transformations = tr;
    }
    
    public double getMean(){
        return mean;
    }

    public void setMean(double mean){
        this.mean=mean;
    }
    
    public double getStdev(){
        return stdev;
    }

    public void setStdev(double e){
        this.stdev=e;
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
    
    public String getName(){
        return name;
    }
    
    public void setName(String name){
        this.name=name;
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
                && mean==spec.mean && stdev == spec.stdev;
    }

    public static void fillDictionary(String prefix, Map<String, Class> dic) {
        dic.put(InformationSet.item(prefix, NAME), String.class);
        dic.put(InformationSet.item(prefix, SERIESTRANSFORMATIONS), String[].class);
        dic.put(InformationSet.item(prefix, FACTORTRANSFORMATION), String.class);
        dic.put(InformationSet.item(prefix, MEAN), Double.class);
        dic.put(InformationSet.item(prefix, STDEV), Double.class);
        dic.put(InformationSet.item(prefix, VAR), Parameter.class);
        dic.put(InformationSet.item(prefix, COEFF), Parameter[].class);
    }

}
