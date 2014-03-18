/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.nbb.demetra.dfm;

import com.google.common.base.Optional;
import ec.nbdemetra.ui.DemetraUI;
import ec.nbdemetra.ui.DemetraUiIcon;
import ec.nbdemetra.ui.NbComponents;
import ec.nbdemetra.ws.WorkspaceItem;
import ec.nbdemetra.ws.ui.WorkspaceTopComponent;
import ec.tss.Dfm.DfmDocument;
import ec.tss.Dfm.DfmResults;
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
import ec.util.chart.swing.JTimeSeriesChart;
import static ec.util.chart.swing.JTimeSeriesChartCommand.applyColorSchemeSupport;
import static ec.util.chart.swing.JTimeSeriesChartCommand.applyLineThickness;
import static ec.util.chart.swing.JTimeSeriesChartCommand.copyImage;
import static ec.util.chart.swing.JTimeSeriesChartCommand.printImage;
import static ec.util.chart.swing.JTimeSeriesChartCommand.saveImage;
import ec.util.chart.swing.SwingColorSchemeSupport;
import ec.util.various.swing.FontAwesome;
import static ec.util.various.swing.FontAwesome.FA_COGS;
import static ec.util.various.swing.FontAwesome.FA_EXCLAMATION_TRIANGLE;
import static ec.util.various.swing.FontAwesome.FA_INFO_CIRCLE;
import static ec.util.various.swing.FontAwesome.FA_SPINNER;
import ec.util.various.swing.JCommand;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import org.netbeans.api.settings.ConvertAsProperties;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewDescription;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
import org.openide.util.ImageUtilities;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//be.nbb.demetra.dfm//DfmOutputView//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "DfmOutputViewTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@Messages({
    "CTL_DfmOutputViewAction=DfmOutputView",
    "CTL_DfmOutputViewTopComponent=DfmOutputView Window",
    "HINT_DfmOutputViewTopComponent=This is a DfmOutputView window"
})
public final class DfmOutputViewTopComponent extends WorkspaceTopComponent<DfmDocument> implements MultiViewElement, MultiViewDescription {

    private static final String ACTUAL_VISIBLE_PROPERTY = "actualVisible";
    private static final String SIGNAL_VISIBLE_PROPERTY = "signalVisible";
    private static final String INITIAL_FACTOR_VISIBLE_PROPERTY = "initialFactorVisible";
    private static final String NOISE_VISIBLE_PROPERTY = "noiseVisible";

    private final DfmController controller;
    private final JTimeSeriesChart chart;
    private final XLabel label;
    private boolean actualVisible;
    private boolean signalVisible;
    private boolean initialFactorVisible;
    private boolean noiseVisible;

    public DfmOutputViewTopComponent() {
        this(null, new DfmController());
    }

