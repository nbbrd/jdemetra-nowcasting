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
import ec.tss.Ts;
import ec.tss.TsCollection;
import ec.tss.TsFactory;
import ec.tss.TsInformationType;
import ec.tss.datatransfer.TssTransferSupport;
import ec.tss.dfm.DfmDocument;
import ec.tss.tsproviders.utils.MultiLineNameUtil;
import ec.tstoolkit.Parameter;
import ec.tstoolkit.ParameterType;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementType;
import ec.tstoolkit.dfm.MeasurementSpec;
import ec.tstoolkit.dfm.MeasurementSpec.Transformation;
import ec.util.chart.swing.SwingColorSchemeSupport;
import ec.util.grid.swing.XTable;
import ec.util.various.swing.BasicSwingLauncher;
import ec.util.various.swing.FontAwesome;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.COPY;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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
    public static final String MODEL_CHANGED_PROPERTY = "modelChanged";
    //
    private final XTable view;
    private DfmDocument model;

    public DfmModelSpecView() {
        this.view = new XTable();

        view.addMouseListener(new PopupListener.PopupAdapter(createMenu().getPopupMenu()));
        view.setDefaultRenderer(String.class, new SeriesNameRenderer());
        view.setDefaultRenderer(Transformation[].class, new TransformationRenderer());
        view.setDefaultRenderer(Integer.class, new DelayRenderer());
        view.setDefaultRenderer(MeasurementType.class, new MeasurementTypeRenderer());
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
            view.getColumnModel().getColumn(i + 4).setPreferredWidth(10);
        }
        view.setEnabled(!model.isLocked());

        firePropertyChange(MODEL_CHANGED_PROPERTY, null, model);
    }

    //<editor-fold defaultstate="collapsed" desc="Getters/Setters">
    public DfmDocument getModel() {
        return model;
    }

    public void setModel(DfmDocument model) {
        if (this.model == model && (model == null || check(model.getInput()))) {
            return;
        }
        this.model = model != null ? model : new DfmDocument();
        variables.replace(this.model.getInput());
        firePropertyChange(MODEL_PROPERTY, null, this.model);
    }

    public void updateModel() {
        firePropertyChange(MODEL_PROPERTY, null, this.model);
    }
    //</editor-fold>

    private final TsCollection variables = TsFactory.instance.createTsCollection();

    public void appendTsVariables(TsCollection col) {
        if (model.isLocked()) {
            return;
        }
        DfmSpec spec = model.getSpecification().cloneDefinition();
        for (Ts o : col) {
            if (!variables.contains(o)) {
                variables.add(o);
                spec.getModelSpec().getMeasurements().add(new MeasurementSpec(spec.getModelSpec().getVarSpec().getEquationsCount()));
            }
        }
        model.setInput(variables.toArray());
        model.setSpecification(spec);
        firePropertyChange(MODEL_PROPERTY, null, model);
    }

    private JMenu createMenu() {
        JMenu result = new JMenu();
        result.add(new ApplyToAllCommand().toAction(view)).setText("Apply to all");
        result.add(new RemoveVariableCommand().toAction(view)).setText("Remove");
        result.add(new RemoveAllVariablesCommand().toAction(view)).setText("Clear all");
        result.add(new Separator());
        result.add(new MoveVariableUpCommand().toAction(view)).setText("Move up");
        result.add(new MoveVariableDownCommand().toAction(view)).setText("Move down");
        result.add(new Separator());
        result.add(new WatchSerieCommand().toAction(view)).setText("Enable/disable the generation of data");
        result.add(new UseForGenerationCommand().toAction(view)).setText("Enable/disable use for calendar generation");
        return result;
    }

    private boolean check(Ts[] input) {
        if (input == null) {
            return variables.isEmpty();
        }
        if (input.length != variables.getCount()) {
            return false;
        }
        for (int i = 0; i < input.length; ++i) {
            if (input[i] != variables.get(i)) {
                return false;
            }
        }
        return true;
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
            return 4 + model.getSpecification().getModelSpec().getVarSpec().getEquationsCount();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            MeasurementSpec ms = model.getSpecification().getModelSpec().getMeasurements().get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return rowIndex >= variables.getCount() ? "var" + rowIndex : variables.get(rowIndex).getName();
                case 1:
                    return ms.getDelay();
                case 2:
                    return ms.getSeriesTransformations();
                case 3:
                    return ms.getFactorsTransformation();
                default:
                    return ms.getCoefficients()[columnIndex - 4].getType() != ParameterType.Fixed;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex > 0;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            DfmSpec spec = model.getSpecification().cloneDefinition();
            MeasurementSpec ms = spec.getModelSpec().getMeasurements().get(rowIndex);
            MeasurementSpec xms = ms.clone();
            switch (columnIndex) {
                case 1:
                    ms.setDelay((int) aValue);
                    break;
                case 2:
                    ms.setSeriesTransformations((Transformation[]) aValue);
                    break;
                case 3:
                    ms.setFactorsTransformation((MeasurementType) aValue);
                    break;
                default:
                    ms.getCoefficients()[columnIndex - 4].setType(((Boolean) aValue) ? ParameterType.Undefined : ParameterType.Fixed);
                    break;
            }
            if (!ms.equals(xms)) {
                model.setSpecification(spec);
                firePropertyChange(MODEL_CHANGED_PROPERTY, null, model);
            }
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return "Series";
                case 1:
                    return "Delay";
                case 2:
                    return "Series trans.";
                case 3:
                    return "Factors trans.";
                default:
                    return "F" + (columnIndex - 3);
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return String.class;
                case 1:
                    return Integer.class;
                case 2:
                    return Transformation[].class;
                case 3:
                    return MeasurementType.class;
                default:
                    return Boolean.class;
            }
        }

        public void clear() {
            if (!model.isLocked() && !variables.isEmpty()) {
                DfmSpec spec = model.getSpecification().cloneDefinition();
                spec.getModelSpec().getMeasurements().clear();
                variables.clear();

                model.setInput(variables.toArray());
                model.setSpecification(spec);
                firePropertyChange(MODEL_PROPERTY, null, model);
            }
        }

        public void removeVariable(int row) {
            if (!model.isLocked() && row >= 0 && row < variables.getCount()) {
                DfmSpec spec = model.getSpecification().cloneDefinition();
                spec.getModelSpec().getMeasurements().remove(row);
                variables.removeAt(row);

                model.setInput(variables.isEmpty() ? null : variables.toArray());
                model.setSpecification(spec);
                firePropertyChange(MODEL_PROPERTY, null, model);
            }
        }

        public void useForCalendarGeneration(int[] rows) {
            if (!model.isLocked() && rows != null && rows.length > 0 && rows.length <= variables.getCount()) {
                DfmSpec spec = model.getSpecification().cloneDefinition();
                for (int r : rows) {
                    spec.getModelSpec().getMeasurements().get(r).changeUseForGeneration();
                }

                model.setInput(variables.isEmpty() ? null : variables.toArray());
                model.setSpecification(spec);
                firePropertyChange(MODEL_PROPERTY, null, model);
            }
        }

        public void watchSeries(int[] rows) {
            if (!model.isLocked() && rows != null && rows.length > 0 && rows.length <= variables.getCount()) {
                DfmSpec spec = model.getSpecification().cloneDefinition();
                for (int r : rows) {
                    spec.getModelSpec().getMeasurements().get(r).toggleWatched();
                }

                model.setInput(variables.isEmpty() ? null : variables.toArray());
                model.setSpecification(spec);
                firePropertyChange(MODEL_PROPERTY, null, model);
            }
        }

        public void moveVariableUp(int row) {
            if (!model.isLocked() && row > 0) {
                DfmSpec spec = model.getSpecification().cloneDefinition();
                List<MeasurementSpec> ms = spec.getModelSpec().getMeasurements();
                Collections.swap(ms, row, row - 1);
                List<Ts> ts = Arrays.asList(variables.toArray());
                Collections.swap(ts, row, row - 1);

                model.setInput((Ts[]) ts.toArray());
                model.setSpecification(spec);
                firePropertyChange(MODEL_PROPERTY, null, model);
            }
        }

        public void moveVariableDown(int row) {
            if (!model.isLocked() && row < variables.getCount() - 1) {
                DfmSpec spec = model.getSpecification().cloneDefinition();
                List<MeasurementSpec> ms = spec.getModelSpec().getMeasurements();
                Collections.swap(ms, row, row + 1);
                List<Ts> ts = Arrays.asList(variables.toArray());
                Collections.swap(ts, row, row + 1);

                model.setInput((Ts[]) ts.toArray());
                model.setSpecification(spec);
                firePropertyChange(MODEL_PROPERTY, null, model);
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

    private final class MeasurementTypeRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));

            SwingColorSchemeSupport colorSchemeSupport = SwingColorSchemeSupport.from(DemetraUI.getDefault().getColorScheme());
            Color color = colorSchemeSupport.getLineColor(model.getSpecification().getModelSpec().getMeasurements().get(row).getFactorsTransformation().ordinal());

            JLabel l = new JLabel(value.toString());
            l.setFont(new java.awt.Font("Tahoma", 0, 10));
            l.setBorder(BorderFactory.createCompoundBorder(new LineBorder(isSelected ? Color.WHITE : color, 1, false), new EmptyBorder(1, 2, 1, 2)));
            l.setForeground(isSelected ? Color.WHITE : color);
            l.setHorizontalTextPosition(JLabel.CENTER);

            p.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

            p.add(l);

            return p;
        }
    }

    private static final class DelayRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            ModelSpecModel model = (ModelSpecModel) table.getModel();
            boolean used = model.getMeasurements().get(row).isUsedForGeneration();

            l.setText(convertToString(value));
            l.setIcon(FontAwesome.FA_CALENDAR.getIcon(used ? isSelected ? Color.WHITE : Color.RED : Color.LIGHT_GRAY, l.getFont().getSize() - 1));
            l.setToolTipText(used ? "Used to generate publication calendar" : null);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            l.setHorizontalTextPosition(SwingConstants.LEFT);

            return l;
        }

        private String convertToString(Object value) {
            if (value instanceof Integer) {
                int val = (int) value;
                if (val == 0) {
                    return "0 days";
                } else {
                    String string = "";
                    if (val % 30 == 0) {
                        int months = Math.abs(val / 30);
                        string = months + " month" + (months > 1 ? "s" : "");
                    } else if (val % 7 == 0) {
                        int weeks = Math.abs(val / 7);
                        string = weeks + " week" + (weeks > 1 ? "s" : "");
                    } else {
                        string = Math.abs(val) + " day" + (Math.abs(val) > 1 ? "s" : "");
                    }

                    string += val > 0 ? " after" : " before";
                    return string;
                }
            } else {
                return value.toString();
            }
        }
    }

    private static final class SeriesNameRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            ModelSpecModel model = (ModelSpecModel) table.getModel();
            boolean watched = model.getMeasurements().get(row).isWatched();

            l.setText(MultiLineNameUtil.join(String.valueOf(value)));
            l.setIcon(FontAwesome.FA_EYE.getIcon(watched ? isSelected ? Color.WHITE : Color.BLUE : Color.LIGHT_GRAY, l.getFont().getSize()));
            l.setToolTipText(watched ? "Data for this series will be generated" : null);
            l.setHorizontalAlignment(SwingConstants.LEFT);
            l.setHorizontalTextPosition(SwingConstants.RIGHT);

            return l;
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

    private static final class RemoveVariableCommand extends JCommand<XTable> {

        @Override
        public void execute(XTable component) throws Exception {
            if (!component.isEnabled()) {
                return;
            }

            ModelSpecModel model = (ModelSpecModel) component.getModel();
            int index = component.convertRowIndexToModel(component.getSelectedRows()[0]);
            model.removeVariable(index);
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

    private static final class RemoveAllVariablesCommand extends JCommand<XTable> {

        @Override
        public void execute(XTable component) throws Exception {
            if (!component.isEnabled()) {
                return;
            }

            ModelSpecModel model = (ModelSpecModel) component.getModel();
            model.clear();
        }

        @Override
        public boolean isEnabled(XTable component) {
            return !component.isEditing();
        }

        @Override
        public JCommand.ActionAdapter toAction(XTable component) {
            return super.toAction(component)
                    .withWeakListSelectionListener(component.getSelectionModel())
                    .withWeakPropertyChangeListener(component, "tableCellEditor");
        }
    }

    private static final class ApplyToAllCommand extends JCommand<XTable> {

        @Override
        public void execute(XTable component) throws Exception {
            if (!component.isEnabled()) {
                return;
            }
            ModelSpecModel model = (ModelSpecModel) component.getModel();
            int index = component.convertRowIndexToModel(component.getSelectedRows()[0]);
            MeasurementSpec selected = model.getMeasurements().get(index);
            for (MeasurementSpec o : model.getMeasurements()) {
                if (o != selected) {
                    o.setSeriesTransformations(Arrays3.cloneIfNotNull(selected.getSeriesTransformations()));
                    o.setFactorsTransformation(selected.getFactorsTransformation());
                    o.setCoefficients(Parameter.clone(selected.getCoefficients()));
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

    private static final class UseForGenerationCommand extends JCommand<XTable> {

        @Override
        public void execute(XTable component) throws Exception {
            if (!component.isEnabled()) {
                return;
            }

            ModelSpecModel model = (ModelSpecModel) component.getModel();
            int[] rows = new int[component.getSelectedRowCount()];
            for (int i = 0; i < component.getSelectedRowCount(); i++) {
                rows[i] = component.convertRowIndexToModel(component.getSelectedRows()[i]);
            }
            model.useForCalendarGeneration(rows);
        }

        @Override
        public boolean isEnabled(XTable component) {
            return !component.isEditing() && component.getSelectedRowCount() > 0;
        }

        @Override
        public JCommand.ActionAdapter toAction(XTable component) {
            return super.toAction(component)
                    .withWeakListSelectionListener(component.getSelectionModel())
                    .withWeakPropertyChangeListener(component, "tableCellEditor");
        }
    }

    private static final class WatchSerieCommand extends JCommand<XTable> {

        @Override
        public void execute(XTable component) throws Exception {
            if (!component.isEnabled()) {
                return;
            }

            ModelSpecModel model = (ModelSpecModel) component.getModel();
            int[] rows = new int[component.getSelectedRowCount()];
            for (int i = 0; i < component.getSelectedRowCount(); i++) {
                rows[i] = component.convertRowIndexToModel(component.getSelectedRows()[i]);
            }
            model.watchSeries(rows);
        }

        @Override
        public boolean isEnabled(XTable component) {
            return !component.isEditing() && component.getSelectedRowCount() > 0;
        }

        @Override
        public JCommand.ActionAdapter toAction(XTable component) {
            return super.toAction(component)
                    .withWeakListSelectionListener(component.getSelectionModel())
                    .withWeakPropertyChangeListener(component, "tableCellEditor");
        }
    }

    private static final class MoveVariableUpCommand extends JCommand<XTable> {

        @Override
        public void execute(XTable component) throws Exception {
            if (!component.isEnabled()) {
                return;
            }

            ModelSpecModel model = (ModelSpecModel) component.getModel();
            int index = component.convertRowIndexToModel(component.getSelectedRows()[0]);
            model.moveVariableUp(index);
        }

        @Override
        public boolean isEnabled(XTable component) {
            return !component.isEditing() && component.getSelectedRowCount() == 1 && component.getSelectedRow() > 0;
        }

        @Override
        public JCommand.ActionAdapter toAction(XTable component) {
            return super.toAction(component)
                    .withWeakListSelectionListener(component.getSelectionModel())
                    .withWeakPropertyChangeListener(component, "tableCellEditor");
        }
    }

    private static final class MoveVariableDownCommand extends JCommand<XTable> {

        @Override
        public void execute(XTable component) throws Exception {
            if (!component.isEnabled()) {
                return;
            }

            ModelSpecModel model = (ModelSpecModel) component.getModel();
            int index = component.convertRowIndexToModel(component.getSelectedRows()[0]);
            model.moveVariableDown(index);
        }

        @Override
        public boolean isEnabled(XTable component) {
            int size = component.getModel().getRowCount() - 1;
            return !component.isEditing() && component.getSelectedRowCount() == 1 && component.getSelectedRow() < size - 1;
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
            DemetraUI.getDefault().getTsAction().open(cur);
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
            boolean result = TssTransferSupport.getDefault().canImport(support.getDataFlavors());
            if (result && support.isDrop()) {
                support.setDropAction(COPY);
            }
            return result;
        }

        @Override
        public boolean importData(TransferSupport support) {
            long count = TssTransferSupport.getDefault()
                    .toTsCollectionStream(support.getTransferable())
                    .peek(o -> o.load(TsInformationType.All))
                    .filter(o -> !o.isEmpty())
                    .peek(o -> appendTsVariables(o))
                    .count();
            return count > 0;
        }
    }
}
