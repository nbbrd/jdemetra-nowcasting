/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.nbb.demetra.dfm;

import ec.nbdemetra.ui.NbComponents;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.core.spi.multiview.MultiViewDescription;
import org.netbeans.core.spi.multiview.MultiViewFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
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

    int i = 0;

    @Override
    public void actionPerformed(ActionEvent e) {
        String name = "DFM doc " + i++;
        TopComponent c = NbComponents.findTopComponentByName(name);
        if (c == null) {
            MultiViewDescription[] descriptions = {new DfmModelSpecViewTopComponent(), new DfmExecViewTopComponent(), new DfmOutputViewTopComponent()};
            c = MultiViewFactory.createMultiView(descriptions, descriptions[0]);
            c.setName(name);
            c.open();
        }
        c.requestActive();
    }
}
