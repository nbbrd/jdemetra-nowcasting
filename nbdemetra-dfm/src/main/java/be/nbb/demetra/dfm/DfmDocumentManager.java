/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.nbb.demetra.dfm;

import ec.nbdemetra.ui.DocumentUIServices;
import ec.nbdemetra.ws.AbstractWorkspaceItemManager;
import ec.nbdemetra.ws.IWorkspaceItemManager;
import ec.nbdemetra.ws.WorkspaceFactory;
import ec.nbdemetra.ws.WorkspaceItem;
import ec.tss.Dfm.DfmDocument;
import ec.tss.Dfm.DfmProcessingFactory;
import ec.tstoolkit.descriptors.IObjectDescriptor;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.utilities.Id;
import ec.tstoolkit.utilities.LinearId;
import ec.ui.view.tsprocessing.IProcDocumentView;
import ec.ui.view.tsprocessing.ProcDocumentViewFactory;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;

/**
 *
 * @author palatej
 */
@ServiceProvider(service = IWorkspaceItemManager.class, position = 4500)
public class DfmDocumentManager extends AbstractWorkspaceItemManager<DfmDocument> {

    static {
        DocumentUIServices.getDefault().register(DfmDocument.class, new DocumentUIServices.AbstractUIFactory<DfmSpec, DfmDocument>() {
            @Override
            public IProcDocumentView<DfmDocument> getDocumentView(DfmDocument document) {
                return new ProcDocumentViewFactory<DfmDocument>() {
                    {
                        registerFromLookup(DfmDocument.class);
                    }

                    @Override
                    public Id getPreferredView() {
                        return new LinearId("ShocksDecomposition");
                    }
                }.create(document);
            }

            @Override
            public IObjectDescriptor<DfmSpec> getSpecificationDescriptor(DfmDocument doc) {
                return null;
            }
        });
    }

    public static final LinearId ID = new LinearId(DfmProcessingFactory.DESCRIPTOR.family, "documents", DfmProcessingFactory.DESCRIPTOR.name);
    public static final String PATH = "dfm.doc";
    public static final String ITEMPATH = "dfm.doc.item";
    public static final String CONTEXTPATH = "dfm.doc.context";

    @Override
    protected String getItemPrefix() {
        return "DfmDoc";
    }

    @Override
    public Id getId() {
        return ID;
    }

    @Override
    protected DfmDocument createNewObject() {
        return new DfmDocument();
    }

    @Override
    public IWorkspaceItemManager.ItemType getItemType() {
        return IWorkspaceItemManager.ItemType.Doc;
    }

    @Override
    public String getActionsPath() {
        return PATH;
    }

    @Override
    public IWorkspaceItemManager.Status getStatus() {
        return IWorkspaceItemManager.Status.Certified;
    }

    @Override
    public Class<DfmDocument> getItemClass() {
        return DfmDocument.class;
    }

    public void openDocument(WorkspaceItem<DfmDocument> doc) {
        if (doc == null || doc.getElement() == null) {
            return;
        }

        TopComponent view = NewDfmDocumentAction.createView(doc);
        view.open();
        view.requestActive();
    }

    @Override
    public Action getPreferredItemAction(final Id child) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkspaceItem<DfmDocument> doc = (WorkspaceItem<DfmDocument>) WorkspaceFactory.getInstance().getActiveWorkspace().searchDocument(child);
                if (doc != null) {
                    openDocument(doc);
                }
            }

        };
    }
}
