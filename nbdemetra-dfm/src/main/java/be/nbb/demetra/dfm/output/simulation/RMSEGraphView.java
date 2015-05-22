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
package be.nbb.demetra.dfm.output.simulation;

import be.nbb.demetra.dfm.output.simulation.utils.FilterEvaluationSamplePanel;
import com.google.common.base.Optional;
import ec.nbdemetra.ui.DemetraUI;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.DfmSeriesDescriptor;
import ec.tss.dfm.DfmSimulation;
import ec.tss.dfm.DfmSimulationResults;
import ec.tss.dfm.ForecastEvaluationResults;
import ec.tss.tsproviders.utils.Formatters;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataCollector;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.ui.chart.TsCharts;
import ec.ui.interfaces.ITsChart.LinesThickness;
import ec.ui.view.JChartPanel;
import ec.util.chart.ColorScheme;
import ec.util.chart.ColorScheme.KnownColor;
import ec.util.chart.swing.Charts;
import ec.util.chart.swing.SwingColorSchemeSupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 *
 * @author Mats Maggi
 */
public class RMSEGraphView extends javax.swing.JPanel {

    public static final String DFM_SIMULATION_PROPERTY = "dfmSimulation";
    private static final int DFM_INDEX = 0;
    private static final int ARIMA_INDEX = 1;

    private Optional<DfmSimulation> dfmSimulation;

    private final DemetraUI demetraUI;
    private Formatters.Formatter<Number> formatter;
    private SwingColorSchemeSupport defaultColorSchemeSupport;
    private final JChartPanel chartPanel;
    private final XYLineAndShapeRenderer dfmRenderer, arimaRenderer;
    private static XYItemEntity highlight;
    private final RevealObs revealObs;

    private DfmDocument document;

    private FilterEvaluationSamplePanel filterPanel;

