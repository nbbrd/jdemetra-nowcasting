/*
 * Copyright 2013 National Bank of Belgium
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
package be.nbb.demetra.dfm.output.html;

import ec.tss.dfm.DfmSeriesDescriptor;
import ec.tss.html.AbstractHtmlElement;
import ec.tss.html.HtmlStream;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.dfm.DfmInformationSet;
import ec.tstoolkit.dfm.DfmInformationUpdates;
import ec.tstoolkit.dfm.DfmNews;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Jean Palate
 */
public class HtmlNewsWeights extends AbstractHtmlElement {
    
    private final DfmNews doc_;
    private final DfmSeriesDescriptor[] desc_;
    
    public HtmlNewsWeights(DfmSeriesDescriptor[] desc, DfmNews doc) {
        desc_ = desc;
        doc_ = doc;
    }
    
    @Override
    public void write(HtmlStream stream) throws IOException {
        DfmInformationUpdates details = doc_.newsDetails();
        List<DfmInformationUpdates.Update> updates = details.updates();
        if (updates.isEmpty()) {
            stream.write("No updates");
            return;
        } else {
            DataBlock n = doc_.news();
            DfmInformationSet data = doc_.getNewInformationSet();
            for (int i = 0; i < data.getSeriesCount(); ++i) {
                TsData s = data.series(i);
                int pos=s.getDomain().search(doc_.getNewsDomain().getStart().firstday());
                if (pos < 0)
                    continue;
                TsFrequency freq=doc_.getNewsDomain().getFrequency();
                for (int j = pos; j < s.getLength(); ++j) {
                    int cur=0;
                    if (s.isMissing(j)) {
                        ++cur;
                        TsPeriod p = s.getDomain().get(j).lastPeriod(freq);
                        stream.write(i);
                        stream.write('\t');
                        stream.write(p.toString());
                        stream.write('\t');
                        DataBlock weights = doc_.weights(i, p);
                        stream.write(n.dot(weights));
                        stream.write('\t');
                        stream.write(doc_.getOldForecast(i, p));
                        stream.write('\t');
                        stream.write(doc_.getNewForecast(i, p));
                        stream.write('\t');
                        stream.write(weights.toString());
                        stream.newLine();
                    }
                    if (cur>3)
                        break;
                }
            }
        }
    }
}
