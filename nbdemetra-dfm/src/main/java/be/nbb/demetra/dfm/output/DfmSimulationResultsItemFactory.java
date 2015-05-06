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
package be.nbb.demetra.dfm.output;

import com.google.common.base.Optional;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.DfmSimulation;
import ec.tstoolkit.utilities.DefaultInformationExtractor;
import ec.tstoolkit.utilities.Id;
import ec.ui.view.tsprocessing.ComposedProcDocumentItemFactory;
import ec.ui.view.tsprocessing.IProcDocumentView;
import ec.ui.view.tsprocessing.ItemUI;

/**
 *
 * @author Mats Maggi
 */
public class DfmSimulationResultsItemFactory extends ComposedProcDocumentItemFactory<DfmDocument, Optional<DfmSimulation>> {
    
    protected DfmSimulationResultsItemFactory(Id itemId, ItemUI<? extends IProcDocumentView<DfmDocument>, Optional<DfmSimulation>> itemUI) {
        super(DfmDocument.class, itemId, DfmSimulationResultsExtractor.INSTANCE, itemUI);
    }
    
    private static final class DfmSimulationResultsExtractor extends DefaultInformationExtractor<DfmDocument, Optional<DfmSimulation>> {

        public static final DfmSimulationResultsExtractor INSTANCE = new DfmSimulationResultsExtractor();

        @Override
        public Optional<DfmSimulation> retrieve(DfmDocument source) {
            return Optional.fromNullable(source.getSimulationResults());
        }
    }
}
