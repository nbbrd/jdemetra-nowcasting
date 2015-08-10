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
import ec.tss.datatransfer.TssTransferSupport;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.DfmSeriesDescriptor;
import ec.tss.dfm.DfmSimulation;
import ec.tss.dfm.DfmSimulationResults;
import ec.tss.dfm.ForecastEvaluationResults;
import ec.tss.dfm.SimulationResultsDocument;
import ec.tss.tsproviders.utils.Formatters;
import ec.tstoolkit.data.Table;
import ec.tstoolkit.timeseries.Day;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataCollector;
import ec.tstoolkit.timeseries.simplets.TsDomain;
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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
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
    private static final int STDEV_INDEX = 2;

    private Optional<DfmSimulation> dfmSimulation;

    private final DemetraUI demetraUI;
    private Formatters.Formatter<Number> formatter;
    private SwingColorSchemeSupport defaultColorSchemeSupport;
    private final JChartPanel chartPanel;
    private final XYLineAndShapeRenderer dfmRenderer, arimaRenderer, stdevRenderer;
    private static XYItemEntity highlight;
    private final RevealObs revealObs;

    private FilterEvaluationSamplePanel filterPanel;

    /**
     * Creates new form RMSEGraphView
     */
    public RMSEGraphView(DfmDocument doc) {
        initComponents();

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
        stdevRenderer = new LineRenderer(STDEV_INDEX);

        highlight = null;

        chartPanel = new JChartPanel(createChart());
        Charts.avoidScaling(chartPanel);
        Charts.enableFocusOnClick(chartPanel);
        
        comboBox.setRenderer(new ComboBoxRenderer());
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

        chartPanel.setPopupMenu(buildMenu().getPopupMenu());

        chartPanel.addChartMouseListener(new HighlightChartMouseListener2());
        chartPanel.addKeyListener(revealObs);
        chartPanel.getChart().getPlot().setNoDataMessage("Select evaluation sample by clicking on the toolbar button.");

        add(chartPanel, BorderLayout.CENTER);
    }

    private JMenu buildMenu() {
        JMenu menu = new JMenu();
        JMenuItem item;
        item = new JMenuItem(new CopyAction());
        item.setText("Copy data");
        menu.add(item);

        return menu;
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

        plot.setDataset(STDEV_INDEX, Charts.emptyXYDataset());
        plot.setRenderer(STDEV_INDEX, stdevRenderer);
        plot.mapDatasetToDomainAxis(STDEV_INDEX, 0);
        plot.mapDatasetToRangeAxis(STDEV_INDEX, 0);

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
        stdevRenderer.setBasePaint(defaultColorSchemeSupport.getLineColor(KnownColor.YELLOW));
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
            comboBox.setModel(toComboBoxModel(dfmSimulation.get().getDescriptions()));
            comboBox.setEnabled(true);
        } else {
            comboBox.setModel(new DefaultComboBoxModel());
            comboBox.setEnabled(false);
        }
    }

    private DefaultComboBoxModel toComboBoxModel(List<DfmSeriesDescriptor> data) {
        List<DfmSeriesDescriptor> desc = new ArrayList<>();
        List<Boolean> watched = dfmSimulation.get().getWatched();
        for (int i = 0; i < watched.size(); i++) {
            if (watched.get(i)) {
                desc.add(data.get(i));
            }
        }
        DefaultComboBoxModel result = new DefaultComboBoxModel(desc.toArray());
        return result;
    }

    private List<Integer> horizons;
    private List<TsPeriod> periods;
    private List<TsPeriod> filteredPeriods;
    private double[] xvalues;
    private double[] dfmValues;
    private double[] arimaValues;
    List<Integer> xStdev;
    List<Double> yStdev;

    private void toDataset(DfmSimulation dfmSimulation) {
        DefaultXYDataset dfmDataset = new DefaultXYDataset();
        DefaultXYDataset arimaDataset = new DefaultXYDataset();
        DefaultXYDataset stdevDataset = new DefaultXYDataset();

        Objects.requireNonNull(dfmSimulation);

        int selectedIndex = comboBox.getSelectedIndex();
        DfmSimulationResults dfm = dfmSimulation.getDfmResults().get(selectedIndex);
        DfmSimulationResults arima = dfmSimulation.getArimaResults().get(selectedIndex);
        List<Double> trueValues = dfm.getTrueValues();
        horizons = dfm.getForecastHorizons();
        periods = dfm.getEvaluationSample();

        // Remove periods of evaluation sample not in true values domain
        filteredPeriods = filterEvaluationSample(trueValues);

        if (filterPanel == null) {
            filterPanel = new FilterEvaluationSamplePanel(filteredPeriods);
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
        xvalues = new double[size];
        dfmValues = new double[size];
        arimaValues = new double[size];

        TsPeriod start = filteredPeriods.get(filterPanel.getStart());
        TsPeriod end = filteredPeriods.get(filterPanel.getEnd());
        TsDomain dom = new TsDomain(start, end.minus(start)+1);

        int index = 0;
        for (Integer horizon : horizons) {
            if (dfmTs.containsKey(horizon)) {
                ForecastEvaluationResults rslt = new ForecastEvaluationResults(dfmTs.get(horizon).fittoDomain(dom), arimaTs.get(horizon).fittoDomain(dom), trueTsData);
                dfmValues[index] = rslt.calcRMSE();
                arimaValues[index] = rslt.calcRMSE_Benchmark();
                xvalues[index] = horizon;
                index++;
            }
        }
        dfmDataset.addSeries("RMSE (simulation based rec. est.)", new double[][]{xvalues, dfmValues});
        arimaDataset.addSeries("RMSE (Arima recursive est.)", new double[][]{xvalues, arimaValues});

        // Stdev
        Map<Day, SimulationResultsDocument> results = dfmSimulation.getResults();
        Day[] cal = new Day[results.size()];
        cal = results.keySet().toArray(cal);
        Arrays.sort(cal);
        TsPeriod lastPeriod = filteredPeriods.get(filterPanel.getEnd());

        xStdev = new ArrayList<>();
        yStdev = new ArrayList<>();

        index = 0;
        DfmSeriesDescriptor selected = (DfmSeriesDescriptor)comboBox.getSelectedItem();
        List<DfmSeriesDescriptor> descs = dfmSimulation.getDescriptions();
        int realIndex = 0;
        boolean found = false;
        while (!found) {
            if (selected.description.equals(descs.get(realIndex).description)) {
                found = true;
            } else {
                realIndex++;
            } 
        }
        
        for (Day d : cal) {
            int horizon = d.difference(lastPeriod.lastday());
            if (dfmTs.containsKey(horizon)) {
                TsData stdevs = results.get(d).getSmoothedSeriesStdev()[selectedIndex];
                if (!stdevs.getFrequency().equals(lastPeriod.getFrequency())) {
                    stdevs = stdevs.changeFrequency(lastPeriod.getFrequency(), TsAggregationType.Last, true);
                }
                Double stdev = stdevs.get(lastPeriod);
                xStdev.add(horizon);
                yStdev.add(stdev);
                index++;
            }
        }

        double[] xStdevArray = new double[xStdev.size()];
        double[] yStdevArray = new double[xStdev.size()];
        for (int i = 0; i < xStdevArray.length; i++) {
            xStdevArray[i] = xStdev.get(i);
            yStdevArray[i] = yStdev.get(i);
        }

        stdevDataset.addSeries("Stdev", new double[][]{xStdevArray, yStdevArray});

        XYPlot plot = chartPanel.getChart().getXYPlot();

        plot.setDataset(DFM_INDEX, dfmDataset);
        plot.setDataset(ARIMA_INDEX, arimaDataset);
        plot.setDataset(STDEV_INDEX, stdevDataset);

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

            if (ts.getStart().isNotAfter(filteredPeriods.get(filterPanel.getStart()))
                    && ts.getEnd().isNotBefore(filteredPeriods.get(filterPanel.getEnd()))) {
                map.put(horizons.get(i), coll.make(freq, TsAggregationType.None));
            }
        }
    }

    private List<TsPeriod> filterEvaluationSample(List<Double> trueValues) {
        List<TsPeriod> p = new ArrayList<>();
        for (int i = 0; i < trueValues.size(); i++) {
            if (trueValues.get(i) != null) {
                p.add(periods.get(i));
            }
        }
        return p;
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
        variableLabel = new javax.swing.JLabel();
        comboBox = new javax.swing.JComboBox();
        filterButton = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        comboBoxPanel.setLayout(new javax.swing.BoxLayout(comboBoxPanel, javax.swing.BoxLayout.LINE_AXIS));

        org.openide.awt.Mnemonics.setLocalizedText(variableLabel, org.openide.util.NbBundle.getMessage(RMSEGraphView.class, "RMSEGraphView.variableLabel.text")); // NOI18N
        variableLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 10));
        comboBoxPanel.add(variableLabel);

        comboBoxPanel.add(comboBox);

        org.openide.awt.Mnemonics.setLocalizedText(filterButton, org.openide.util.NbBundle.getMessage(RMSEGraphView.class, "RMSEGraphView.filterButton.text")); // NOI18N
        filterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterButtonActionPerformed(evt);
            }
        });
        comboBoxPanel.add(filterButton);

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
            switch (index) {
                case DFM_INDEX:
                    this.color = defaultColorSchemeSupport.getLineColor(ColorScheme.KnownColor.BLUE);
                    break;
                case ARIMA_INDEX:
                    this.color = defaultColorSchemeSupport.getLineColor(ColorScheme.KnownColor.RED);
                    break;
                case STDEV_INDEX:
                    this.color = defaultColorSchemeSupport.getLineColor(ColorScheme.KnownColor.YELLOW);
                    break;
                default:
                    this.color = defaultColorSchemeSupport.getLineColor(ColorScheme.KnownColor.GRAY);
            }
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

    private class CopyAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (xvalues != null && arimaValues != null && dfmValues != null) {
                Table t = new Table(xvalues.length + 1, 4);
                t.set(0, 0, "Forecast horizon");
                t.set(0, 1, "RMSE (simulation based recursive estimation)");
                t.set(0, 2, "RMSE (arima recursive estimation)");
                t.set(0, 3, "Stdev");
                for (int i = 0; i < xvalues.length; i++) {
                    t.set(i + 1, 0, xvalues[i]);
                    t.set(i + 1, 1, (dfmValues[i] == Double.NaN) ? "" : dfmValues[i]);
                    t.set(i + 1, 2, (arimaValues[i] == Double.NaN) ? "" : arimaValues[i]);

                    int index = xStdev.indexOf((int) xvalues[i]);
                    if (index > -1) {
                        t.set(i + 1, 3, (yStdev.get(index) == Double.NaN) ? "" : yStdev.get(index));
                    }
                }

                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(TssTransferSupport.getDefault().fromTable(t), null);
            }
        }

    }
}