    DfmOutputViewTopComponent(WorkspaceItem<DfmDocument> document, DfmController controller) {
        super(document);
        initComponents();
        setName(Bundle.CTL_DfmOutputViewTopComponent());
        setToolTipText(Bundle.HINT_DfmOutputViewTopComponent());

        this.controller = controller;
        this.chart = new JTimeSeriesChart();
        this.label = new XLabel();
        this.actualVisible = true;
        this.signalVisible = true;
        this.initialFactorVisible = true;
        this.noiseVisible = true;

        controller.addPropertyChangeListener(DfmController.DFM_STATE_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // forward event
                firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
                updateComboBox();
                updateChart();
            }
        });

        jComboBox1.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateChart();
            }
        });
        jComboBox1.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText("Var " + (index + 1));
                return this;
            }
        });

        {
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
        }

        updateComboBox();
        updateChart();

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
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jComboBox1 = new javax.swing.JComboBox();

        setLayout(new java.awt.BorderLayout());

        add(jComboBox1, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jComboBox1;
    // End of variables declaration//GEN-END:variables
    void writeProperties(java.util.Properties p) {
    }

    void readProperties(java.util.Properties p) {
    }

    //<editor-fold defaultstate="collapsed" desc="MultiViewElement">
    @Override
    public void componentOpened() {
        super.componentOpened();
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
        // TODO add custom code on component closing
    }

    @Override
    public JComponent getVisualRepresentation() {
        return this;
    }

    @Override
    public JComponent getToolbarRepresentation() {
        JToolBar toolbar = NbComponents.newInnerToolbar();
        toolbar.addSeparator();

        JButton copy = toolbar.add(CopyCommand.INSTANCE.toAction(this));
        copy.setIcon(DemetraUiIcon.EDIT_COPY_16);
        copy.setDisabledIcon(ImageUtilities.createDisabledIcon(copy.getIcon()));

        return toolbar;
    }

    @Override
    public void setMultiViewCallback(MultiViewElementCallback callback) {
    }

    @Override
    public CloseOperationState canCloseElement() {
        return CloseOperationState.STATE_OK;
    }

    @Override
    public void componentActivated() {
        super.componentActivated();
    }

    @Override
    public void componentDeactivated() {
        super.componentDeactivated();
    }

    @Override
    public void componentHidden() {
        super.componentHidden();
    }

    @Override
    public void componentShowing() {
        super.componentShowing();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="MultiViewDescription">
    @Override
    public MultiViewElement createElement() {
        return this;
    }

    @Override
    public String preferredID() {
        return super.preferredID();
    }
    //</editor-fold>    

    @Override
    protected String getContextPath() {
        return DfmDocumentManager.CONTEXTPATH;
    }

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
    //</editor-fold>

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

    @Nonnull
    private Optional<DfmResults> getDfmResults() {
        return Optional.fromNullable(getDocument().getElement().getResults().get("dfm", DfmResults.class));
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

        TsCollection col = TsFactory.instance.createTsCollection();

        int selectedIndex = jComboBox1.getSelectedIndex();
        if (actualVisible) {
            col.quietAdd(TsFactory.instance.createTs("Actual", null, dfmResults.getTheData()[selectedIndex]));
        }
        if (signalVisible) {
            col.quietAdd(TsFactory.instance.createTs("Signal", null, dfmResults.getSignal()[selectedIndex]));
        }
        TsData[][] x = dfmResults.getShocksDecomposition();
        for (int i = 0; i < x.length - 2; i++) {
            col.quietAdd(TsFactory.instance.createTs("F" + i, null, x[i][selectedIndex]));
        }
        if (initialFactorVisible) {
            col.quietAdd(TsFactory.instance.createTs("Initial factor", null, x[x.length - 2][selectedIndex]));
        }
        if (noiseVisible) {
            col.quietAdd(TsFactory.instance.createTs("Noise", null, x[x.length - 1][selectedIndex]));
        }

        return col;
    }

    private void updateComboBox() {
        switch (controller.getDfmState()) {
            case DONE:
                Optional<DfmResults> dfmResult = getDfmResults();
                if (dfmResult.isPresent()) {
                    jComboBox1.setModel(new InputModel(dfmResult.get().getInput()));
                    jComboBox1.setSelectedIndex(0);
                    jComboBox1.setEnabled(true);
                } else {
                    jComboBox1.setModel(new DefaultComboBoxModel());
                    jComboBox1.setSelectedIndex(-1);
                    jComboBox1.setEnabled(false);
                }
                break;
            default:
                jComboBox1.setModel(new DefaultComboBoxModel());
                jComboBox1.setSelectedIndex(-1);
                jComboBox1.setEnabled(false);
                break;
        }
    }

    private void switchTo(Component c) {
        removeAll();
        add(jComboBox1, BorderLayout.NORTH);
        add(c, BorderLayout.CENTER);
    }

    private void updateChart() {
        switch (controller.getDfmState()) {
            case DONE:
                Optional<DfmResults> dfmResults = getDfmResults();
                if (dfmResults.isPresent()) {
                    TsXYDatasets.Builder b = TsXYDatasets.builder();
                    for (Ts o : toCollection(dfmResults.get())) {
                        b.add(o.getName(), o.getTsData());
                    }
                    chart.setDataset(b.build());
                    switchTo(chart);
                } else {
                    switchTo(label.with(FA_EXCLAMATION_TRIANGLE, "No data produced"));
                }
                break;
            case CANCELLED:
                switchTo(label.with(FA_INFO_CIRCLE, "Cancelled"));
                break;
            case CANCELLING:
                switchTo(label.with(FA_SPINNER, "Cancelling"));
                break;
            case FAILED:
                switchTo(label.with(FA_EXCLAMATION_TRIANGLE, "Failed"));
                break;
            case READY:
                switchTo(label.with(FA_COGS, "Ready"));
                break;
            case STARTED:
                switchTo(label.with(FA_SPINNER, "Started"));
                break;
        }
    }

    private static final class InputModel extends AbstractListModel<TsData> implements ComboBoxModel<TsData> {

        private final DfmInformationSet data;
        private TsData selectedItem;

        public InputModel(DfmInformationSet data) {
            this.data = data;
            this.selectedItem = null;
        }

        @Override
        public int getSize() {
            return data.getSeriesCount();
        }

        @Override
        public TsData getElementAt(int index) {
            return data.series(index);
        }

        @Override
        public void setSelectedItem(Object anItem) {
            if ((selectedItem != null && !selectedItem.equals(anItem))
                    || selectedItem == null && anItem != null) {
                selectedItem = (TsData) anItem;
                fireContentsChanged(this, -1, -1);
            }
        }

        @Override
        public Object getSelectedItem() {
            return selectedItem;
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Commands">
    private static final class ActualCommand extends JCommand<DfmOutputViewTopComponent> {

        public static final ActualCommand INSTANCE = new ActualCommand();

        @Override
        public void execute(DfmOutputViewTopComponent component) throws Exception {
            component.setActualVisible(!component.isActualVisible());
        }

        @Override
        public boolean isSelected(DfmOutputViewTopComponent component) {
            return component.isActualVisible();
        }

        @Override
        public JCommand.ActionAdapter toAction(DfmOutputViewTopComponent component) {
            return super.toAction(component).withWeakPropertyChangeListener(component, ACTUAL_VISIBLE_PROPERTY);
        }
    }

    private static final class SignalCommand extends JCommand<DfmOutputViewTopComponent> {

        public static final SignalCommand INSTANCE = new SignalCommand();

        @Override
        public void execute(DfmOutputViewTopComponent component) throws Exception {
            component.setSignalVisible(!component.isSignalVisible());
        }

        @Override
        public boolean isSelected(DfmOutputViewTopComponent component) {
            return component.isSignalVisible();
        }

        @Override
        public JCommand.ActionAdapter toAction(DfmOutputViewTopComponent component) {
            return super.toAction(component).withWeakPropertyChangeListener(component, SIGNAL_VISIBLE_PROPERTY);
        }
    }

    private static final class InitialFactorCommand extends JCommand<DfmOutputViewTopComponent> {

        public static final InitialFactorCommand INSTANCE = new InitialFactorCommand();

        @Override
        public void execute(DfmOutputViewTopComponent component) throws Exception {
            component.setInitialFactorVisible(!component.isInitialFactorVisible());
        }

        @Override
        public boolean isSelected(DfmOutputViewTopComponent component) {
            return component.isInitialFactorVisible();
        }

        @Override
        public ActionAdapter toAction(DfmOutputViewTopComponent component) {
            return super.toAction(component).withWeakPropertyChangeListener(component, INITIAL_FACTOR_VISIBLE_PROPERTY);
        }
    }

    private static final class NoiseCommand extends JCommand<DfmOutputViewTopComponent> {

        public static final NoiseCommand INSTANCE = new NoiseCommand();

        @Override
        public void execute(DfmOutputViewTopComponent component) throws Exception {
            component.setNoiseVisible(!component.isNoiseVisible());
        }

        @Override
        public boolean isSelected(DfmOutputViewTopComponent component) {
            return component.isNoiseVisible();
        }

        @Override
        public ActionAdapter toAction(DfmOutputViewTopComponent component) {
            return super.toAction(component).withWeakPropertyChangeListener(component, NOISE_VISIBLE_PROPERTY);
        }
    }

    private static final class CopyCommand extends JCommand<DfmOutputViewTopComponent> {

        public static final CopyCommand INSTANCE = new CopyCommand();

        @Override
        public void execute(DfmOutputViewTopComponent c) throws Exception {
            Optional<DfmResults> dfmResults = c.getDfmResults();
            if (dfmResults.isPresent()) {
                Transferable t = TssTransferSupport.getInstance().fromTsCollection(c.toCollection(dfmResults.get()));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        }

        @Override
        public boolean isEnabled(DfmOutputViewTopComponent c) {
            return c.controller.getDfmState() == DfmController.DfmState.DONE && c.getDfmResults().isPresent();
        }

        @Override
        public ActionAdapter toAction(DfmOutputViewTopComponent c) {
            return super.toAction(c).withWeakPropertyChangeListener(c, DfmController.DFM_STATE_PROPERTY);
        }
    }
    //</editor-fold>

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

    private static final class XLabel extends JLabel {

        public XLabel() {
            setOpaque(true);
            JList resource = new JList();
            setBackground(resource.getSelectionForeground());
            setForeground(resource.getSelectionBackground());
            setFont(resource.getFont().deriveFont(resource.getFont().getSize2D() * 2));
            setHorizontalAlignment(SwingConstants.CENTER);
            //
            setHorizontalTextPosition(JLabel.CENTER);
            setVerticalTextPosition(JLabel.BOTTOM);
        }

        public XLabel with(FontAwesome icon, String text) {
            setIcon(icon.getIcon(getForeground(), getFont().getSize2D() * 2));
            setText(text);
            return this;
        }
    }
}
