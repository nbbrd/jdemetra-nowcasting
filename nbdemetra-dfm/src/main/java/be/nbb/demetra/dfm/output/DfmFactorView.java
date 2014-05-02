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
import ec.tss.Dfm.DfmDocument;
import ec.tss.Dfm.DfmResults;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.utilities.DefaultInformationExtractor;
import ec.tstoolkit.utilities.Id;
import ec.tstoolkit.utilities.LinearId;
import ec.ui.view.tsprocessing.ComposedProcDocumentItemFactory;
import ec.ui.view.tsprocessing.EstimationUI;
import ec.ui.view.tsprocessing.IProcDocumentView;
import ec.ui.view.tsprocessing.ItemUI;
import ec.ui.view.tsprocessing.ProcDocumentItemFactory;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jean Palate
 */
@ServiceProvider(service = ProcDocumentItemFactory.class, position = 200120)
public class DfmFactorView extends ComposedProcDocumentItemFactory<DfmDocument, EstimationUI.Information> {

    public DfmFactorView() {
        super(DfmDocument.class, newId(), DfmResultsExtractor.INSTANCE, newItemUI());
    }

    private static Id newId() {
        return new LinearId("Model", "Factors");
    }

    private static ItemUI<IProcDocumentView<DfmDocument>, EstimationUI.Information> newItemUI() {
        return new EstimationUI();
    }

    private static final class DfmResultsExtractor extends DefaultInformationExtractor<DfmDocument, EstimationUI.Information> {

        public static final DfmResultsExtractor INSTANCE = new DfmResultsExtractor();

        @Override
        public EstimationUI.Information retrieve(DfmDocument source) {
            TsData o = null, l = null, u = null;
            DfmResults rslts = source.getDfmResults();
            if (rslts != null) {
                o = rslts.getFactor(0);
                TsData e = rslts.getFactorStdev(0);
                if (e != null) {
                    e = e.times(2);
                    u = TsData.add(o, e);
                    l = TsData.subtract(o, e);
                }
            }
            return new EstimationUI.Information(o, null, l, u);
        }
    }
}
