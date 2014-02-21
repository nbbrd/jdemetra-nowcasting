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
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jean Palate
 */
public class DfmModelSpec implements IProcSpecification, Cloneable {

    public static final String VSPEC = "var", MSPEC = "measurement", MSPECS = "measurement*";
    private VarSpec vspec;
    private final List<MeasurementSpec> mspecs = new ArrayList<>();

    public DfmModelSpec(){
        vspec=new VarSpec();
        vspec.setSize(2, 2);
    }
    
    public VarSpec getVarSpec() {
        return vspec;
    }

    public void setVarSpec(VarSpec spec) {
        vspec = spec;
    }

    public List<MeasurementSpec> getMeasurements() {
        return mspecs;
    }

    @Override
    public DfmModelSpec clone() {
        try {
            DfmModelSpec spec = (DfmModelSpec) super.clone();
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
        vspec = new VarSpec();
        if (vspec.read(info.getSubSet(VSPEC))) {
            return false;
        }
        mspecs.clear();
        List<Information<InformationSet>> sel = info.select(MSPEC + "*", InformationSet.class);
        for (Information<InformationSet> m : sel) {
            MeasurementSpec x = new MeasurementSpec();
            if (x.read(m.value)) {
                mspecs.add(x);
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof DfmModelSpec && equals((DfmModelSpec) obj));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.vspec);
        return hash;
    }

    public boolean equals(DfmModelSpec spec) {
        if (!vspec.equals(spec.vspec)) {
            return false;
        }
        if (mspecs.size() != spec.mspecs.size()) {
            return false;
        }
        for (int i = 0; i < mspecs.size(); ++i) {
            if (!mspecs.get(i).equals(spec.mspecs.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static DfmModelSpec of(DynamicFactorModel m) {
        DfmModelSpec spec = new DfmModelSpec();
        spec.vspec = new VarSpec();
        spec.vspec.setSize(m.getFactorsCount(), m.getTransition().nlags);
        // fill the transition equation
        Table<Parameter> v = spec.vspec.getVarParams();
        Matrix vparams = m.getTransition().varParams;
        for (int r = 0; r < v.getRowsCount(); ++r) {
            for (int c = 0; c < v.getColumnsCount(); ++c) {
                v.set(r, c, new Parameter(vparams.get(r, c), ParameterType.Estimated));
            }
        }
        // copy noises
        Table<Parameter> n = spec.vspec.getNoiseParams();
        Matrix tvar = m.getTransition().covar;
        for (int r = 0; r < n.getRowsCount(); ++r) {
            for (int c = 0; c <= r; ++c) {
                n.set(r, c, new Parameter(tvar.get(r, c), ParameterType.Estimated));
            }
        }

        return spec;
    }

    public DynamicFactorModel build() {
        int nb = vspec.getEquationsCount(), nl = vspec.getLagsCount();
        int blocksize = 0;
        for (MeasurementSpec m : mspecs) {
            IMeasurement type = DynamicFactorModel.measurement(m.getFactorsTransformation());
            int len = type.getLength();
            if (len > blocksize) {
                blocksize = len;
            }
        }
        if (blocksize < nl) {
            blocksize = nl;
        }
        DynamicFactorModel dfm = new DynamicFactorModel(blocksize, nb);
        DynamicFactorModel.TransitionDescriptor tdesc
                = new DynamicFactorModel.TransitionDescriptor(nb, nl);
        // copy equations
        Table<Parameter> v = vspec.getVarParams();
        for (int r = 0; r < v.getRowsCount(); ++r) {
            for (int c = 0; c < v.getColumnsCount(); ++c) {
                Parameter p = v.get(r, c);
                if (Parameter.isDefined(p)) {
                    tdesc.varParams.set(r, c, p.getValue());
                }
            }
        }
        // copy noises
        Table<Parameter> n = vspec.getNoiseParams();
        for (int r = 0; r < n.getRowsCount(); ++r) {
            for (int c = 0; c <= r; ++c) {
                Parameter p = n.get(r, c);
                if (Parameter.isDefined(p)) {
                    tdesc.covar.set(r, c, p.getValue());
                }
            }
        }
        SymmetricMatrix.fromLower(tdesc.covar);
        dfm.setTransition(tdesc);
        // measurements
        for (MeasurementSpec m : mspecs) {
            IMeasurement type = DynamicFactorModel.measurement(m.getFactorsTransformation());
            double[] coeff = new double[nb];
            Parameter p[] = m.getCoefficients();
            for (int i = 0; i < nb; ++i) {
                if (Parameter.isDefined(p[i])) {
                    if (p[i].isFixed() && p[i].getValue() == 0) {
                        coeff[i] = Double.NaN;
                    } else {
                        coeff[i] = p[i].getValue();
                    }
                }
            }
            double var = 1;
            if (!Parameter.isDefault(m.getVariance())) {
                var = m.getVariance().getValue();
            }
            dfm.addMeasurement(new MeasurementDescriptor(type, coeff, var));
        }
        dfm.setInitialization(vspec.getInitialization());
        return dfm;
    }

    public static void fillDictionary(String prefix, Map<String, Class> dic) {
        VarSpec.fillDictionary(InformationSet.item(prefix, VSPEC), dic);
        MeasurementSpec.fillDictionary(InformationSet.item(prefix, MSPECS), dic);
    }

}
