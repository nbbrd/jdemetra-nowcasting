/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.nbb.demetra.dfm;

import be.nbb.demetra.dfm.DfmController.DfmState;
import static be.nbb.demetra.dfm.DfmController.DfmState.CANCELLED;
import static be.nbb.demetra.dfm.DfmController.DfmState.DONE;
import static be.nbb.demetra.dfm.DfmController.DfmState.FAILED;
import static be.nbb.demetra.dfm.DfmController.DfmState.READY;
import static be.nbb.demetra.dfm.DfmController.DfmState.STARTED;
import ec.nbdemetra.ws.WorkspaceFactory;
import ec.nbdemetra.ws.WorkspaceItem;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.VersionedDfmDocument;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.SwingUtilities;
import org.netbeans.core.spi.multiview.CloseOperationHandler;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewDescription;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@ActionID(
        category = "View",
        id = "be.nbb.demetra.dfm.NewDfmDocumentAction"
)
@ActionRegistration(
        displayName = "#CTL_NewDfmDocumentAction"
)
@ActionReference(path = "Menu/Statistical methods/Nowcasting", position = 5000)
@Messages("CTL_NewDfmDocumentAction=Dynamic factor model")
public final class NewDfmDocumentAction implements ActionListener {
    
    @Override
    public void actionPerformed(ActionEvent e) {
        DfmDocumentManager mgr = WorkspaceFactory.getInstance().getManager(DfmDocumentManager.class);
        WorkspaceItem<VersionedDfmDocument> doc = mgr.create(WorkspaceFactory.getInstance().getActiveWorkspace());
        TopComponent c = createView(doc);
        c.open();
        c.requestActive();
    }
    
    public static TopComponent createView(final WorkspaceItem<VersionedDfmDocument> doc) {
        if (doc.isOpen()) {
            return doc.getView();
        }
        final DfmController controller = new DfmController();
        // views
        final DfmModelSpecViewTopComponent modelView = new DfmModelSpecViewTopComponent(doc, controller);
        final DfmExecViewTopComponent execView = new DfmExecViewTopComponent(doc, controller);
        final DfmOutputViewTopComponent outputView = new DfmOutputViewTopComponent(doc, controller);
        final DfmNewsViewTopComponent newsView = new DfmNewsViewTopComponent(doc, controller);
        
        MultiViewDescription[] descriptions = {
            new QuickAndDirtyDescription("Model", modelView),
            new QuickAndDirtyDescription("Processing", execView),
            new QuickAndDirtyDescription("Output", outputView),
            new QuickAndDirtyDescription("News", newsView)}
                ;
        
        final TopComponent result = MultiViewFactory.createMultiView(descriptions, descriptions[0]);
        result.setName(doc.getDisplayName());
        controller.addPropertyChangeListener(DfmController.DFM_STATE_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch ((DfmState) evt.getNewValue()) {
                    case CANCELLED:
                        result.makeBusy(false);
                        result.requestAttention(true);
                        break;
                    case DONE:
                        result.makeBusy(false);
                        result.requestAttention(true);
                        break;
                    case FAILED:
                        result.makeBusy(false);
                        result.requestAttention(true);
                        break;
                    case READY:
                        result.makeBusy(false);
                        break;
                    case STARTED:
                        result.makeBusy(true);
                        break;
                }
            }
        });
        doc.setView(result);
        DfmDocument cur = doc.getElement().getCurrent();
        if (cur.getSpecification().getModelSpec().isDefined()) {
            controller.setDfmState(DONE);
        }
        return result;
    }
    
    static class QuickAndDirtyDescription implements MultiViewDescription, Serializable {
        
        final String name;
        final MultiViewElement multiViewElement;
        
        public QuickAndDirtyDescription(String name, MultiViewElement multiViewElement) {
            this.name = name;
            this.multiViewElement = multiViewElement;
        }
        
        @Override
        public int getPersistenceType() {
            return TopComponent.PERSISTENCE_NEVER;
        }
        
        @Override
        public String getDisplayName() {
            return name;
        }
        
        @Override
        public Image getIcon() {
            return null;
        }
        
        @Override
        public HelpCtx getHelpCtx() {
            return null;
        }
        
        @Override
        public String preferredID() {
            return name;
        }
        
        @Override
        public MultiViewElement createElement() {
            return multiViewElement;
        }
    }
}
