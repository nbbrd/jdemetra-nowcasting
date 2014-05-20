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

package be.nbb.demetra.dfm.output;

import com.google.common.base.Optional;
import ec.tss.Ts;
import ec.tss.dfm.DfmDocument;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.utilities.Id;
import ec.tstoolkit.utilities.LinearId;
import ec.ui.view.tsprocessing.DefaultItemUI;
import ec.ui.view.tsprocessing.IProcDocumentView;
import ec.ui.view.tsprocessing.ItemUI;
import ec.ui.view.tsprocessing.ProcDocumentItemFactory;
import java.util.Arrays;
import javax.swing.JComponent;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jean Palate
 */
@ServiceProvider(service = ProcDocumentItemFactory.class, position = 100200)
public class InputGridFactory extends DfmDocumentItemFactory {

    public InputGridFactory() {
        super(newId(), newItemUI());
    }

    private static Id newId() {
        return new LinearId("Input", "Original data");
    }

    private static ItemUI<IProcDocumentView<DfmDocument>, Optional<CompositeResults>> newItemUI() {
        return new DefaultItemUI<IProcDocumentView<DfmDocument>, Optional<CompositeResults>>() {
            @Override
            public JComponent getView(IProcDocumentView<DfmDocument> host, Optional<CompositeResults> information) {
                Ts[] input = host.getDocument().getInput();
                return host.getToolkit().getGrid(Arrays.asList(input));
            }
        };
    }
}
