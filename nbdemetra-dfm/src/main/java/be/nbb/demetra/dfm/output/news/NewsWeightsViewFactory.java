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
package be.nbb.demetra.dfm.output.news;

import be.nbb.demetra.dfm.output.DfmNewsItemFactory;
import com.google.common.base.Optional;
import ec.tss.dfm.VersionedDfmDocument;
import ec.tstoolkit.dfm.DfmNews;
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
 * @author Jean Palate
 */
@ServiceProvider(service = ProcDocumentItemFactory.class, position = 300040)
public class NewsWeightsViewFactory extends DfmNewsItemFactory {

    public NewsWeightsViewFactory() {
        super(DfmNewsItemFactory.Updates.NEWS, newId(), newItemUI());
        setAsync(true);
    }

    private static Id newId() {
        return new LinearId("News", "Weights");
    }

    private static ItemUI<IProcDocumentView<VersionedDfmDocument>, Optional<DfmNews>> newItemUI() {
        return new DefaultItemUI<IProcDocumentView<VersionedDfmDocument>, Optional<DfmNews>>() {
            @Override
            public JComponent getView(IProcDocumentView<VersionedDfmDocument> host, Optional<DfmNews> information) {
                if (information.isPresent()) {
                    NewsWeightsView v = new NewsWeightsView();
                    v.setResults(host.getDocument().getCurrent().getDfmResults(), information.get());
                    return v;
                } else {
                    return null;
                }
            }
        };
    }

}
