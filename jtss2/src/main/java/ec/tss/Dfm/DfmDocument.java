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
public class DfmDocument extends ActiveDocument<DfmSpec, TsVariables, CompositeResults> implements Cloneable {

    public static final String DFM = "Dynamic factor model";

    public DfmDocument() {
        super(DFM);
        setSpecification(new DfmSpec());
    }

    public DfmDocument(ProcessingContext context) {
        super(DFM, context);
        setSpecification(new DfmSpec());
    }

    public void setVariables(TsVariables vars) {
        super.setInput(vars);
    }

    @Override
    public DfmDocument clone() {
        return (DfmDocument) super.clone();
    }

    @Override
    protected CompositeResults recalc(DfmSpec spec, TsVariables input) {
        return DfmProcessingFactory.instance.generateProcessing(spec, null).process(input);
    }
    
    @Override
    public InformationSet write(boolean verbose) {
        InformationSet info = super.write(verbose);
        info.set(INPUT, this.getInput().write(verbose));
        info.set(SPEC, this.getSpecification().write(verbose));
        return info;
    }

    @Override
    public boolean read(InformationSet info) {
        if (!super.read(info)) {
            return false;
        }
        InformationSet sinfo = info.getSubSet(SPEC);
        if (sinfo == null) {
            return false;
        }
        DfmSpec spec = new DfmSpec();
        if (!spec.read(sinfo)) {
            return false;
        }
       
        setSpecification(spec);
        InformationSet iinfo = info.getSubSet(INPUT);
        if (iinfo == null) {
            return false;
        }
        TsVariables var = new TsVariables("s", new DefaultNameValidator("+-*=.;"));
        if (!var.read(iinfo)) {
            return false;
        }
        this.setInput(var);

        return true;
    }
    
}