    /**
     * Creates new form FixedHorizonsGraphView
     */
    public RMSEGraphView(DfmDocument doc) {
        initComponents();
        this.document = doc;

        revealObs = new RevealObs();

        demetraUI = DemetraUI.getDefault();
        formatter = demetraUI.getDataFormat().numberFormatter();
        defaultColorSchemeSupport = new SwingColorSchemeSupport() {
            @Override
            public ColorScheme getColorScheme() {
                return demetraUI.getColorScheme();
            }
        };

        this.dfmSimulation = Optional.absent();

        dfmRenderer = new LineRenderer(DFM_INDEX);
        arimaRenderer = new LineRenderer(ARIMA_INDEX);

        highlight = null;

        chartPanel = new JChartPanel(createChart());
        Charts.avoidScaling(chartPanel);
        Charts.enableFocusOnClick(chartPanel);

        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                filterPanel = null;
                updateChart();
            }
        });

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case DFM_SIMULATION_PROPERTY:
                        updateComboBox();
                        updateChart();
                        break;
                }
            }
        });

        updateComboBox();
        updateChart();
        onColorSchemeChanged();

        demetraUI.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case DemetraUI.DATA_FORMAT_PROPERTY:
                        onDataFormatChanged();
                        break;
                    case DemetraUI.COLOR_SCHEME_NAME_PROPERTY:
                        onColorSchemeChanged();
                        break;
                }
            }
        });

        chartPanel.addChartMouseListener(new HighlightChartMouseListener2());
        chartPanel.addKeyListener(revealObs);
        chartPanel.getChart().getPlot().setNoDataMessage("Select the evaluation sample by clicking the toolbar button.");

        add(chartPanel, BorderLayout.CENTER);
    }

    private JFreeChart createChart() {
        JFreeChart result = ChartFactory.createXYBarChart("", "", false, "", Charts.emptyXYDataset(), PlotOrientation.VERTICAL, false, false, false);
        result.setPadding(TsCharts.CHART_PADDING);
        result.getTitle().setFont(TsCharts.CHART_TITLE_FONT);

        XYPlot plot = result.getXYPlot();

        plot.setDataset(DFM_INDEX, Charts.emptyXYDataset());
        plot.setRenderer(DFM_INDEX, dfmRenderer);
        plot.mapDatasetToDomainAxis(DFM_INDEX, 0);
        plot.mapDatasetToRangeAxis(DFM_INDEX, 0);

        plot.setDataset(ARIMA_INDEX, Charts.emptyXYDataset());
        plot.setRenderer(ARIMA_INDEX, arimaRenderer);
        plot.mapDatasetToDomainAxis(ARIMA_INDEX, 0);
        plot.mapDatasetToRangeAxis(ARIMA_INDEX, 0);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        NumberAxis rangeAxis = new NumberAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setTickLabelPaint(TsCharts.CHART_TICK_LABEL_COLOR);
        plot.setRangeAxis(rangeAxis);

        NumberAxis domainAxis = new NumberAxis();
        domainAxis.setAutoRangeIncludesZero(false);
        domainAxis.setTickLabelPaint(TsCharts.CHART_TICK_LABEL_COLOR);
        plot.setDomainAxis(domainAxis);

        return result;
    }

    private void onDataFormatChanged() {
        formatter = demetraUI.getDataFormat().numberFormatter();
    }

    private void onColorSchemeChanged() {
        defaultColorSchemeSupport = new SwingColorSchemeSupport() {
            @Override
            public ColorScheme getColorScheme() {
                return demetraUI.getColorScheme();
            }
        };
        
        XYPlot plot = chartPanel.getChart().getXYPlot();
        plot.setBackgroundPaint(defaultColorSchemeSupport.getPlotColor());
        plot.setDomainGridlinePaint(defaultColorSchemeSupport.getGridColor());
        plot.setRangeGridlinePaint(defaultColorSchemeSupport.getGridColor());
        chartPanel.getChart().setBackgroundPaint(defaultColorSchemeSupport.getBackColor());
        
        arimaRenderer.setBasePaint(defaultColorSchemeSupport.getLineColor(KnownColor.RED));
        dfmRenderer.setBasePaint(defaultColorSchemeSupport.getLineColor(KnownColor.BLUE));
        //chart.setColorSchemeSupport(defaultColorSchemeSupport);
    }

    //<editor-fold defaultstate="collapsed" desc="Getters/Setters">
    public Optional<DfmSimulation> getSimulationResults() {
        return dfmSimulation;
    }

    public void setSimulationResults(Optional<DfmSimulation> dfmSimulation) {
        Optional<DfmSimulation> old = this.dfmSimulation;
        this.dfmSimulation = dfmSimulation != null ? dfmSimulation : Optional.<DfmSimulation>absent();
        firePropertyChange(DFM_SIMULATION_PROPERTY, old, this.dfmSimulation);
    }
    //</editor-fold>

    private void updateChart() {
        XYPlot plot = chartPanel.getChart().getXYPlot();

        if (dfmSimulation.isPresent() && comboBox.getSelectedIndex() != -1) {
            toDataset(dfmSimulation.get());
        } else {
            plot.setDataset(null);
        }
    }

    private void updateComboBox() {
        filterPanel = null;
        if (dfmSimulation.isPresent()) {
            comboBox.setModel(toComboBoxModel(document.getDfmResults().getDescriptions()));
            comboBox.setEnabled(true);
        } else {
            comboBox.setModel(new DefaultComboBoxModel());
            comboBox.setEnabled(false);
        }
    }

    private static DefaultComboBoxModel toComboBoxModel(DfmSeriesDescriptor[] data) {
        DefaultComboBoxModel result = new DefaultComboBoxModel(data);
        return result;
    }

    List<Integer> horizons;
    List<TsPeriod> periods;

    private void toDataset(DfmSimulation dfmSimulation) {
        DefaultXYDataset dfmDataset = new DefaultXYDataset();
        DefaultXYDataset arimaDataset = new DefaultXYDataset();

        Objects.requireNonNull(dfmSimulation);

        int selectedIndex = comboBox.getSelectedIndex();
        DfmSimulationResults dfm = dfmSimulation.getDfmResults().get(selectedIndex);
        DfmSimulationResults arima = dfmSimulation.getArimaResults().get(selectedIndex);
        List<Double> trueValues = dfm.getTrueValues();
        horizons = dfm.getForecastHorizons();
        periods = dfm.getEvaluationSample();

        if (filterPanel == null) {
            filterPanel = new FilterEvaluationSamplePanel(periods);
        }

        Double[][] dfmFcts = dfm.getForecastsArray();
        Double[][] arimaFcts = arima.getForecastsArray();

        Map<Integer, TsData> dfmTs = new HashMap<>();
        Map<Integer, TsData> arimaTs = new HashMap<>();

        TsFrequency freq = periods.get(0).getFrequency();

        TsDataCollector coll = new TsDataCollector();
        for (int i = 0; i < periods.size(); i++) {
            if (trueValues.get(i) != null) {
                coll.addObservation(periods.get(i).middle(), trueValues.get(i));
            } else {
                coll.addMissingValue(periods.get(i).middle());
            }
        }
        TsData trueTsData = coll.make(freq, TsAggregationType.None);

        fillMap(dfmTs, dfmFcts, freq);
        fillMap(arimaTs, arimaFcts, freq);

        int size = dfmTs.size();
        double[] xvalues = new double[size];
        double[] dfmValues = new double[size];
        double[] arimaValues = new double[size];

        int index = 0;
        for (Integer horizon : horizons) {
            if (dfmTs.containsKey(horizon)) {
                ForecastEvaluationResults rslt = new ForecastEvaluationResults(dfmTs.get(horizon), arimaTs.get(horizon), trueTsData);
                dfmValues[index] = rslt.calcRMSE();
                arimaValues[index] = rslt.calcRMSE_Benchmark();
                xvalues[index] = horizon;
                index++;
            }
        }
        dfmDataset.addSeries("RMSE (simulation based rec. est.)", new double[][]{xvalues, dfmValues});
        arimaDataset.addSeries("RMSE (Arima recursive est.)", new double[][]{xvalues, arimaValues});

        XYPlot plot = chartPanel.getChart().getXYPlot();

        plot.setDataset(DFM_INDEX, dfmDataset);
        plot.setDataset(ARIMA_INDEX, arimaDataset);

        chartPanel.getChart().setTitle("Evaluation sample from " + periods.get(filterPanel.getStart()).toString()
                + " to " + periods.get(filterPanel.getEnd()).toString());
    }

    private void fillMap(Map<Integer, TsData> map, Double[][] fcts, TsFrequency freq) {
        TsDataCollector coll = new TsDataCollector();
        for (int i = 0; i < horizons.size(); i++) {
            coll.clear();
            for (int j = 0; j < periods.size(); j++) {
                if (fcts[i][j] != null) {
                    coll.addObservation(periods.get(j).middle(), fcts[i][j]);
                } else {
                    coll.addMissingValue(periods.get(j).middle());
                }
            }
            TsData ts = coll.make(freq, TsAggregationType.None);
            ts = ts.cleanExtremities();

            if (ts.getStart().isNotAfter(periods.get(filterPanel.getStart()))
                    && ts.getEnd().isNotBefore(periods.get(filterPanel.getEnd()))) {
                map.put(horizons.get(i), coll.make(freq, TsAggregationType.None));
            }
        }
    }

    private final class HighlightChartMouseListener2 implements ChartMouseListener {

        @Override
        public void chartMouseClicked(ChartMouseEvent event) {
        }

        @Override
        public void chartMouseMoved(ChartMouseEvent event) {
            if (event.getEntity() instanceof XYItemEntity) {
                XYItemEntity xxx = (XYItemEntity) event.getEntity();
                setHighlightedObs(xxx);
            } else {
                setHighlightedObs(null);
            }
        }
    }

    private void setHighlightedObs(XYItemEntity item) {
        if (item == null || highlight != item) {
            highlight = item;
            chartPanel.getChart().fireChartChanged();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        comboBoxPanel = new javax.swing.JPanel();
        comboBox = new javax.swing.JComboBox();
        variableLabel = new javax.swing.JLabel();
        filterButton = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        comboBoxPanel.setLayout(new java.awt.BorderLayout());

        comboBoxPanel.add(comboBox, java.awt.BorderLayout.CENTER);

        org.openide.awt.Mnemonics.setLocalizedText(variableLabel, org.openide.util.NbBundle.getMessage(RMSEGraphView.class, "RMSEGraphView.variableLabel.text")); // NOI18N
        variableLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 10));
        comboBoxPanel.add(variableLabel, java.awt.BorderLayout.WEST);

        org.openide.awt.Mnemonics.setLocalizedText(filterButton, org.openide.util.NbBundle.getMessage(RMSEGraphView.class, "RMSEGraphView.filterButton.text")); // NOI18N
        filterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterButtonActionPerformed(evt);
            }
        });
        comboBoxPanel.add(filterButton, java.awt.BorderLayout.LINE_END);

        add(comboBoxPanel, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents

    private void filterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterButtonActionPerformed
        int r = JOptionPane.showConfirmDialog(chartPanel, filterPanel, "Select evaluation sample", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r == JOptionPane.OK_OPTION) {
            updateChart();
        }
    }//GEN-LAST:event_filterButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comboBox;
    private javax.swing.JPanel comboBoxPanel;
    private javax.swing.JButton filterButton;
    private javax.swing.JLabel variableLabel;
    // End of variables declaration//GEN-END:variables

    private static final Shape ITEM_SHAPE = new Ellipse2D.Double(-3, -3, 6, 6);

    private class LineRenderer extends XYLineAndShapeRenderer {

        private final int index;
        private final Color color;

        public LineRenderer(int index) {
            setBaseItemLabelsVisible(true);
            setAutoPopulateSeriesShape(false);
            setAutoPopulateSeriesFillPaint(false);
            setAutoPopulateSeriesOutlineStroke(false);
            setBaseShape(ITEM_SHAPE);
            setUseFillPaint(true);
            this.index = index;
            this.color = defaultColorSchemeSupport.getLineColor(index == 0 ? ColorScheme.KnownColor.BLUE : ColorScheme.KnownColor.RED);
        }

        @Override
        public boolean getItemShapeVisible(int series, int item) {
            return revealObs.isEnabled() || isObsHighlighted(series, item);
        }

        private boolean isObsHighlighted(int series, int item) {
            XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
            if (highlight != null && highlight.getDataset().equals(plot.getDataset(index))) {
                return highlight.getSeriesIndex() == series && highlight.getItem() == item;
            } else {
                return false;
            }
        }

        @Override
        public boolean isItemLabelVisible(int series, int item) {
            return isObsHighlighted(series, item);
        }

        @Override
        public Paint getSeriesPaint(int series) {
            return color;
        }

        @Override
        public Paint getItemPaint(int series, int item) {
            return color;
        }

        @Override
        public Paint getItemFillPaint(int series, int item) {
            return chartPanel.getChart().getPlot().getBackgroundPaint();
        }

        @Override
        public Stroke getSeriesStroke(int series) {
            return TsCharts.getStrongStroke(LinesThickness.Thin);
        }

        @Override
        public Stroke getItemOutlineStroke(int series, int item) {
            return TsCharts.getStrongStroke(LinesThickness.Thin);
        }

        @Override
        protected void drawItemLabel(Graphics2D g2, PlotOrientation orientation, XYDataset dataset, int series, int item, double x, double y, boolean negative) {
            String label = generateLabel();
            Font font = chartPanel.getFont();
            Paint paint = chartPanel.getChart().getPlot().getBackgroundPaint();
            Paint fillPaint = color;
            Stroke outlineStroke = AbstractRenderer.DEFAULT_STROKE;
            Charts.drawItemLabelAsTooltip(g2, x, y, 3d, label, font, paint, fillPaint, paint, outlineStroke);
        }

        private String generateLabel() {
            XYDataset dataset = highlight.getDataset();
            int series = highlight.getSeriesIndex();
            int item = highlight.getItem();
            String name = dataset.getSeriesKey(series).toString();
            double x = dataset.getXValue(series, item);
            double y = dataset.getYValue(series, item);
            String label = name + "\nForecast horizon : " + (int) x + "\nValue : ";
            label += y;
            return label;
        }
    }

    private final class RevealObs implements KeyListener {

        private boolean enabled = false;

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyChar() == 'r') {
                setEnabled(true);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyChar() == 'r') {
                setEnabled(false);
            }
        }

        private void setEnabled(boolean enabled) {
            if (this.enabled != enabled) {
                this.enabled = enabled;
                firePropertyChange("revealObs", !enabled, enabled);
                chartPanel.getChart().fireChartChanged();
            }
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}

