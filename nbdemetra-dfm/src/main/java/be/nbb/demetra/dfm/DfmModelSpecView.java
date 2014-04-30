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
package be.nbb.demetra.dfm;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import ec.nbdemetra.ui.DemetraUI;
import ec.nbdemetra.ui.NbComponents;
import ec.nbdemetra.ui.awt.PopupListener;
import ec.nbdemetra.ui.properties.ListSelection;
import ec.tss.Dfm.DfmDocument;
import ec.tss.DynamicTsVariable;
import ec.tss.Ts;
import ec.tss.TsCollection;
import ec.tss.TsFactory;
import ec.tss.TsInformationType;
import ec.tss.TsMoniker;
import ec.tss.datatransfer.TssTransferSupport;
import ec.tstoolkit.Parameter;
import ec.tstoolkit.ParameterType;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementType;
import ec.tstoolkit.dfm.MeasurementSpec;
import ec.tstoolkit.dfm.MeasurementSpec.Transformation;
import ec.tstoolkit.timeseries.regression.ITsVariable;
import ec.tstoolkit.timeseries.regression.TsVariable;
import ec.tstoolkit.timeseries.regression.TsVariables;
import ec.util.grid.swing.XTable;
import ec.util.various.swing.BasicSwingLauncher;
import ec.util.various.swing.JCommand;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.Beans;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.COPY;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Philippe Charles
 */
public final class DfmModelSpecView extends JComponent {

    public static void main(String[] args) {
        new BasicSwingLauncher().content(new Callable<Component>() {
            @Override
            public Component call() throws Exception {
                DfmModelSpecView demo = new DfmModelSpecView();
                demo.setModel(DfmModelSpecDemo.getDemo());
                return demo;
            }
        }).launch();
    }

    public static final String MODEL_PROPERTY = "model";
    //
    private final XTable view;
    private DfmDocument model;

