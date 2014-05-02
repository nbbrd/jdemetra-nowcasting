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
package be.nbb.demetra.dfm.output;

import com.google.common.base.Optional;
import ec.nbdemetra.ui.DemetraUI;
import ec.tss.Dfm.DfmResults;
import ec.tss.Dfm.DfmSeriesDescriptor;
import ec.tss.Ts;
import ec.tss.TsCollection;
import ec.tss.TsFactory;
import ec.tss.datatransfer.TssTransferSupport;
import ec.tstoolkit.dfm.DfmInformationSet;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.ui.chart.TsXYDatasets;
import ec.util.chart.ColorScheme;
import ec.util.chart.ObsFunction;
import ec.util.chart.SeriesFunction;
import ec.util.chart.TimeSeriesChart;
import ec.util.chart.swing.ColorSchemeIcon;
import static ec.util.chart.swing.JTimeSeriesChartCommand.applyColorSchemeSupport;
import static ec.util.chart.swing.JTimeSeriesChartCommand.applyLineThickness;
import static ec.util.chart.swing.JTimeSeriesChartCommand.copyImage;
import static ec.util.chart.swing.JTimeSeriesChartCommand.printImage;
import static ec.util.chart.swing.JTimeSeriesChartCommand.saveImage;
import ec.util.chart.swing.SwingColorSchemeSupport;
import static ec.util.chart.swing.SwingColorSchemeSupport.withAlpha;
import ec.util.various.swing.JCommand;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.Objects;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 *
 * @author Philippe Charles
 */
final class ShocksDecompositionView extends javax.swing.JPanel {

    public static final String ACTUAL_VISIBLE_PROPERTY = "actualVisible";
    public static final String SIGNAL_VISIBLE_PROPERTY = "signalVisible";
    public static final String INITIAL_FACTOR_VISIBLE_PROPERTY = "initialFactorVisible";
    public static final String NOISE_VISIBLE_PROPERTY = "noiseVisible";
    public static final String DFM_RESULTS_PROPERTY = "dfmResults";

    private boolean actualVisible;
    private boolean signalVisible;
    private boolean initialFactorVisible;
    private boolean noiseVisible;
    private Optional<DfmResults> dfmResults;

