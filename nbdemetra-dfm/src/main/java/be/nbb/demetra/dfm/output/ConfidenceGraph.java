/*
 * Copyright 2013 National Bank of Belgium
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
package be.nbb.demetra.dfm.output;

import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.ui.ATsControl;
import ec.ui.chart.TsCharts;
import ec.ui.chart.TsXYDatasets;
import ec.ui.interfaces.IColorSchemeAble;
import ec.ui.interfaces.ITsChart.LinesThickness;
import ec.util.chart.ColorScheme;
import ec.util.chart.ColorScheme.KnownColor;
import ec.util.chart.swing.Charts;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

/**
 *
 * @author Mats Maggi
 */
public class ConfidenceGraph extends ATsControl implements IColorSchemeAble {

    private static final String DATA_PROPERTY = "data";
    private static final String CONFIDENCE_PROPERTY = "confidenceVisibility";
    private static final KnownColor MAIN_COLOR = KnownColor.RED;
    private static final KnownColor CONFIDENCE_COLOR = KnownColor.BLUE;

    private static final int MAIN_INDEX = 1000;

    private final int CONFIDENCE99_INDEX = 0;
    private final int CONFIDENCE95_INDEX = 10;
    private final int CONFIDENCE90_INDEX = 20;
    private final int CONFIDENCE80_INDEX = 30;
    private final int CONFIDENCE70_INDEX = 40;
    private final int CONFIDENCE60_INDEX = 50;

    private final int[] indexes = {CONFIDENCE60_INDEX, CONFIDENCE70_INDEX, CONFIDENCE80_INDEX,
        CONFIDENCE90_INDEX, CONFIDENCE95_INDEX, CONFIDENCE99_INDEX};

    private final int intermediateValues = 5;

    private ConfidenceData data;
    private final ChartPanel chartPanel;
    private static XYItemEntity highlight;

    double max = CONFIDENCE60_INDEX;
    double min = CONFIDENCE99_INDEX;
    private Color highValueColour = Color.BLACK;
    private Color lowValueColour = Color.WHITE;

    // How many RGB steps there are between the high and low colours.
    private int colourValueDistance;

    private final RevealObs revealObs;

