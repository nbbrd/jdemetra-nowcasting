/*
 * Copyright 2013 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
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
import ec.tss.dfm.DfmResults;
import ec.tstoolkit.utilities.Id;
import ec.tstoolkit.utilities.LinearId;
import ec.ui.view.tsprocessing.DefaultItemUI;
import ec.ui.view.tsprocessing.IProcDocumentView;
import ec.ui.view.tsprocessing.ItemUI;
import ec.ui.view.tsprocessing.ProcDocumentItemFactory;
import javax.swing.JComponent;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Mats Maggi
 */
@ServiceProvider(service = ProcDocumentItemFactory.class, position = 200320)
public class FitSignalUncertaintyFactory extends DfmResultsItemFactory {

    public FitSignalUncertaintyFactory() {
        super(newId(), newItemUI());
        setAsync(true);
    }

    private static Id newId() {
        return new LinearId("Forecasts", "News Uncertainty");
    }

    private static ItemUI<IProcDocumentView<DfmDocument>, Optional<DfmResults>> newItemUI() {
        return new DefaultItemUI<IProcDocumentView<DfmDocument>, Optional<DfmResults>>() {
            @Override
            public JComponent getView(IProcDocumentView<DfmDocument> host, Optional<DfmResults> information) {
                ConfidenceSignalGraphView result = new ConfidenceSignalGraphView();
                result.setDfmResults(information);
                return result;
            }
        };
    }
}
