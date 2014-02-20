/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.nbb.demetra.dfm;

import ec.nbdemetra.ui.NbComponents;
import ec.tss.Dfm.DfmDocument;
import ec.tstoolkit.dfm.DfmModelSpec;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.var.VarSpec;
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
            DfmDocument document = newDocument(name);
            MultiViewDescription[] descriptions = {
                new DfmModelSpecViewTopComponent(document),
                new DfmExecViewTopComponent(document),
                new DfmOutputViewTopComponent(document)};
            c = MultiViewFactory.createMultiView(descriptions, descriptions[0]);
            c.setName(name);
            c.open();
        }
        c.requestActive();
    }

    public static DfmDocument newDocument(String name) {
        DfmDocument result = new DfmDocument(name);
        DfmSpec spec = new DfmSpec();
        spec.setModelSpec(newDfmModelSpec(4, 4));
        result.setSpecification(spec);
        return result;
    }

    public static DfmModelSpec newDfmModelSpec(int nvars, int nlags) {
        DfmModelSpec m = new DfmModelSpec();
        VarSpec vs = new VarSpec();
        vs.setSize(nvars, nlags);
        m.setVarSpec(vs);
        return m;
    }
}
