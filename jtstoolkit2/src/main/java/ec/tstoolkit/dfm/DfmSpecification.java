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

import ec.tstoolkit.var.VarSpecification;
import ec.tstoolkit.algorithm.IProcSpecification;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.information.Information;
import ec.tstoolkit.information.InformationSet;
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
    private VarSpecification vspec;
    private List<MeasurementSpecification> mspecs = new ArrayList<>();
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
            for (MeasurementSpecification mspec : mspecs) {
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
        for (MeasurementSpecification mspec : mspecs) {
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
        vspec = new VarSpecification();
        if (vspec.read(info.getSubSet(VSPEC))) {
            return false;
        }
        mspecs.clear();
        List<Information<InformationSet>> sel = info.select(MSPEC+"*", InformationSet.class);
        for (Information<InformationSet> m : sel){
            MeasurementSpecification x=new MeasurementSpecification();
            if (x.read(m.value))
                mspecs.add(x);
        }
        return true;
    }
}