//    private TsCollection dragSelection = null;
//
//    protected Transferable transferableOnSelection() {
//        TsCollection col = TsFactory.instance.createTsCollection();
//
//        ListSelectionModel model = chart.getSeriesSelectionModel();
//        if (!model.isSelectionEmpty()) {
//            for (int i = model.getMinSelectionIndex(); i <= model.getMaxSelectionIndex(); i++) {
//                if (model.isSelectedIndex(i)) {
//                    col.quietAdd(collection.get(i));
//                }
//            }
//        }
//        dragSelection = col;
//        return TssTransferSupport.getDefault().fromTsCollection(dragSelection);
//    }
//
//    public class TsCollectionTransferHandler extends TransferHandler {
//
//        @Override
//        public int getSourceActions(JComponent c) {
//            transferableOnSelection();
//            TsDragRenderer r = dragSelection.getCount() < 10 ? TsDragRenderer.asChart() : TsDragRenderer.asCount();
//            Image image = r.getTsDragRendererImage(Arrays.asList(dragSelection.toArray()));
//            setDragImage(image);
//            return COPY;
//        }
//
//        @Override
//        protected Transferable createTransferable(JComponent c) {
//            return transferableOnSelection();
//        }
//
//        @Override
//        public boolean canImport(TransferHandler.TransferSupport support) {
//            return false;
//        }
//    }
