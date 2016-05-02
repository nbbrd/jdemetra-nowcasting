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
        id = "be.nbb.demetra.dfm.actions.UnlockAction")
@ActionRegistration(displayName = "#CTL_Unlock", lazy = false)
@ActionReferences({
    @ActionReference(path = DfmDocumentManager.CONTEXTPATH, position = 3000, separatorBefore = 2999)
})
@Messages("CTL_Unlock=Unlock")
public final class UnlockAction extends AbstractViewAction<AbstractDfmDocumentTopComponent> {

    public static final String UNLOCK_MESSAGE = "Are you sure you want to unlock the current model? All the previous versions will be lost!";

    public UnlockAction() {
        super(AbstractDfmDocumentTopComponent.class);
        putValue(NAME, Bundle.CTL_Unlock());
        refreshAction();
    }

    @Override
    protected void refreshAction() {
        AbstractDfmDocumentTopComponent ui = this.context();
        enabled = ui != null && ui.getDocument().getElement().getCurrent().isLocked();
    }

    @Override
    protected void process(AbstractDfmDocumentTopComponent ui) {
        AbstractDfmDocumentTopComponent top = this.context();
        VersionedDfmDocument element = top.getDocument().getElement();
        refresh(element);
        ui.getController().setDfmState(DfmController.DfmState.READY);
    }

    public static void refresh(VersionedDfmDocument doc) {
        if (doc == null) {
            return;
        }
        if (doc.getCurrent().isLocked()) {
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(UNLOCK_MESSAGE, NotifyDescriptor.OK_CANCEL_OPTION);
            if (DialogDisplayer.getDefault().notify(nd) != NotifyDescriptor.OK_OPTION) {
                return;
            }
            doc.unlockModel();
            doc.clearVersions(0);

        }

    }
}
