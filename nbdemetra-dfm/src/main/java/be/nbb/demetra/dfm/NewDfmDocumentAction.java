/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.nbb.demetra.dfm;

import ec.nbdemetra.ui.NbComponents;
import ec.nbdemetra.ws.WorkspaceFactory;
import ec.nbdemetra.ws.WorkspaceItem;
import ec.tss.Dfm.DfmDocument;
import ec.tstoolkit.dfm.DfmModelSpec;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.var.VarSpec;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
@ActionReference(path = "Menu/View", position = 50)
@Messages("CTL_NewDfmDocumentAction=New DFM document")
public final class NewDfmDocumentAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        DfmDocumentManager mgr = WorkspaceFactory.getInstance().getManager(DfmDocumentManager.class);
        WorkspaceItem<DfmDocument> doc = mgr.create(WorkspaceFactory.getInstance().getActiveWorkspace());
        TopComponent c = createView(doc);
        c.open();
        c.requestActive();
    }

    public static TopComponent createView(final WorkspaceItem<DfmDocument> doc) {
        if (doc.isOpen())
            return doc.getView();
        // views
        final DfmModelSpecViewTopComponent modelView = new DfmModelSpecViewTopComponent(doc);
        final DfmExecViewTopComponent execView = new DfmExecViewTopComponent(doc);
        final DfmOutputViewTopComponent outputView = new DfmOutputViewTopComponent(doc);

        final TopComponent[] top = new TopComponent[1];
        final Lookup.Result<WorkspaceFactory.Event> lookup = WorkspaceFactory.getInstance().getLookup().lookupResult(WorkspaceFactory.Event.class);
        final LookupListener closeListener = new LookupListener() {

            @Override
            public void resultChanged(LookupEvent le) {
                Collection<? extends WorkspaceFactory.Event> all = lookup.allInstances();
                if (!all.isEmpty()) {
                    Iterator<? extends WorkspaceFactory.Event> iterator = all.iterator();
                    while (iterator.hasNext()) {
                        WorkspaceFactory.Event ev = iterator.next();
                        if (ev.info == WorkspaceFactory.Event.REMOVINGITEM) {
                            WorkspaceItem<?> wdoc = ev.workspace.searchDocument(ev.id);
                            if (wdoc.getElement() == doc.getElement()) {
                                SwingUtilities.invokeLater(new Runnable() {

                                    @Override
                                    public void run() {
                                        top[0].close();
                                    }
                                });
                            }
                        }
                    }
                }
            }
        };
        lookup.addLookupListener(closeListener);

        MultiViewDescription[] descriptions = {
            new QuickAndDirtyDescription("Model", modelView),
            new QuickAndDirtyDescription("Processing", execView),
            new QuickAndDirtyDescription("Output", outputView),};


        final TopComponent result = MultiViewFactory.createMultiView(descriptions, descriptions[0],
                new CloseOperationHandler() {

                    @Override
                    public boolean resolveCloseOperation(CloseOperationState[] coss) {
                        lookup.removeLookupListener(closeListener);
                        doc.setView(null);
                        return true;
                    }
                });
        result.setName(doc.getDisplayName());
        doc.setView(result);
        
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
