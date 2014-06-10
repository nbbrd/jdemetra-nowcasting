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
import ec.tss.html.HtmlTable;
import ec.tss.html.HtmlTableCell;
import ec.tss.html.HtmlTableHeader;
import ec.tss.html.HtmlTag;
import ec.tstoolkit.MetaData;
import java.io.IOException;

/**
 *
 * @author Jean Palate
 */
public class HtmlVersions extends AbstractHtmlElement {

    private final VersionedDfmDocument doc_;

    public HtmlVersions(VersionedDfmDocument doc) {
        doc_ = doc;

    }

    @Override
    public void write(HtmlStream stream) throws IOException {
        int n = doc_.getVersionCount();
        if (n == 0) {
            stream.write(HtmlTag.HEADER3, h3, "No archive");
        } else {
            stream.write(HtmlTag.HEADER3, h3, "Archived versions");
            stream.newLine();
            stream.open(new HtmlTable(0, 300));

            stream.open(HtmlTag.TABLEROW);
            stream.write(new HtmlTableHeader("Version number"));
            stream.write(new HtmlTableHeader("Timestamp"));
            stream.close(HtmlTag.TABLEROW);

            for (int i = 0; i < n; ++i) {
                stream.open(HtmlTag.TABLEROW);
                stream.write(new HtmlTableCell(Integer.toString(i), 100));
                String date = doc_.getVersion(i).getMetaData().get(MetaData.DATE);
                stream.write(new HtmlTableCell(date, 200));
                stream.close(HtmlTag.TABLEROW);
            }
            stream.close(HtmlTag.TABLE);
        }
    }
}
