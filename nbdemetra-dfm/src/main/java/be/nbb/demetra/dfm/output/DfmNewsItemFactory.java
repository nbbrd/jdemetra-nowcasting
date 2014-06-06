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
import ec.tss.dfm.VersionedDfmDocument;
import ec.tstoolkit.dfm.DfmNews;
import ec.tstoolkit.utilities.Id;
import ec.ui.view.tsprocessing.ComposedProcDocumentItemFactory;
import ec.ui.view.tsprocessing.DocumentInformationExtractor;
import ec.ui.view.tsprocessing.IProcDocumentView;
import ec.ui.view.tsprocessing.ItemUI;

/**
 *
 * @author Philippe Charles
 */
public abstract class DfmNewsItemFactory extends ComposedProcDocumentItemFactory<VersionedDfmDocument, Optional<DfmNews>> {

    protected static enum Updates{
        NONE, NEWS, REVISIONS
    }
    
    protected DfmNewsItemFactory(Updates news, Id itemId, ItemUI<? extends IProcDocumentView<VersionedDfmDocument>, Optional<DfmNews>> itemUI) {
        super(VersionedDfmDocument.class, itemId, DfmNewsExtractor.from(news), itemUI);
    }

    private static final class DfmNewsExtractor extends DocumentInformationExtractor<VersionedDfmDocument, Optional<DfmNews>> {

        static DfmNewsExtractor from(Updates upd){
            switch (upd){
                case REVISIONS: return REV_INSTANCE;
                case NEWS:return NEWS_INSTANCE;
                default:return null;
            }
        }
        
        private static final DfmNewsExtractor NEWS_INSTANCE = new DfmNewsExtractor(Updates.NEWS);

        private static final DfmNewsExtractor REV_INSTANCE = new DfmNewsExtractor(Updates.REVISIONS);
        
        private final Updates updt;
        
        DfmNewsExtractor(Updates updt){
            this.updt=updt;
        }
    
        @Override
        protected Optional<DfmNews> buildInfo(VersionedDfmDocument source) {
            switch (updt){
                case REVISIONS:return Optional.fromNullable(source.getRevisionsNews(-1));
                default:return Optional.fromNullable(source.getNews(-1));
            }
            
        }
    }
}
