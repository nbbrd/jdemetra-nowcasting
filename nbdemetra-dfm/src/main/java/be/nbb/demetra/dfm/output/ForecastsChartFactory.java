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
import ec.tss.Ts;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.DfmProcessingFactory;
import ec.tss.dfm.DfmResults;
import ec.tss.documents.DocumentManager;
import ec.tstoolkit.information.InformationSet;
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
 * @author Philippe Charles
 */
@ServiceProvider(service = ProcDocumentItemFactory.class, position = 200310)
public class ForecastsChartFactory extends DfmResultsItemFactory {

    public ForecastsChartFactory() {
        super(newId(), newItemUI());
    }

    private static Id newId() {
        return new LinearId("Estimation", "Forecasts", "Forecasts Uncertainty");
    }

    private static ItemUI<IProcDocumentView<DfmDocument>, Optional<DfmResults>> newItemUI() {
        return new DefaultItemUI<IProcDocumentView<DfmDocument>, Optional<DfmResults>>() {
            @Override
            public JComponent getView(IProcDocumentView<DfmDocument> host, Optional<DfmResults> information) {
                ConfidenceGraphView result = new ConfidenceGraphView();
                
                Ts[] input = host.getDocument().getInput().clone();
                Ts[] stdevs = host.getDocument().getInput().clone();
                String prefix=InformationSet.item(DfmProcessingFactory.DFM, "smoothed");
                String prefix2=InformationSet.item(DfmProcessingFactory.DFM, "esmoothed");
                for (int i=0; i<input.length; ++i){
                    Ts ts = DocumentManager.instance.getTs(host.getDocument(), 
                            prefix+(i+1));
                    if (ts != null){
                        input[i]=ts.rename(input[i].getRawName());
                    }
                    
                    ts = DocumentManager.instance.getTs(host.getDocument(), 
                            prefix2+(i+1));
                    if (ts != null){
                        stdevs[i]=ts.rename(stdevs[i].getRawName());
                    }
                }
                
                result.setResults(input, stdevs);
                return result;
            }
        };
    }
}
