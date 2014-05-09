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
package ec.tss.dfm;

import ec.tss.Ts;
import ec.tss.documents.VersionedDocument;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.dfm.DfmSpec;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jean Palate
 */
public class VersionedDfmDocument extends VersionedDocument<DfmSpec, Ts[], CompositeResults, DfmDocument>
        implements Cloneable {

    public VersionedDfmDocument() {
        super(new DfmDocument());
    }

    @Override
    public VersionedDfmDocument clone() {
        try {
            VersionedDfmDocument doc = (VersionedDfmDocument) super.clone();
            setCurrent(getCurrent().clone());
            doc.clearVersions(0);
            for (int i = 0; i < getVersionCount(); ++i) {
                doc.add(getVersion(i).clone());
            }
            return doc;
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }

    @Override
    protected DfmDocument newDocument() {
        return new DfmDocument(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected DfmDocument restore(DfmDocument document) {
        document.setLocked(false);
        document.unfreezeTs();
        return document;
    }

    @Override
    protected DfmDocument archive(DfmDocument document) {
        document.setLocked(true);
        document.freezeTs();
        return document;
    }

    public void unfreeze() {
        DfmDocument current = getCurrent();
        if (current != null) {
            current.unfreezeTs();
        }
    }

}