    public ConfidenceGraph() {
        chartPanel = new ChartPanel(createMarginViewChart());
        data = new ConfidenceData(null, null);
        revealObs = new RevealObs();

        Charts.avoidScaling(chartPanel);
        Charts.enableFocusOnClick(chartPanel);

        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case DATA_PROPERTY:
                        onDataChange();
                        break;
                    case CONFIDENCE_PROPERTY:
                        onDataChange();
                        break;
                }
            }
        });

        chartPanel.addChartMouseListener(new HighlightChartMouseListener2());
        chartPanel.addKeyListener(revealObs);
        
        highlight = null;

        onDataFormatChange();
        onColorSchemeChange();
    }

    private void onDataChange() {
        chartPanel.getChart().setNotify(false);

        XYPlot plot = chartPanel.getChart().getXYPlot();

        if (data.series != null && data.stdev != null) {
            plot.setDataset(MAIN_INDEX, TsXYDatasets.from("series", data.series));

            TsData stdev60 = data.stdev.times(0.84); // Confidence of 60%
            TsData stdev70 = data.stdev.times(1.035); // Confidence of 70%
            TsData stdev80 = data.stdev.times(1.28); // Confidence of 80%
            TsData stdev90 = data.stdev.times(1.645); // Confidence of 90%
            TsData stdev95 = data.stdev.times(1.96); // Confidence of 95%
            TsData stdev99 = data.stdev.times(2.575); // Confidence of 99%
            TsData diff6070 = stdev70.minus(stdev60).div(intermediateValues);
            TsData diff7080 = stdev80.minus(stdev70).div(intermediateValues);
            TsData diff8090 = stdev90.minus(stdev80).div(intermediateValues);
            TsData diff9095 = stdev95.minus(stdev90).div(intermediateValues);
            TsData diff9599 = stdev99.minus(stdev95).div(intermediateValues);

            TsData[] stdevs = {stdev60, stdev70, stdev80, stdev90, stdev95};
            TsData[] diffs = {diff6070, diff7080, diff8090, diff9095, diff9599};

            for (int i = 0; i < stdevs.length; i++) {
                plot.setDataset(indexes[i], TsXYDatasets.builder().add("lower", data.series.minus(stdevs[i])).add("upper", data.series.plus(stdevs[i])).build());
                for (int j = 1; j < intermediateValues; j++) {
                    TsData l = data.series.minus(stdevs[i]).minus(diffs[i].times(j));
                    TsData u = data.series.plus(stdevs[i]).plus(diffs[i].times(j));
                    plot.setDataset(indexes[i] - j, TsXYDatasets.builder().add("lower" + j, l).add("upper" + j, u).build());
                }
            }

            plot.setDataset(CONFIDENCE99_INDEX, TsXYDatasets.builder().add("lower99", data.series.minus(stdev99)).add("upper99", data.series.plus(stdev99)).build());

        } else {
            plot.setDataset(MAIN_INDEX, null);

            for (int i = 0; i < indexes.length; i++) {
                plot.setDataset(indexes[i], null);
                if (i != indexes.length - 1) {
                    for (int j = 1; j < intermediateValues; j++) {
                        plot.setDataset(indexes[i] - j, null);
                    }
                }
            }
        }
        onDataFormatChange();

        chartPanel.getChart().setNotify(true);
    }

    private JFreeChart createMarginViewChart() {
        final JFreeChart result = ChartFactory.createXYLineChart("", "", "", Charts.emptyXYDataset(), PlotOrientation.VERTICAL, false, false, false);
        result.setPadding(TsCharts.CHART_PADDING);

        XYPlot plot = result.getXYPlot();
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        XYLineAndShapeRenderer main = new LineRenderer();
        plot.setRenderer(MAIN_INDEX, main);

        for (int i = 0; i < indexes.length - 1; i++) {
            plot.setRenderer(indexes[i], getDifferenceRenderer());
            for (int j = 1; j < intermediateValues; j++) {
                plot.setRenderer(indexes[i] - j, getDifferenceRenderer());
            }
        }

        plot.setRenderer(CONFIDENCE99_INDEX, getDifferenceRenderer());

        DateAxis domainAxis = new DateAxis();
        domainAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
        domainAxis.setTickLabelPaint(TsCharts.CHART_TICK_LABEL_COLOR);
        plot.setDomainAxis(domainAxis);

        NumberAxis rangeAxis = new NumberAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setTickLabelPaint(TsCharts.CHART_TICK_LABEL_COLOR);
        plot.setRangeAxis(rangeAxis);

        return result;
    }

    private XYDifferenceRenderer getDifferenceRenderer() {
        XYDifferenceRenderer confidence = new XYDifferenceRenderer();
        confidence.setAutoPopulateSeriesPaint(false);
        confidence.setAutoPopulateSeriesStroke(false);
        confidence.setBaseStroke(TsCharts.getNormalStroke(LinesThickness.Thin));
        return confidence;
    }

    @Override
    protected void onDataFormatChange() {
        DateAxis domainAxis = (DateAxis) chartPanel.getChart().getXYPlot().getDomainAxis();
        try {
            domainAxis.setDateFormatOverride(themeSupport.getDataFormat().newDateFormat());
        } catch (IllegalArgumentException ex) {
            // do nothing?
        }
    }

    @Override
    protected void onColorSchemeChange() {
        highValueColour = themeSupport.getLineColor(CONFIDENCE_COLOR);
        lowValueColour = themeSupport.getPlotColor();
        updateColourDistance();
        XYPlot plot = chartPanel.getChart().getXYPlot();
        plot.setBackgroundPaint(themeSupport.getPlotColor());
        plot.setDomainGridlinePaint(themeSupport.getGridColor());
        plot.setRangeGridlinePaint(themeSupport.getGridColor());
        chartPanel.getChart().setBackgroundPaint(themeSupport.getBackColor());

        XYLineAndShapeRenderer main = (XYLineAndShapeRenderer) plot.getRenderer(MAIN_INDEX);
        main.setBasePaint(themeSupport.getLineColor(MAIN_COLOR));

        for (int i = 0; i < indexes.length; i++) {
            XYDifferenceRenderer confidence = ((XYDifferenceRenderer) plot.getRenderer(indexes[i]));
            Color diffArea = getCellColour(indexes[i]);
            confidence.setPositivePaint(diffArea);
            confidence.setNegativePaint(diffArea);
            confidence.setBasePaint(diffArea);
            if (i != indexes.length - 1) {
                for (int j = 1; j < intermediateValues; j++) {
                    confidence = ((XYDifferenceRenderer) plot.getRenderer(indexes[i] - j));
                    diffArea = getCellColour(indexes[i] - ((10 / intermediateValues) * j));
                    confidence.setPositivePaint(diffArea);
                    confidence.setNegativePaint(diffArea);
                    confidence.setBasePaint(diffArea);
                }
            }
        }
    }

    @Override
    public ColorScheme getColorScheme() {
        return themeSupport.getLocalColorScheme();
    }

    @Override
    public void setColorScheme(ColorScheme colorScheme) {
        themeSupport.setLocalColorScheme(colorScheme);
    }

    public void setData(TsData series, TsData stdev) {
        this.data = new ConfidenceData(series, stdev);
        firePropertyChange(DATA_PROPERTY, null, data);
    }

    private static final class ConfidenceData {

        final TsData series;
        final TsData stdev;

        public ConfidenceData(TsData series, TsData stdev) {
            this.series = series;
            this.stdev = stdev;
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

    //==================================================================
    private Color getCellColour(double data) {
        double range = max - min;
        double position = data - min;

        // What proportion of the way through the possible values is that.
        double percentPosition = position / range;

        // Which colour group does that put us in.
        int colourPosition = getColourPosition(percentPosition);

        int r = lowValueColour.getRed();
        int g = lowValueColour.getGreen();
        int b = lowValueColour.getBlue();

        // Make n shifts of the colour, where n is the colourPosition.
        for (int i = 0; i < colourPosition; i++) {
            int rDistance = r - highValueColour.getRed();
            int gDistance = g - highValueColour.getGreen();
            int bDistance = b - highValueColour.getBlue();

            if ((Math.abs(rDistance) >= Math.abs(gDistance))
                    && (Math.abs(rDistance) >= Math.abs(bDistance))) {
                // Red must be the largest.
                r = changeColourValue(r, rDistance);
            } else if (Math.abs(gDistance) >= Math.abs(bDistance)) {
                // Green must be the largest.
                g = changeColourValue(g, gDistance);
            } else {
                // Blue must be the largest.
                b = changeColourValue(b, bDistance);
            }
        }

        return new Color(r, g, b);
    }

    private int getColourPosition(double percentPosition) {
        return (int) Math.round(colourValueDistance * Math.pow(percentPosition, 1.0));
    }

    private int changeColourValue(int colourValue, int colourDistance) {
        if (colourDistance < 0) {
            return colourValue + 1;
        } else if (colourDistance > 0) {
            return colourValue - 1;
        } else {
            // This shouldn't actually happen here.
            return colourValue;
        }
    }

    private void updateColourDistance() {
        int r1 = lowValueColour.getRed();
        int g1 = lowValueColour.getGreen();
        int b1 = lowValueColour.getBlue();
        int r2 = highValueColour.getRed();
        int g2 = highValueColour.getGreen();
        int b2 = highValueColour.getBlue();

        colourValueDistance = Math.abs(r1 - r2);
        colourValueDistance += Math.abs(g1 - g2);
        colourValueDistance += Math.abs(b1 - b2);
    }

    private static final Shape ITEM_SHAPE = new Ellipse2D.Double(-3, -3, 6, 6);

    private class LineRenderer extends XYLineAndShapeRenderer {

        public LineRenderer() {
            setBaseItemLabelsVisible(true);
            setAutoPopulateSeriesShape(false);
            setAutoPopulateSeriesFillPaint(false);
            setAutoPopulateSeriesOutlineStroke(false);
            setBaseShape(ITEM_SHAPE);
            setUseFillPaint(true);
        }

        @Override
        public boolean getItemShapeVisible(int series, int item) {
            return revealObs.isEnabled() || isObsHighlighted(series, item);
        }

        private boolean isObsHighlighted(int series, int item) {
            XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
            if (highlight != null && highlight.getDataset().equals(plot.getDataset(MAIN_INDEX))) {
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
            return themeSupport.getLineColor(MAIN_COLOR);
        }

        @Override
        public Paint getItemPaint(int series, int item) {
            return themeSupport.getLineColor(MAIN_COLOR);
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
            Paint fillPaint = themeSupport.getLineColor(MAIN_COLOR);
            Stroke outlineStroke = AbstractRenderer.DEFAULT_STROKE;
            Charts.drawItemLabelAsTooltip(g2, x, y, 3d, label, font, paint, fillPaint, paint, outlineStroke);
        }

        private String generateLabel() {
            TsPeriod p = new TsPeriod(data.series.getFrequency(), new Date(highlight.getDataset().getX(0, highlight.getItem()).longValue()));
            String label = "Period : " + p.toString() + "\nValue : ";
            label += themeSupport.getDataFormat().numberFormatter().formatAsString(data.series.get(p));
            return label;
        }
    };

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
