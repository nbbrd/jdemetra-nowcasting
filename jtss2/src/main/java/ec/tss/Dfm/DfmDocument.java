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
package ec.tss.Dfm;

import ec.tstoolkit.dfm.DfmModelSpec;
import ec.tss.documents.ActiveDocument;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.algorithm.ProcessingContext;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.timeseries.regression.TsVariables;

/**
 *
 * @author Jean Palate
 */
public class DfmDocument extends ActiveDocument<DfmSpec, TsVariables, CompositeResults> implements Cloneable{

   public DfmDocument(String desc){
        super(desc);
    }
   
    public DfmDocument(String desc, ProcessingContext context) {
        super(desc, context);
    }
    
    public void setVariables(TsVariables vars) {
        super.setInput(vars);
    }

    @Override
    public DfmDocument clone(){
        return (DfmDocument) super.clone();
    }
    
    @Override
    protected CompositeResults recalc(DfmSpec spec, TsVariables input) {
        return DfmProcessingFactory.instance.generateProcessing(spec, null).process(input);
    }
}  

