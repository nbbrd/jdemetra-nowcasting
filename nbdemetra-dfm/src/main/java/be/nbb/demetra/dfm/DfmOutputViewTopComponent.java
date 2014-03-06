/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.nbb.demetra.dfm;

import ec.nbdemetra.ui.DemetraUI;
import ec.nbdemetra.ws.WorkspaceItem;
import ec.nbdemetra.ws.ui.WorkspaceTopComponent;
import ec.tss.Dfm.DfmDocument;
import ec.tss.Dfm.DfmResults;
import ec.tstoolkit.dfm.DfmInformationSet;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.ui.chart.TsXYDatasets;
import ec.util.chart.ColorScheme;
import ec.util.chart.ObsFunction;
import ec.util.chart.SeriesFunction;
import ec.util.chart.TimeSeriesChart;
import ec.util.chart.swing.ColorSchemeIcon;
import static ec.util.chart.swing.JTimeSeriesChartCommand.applyColorScheme;
import static ec.util.chart.swing.JTimeSeriesChartCommand.applyLineThickness;
import ec.util.various.swing.JCommand;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import org.netbeans.api.settings.ConvertAsProperties;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewDescription;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
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

    private static final String INITIAL_FACTOR_VISIBLE_PROPERTY = "initialFactorVisible";
    private static final String NOISE_VISIBLE_PROPERTY = "noiseVisible";

    private boolean initialFactorVisible;
    private boolean noiseVisible;

    public DfmOutputViewTopComponent() {
        this(null);
    }

    public DfmOutputViewTopComponent(WorkspaceItem<DfmDocument> document) {
        super(document);
        initComponents();
        setName(Bundle.CTL_DfmOutputViewTopComponent());
        setToolTipText(Bundle.HINT_DfmOutputViewTopComponent());

        this.initialFactorVisible = true;
        this.noiseVisible = true;

        jComboBox1.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateChart();
            }
        });

        {
            jTimeSeriesChart1.setSeriesRenderer(new SeriesFunction<TimeSeriesChart.RendererType>() {
                @Override
                public TimeSeriesChart.RendererType apply(int series) {
                    return series == 0 ? TimeSeriesChart.RendererType.LINE : TimeSeriesChart.RendererType.STACKED_COLUMN;
                }
            });
            jTimeSeriesChart1.setSeriesFormatter(new SeriesFunction<String>() {
                @Override
                public String apply(int series) {
                    return jTimeSeriesChart1.getDataset().getSeriesKey(series).toString();
                }
            });
            jTimeSeriesChart1.setObsFormatter(new ObsFunction<String>() {
                @Override
                public String apply(int series, int obs) {
                    return jTimeSeriesChart1.getSeriesFormatter().apply(series) + " - obs " + obs;
                }
            });

            JMenu chartMenu = new JMenu();
            JMenu item;
            item = new JMenu("Color scheme");
            for (ColorScheme o : DemetraUI.getInstance().getColorSchemes()) {
                JMenuItem subItem = item.add(new JCheckBoxMenuItem(applyColorScheme(o).toAction(jTimeSeriesChart1)));
                subItem.setText(o.getDisplayName());
                subItem.setIcon(new ColorSchemeIcon(o));
            }
            chartMenu.add(item);
            item = new JMenu("Line thickness");
            item.add(new JCheckBoxMenuItem(applyLineThickness(1f).toAction(jTimeSeriesChart1))).setText("Thin");
            item.add(new JCheckBoxMenuItem(applyLineThickness(2f).toAction(jTimeSeriesChart1))).setText("Thick");
            chartMenu.add(item);
            jTimeSeriesChart1.setPopupMenu(chartMenu.getPopupMenu());
        }

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case INITIAL_FACTOR_VISIBLE_PROPERTY:
                        updateChart();
                        break;
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

        jTimeSeriesChart1 = new ec.util.chart.swing.JTimeSeriesChart();
        jComboBox1 = new javax.swing.JComboBox();

        setLayout(new java.awt.BorderLayout());
        add(jTimeSeriesChart1, java.awt.BorderLayout.CENTER);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        add(jComboBox1, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jComboBox1;
    private ec.util.chart.swing.JTimeSeriesChart jTimeSeriesChart1;
    // End of variables declaration//GEN-END:variables
    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
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
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.addSeparator();

        toolbar.add(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateComboBox();
            }
        }).setText("UPDATE");

        JToggleButton b1 = (JToggleButton) toolbar.add(new JToggleButton(InitialFactorCommand.INSTANCE.toAction(this)));
        b1.setText("Show initial factor");

        JToggleButton b2 = (JToggleButton) toolbar.add(new JToggleButton(NoiseCommand.INSTANCE.toAction(this)));
        b2.setText("Show noise");

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

    private void updateComboBox() {
        DfmResults dfmResult = (DfmResults) getDocument().getElement().getResults().get("dfm");

        jComboBox1.setModel(new InputModel(dfmResult.getInput()));
        jComboBox1.setSelectedIndex(0);
    }

    private void updateChart() {
        DfmResults dfmResult = (DfmResults) getDocument().getElement().getResults().get("dfm");

        int selectedIndex = jComboBox1.getSelectedIndex();
        TsXYDatasets.Builder b = TsXYDatasets.builder()
                .add("Actual", dfmResult.getTheData()[selectedIndex]);
//                .add("Signal", dfmResult.getSignal()[selectedIndex]);
        TsData[][] x = dfmResult.getShocksDecomposition();
        for (int i = 0; i < x.length - 2; i++) {
            b.add("Factor " + i, x[i][selectedIndex]);
        }
        if (initialFactorVisible) {
            b.add("Initial factor", x[x.length - 2][selectedIndex]);
        }
        if (noiseVisible) {
            b.add("Noise", x[x.length - 1][selectedIndex]);
        }

        jTimeSeriesChart1.setDataset(b.build());
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
}
