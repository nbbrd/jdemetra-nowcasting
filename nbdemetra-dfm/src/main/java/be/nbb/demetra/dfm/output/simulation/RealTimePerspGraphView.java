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
import ec.tss.tsproviders.utils.Formatters;
import ec.tstoolkit.data.Table;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.ui.chart.BasicXYDataset;
import ec.ui.chart.TsCharts;
import ec.ui.view.JChartPanel;
import ec.util.chart.ColorScheme;
import ec.util.chart.ColorScheme.KnownColor;
import ec.util.chart.swing.Charts;
import ec.util.chart.swing.SwingColorSchemeSupport;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 *
 * @author Mats Maggi
 */
public class RealTimePerspGraphView extends javax.swing.JPanel {

    public static final String DFM_SIMULATION_PROPERTY = "dfmSimulation";

    private Optional<DfmSimulation> dfmSimulation;

    private static final int FCTS_INDEX = 0;
    private static final int TRUE_DATA_INDEX = 1;
    private static final int ARIMA_DATA_INDEX = 2;

    private static final Stroke MARKER_STROKE = new BasicStroke(0.5f);
    private static final Paint MARKER_PAINT = Color.GRAY;
    private static final float MARKER_ALPHA = 1f;

    private final DemetraUI demetraUI;
    private Formatters.Formatter<Number> formatter;
    private SwingColorSchemeSupport defaultColorSchemeSupport;

    private final XYLineAndShapeRenderer trueDataRenderer;
    private final XYLineAndShapeRenderer forecastsRenderer;
    private final XYLineAndShapeRenderer arimaRenderer;

    private final Map<Bornes, Graphs> graphs_;
    private Graphs selectedGraph;

    private JFreeChart mainChart;
    private JFreeChart detailChart;
    private JChartPanel chartPanel;

    private FilterEvaluationSamplePanel filterPanel;

    private int indexSelected = -1;

