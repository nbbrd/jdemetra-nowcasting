/*
 * Copyright 2013-2014 National Bank of Belgium
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

package ec.tss.dfm;

import ec.tstoolkit.dfm.MeasurementSpec;

/**
 *
 * @author Jean Palate
 */
public class DfmSeriesDescriptor {
    
    private static final MeasurementSpec.Transformation[] EMPTY=new MeasurementSpec.Transformation[0];
    
    public DfmSeriesDescriptor(String desc,MeasurementSpec.Transformation[] tr, double mean, double stdev){
        description=desc;
        transformations=tr == null ? EMPTY : tr.clone();
        this.mean=mean;
        this.stdev=stdev;
    }
    
    public DfmSeriesDescriptor(String desc,MeasurementSpec.Transformation[] tr){
        description=desc;
        transformations=tr == null ? EMPTY : tr.clone();
        this.mean=0;
        this.stdev=1;
    }

    public DfmSeriesDescriptor(int pos){
        description="var-"+(pos+1);
        transformations=EMPTY;
        this.mean=0;
        this.stdev=1;
    }

    public MeasurementSpec.Transformation[] transformations;
    public double mean, stdev;
    public String description;
    // Number of days between the end of the observation period and its publication
    public int delay;
    
    @Override
    public String toString(){
        return description;
    }
}
