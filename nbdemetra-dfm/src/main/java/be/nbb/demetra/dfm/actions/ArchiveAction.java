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
        id = "be.nbb.demetra.dfm.actions.ArchiveAction")
@ActionRegistration(displayName = "#CTL_Archive", lazy=false)
@ActionReferences({
    @ActionReference(path = DfmDocumentManager.CONTEXTPATH, position = 2710)
})
@Messages("CTL_Archive=Archive")
public final class ArchiveAction extends AbstractViewAction<AbstractDfmDocumentTopComponent> {

    public static final String ARCHIVE_MESSAGE = "Are you sure you want to archive the current estimation?";

    public ArchiveAction() {
        super(AbstractDfmDocumentTopComponent.class);
        putValue(NAME, Bundle.CTL_Archive());
        refreshAction();
    }

    @Override
    protected void refreshAction() {
        AbstractDfmDocumentTopComponent ui = this.context();
        enabled = ui != null && ui.getDocument().getElement().getSpecification().getModelSpec().isSpecified();
    }

    @Override
    protected void process(AbstractDfmDocumentTopComponent ui) {
        AbstractDfmDocumentTopComponent top = this.context();
        VersionedDfmDocument element = top.getDocument().getElement();
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(ARCHIVE_MESSAGE, NotifyDescriptor.OK_CANCEL_OPTION);
            if (DialogDisplayer.getDefault().notify(nd) != NotifyDescriptor.OK_OPTION) {
                return;
            }
        element.archive();
        ui.getController().setDfmState(DfmController.DfmState.READY);
        ui.getController().setDfmState(DfmController.DfmState.DONE);
    }
}
