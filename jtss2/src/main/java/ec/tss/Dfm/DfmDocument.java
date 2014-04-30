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

import ec.tss.documents.ActiveDocument;
import ec.tss.documents.MultiTsDocument;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.algorithm.ProcessingContext;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.timeseries.regression.TsVariables;
import ec.tstoolkit.utilities.DefaultNameValidator;

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
    
    public DfmDocument clone(){
        DfmDocument doc=(DfmDocument) super.clone();
        doc.factory_=new DfmProcessingFactory();
        return doc;
    }
    
}