    /**
     * Creates new form FixedHorizonsGraphView
     */
    public RealTimePerspGraphView(DfmDocument doc) {
        initComponents();

        demetraUI = DemetraUI.getDefault();
        formatter = demetraUI.getDataFormat().numberFormatter();
        defaultColorSchemeSupport = new SwingColorSchemeSupport() {
            @Override
            public ColorScheme getColorScheme() {
                return demetraUI.getColorScheme();
            }
        };

        this.graphs_ = new LinkedHashMap<>();

        trueDataRenderer = new XYLineAndShapeRenderer(true, false);
        trueDataRenderer.setAutoPopulateSeriesPaint(false);
        trueDataRenderer.setBasePaint(defaultColorSchemeSupport.getLineColor(KnownColor.RED));

        forecastsRenderer = new XYLineAndShapeRenderer(true, false);
        forecastsRenderer.setAutoPopulateSeriesPaint(false);
        forecastsRenderer.setAutoPopulateSeriesShape(false);
        forecastsRenderer.setBaseShape(new Ellipse2D.Double(-2, -2, 4, 4));
        forecastsRenderer.setBasePaint(defaultColorSchemeSupport.getLineColor(KnownColor.BLUE));
        forecastsRenderer.setBaseShapesFilled(false);

        arimaRenderer = new XYLineAndShapeRenderer(true, false);
        arimaRenderer.setAutoPopulateSeriesPaint(false);
        arimaRenderer.setAutoPopulateSeriesShape(false);
        arimaRenderer.setBaseShape(new Ellipse2D.Double(-2, -2, 4, 4));
        arimaRenderer.setBasePaint(defaultColorSchemeSupport.getLineColor(KnownColor.GREEN));
        arimaRenderer.setBaseShapesFilled(false);

        mainChart = createChart();
        detailChart = createChart();

        this.dfmSimulation = Optional.absent();

        chartPanel = new JChartPanel(null);
        chartPanel.setPopupMenu(buildMenu().getPopupMenu());

        chartPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                indexSelected = -1;
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    double x = chartPanel.getChartX(e.getX());
                    Graphs g = null;
                    for (Bornes b : graphs_.keySet()) {
                        indexSelected++;
                        if (x >= b.min_ && x <= b.max_) {
                            g = graphs_.get(b);
                            break;
                        }
                    }
                    if (g == null) {
                        return;
                    }

                    showDetail(g);
                }
            }
        });

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

        add(chartPanel, BorderLayout.CENTER);
    }

    private JFreeChart createChart() {
        XYPlot plot = new XYPlot();

        plot.setDataset(TRUE_DATA_INDEX, Charts.emptyXYDataset());
        plot.setRenderer(TRUE_DATA_INDEX, trueDataRenderer);
        plot.mapDatasetToDomainAxis(TRUE_DATA_INDEX, 0);
        plot.mapDatasetToRangeAxis(TRUE_DATA_INDEX, 0);

        plot.setDataset(FCTS_INDEX, Charts.emptyXYDataset());
        plot.setRenderer(FCTS_INDEX, forecastsRenderer);
        plot.mapDatasetToDomainAxis(FCTS_INDEX, 0);
        plot.mapDatasetToRangeAxis(FCTS_INDEX, 0);

        plot.setDataset(ARIMA_DATA_INDEX, Charts.emptyXYDataset());
        plot.setRenderer(ARIMA_DATA_INDEX, arimaRenderer);
        plot.mapDatasetToDomainAxis(ARIMA_DATA_INDEX, 0);
        plot.mapDatasetToRangeAxis(ARIMA_DATA_INDEX, 0);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        JFreeChart result = new JFreeChart("", TsCharts.CHART_TITLE_FONT, plot, false);
        result.setPadding(TsCharts.CHART_PADDING);
        return result;
    }

    private JMenu buildMenu() {
        JMenu menu = new JMenu();

        JMenuItem back = new JMenuItem(new AbstractAction("Show complete data...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showMain();
                indexSelected = -1;
            }
        });
        menu.add(back);

        JMenuItem item = new JMenuItem(new CopyDetailAction());
        item.setText("Copy current data");
        menu.add(item);

        return menu;
    }

    private void showMain() {
        XYPlot plot = mainChart.getXYPlot();
        rescaleAxis((NumberAxis) plot.getRangeAxis());
        chartPanel.setChart(mainChart);
        onColorSchemeChanged();
        chartPanel.setPopupMenu(null);
    }

    private void rescaleAxis(NumberAxis axis) {
        axis.setAutoRangeIncludesZero(false);
    }

    private void showDetail(Graphs g) {
        XYPlot plot = detailChart.getXYPlot();

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelPaint(Color.GRAY);
        plot.setRangeAxis(yAxis);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setTickLabelPaint(Color.GRAY);
        xAxis.setTickUnit(new NumberTickUnit(10));
        xAxis.setRange(horizons.get(0), horizons.get(horizons.size() - 1));
        xAxis.setVerticalTickLabels(true);
        plot.setDomainAxis(xAxis);

        plot.setDataset(TRUE_DATA_INDEX, new BasicXYDataset(Collections.singletonList(g.S1_)));
        plot.setDataset(FCTS_INDEX, new BasicXYDataset(Collections.singletonList(g.S2_)));
        plot.setDataset(ARIMA_DATA_INDEX, new BasicXYDataset(Collections.singletonList(g.S3_)));

        rescaleAxis((NumberAxis) plot.getRangeAxis());

        detailChart.setTitle(g.label_);
        chartPanel.setChart(detailChart);
        chartPanel.setToolTipText("Right click to show complete data");
        chartPanel.setPopupMenu(buildMenu().getPopupMenu());
        onColorSchemeChanged();

        selectedGraph = g;
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
        forecastsRenderer.setBasePaint(defaultColorSchemeSupport.getLineColor(KnownColor.BLUE));
        trueDataRenderer.setBasePaint(defaultColorSchemeSupport.getLineColor(KnownColor.RED));
        arimaRenderer.setBasePaint(defaultColorSchemeSupport.getLineColor(KnownColor.GREEN));

        XYPlot mainPlot = mainChart.getXYPlot();
        mainPlot.setBackgroundPaint(defaultColorSchemeSupport.getPlotColor());
        mainPlot.setDomainGridlinePaint(defaultColorSchemeSupport.getGridColor());
        mainPlot.setRangeGridlinePaint(defaultColorSchemeSupport.getGridColor());
        mainChart.setBackgroundPaint(defaultColorSchemeSupport.getBackColor());

        XYPlot detailPlot = detailChart.getXYPlot();
        detailPlot.setBackgroundPaint(defaultColorSchemeSupport.getPlotColor());
        detailPlot.setDomainGridlinePaint(defaultColorSchemeSupport.getGridColor());
        detailPlot.setRangeGridlinePaint(defaultColorSchemeSupport.getGridColor());
        detailChart.setBackgroundPaint(defaultColorSchemeSupport.getBackColor());
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
        if (dfmSimulation.isPresent() && comboBox.getSelectedIndex() != -1) {
            displayResults(dfmSimulation.get());
            showMain();
            indexSelected = -1;
        }
    }

    private void updateComboBox() {
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

    private void displayResults(DfmSimulation dfmSimulation) {
        graphs_.clear();
        chartPanel.setChart(null);

        BasicXYDataset fctsDataset = new BasicXYDataset();
        BasicXYDataset trueDataset = new BasicXYDataset();
        BasicXYDataset arimaDataset = new BasicXYDataset();

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

        int np = horizons.size() - 1; // - 1 ???
        double xstart = -0.4;
        double xend = 0.4;
        final double xstep = 0.8 / np;

        Double[][] dfmFcts = dfm.getForecastsArray();
        Double[][] arimaFcts = arima.getForecastsArray();

        //Double[][] arimaFcts = arima.getForecastsArray();
        TsPeriod start = filteredPeriods.get(filterPanel.getStart());
        TsPeriod end = filteredPeriods.get(filterPanel.getEnd());
        int startIndex = periods.indexOf(start);
        int endIndex = periods.indexOf(end);

        for (int i = startIndex; i <= endIndex; i++) {
            double x = xstart;
            double[] dfmData = new double[horizons.size()];
            double[] arimaData = new double[horizons.size()];
            for (int j = 0; j < horizons.size(); j++) {
                dfmData[j] = dfmFcts[j][i] == null ? Double.NaN : dfmFcts[j][i];
                arimaData[j] = arimaFcts[j][i] == null ? Double.NaN : arimaFcts[j][i];
            }

            int n = dfmData.length;
            double m = trueValues.get(i) == null ? Double.NaN : trueValues.get(i);
            double[] trueX = {xstart, xend};
            double[] true2X = {horizons.get(0), horizons.get(np)};
            double[] trueY = {m, m};
            double[] dfmX = new double[n], dfm2X = new double[n];
            double[] dfmY = new double[n], dfm2Y = new double[n];
            double[] arimaX = new double[n], arima2X = new double[n];
            double[] arimaY = new double[n], arima2Y = new double[n];

            for (int j = 0; j < n; j++) {
                dfmX[j] = x;
                dfmY[j] = dfmData[j];
                dfm2X[j] = horizons.get(j).doubleValue();
                dfm2Y[j] = dfmData[j];

                arimaX[j] = x;
                arimaY[j] = arimaData[j];
                arima2X[j] = horizons.get(j).doubleValue();
                arima2Y[j] = arimaData[j];

                x += xstep;
            }

            String itemName = periods.get(i).toString();
            BasicXYDataset.Series mean = BasicXYDataset.Series.of(itemName, trueX, trueY);
            BasicXYDataset.Series mean2 = BasicXYDataset.Series.of(itemName, true2X, trueY);
            BasicXYDataset.Series points = BasicXYDataset.Series.of(itemName, dfmX, dfmY);
            BasicXYDataset.Series points2 = BasicXYDataset.Series.of(itemName, dfm2X, dfm2Y);
            BasicXYDataset.Series arimaSeries = BasicXYDataset.Series.of(itemName, arimaX, arimaY);
            BasicXYDataset.Series arima2Series = BasicXYDataset.Series.of(itemName, arima2X, arima2Y);

            Bornes b = new Bornes(xstart, xend);
            Graphs g = new Graphs(mean2, points2, arima2Series, itemName);
            graphs_.put(b, g);

            trueDataset.addSeries(mean);
            fctsDataset.addSeries(points);
            arimaDataset.addSeries(arimaSeries);

            xstart++;
            xend++;
        }

        XYPlot plot = mainChart.getXYPlot();
        configureAxis(plot, startIndex, endIndex);
        plot.setDataset(TRUE_DATA_INDEX, trueDataset);
        plot.setDataset(FCTS_INDEX, fctsDataset);
        plot.setDataset(ARIMA_DATA_INDEX, arimaDataset);
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

    private void configureAxis(XYPlot plot, int start, int end) {
        int nb = graphs_.size();
        List<String> names = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            names.add(periods.get(i).toString());
        }

        NumberAxis xAxis = new NumberAxis();
        xAxis.setTickLabelPaint(Color.GRAY);
        xAxis.setTickUnit(new MyTickUnit(names));
        xAxis.setRange(-0.5, nb - 0.5);
        xAxis.setVerticalTickLabels(true);
        plot.setDomainAxis(xAxis);
        plot.setDomainGridlinesVisible(false);
        NumberAxis yaxis = new NumberAxis();
        rescaleAxis(yaxis);
        yaxis.configure();
        plot.setRangeAxis(yaxis);

        for (int i = 0; i < nb; i++) {
            ValueMarker marker = new ValueMarker(i + 0.5);
            marker.setStroke(MARKER_STROKE);
            marker.setPaint(MARKER_PAINT);
            marker.setAlpha(MARKER_ALPHA);
            plot.addDomainMarker(marker);
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
        filterSampleButton = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        comboBoxPanel.setLayout(new javax.swing.BoxLayout(comboBoxPanel, javax.swing.BoxLayout.LINE_AXIS));

        org.openide.awt.Mnemonics.setLocalizedText(variableLabel, org.openide.util.NbBundle.getMessage(RealTimePerspGraphView.class, "RealTimePerspGraphView.variableLabel.text")); // NOI18N
        variableLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 10));
        comboBoxPanel.add(variableLabel);

        comboBoxPanel.add(comboBox);

        org.openide.awt.Mnemonics.setLocalizedText(filterSampleButton, org.openide.util.NbBundle.getMessage(RealTimePerspGraphView.class, "RealTimePerspGraphView.filterSampleButton.text")); // NOI18N
        filterSampleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterSampleButtonActionPerformed(evt);
            }
        });
        comboBoxPanel.add(filterSampleButton);

        add(comboBoxPanel, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents

    private void filterSampleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterSampleButtonActionPerformed
        int r = JOptionPane.showConfirmDialog(chartPanel, filterPanel, "Select evaluation sample", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r == JOptionPane.OK_OPTION) {
            updateChart();
        }
    }//GEN-LAST:event_filterSampleButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comboBox;
    private javax.swing.JPanel comboBoxPanel;
    private javax.swing.JButton filterSampleButton;
    private javax.swing.JLabel variableLabel;
    // End of variables declaration//GEN-END:variables

    static class Bornes {

        static final Bornes ZERO = new Bornes(0, 0);
        final double min_;
        final double max_;

        Bornes(double min, double max) {
            min_ = min;
            max_ = max;
        }
    }

    static class Graphs {

        final BasicXYDataset.Series S1_;
        final BasicXYDataset.Series S2_;
        final BasicXYDataset.Series S3_;
        final String label_;

        Graphs(BasicXYDataset.Series s1, BasicXYDataset.Series s2, BasicXYDataset.Series s3, String label) {
            S1_ = s1;
            S2_ = s2;
            S3_ = s3;
            label_ = label;
        }

        int getMaxElements() {
            int elements = S1_.getItemCount();
            if (elements < S2_.getItemCount()) {
                elements = S2_.getItemCount();
            }

            if (elements < S3_.getItemCount()) {
                elements = S3_.getItemCount();
            }

            return elements;
        }
    }

    static class MyTickUnit extends NumberTickUnit {

        private List<String> names;

        public MyTickUnit(List<String> names) {
            super(1);
            this.names = names;
        }

        @Override
        public String valueToString(double value) {
            if (value < 0 || value >= names.size()) {
                return "";
            }

            return names.get((int) value);
        }
    }

    private class CopyDetailAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (chartPanel.getChart().equals(detailChart) && detailChart != null && selectedGraph != null) {
                BasicXYDataset.Series trueData = selectedGraph.S1_;
                BasicXYDataset.Series fcts = selectedGraph.S2_;
                BasicXYDataset.Series arima = selectedGraph.S3_;

                int trueCount = trueData == null ? 0 : trueData.getItemCount();
                int fctsCount = fcts == null ? 0 : fcts.getItemCount();
                int arimaCount = arima == null ? 0 : arima.getItemCount();

                int maxItems = Math.max(arimaCount, Math.max(trueCount, fctsCount));

                Table t = new Table(maxItems + 2, 6);
                t.set(0, 2, selectedGraph.label_);
                t.set(1, 0, "Horizons");
                t.set(1, 1, "True data");
                t.set(1, 2, "Horizons");
                t.set(1, 3, "Forecasts");
                t.set(1, 4, "Horizons");
                t.set(1, 5, "Arima");

                for (int i = 0; i < trueCount; i++) {
                    t.set((i + 2), 0, trueData.getXValue(i));
                    t.set((i + 2), 1, trueData.getYValue(i));
                }

                for (int i = 0; i < fctsCount; i++) {
                    t.set((i + 2), 2, fcts.getXValue(i));
                    t.set((i + 2), 3, fcts.getYValue(i));
                }

                for (int i = 0; i < arimaCount; i++) {
                    t.set((i + 2), 4, arima.getXValue(i));
                    t.set((i + 2), 5, arima.getYValue(i));
                }

                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(TssTransferSupport.getDefault().fromTable(t), null);
            }
        }
    }
}
