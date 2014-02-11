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
import ec.tstoolkit.ParameterType;
import ec.tstoolkit.var.VarSpec;
import ec.tstoolkit.algorithm.IProcSpecification;
import ec.tstoolkit.data.Table;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.dfm.DynamicFactorModel.IMeasurement;
import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementDescriptor;
import ec.tstoolkit.information.Information;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jean Palate
 */
public class DfmSpecification implements IProcSpecification, Cloneable {

    public static final String BLOCK = "blocksize", VSPEC = "varspec", MSPEC = "mspec";
    private VarSpec vspec;
    private List<MeasurementSpec> mspecs = new ArrayList<MeasurementSpec>();
    private int blocksize;
    
 
    public void setBlockSize(final int blocksize) {
        this.blocksize = blocksize;
    }

    public int getBlockSize() {
        return blocksize;
    }

    @Override
    public DfmSpecification clone() {
        try {
            DfmSpecification spec = (DfmSpecification) super.clone();
            spec.vspec = vspec.clone();
            for (MeasurementSpec mspec : mspecs) {
                spec.mspecs.add(mspec.clone());
            }
            return spec;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public InformationSet write(boolean verbose) {
        InformationSet info = new InformationSet();
        info.add(BLOCK, blocksize);
        info.add(VSPEC, vspec.write(verbose));
        int i = 0;
        for (MeasurementSpec mspec : mspecs) {
            info.add(MSPEC + (i++), mspec.write(verbose));
        }
        return info;
    }

    @Override
    public boolean read(InformationSet info) {
        if (info == null) {
            return false;
        }
        Integer b = info.get(BLOCK, Integer.class);
        if (b == null) {
            return false;
        }
        blocksize = b;
        vspec = new VarSpec();
        if (vspec.read(info.getSubSet(VSPEC))) {
            return false;
        }
        mspecs.clear();
        List<Information<InformationSet>> sel = info.select(MSPEC+"*", InformationSet.class);
        for (Information<InformationSet> m : sel){
            MeasurementSpec x=new MeasurementSpec();
            if (x.read(m.value))
                mspecs.add(x);
        }
        return true;
    }
    
    public static DfmSpecification of (DynamicFactorModel m){
        DfmSpecification spec=new DfmSpecification();
        spec.blocksize=m.getBlockLength();
        spec.vspec=new VarSpec();
        spec.vspec.setSize(m.getFactorsCount(), m.getTransition().nlags);
        // fill the transition equation
        Table<Parameter> v = spec.vspec.getVarParams();
        Matrix vparams=m.getTransition().varParams;
        for (int r=0; r<v.getRowsCount(); ++r){
            for (int c=0; c<v.getColumnsCount(); ++c){
                v.set(r, c, new Parameter(vparams.get(r, c), ParameterType.Estimated));
            }
        }
       // copy noises
        Table<Parameter> n = spec.vspec.getNoiseParams();
        Matrix tvar=m.getTransition().covar;
        for (int r=0; r<n.getRowsCount(); ++r){
            for (int c=0; c<=r; ++c){
                n.set(r, c,new Parameter(tvar.get(r, c), ParameterType.Estimated));
            }
        }
        
        return spec;
    }
    
    public DynamicFactorModel build(){
        int nb=vspec.getEquationsCount(), nl=vspec.getLagsCount();
        DynamicFactorModel dfm=new DynamicFactorModel(blocksize, nb);
        DynamicFactorModel.TransitionDescriptor tdesc=
                new DynamicFactorModel.TransitionDescriptor(nb, nl);
        // copy equations
        Table<Parameter> v = vspec.getVarParams();
        for (int r=0; r<v.getRowsCount(); ++r){
            for (int c=0; c<v.getColumnsCount(); ++c){
                Parameter p=v.get(r, c);
                if (Parameter.isDefined(p))
                    tdesc.varParams.set(r,c,p.getValue());
            }
        }
       // copy noises
        Table<Parameter> n = vspec.getNoiseParams();
        for (int r=0; r<n.getRowsCount(); ++r){
            for (int c=0; c<=r; ++c){
                Parameter p=n.get(r, c);
                if (Parameter.isDefined(p))
                    tdesc.covar.set(r,c,p.getValue());
            }
        }
        SymmetricMatrix.fromLower(tdesc.covar);
        dfm.setTransition(tdesc);
        // measurements
        for (MeasurementSpec m: mspecs){
            IMeasurement type=DynamicFactorModel.measurement(m.getFactorsTransformation());
            double[] coeff=new double[nb];
            Parameter p[]=m.getCoefficients();
            for (int i=0; i<nb; ++i){
                if (Parameter.isDefined(p[i])){
                    if (p[i].isFixed() && p[i].getValue() == 0)
                        coeff[i]=Double.NaN;
                    else 
                        coeff[i]=p[i].getValue();
                }
                double var=1;
               if (Parameter.isDefault(m.getVariance()))
                   var=m.getVariance().getValue();
               dfm.addMeasurement(new MeasurementDescriptor(type, coeff,var));
            }
        }
        return dfm;
    }
}