    /**
     * Creates new form ShocksDecompositionView
     */
    public ShocksDecompositionView() {
        initComponents();

        this.actualVisible = true;
        this.signalVisible = true;
        this.initialFactorVisible = true;
        this.noiseVisible = true;
        this.dfmResults = Optional.absent();

        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateChart();
            }
        });

        chart.setValueFormat(new DecimalFormat("#.###"));
        chart.setSeriesRenderer(new SeriesFunction<TimeSeriesChart.RendererType>() {
            @Override
            public TimeSeriesChart.RendererType apply(int series) {
                switch (getType(series)) {
                    case ACTUAL:
                    case SIGNAL:
                        return TimeSeriesChart.RendererType.LINE;
                    default:
                        return TimeSeriesChart.RendererType.STACKED_COLUMN;
                }
            }
        });
        chart.setSeriesFormatter(new SeriesFunction<String>() {
            @Override
            public String apply(int series) {
                return chart.getDataset().getSeriesKey(series).toString();
            }
        });
        chart.setObsFormatter(new ObsFunction<String>() {
            @Override
            public String apply(int series, int obs) {
                return chart.getSeriesFormatter().apply(series)
                        + " - " + chart.getPeriodFormat().format(chart.getDataset().getX(series, obs))
                        + "\n" + chart.getValueFormat().format(chart.getDataset().getY(series, obs));
            }
        });
        chart.setPopupMenu(createChartMenu().getPopupMenu());
        chart.setColorSchemeSupport(defaultColorSchemeSupport);
        chart.setNoDataMessage("No data produced");

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case ACTUAL_VISIBLE_PROPERTY:
                    case SIGNAL_VISIBLE_PROPERTY:
                    case INITIAL_FACTOR_VISIBLE_PROPERTY:
                    case NOISE_VISIBLE_PROPERTY:
                        updateChart();
                        break;
                    case DFM_RESULTS_PROPERTY:
                        updateComboBox();
                        updateChart();
                        break;
                }
            }
        });

        updateComboBox();
        updateChart();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        comboBox = new javax.swing.JComboBox();
        chart = new ec.util.chart.swing.JTimeSeriesChart();

        setLayout(new java.awt.BorderLayout());

        add(comboBox, java.awt.BorderLayout.PAGE_START);
        add(chart, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ec.util.chart.swing.JTimeSeriesChart chart;
    private javax.swing.JComboBox comboBox;
    // End of variables declaration//GEN-END:variables

    //<editor-fold defaultstate="collapsed" desc="Getters/Setters">
    public boolean isActualVisible() {
        return actualVisible;
    }

    public void setActualVisible(boolean actualVisible) {
        boolean old = this.actualVisible;
        this.actualVisible = actualVisible;
        firePropertyChange(ACTUAL_VISIBLE_PROPERTY, old, this.actualVisible);
    }

    public boolean isSignalVisible() {
        return signalVisible;
    }

    public void setSignalVisible(boolean signalVisible) {
        boolean old = this.signalVisible;
        this.signalVisible = signalVisible;
        firePropertyChange(SIGNAL_VISIBLE_PROPERTY, old, this.signalVisible);
    }

    public boolean isInitialFactorVisible() {
        return initialFactorVisible;
    }

    public void setInitialFactorVisible(boolean initialFactorVisible) {
        boolean old = this.initialFactorVisible;
        this.initialFactorVisible = initialFactorVisible;
        firePropertyChange(INITIAL_FACTOR_VISIBLE_PROPERTY, old, this.initialFactorVisible);
    }

    public boolean isNoiseVisible() {
        return noiseVisible;
    }

    public void setNoiseVisible(boolean noiseVisible) {
        boolean old = this.noiseVisible;
        this.noiseVisible = noiseVisible;
        firePropertyChange(NOISE_VISIBLE_PROPERTY, old, this.noiseVisible);
    }

    public Optional<DfmResults> getDfmResults() {
        return dfmResults;
    }

    public void setDfmResults(Optional<DfmResults> dfmResults) {
        Optional<DfmResults> old = this.dfmResults;
        this.dfmResults = dfmResults != null ? dfmResults : Optional.<DfmResults>absent();
        firePropertyChange(DFM_RESULTS_PROPERTY, old, this.dfmResults);
    }
    //</editor-fold>

    private void updateComboBox() {
        if (dfmResults.isPresent()) {
            comboBox.setModel(toComboBoxModel(dfmResults.get().getDescriptions()));
            comboBox.setEnabled(true);
        } else {
            comboBox.setModel(new DefaultComboBoxModel());
            comboBox.setEnabled(false);
        }
    }

    private void updateChart() {
        if (dfmResults.isPresent() && comboBox.getSelectedIndex() != -1) {
            TsXYDatasets.Builder b = TsXYDatasets.builder();
            for (Ts o : toCollection(dfmResults.get())) {
                b.add(o.getName(), o.getTsData());
            }
            chart.setDataset(b.build());
        } else {
            chart.setDataset(null);
        }
    }

    private enum SeriesType {

        ACTUAL, SIGNAL, FACTOR, NOISE
    }

    private SeriesType getType(int series) {
        String key = (String) chart.getDataset().getSeriesKey(series);
        switch (key) {
            case "Actual":
                return SeriesType.ACTUAL;
            case "Signal":
                return SeriesType.SIGNAL;
            case "Initial factor":
                return SeriesType.FACTOR;
            case "Noise":
                return SeriesType.NOISE;
            default:
                return SeriesType.FACTOR;
        }
    }

    private TsCollection toCollection(DfmResults dfmResults) {
        Objects.requireNonNull(dfmResults);

        TsCollection result = TsFactory.instance.createTsCollection();

        int selectedIndex = comboBox.getSelectedIndex();
        if (actualVisible) {
            result.quietAdd(TsFactory.instance.createTs("Actual", null, dfmResults.getTheData()[selectedIndex]));
        }
        if (signalVisible) {
            result.quietAdd(TsFactory.instance.createTs("Signal", null, dfmResults.getSignal()[selectedIndex]));
        }
        TsData[][] x = dfmResults.getShocksDecomposition();
        for (int i = 0; i < x.length - 2; i++) {
            result.quietAdd(TsFactory.instance.createTs("F" + i, null, x[i][selectedIndex]));
        }
        if (initialFactorVisible) {
            result.quietAdd(TsFactory.instance.createTs("Initial factor", null, x[x.length - 2][selectedIndex]));
        }
        if (noiseVisible) {
            result.quietAdd(TsFactory.instance.createTs("Noise", null, x[x.length - 1][selectedIndex]));
        }

        return result;
    }

    private abstract class CustomSwingColorSchemeSupport extends SwingColorSchemeSupport {

        @Override
        public Color getLineColor(int series) {
            switch (getType(series)) {
                case ACTUAL:
                    return getLineColor(ColorScheme.KnownColor.RED);
                case SIGNAL:
                    return getLineColor(ColorScheme.KnownColor.BLUE);
                case FACTOR:
                    int index0 = (actualVisible ? 1 : 0) + (signalVisible ? 1 : 0);
                    return withAlpha(super.getLineColor(series - index0), 50);
                case NOISE:
                    return withAlpha(getLineColor(ColorScheme.KnownColor.GRAY), 50);
                default:
                    throw new RuntimeException();
            }
        }
    }

    private final CustomSwingColorSchemeSupport defaultColorSchemeSupport = new CustomSwingColorSchemeSupport() {
        @Override
        public ColorScheme getColorScheme() {
            return DemetraUI.getInstance().getColorScheme();
        }
    };

    //<editor-fold defaultstate="collapsed" desc="Menus">
    private JMenu newColorSchemeMenu() {
        JMenu item = new JMenu("Color scheme");
        item.add(new JCheckBoxMenuItem(applyColorSchemeSupport(defaultColorSchemeSupport).toAction(chart))).setText("Default");
        item.addSeparator();
        for (final ColorScheme o : DemetraUI.getInstance().getColorSchemes()) {
            final CustomSwingColorSchemeSupport colorSchemeSupport = new CustomSwingColorSchemeSupport() {
                @Override
                public ColorScheme getColorScheme() {
                    return o;
                }
            };
            JMenuItem subItem = item.add(new JCheckBoxMenuItem(applyColorSchemeSupport(colorSchemeSupport).toAction(chart)));
            subItem.setText(o.getDisplayName());
            subItem.setIcon(new ColorSchemeIcon(o));
        }
        return item;
    }

    private JMenu newLineThicknessMenu() {
        JMenu item = new JMenu("Line thickness");
        item.add(new JCheckBoxMenuItem(applyLineThickness(1f).toAction(chart))).setText("Thin");
        item.add(new JCheckBoxMenuItem(applyLineThickness(2f).toAction(chart))).setText("Thick");
        return item;
    }

    private JMenu createChartMenu() {
        JMenu menu = new JMenu();
        JMenuItem item;

        item = menu.add(CopyCommand.INSTANCE.toAction(this));
        item.setText("Copy");

        menu.addSeparator();
        item = menu.add(new JCheckBoxMenuItem(ActualCommand.INSTANCE.toAction(this)));
        item.setText("Show actual");
        item = menu.add(new JCheckBoxMenuItem(SignalCommand.INSTANCE.toAction(this)));
        item.setText("Show signal");
        item = menu.add(new JCheckBoxMenuItem(InitialFactorCommand.INSTANCE.toAction(this)));
        item.setText("Show initial factor");
        item = menu.add(new JCheckBoxMenuItem(NoiseCommand.INSTANCE.toAction(this)));
        item.setText("Show noise");

        menu.addSeparator();
        menu.add(newColorSchemeMenu());
        menu.add(newLineThicknessMenu());
        menu.addSeparator();
        menu.add(newExportMenu());

        return menu;
    }

    private JMenu newExportMenu() {
        JMenu menu = new JMenu("Export image to");
        menu.add(printImage().toAction(chart)).setText("Printer...");
        menu.add(copyImage().toAction(chart)).setText("Clipboard");
        menu.add(saveImage().toAction(chart)).setText("File...");
        return menu;
    }
        //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Commands">
    private static final class ActualCommand extends JCommand<ShocksDecompositionView> {

        public static final ActualCommand INSTANCE = new ActualCommand();

        @Override
        public void execute(ShocksDecompositionView component) throws Exception {
            component.setActualVisible(!component.isActualVisible());
        }

        @Override
        public boolean isSelected(ShocksDecompositionView component) {
            return component.isActualVisible();
        }

        @Override
        public JCommand.ActionAdapter toAction(ShocksDecompositionView component) {
            return super.toAction(component).withWeakPropertyChangeListener(component, ACTUAL_VISIBLE_PROPERTY);
        }
    }

    private static final class SignalCommand extends JCommand<ShocksDecompositionView> {

        public static final SignalCommand INSTANCE = new SignalCommand();

        @Override
        public void execute(ShocksDecompositionView component) throws Exception {
            component.setSignalVisible(!component.isSignalVisible());
        }

        @Override
        public boolean isSelected(ShocksDecompositionView component) {
            return component.isSignalVisible();
        }

        @Override
        public JCommand.ActionAdapter toAction(ShocksDecompositionView component) {
            return super.toAction(component).withWeakPropertyChangeListener(component, SIGNAL_VISIBLE_PROPERTY);
        }
    }

    private static final class InitialFactorCommand extends JCommand<ShocksDecompositionView> {

        public static final InitialFactorCommand INSTANCE = new InitialFactorCommand();

        @Override
        public void execute(ShocksDecompositionView component) throws Exception {
            component.setInitialFactorVisible(!component.isInitialFactorVisible());
        }

        @Override
        public boolean isSelected(ShocksDecompositionView component) {
            return component.isInitialFactorVisible();
        }

        @Override
        public JCommand.ActionAdapter toAction(ShocksDecompositionView component) {
            return super.toAction(component).withWeakPropertyChangeListener(component, INITIAL_FACTOR_VISIBLE_PROPERTY);
        }
    }

    private static final class NoiseCommand extends JCommand<ShocksDecompositionView> {

        public static final NoiseCommand INSTANCE = new NoiseCommand();

        @Override
        public void execute(ShocksDecompositionView component) throws Exception {
            component.setNoiseVisible(!component.isNoiseVisible());
        }

        @Override
        public boolean isSelected(ShocksDecompositionView component) {
            return component.isNoiseVisible();
        }

        @Override
        public JCommand.ActionAdapter toAction(ShocksDecompositionView component) {
            return super.toAction(component).withWeakPropertyChangeListener(component, NOISE_VISIBLE_PROPERTY);
        }
    }

    private static final class CopyCommand extends JCommand<ShocksDecompositionView> {

        public static final CopyCommand INSTANCE = new CopyCommand();

        @Override
        public void execute(ShocksDecompositionView c) throws Exception {
            Optional<DfmResults> dfmResults = c.getDfmResults();
            if (dfmResults.isPresent()) {
                Transferable t = TssTransferSupport.getInstance().fromTsCollection(c.toCollection(dfmResults.get()));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        }

        @Override
        public boolean isEnabled(ShocksDecompositionView c) {
            return c.getDfmResults().isPresent();
        }

        @Override
        public JCommand.ActionAdapter toAction(ShocksDecompositionView c) {
            return super.toAction(c).withWeakPropertyChangeListener(c, DFM_RESULTS_PROPERTY);
        }
    }
    //</editor-fold>

    private static DefaultComboBoxModel toComboBoxModel(DfmSeriesDescriptor[] desc) {
        DefaultComboBoxModel result = new DefaultComboBoxModel(desc);
        return result;
    }
}
