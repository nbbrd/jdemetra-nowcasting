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

import ec.tss.dfm.VersionedDfmDocument;
import ec.tss.html.AbstractHtmlElement;
import ec.tss.html.HtmlStream;
import ec.tstoolkit.MetaData;
import java.io.IOException;

/**
 *
 * @author Jean Palate
 */
public class HtmlVersions extends AbstractHtmlElement {
    private final VersionedDfmDocument doc_;
    public HtmlVersions(VersionedDfmDocument doc){
        doc_=doc;
        
    }   

    @Override
    public void write(HtmlStream stream) throws IOException {
        stream.write(doc_.getVersionCount());
        //doc_.getVersion(0).getMetaData().get(MetaData.DATE);
    }
}
