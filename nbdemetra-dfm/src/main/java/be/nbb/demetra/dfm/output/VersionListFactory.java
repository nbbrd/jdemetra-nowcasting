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

import be.nbb.demetra.dfm.output.html.HtmlVersions;
import ec.tss.dfm.VersionedDfmDocument;
import ec.tstoolkit.utilities.Id;
import ec.tstoolkit.utilities.LinearId;
import ec.ui.view.tsprocessing.ComposedProcDocumentItemFactory;
import ec.ui.view.tsprocessing.DefaultItemUI;
import ec.ui.view.tsprocessing.IProcDocumentView;
import ec.ui.view.tsprocessing.ItemUI;
import ec.ui.view.tsprocessing.ProcDocumentItemFactory;
import ec.ui.view.tsprocessing.ProcDocumentViewFactory.DoNothingExtractor;
import javax.swing.JComponent;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jean Palate
 */
@ServiceProvider(service = ProcDocumentItemFactory.class, position = 300000)
public class VersionListFactory extends ComposedProcDocumentItemFactory<VersionedDfmDocument, VersionedDfmDocument>{

    public VersionListFactory() {
        super(VersionedDfmDocument.class, newId(), new DoNothingExtractor<VersionedDfmDocument>(), newItemUI());
    }

    private static Id newId() {
        return new LinearId("Versions");
    }

    private static ItemUI<IProcDocumentView<VersionedDfmDocument>, VersionedDfmDocument> newItemUI() {
        return new DefaultItemUI<IProcDocumentView<VersionedDfmDocument>, VersionedDfmDocument>() {
            @Override
            public JComponent getView(IProcDocumentView<VersionedDfmDocument> host, VersionedDfmDocument information) {
                return host.getToolkit().getHtmlViewer(new HtmlVersions(host.getDocument()));
            }
        };
    }
}
