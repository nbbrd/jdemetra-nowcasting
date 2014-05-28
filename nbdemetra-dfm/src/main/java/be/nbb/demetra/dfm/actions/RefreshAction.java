/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.nbb.demetra.dfm.actions;

import be.nbb.demetra.dfm.AbstractDfmDocumentTopComponent;
import be.nbb.demetra.dfm.DfmController;
import be.nbb.demetra.dfm.DfmDocumentManager;
import ec.nbdemetra.ws.actions.AbstractViewAction;
import ec.tss.dfm.VersionedDfmDocument;
import static javax.swing.Action.NAME;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Tools",
        id = "be.nbb.demetra.dfm.actions.RefreshAction")
@ActionRegistration(displayName = "#CTL_Refresh", lazy = false)
@ActionReferences({
    @ActionReference(path = DfmDocumentManager.CONTEXTPATH, position = 2700)
})
@Messages("CTL_Refresh=Refresh")
public final class RefreshAction extends AbstractViewAction<AbstractDfmDocumentTopComponent> {

    public static final String REFRESH_MESSAGE = "Are you sure you want to refresh the current data?";

    public RefreshAction() {
        super(AbstractDfmDocumentTopComponent.class);
        putValue(NAME, Bundle.CTL_Refresh());
        refreshAction();
    }

    @Override
    protected void refreshAction() {
        AbstractDfmDocumentTopComponent ui = this.context();
        enabled = ui != null && ui.getDocument().getElement().getCurrent().isTsFrozen();
    }

    @Override
    protected void process(AbstractDfmDocumentTopComponent ui) {
        AbstractDfmDocumentTopComponent top = this.context();
        VersionedDfmDocument element = top.getDocument().getElement();
        refresh(element);
        ui.getController().setDfmState(DfmController.DfmState.READY);
        if (element.getCurrent().getSpecification().getModelSpec().isSpecified()) {
            boolean ok=element.getCurrent().update();
            ui.getController().setDfmState(ok ? DfmController.DfmState.DONE : DfmController.DfmState.FAILED);
        }
    }

    public static void refresh(VersionedDfmDocument doc) {
        if (doc == null) {
            return;
        }
        if (doc.getCurrent().isTsFrozen()) {
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(REFRESH_MESSAGE, NotifyDescriptor.OK_CANCEL_OPTION);
            if (DialogDisplayer.getDefault().notify(nd) != NotifyDescriptor.OK_OPTION) {
                return;
            }
            doc.refreshData();
        }

    }
}
