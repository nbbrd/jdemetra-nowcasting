/*
 * Copyright 2014 National Bank of Belgium
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
package be.nbb.demetra.dfm;

import ec.nbdemetra.ui.DemetraUiIcon;
import ec.nbdemetra.ui.NbComponents;
import ec.nbdemetra.ui.properties.OpenIdePropertySheetBeanEditor;
import ec.nbdemetra.ws.WorkspaceItem;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.VersionedDfmDocument;
import ec.tstoolkit.dfm.DfmSimulationSpec;
import ec.tstoolkit.dfm.DfmSpec;
import ec.util.various.swing.JCommand;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import org.netbeans.api.settings.ConvertAsProperties;
import static org.openide.util.ImageUtilities.createDisabledIcon;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 *
 * @author Mats Maggi
 */
@ConvertAsProperties(
        dtd = "-//be.nbb.demetra.dfm//DfmSimulationView/EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "DfmSimulationTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@NbBundle.Messages({
    "CTL_DfmSimulationViewAction=DfmSimulationView",
    "CTL_DfmSimulationTopComponent=DfmSimulation Window",
    "HINT_DfmSimulationTopComponent=This is a DfmSimulation window"
})
public class DfmSimulationTopComponent extends AbstractDfmDocumentTopComponent {

    public DfmSimulationTopComponent() {
        this(null, new DfmController());
    }
    
    public DfmSimulationTopComponent(WorkspaceItem<VersionedDfmDocument> document, DfmController controller) {
        super(document, controller);
        setToolTipText(Bundle.HINT_DfmSimulationTopComponent());
    }
    
    @Override
    public JComponent getToolbarRepresentation() {
        JToolBar toolBar = NbComponents.newInnerToolbar();
        toolBar.addSeparator();
        toolBar.add(Box.createRigidArea(new Dimension(5, 0)));
        
//        JToggleButton startStop = (JToggleButton) toolBar.add(new JToggleButton(StartStopCommand.INSTANCE.toAction(this)));
//        startStop.setIcon(DemetraUiIcon.COMPILE_16);
//        startStop.setDisabledIcon(createDisabledIcon(startStop.getIcon()));
//        startStop.setToolTipText("Start/Stop");
        
        JButton edit = toolBar.add(EditSpecCommand.INSTANCE.toAction(this));
        edit.setIcon(DemetraUiIcon.PREFERENCES);
        edit.setDisabledIcon(createDisabledIcon(edit.getIcon()));
        edit.setToolTipText("Specification");
        
        return toolBar;
    }

    void writeProperties(java.util.Properties p) {
    }

    void readProperties(java.util.Properties p) {
    }
    
    private static final class EditSpecCommand extends JCommand<DfmSimulationTopComponent> {

        public static final EditSpecCommand INSTANCE = new EditSpecCommand();

        @Override
        public boolean isEnabled(DfmSimulationTopComponent c) {
            return true;
        }

        @Override
        public void execute(DfmSimulationTopComponent c) throws Exception {
            DfmDocument doc = c.getDocument().getElement().getCurrent();
            DfmSpec spec = doc.getSpecification();
            
            DfmSimulationSpec newValue = spec.getSimulationSpec().clone();
            if (OpenIdePropertySheetBeanEditor.editSheet(DfmSheets.onSimulationSpec(newValue), "Edit spec", null)) {
                doc.getSpecification().setSimulationSpec(newValue);
            }
        }
    }
}
