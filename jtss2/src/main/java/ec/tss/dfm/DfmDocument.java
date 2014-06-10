/*
 * Copyright 2014 National Bank of Belgium
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

import ec.tss.Ts;
import ec.tss.documents.MultiTsDocument;
import ec.tstoolkit.ParameterType;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.algorithm.ProcessingContext;
import ec.tstoolkit.dfm.DfmEstimationSpec;
import ec.tstoolkit.dfm.DfmSpec;

/**
 *
 * @author Jean Palate
 */
public class DfmDocument extends MultiTsDocument<DfmSpec, CompositeResults> implements Cloneable {

   public DfmDocument() {
        super(new DfmProcessingFactory(), null);
        setSpecification(new DfmSpec());
    }
    
    public DfmDocument(ProcessingContext context) {
        super(new DfmProcessingFactory(), context);
        setSpecification(new DfmSpec());
    }

    public DfmDocument clone() {
        DfmDocument doc = (DfmDocument) super.clone();
        doc.factory_ = new DfmProcessingFactory();
        return doc;
    }

    public DfmResults getDfmResults() {
        CompositeResults rslts = getResults();
        if (rslts == null) {
            return null;
        } else {
            return rslts.get(DfmProcessingFactory.DFM, DfmResults.class);
        }
    }
    

    @Override
    protected CompositeResults recalc(DfmSpec spec, Ts[] input) {
        CompositeResults rslts = super.recalc(spec, input);
        if (rslts != null) {
            DfmResults dr = rslts.get(DfmProcessingFactory.DFM, DfmResults.class);
            if (dr != null) {
                DfmSeriesDescriptor[] desc = dr.getDescriptions();
                for (int i = 0; i < desc.length; ++i) {
                    desc[i].description = input[i].getRawName();
                }
                dr.setDescriptions(desc);
                if (!spec.getModelSpec().isSpecified()) {
                    DfmProcessingFactory.update(spec, dr, false);
                    setDirty();
                }
            }
        }
        return rslts;
    }

    public void updateSpecification() {
        if (!isLocked()) {
            CompositeResults rslts = this.getResults();
            if (rslts == null) {
                return;
            }
            DfmResults dr = rslts.get(DfmProcessingFactory.DFM, DfmResults.class);
            if (dr == null) {
                return;
            }
            DfmProcessingFactory.update(this.getSpecification(), dr, false);
        }
    }

    public void forceSpecification(DfmEstimationSpec espec) {
        DfmSpec spec = getSpecification().clone();
        if ( ! spec.getModelSpec().setParameterType(ParameterType.Initial))
            spec= getSpecification().cloneDefinition();
        if (espec != null) {
            spec.setEstimationSpec(espec);
        }
        boolean locked = isLocked();
        if (locked) {
            setLocked(false);
        }
        setSpecification(spec);
        if (locked) {
            setLocked(true);
        }
    }
    
    public void forceFullComputation(){
        DfmSpec spec = getSpecification().cloneDefinition();
        boolean locked = isLocked();
        if (locked) {
            setLocked(false);
        }
        setSpecification(spec);
        if (locked) {
            setLocked(true);
        }
    }
}
