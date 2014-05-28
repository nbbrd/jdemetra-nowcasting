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
import ec.ui.ATsControl;
import ec.ui.chart.TsCharts;
import ec.ui.chart.TsXYDatasets;
import ec.ui.interfaces.IColorSchemeAble;
import ec.ui.interfaces.ITsChart.LinesThickness;
import ec.util.chart.ColorScheme;
import ec.util.chart.ColorScheme.KnownColor;
import ec.util.chart.swing.Charts;
import ec.util.chart.swing.SwingColorSchemeSupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 *
 * @author Mats Maggi
 */
public class ConfidenceGraph extends ATsControl implements IColorSchemeAble {

    private static final String DATA_PROPERTY = "data";
    private static final KnownColor MAIN_COLOR = KnownColor.RED;
    private static final KnownColor CONFIDENCE_COLOR = KnownColor.BLUE;

    private static final int MAIN_INDEX = 3;
    private static final int STDEV_INDEX = 2;
    private static final int CONFIDENCE95_INDEX = 0;
    private static final int CONFIDENCE80_INDEX = 1;

    private ConfidenceData data;
    private final ChartPanel chartPanel;

    public ConfidenceGraph() {
        this.chartPanel = new ChartPanel(createMarginViewChart());
        this.data = new ConfidenceData(null, null);

        Charts.avoidScaling(chartPanel);
        
        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case DATA_PROPERTY:
                        onDataChange();
                        break;
                }
            }
        });

        onDataFormatChange();
        onColorSchemeChange();

        //chartPanel.setPopupMenu(buildMenu().getPopupMenu());
    }

    private void onDataChange() {
        chartPanel.getChart().setNotify(false);

        XYPlot plot = chartPanel.getChart().getXYPlot();

        if (data.series != null && data.stdev != null) {
            plot.setDataset(MAIN_INDEX, TsXYDatasets.from("series", data.series));
            TsData lower = data.series.minus(data.stdev);
            TsData upper = data.series.plus(data.stdev);
            plot.setDataset(STDEV_INDEX, TsXYDatasets.builder().add("lowerStdev", lower).add("upperStdev", upper).build());
            
            TsData stdev80 = data.stdev.times(1.28);
            plot.setDataset(CONFIDENCE80_INDEX, TsXYDatasets.builder().add("lower80", data.series.minus(stdev80)).add("upper80", data.series.plus(stdev80)).build());
            
            TsData stdev95 = data.stdev.times(1.96);
            plot.setDataset(CONFIDENCE95_INDEX, TsXYDatasets.builder().add("lower95", data.series.minus(stdev95)).add("upper95", data.series.plus(stdev95)).build());
            
        } else {
            plot.setDataset(MAIN_INDEX, null);
            plot.setDataset(STDEV_INDEX, null);
            plot.setDataset(CONFIDENCE80_INDEX, null);
            plot.setDataset(CONFIDENCE95_INDEX, null);
        }

        onDataFormatChange();

        chartPanel.getChart().setNotify(true);
    }

    private static JFreeChart createMarginViewChart() {
        JFreeChart result = ChartFactory.createXYLineChart("", "", "", Charts.emptyXYDataset(), PlotOrientation.VERTICAL, false, false, false);
        result.setPadding(TsCharts.CHART_PADDING);
        
        XYPlot plot = result.getXYPlot();
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        LinesThickness linesThickness = LinesThickness.Thin;

        XYLineAndShapeRenderer main = new XYLineAndShapeRenderer(true, false);
        main.setAutoPopulateSeriesPaint(false);
        main.setAutoPopulateSeriesStroke(false);
        main.setBaseStroke(TsCharts.getStrongStroke(linesThickness));
        plot.setRenderer(MAIN_INDEX, main);
        
        XYDifferenceRenderer stdev = new XYDifferenceRenderer();
        stdev.setAutoPopulateSeriesPaint(false);
        stdev.setAutoPopulateSeriesStroke(false);
        stdev.setBaseStroke(TsCharts.getNormalStroke(linesThickness));
        plot.setRenderer(STDEV_INDEX, stdev);
        
        XYDifferenceRenderer confidence80 = new XYDifferenceRenderer();
        confidence80.setAutoPopulateSeriesPaint(false);
        confidence80.setAutoPopulateSeriesStroke(false);
        confidence80.setBaseStroke(TsCharts.getNormalStroke(linesThickness));
        plot.setRenderer(CONFIDENCE80_INDEX, confidence80);

        XYDifferenceRenderer confidence95 = new XYDifferenceRenderer();
        confidence95.setAutoPopulateSeriesPaint(false);
        confidence95.setAutoPopulateSeriesStroke(false);
        confidence95.setBaseStroke(TsCharts.getNormalStroke(linesThickness));
        plot.setRenderer(CONFIDENCE95_INDEX, confidence95);

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
        XYPlot plot = chartPanel.getChart().getXYPlot();
        plot.setBackgroundPaint(themeSupport.getPlotColor());
        plot.setDomainGridlinePaint(themeSupport.getGridColor());
        plot.setRangeGridlinePaint(themeSupport.getGridColor());
        chartPanel.getChart().setBackgroundPaint(themeSupport.getBackColor());

        XYLineAndShapeRenderer main = (XYLineAndShapeRenderer) plot.getRenderer(MAIN_INDEX);
        main.setBasePaint(themeSupport.getLineColor(MAIN_COLOR));
        
        XYDifferenceRenderer stdev = ((XYDifferenceRenderer) plot.getRenderer(STDEV_INDEX));
        Color diffArea = SwingColorSchemeSupport.withAlpha(themeSupport.getAreaColor(CONFIDENCE_COLOR), 255);
        stdev.setPositivePaint(diffArea);
        stdev.setNegativePaint(diffArea);
        stdev.setBasePaint(diffArea);

        XYDifferenceRenderer confidence95 = ((XYDifferenceRenderer) plot.getRenderer(CONFIDENCE95_INDEX));
        diffArea = SwingColorSchemeSupport.withAlpha(themeSupport.getAreaColor(CONFIDENCE_COLOR), 75);
        confidence95.setPositivePaint(diffArea);
        confidence95.setNegativePaint(diffArea);
        confidence95.setBasePaint(diffArea);
        
        XYDifferenceRenderer confidence80 = ((XYDifferenceRenderer) plot.getRenderer(CONFIDENCE80_INDEX));
        diffArea = SwingColorSchemeSupport.withAlpha(themeSupport.getAreaColor(CONFIDENCE_COLOR), 150);
        confidence80.setPositivePaint(diffArea);
        confidence80.setNegativePaint(diffArea);
        confidence80.setBasePaint(diffArea);
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
}