    public DfmModelSpecView() {
        this.view = new XTable();

        view.addMouseListener(new PopupListener.PopupAdapter(createMenu().getPopupMenu()));
        view.setDefaultRenderer(Transformation[].class, new TransformationRenderer());
        view.setDefaultEditor(Transformation[].class, new TransformationEditor());
        view.setDefaultEditor(MeasurementType.class, new MeasurementTypeEditor());
        view.setNoDataRenderer(new XTable.DefaultNoDataRenderer("Drop data here"));
        view.setTransferHandler(new DfmModelSpecViewTransferHandler());
        view.addMouseListener(new TsMouseAdapter());

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case MODEL_PROPERTY:
                        onModelChange();
                        break;
                }
            }
        });

        setLayout(new BorderLayout());
        add(NbComponents.newJScrollPane(view), BorderLayout.CENTER);

        if (Beans.isDesignTime()) {
            setModel(DfmModelSpecDemo.getDemo());
        }
    }

    private void onModelChange() {
        // reset the document
        view.setModel(new ModelSpecModel());
        int nbrFactors = model.getSpecification().getModelSpec().getVarSpec().getEquationsCount();
        for (int i = 0; i < nbrFactors; i++) {
            view.getColumnModel().getColumn(i + 3).setPreferredWidth(10);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Getters/Setters">
    public DfmDocument getModel() {
        return model;
    }

    public void setModel(DfmDocument model) {
        if (this.model == model)
            return;
        DfmDocument old = this.model;
        this.model = model != null ? model : new DfmDocument();
        variables.replace(model.getInput());
        firePropertyChange(MODEL_PROPERTY, old, this.model);
    }
    
    public void updateModel(){
        firePropertyChange(MODEL_PROPERTY, null, this.model);
    }
    //</editor-fold>

    private final TsCollection variables = TsFactory.instance.createTsCollection();

    public void appendTsVariables(TsCollection col) {
        DfmSpec spec = model.getSpecification().clone();
        for (Ts o : col) {
            variables.add(o);
            spec.getModelSpec().getMeasurements().add(new MeasurementSpec(o.getName(), spec.getModelSpec().getVarSpec().getEquationsCount()));
        }
        model.setInput(variables.toArray());
        model.setSpecification(spec);
        firePropertyChange(MODEL_PROPERTY, null, model);
    }

    private JMenu createMenu() {
        JMenu result = new JMenu();
        result.add(new ApplyToAllCommand().toAction(view)).setText("Apply to all");
        return result;
    }

    private final class ModelSpecModel extends AbstractTableModel {

        public Ts[] getVariables() {
            return variables.toArray();
        }

        public List<MeasurementSpec> getMeasurements() {
            return model.getSpecification().getModelSpec().getMeasurements();
        }

        @Override
        public int getRowCount() {
            return model.getSpecification().getModelSpec().getMeasurements().size();
        }

        @Override
        public int getColumnCount() {
            return 3 + model.getSpecification().getModelSpec().getVarSpec().getEquationsCount();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            MeasurementSpec ms = model.getSpecification().getModelSpec().getMeasurements().get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return variables.get(rowIndex).getName();
                case 1:
                    return ms.getSeriesTransformations();
                case 2:
                    return ms.getFactorsTransformation();
                default:
                    return ms.getCoefficients()[columnIndex - 3].getType() == ParameterType.Undefined;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex > 0;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            DfmSpec spec = model.getSpecification().clone();
            MeasurementSpec ms = spec.getModelSpec().getMeasurements().get(rowIndex);
            switch (columnIndex) {
                case 1:
                    ms.setSeriesTransformations((Transformation[]) aValue);
                    break;
                case 2:
                    ms.setFactorsTransformation((MeasurementType) aValue);
                    break;
                default:
                    ms.getCoefficients()[columnIndex - 3].setType(((Boolean) aValue).booleanValue() ? ParameterType.Undefined : ParameterType.Fixed);
                    break;
            }
            model.setSpecification(spec);
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return "Series";
                case 1:
                    return "Series trans.";
                case 2:
                    return "Factors trans.";
                default:
                    return "F" + (columnIndex - 2);
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return String.class;
                case 1:
                    return Transformation[].class;
                case 2:
                    return MeasurementType.class;
                default:
                    return Boolean.class;
            }
        }
    }

    private static final class TransformationRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(Arrays3.enumToString((Transformation[]) value));
            return this;
        }

    }

    private static final class TransformationEditor extends PopupTableCellEditor {

        public TransformationEditor() {
            super(new EditorDelegate() {
                private final ListSelection<Transformation> content = new ListSelection<>();

                {
                    content.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                }

                @Override
                public Component getCustomComponent() {
                    return content;
                }

                @Override
                public String getValueAsString() {
                    return Arrays3.enumToString(Iterables.toArray(content.getSelection(), Transformation.class));
                }

                @Override
                public Object getValue() {
                    return Iterables.toArray(content.getSelection(), Transformation.class);
                }

                @Override
                public void setValue(Object value) {
                    Transformation[] transformations = (Transformation[]) value;
                    List<Transformation> input = Lists.newArrayList(Transformation.values());
                    if (transformations == null) {
                        content.set(input);
                    } else {
                        List<Transformation> sel = Arrays.asList(transformations);
                        input.removeAll(sel);
                        content.set(input, sel);
                    }
                }
            });
        }

    }

    private static final class MeasurementTypeEditor extends DefaultCellEditor {

        public MeasurementTypeEditor() {
            super(new JComboBox<>(new DefaultComboBoxModel<>(MeasurementType.values())));
        }
    }

    private static final class ApplyToAllCommand extends JCommand<XTable> {

        @Override
        public void execute(XTable component) throws Exception {
            ModelSpecModel model = (ModelSpecModel) component.getModel();
            int index = component.convertRowIndexToModel(component.getSelectedRows()[0]);
            MeasurementSpec selected = model.getMeasurements().get(index);
            for (MeasurementSpec o : model.getMeasurements()) {
                if (o != selected) {
                    o.setSeriesTransformations(Arrays3.cloneIfNotNull(selected.getSeriesTransformations()));
                    o.setFactorsTransformation(selected.getFactorsTransformation());
                    o.setCoefficient(Parameter.clone(selected.getCoefficients()));
                }
            }
            model.fireTableDataChanged();
        }

        @Override
        public boolean isEnabled(XTable component) {
            return !component.isEditing() && component.getSelectedRowCount() == 1;
        }

        @Override
        public JCommand.ActionAdapter toAction(XTable component) {
            return super.toAction(component)
                    .withWeakListSelectionListener(component.getSelectionModel())
                    .withWeakPropertyChangeListener(component, "tableCellEditor");
        }
    }

    private static final class OpenTsCommand extends JCommand<XTable> {

        @Override
        public void execute(XTable component) throws Exception {
            ModelSpecModel model = (ModelSpecModel) component.getModel();
            int index = component.convertRowIndexToModel(component.getSelectedRows()[0]);
            Ts cur = model.getVariables()[index];
            String selected = cur.getName();
            DemetraUI.getInstance().getTsAction().open(cur);
        }

        @Override
        public boolean isEnabled(XTable component) {
            return !component.isEditing() && component.getSelectedRowCount() == 1;
        }

        @Override
        public JCommand.ActionAdapter toAction(XTable component) {
            return super.toAction(component)
                    .withWeakListSelectionListener(component.getSelectionModel())
                    .withWeakPropertyChangeListener(component, "tableCellEditor");
        }
    }

    private class TsMouseAdapter extends MouseAdapter {

        private final OpenTsCommand cmd = new OpenTsCommand();

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() > 1 && cmd.isEnabled(view)) {
                cmd.executeSafely(view);
            }
        }
    }

    private final class DfmModelSpecViewTransferHandler extends TransferHandler {

        @Override
        public boolean canImport(TransferSupport support) {
            boolean result = TssTransferSupport.getInstance().canImport(support.getDataFlavors());
            if (result && support.isDrop()) {
                support.setDropAction(COPY);
            }
            return result;
        }

        @Override
        public boolean importData(TransferSupport support) {
            TsCollection col = TssTransferSupport.getInstance().toTsCollection(support.getTransferable());
            if (col != null) {
                col.query(TsInformationType.All);
                if (!col.isEmpty()) {
                    appendTsVariables(col);
                }
                return true;
            }
            return false;
        }
    }
}
