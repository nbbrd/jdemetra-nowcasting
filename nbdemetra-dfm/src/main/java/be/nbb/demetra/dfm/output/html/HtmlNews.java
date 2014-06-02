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
import ec.tstoolkit.dfm.DfmInformationUpdates;
import ec.tstoolkit.dfm.DfmNews;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Jean Palate
 */
public class HtmlNews extends AbstractHtmlElement {
    private final DfmNews doc_;
    private final DfmSeriesDescriptor[] desc_;
    public HtmlNews(DfmSeriesDescriptor[] desc, DfmNews doc){
        desc_=desc;
        doc_=doc;
    }   

    @Override
    public void write(HtmlStream stream) throws IOException {
        DfmInformationUpdates details = doc_.newsDetails();
        List<DfmInformationUpdates.Update> updates = details.updates();
        if (updates.isEmpty()){
            stream.write("No updates");
            return;
        }else{
            for (DfmInformationUpdates.Update updt : updates){
                DfmSeriesDescriptor desc=desc_[updt.series];
                stream.write(desc.description);
                stream.write("    ");
                stream.write(updt.period.toString());
                stream.write("    ");
                stream.write(updt.getForecast()*desc.stdev+desc.mean);
                stream.write("    ");
                stream.write(updt.getObservation()*desc.stdev+desc.mean);
                stream.write("    ");
                stream.newLine();
                
            }
        }
    }
}
