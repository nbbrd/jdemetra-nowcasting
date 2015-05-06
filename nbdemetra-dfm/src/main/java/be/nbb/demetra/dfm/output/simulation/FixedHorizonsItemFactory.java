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
package be.nbb.demetra.dfm.output.simulation;

import be.nbb.demetra.dfm.output.DfmSimulationResultsItemFactory;
import com.google.common.base.Optional;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.DfmSimulation;
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
@ServiceProvider(service = ProcDocumentItemFactory.class, position = 400100)
public class FixedHorizonsItemFactory extends DfmSimulationResultsItemFactory {
    public FixedHorizonsItemFactory() {
        super(newId(), newItemUI());
        setAsync(true);
    }

    private static Id newId() {
        return new LinearId("Forecasts Simulation", "Fixed horizons");
    }
    
    private static ItemUI<IProcDocumentView<DfmDocument>, Optional<DfmSimulation>> newItemUI() {
        return new DefaultItemUI<IProcDocumentView<DfmDocument>, Optional<DfmSimulation>>() {
            @Override
            public JComponent getView(IProcDocumentView<DfmDocument> host, Optional<DfmSimulation> information) {
                FixedHorizonsGraphView result = new FixedHorizonsGraphView(host.getDocument());
                result.setSimulationResults(information);
                return result;
            }
        };
    }
}
