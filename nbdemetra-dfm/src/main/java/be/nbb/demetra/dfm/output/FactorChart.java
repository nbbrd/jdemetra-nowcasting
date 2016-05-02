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
package be.nbb.demetra.dfm.output;

import ec.tss.TsCollection;
import ec.tss.TsFactory;
import ec.tss.datatransfer.TssTransferSupport;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.ui.ATsControl;
import ec.ui.chart.TsCharts;
import ec.ui.chart.TsXYDatasets;
import ec.ui.interfaces.IColorSchemeAble;
import ec.ui.interfaces.ITsChart.LinesThickness;
import ec.util.chart.ColorScheme;
import ec.util.chart.ColorScheme.KnownColor;
import ec.util.chart.swing.ChartCommand;
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
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
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
public class FactorChart extends ATsControl implements IColorSchemeAble {

    private static final String DATA_PROPERTY = "data";
    private static final int FACTOR_INDEX = 1;
    private static final int FILTERED_INDEX = 2;
    private static final int DIFFERENCE_INDEX = 0;
    private static final KnownColor FACTOR_COLOR = KnownColor.RED;
    private static final KnownColor FILTERED_COLOR = KnownColor.GREEN;
    private static final KnownColor DIFFERENCE_COLOR = KnownColor.BLUE;
    
    private final ChartPanel chartPanel;
    private FactorData data;
    
    private final RevealObs revealObs;
    private static XYItemEntity highlight;

    public FactorChart() {
        this.chartPanel = new ChartPanel(createMarginViewChart());
        this.data = new FactorData(null, null, null, null);
        this.revealObs = new RevealObs();
        
        Charts.avoidScaling(chartPanel);
        Charts.enableFocusOnClick(chartPanel);
        chartPanel.setMouseWheelEnabled(true);
        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);
        
        addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case DATA_PROPERTY:
                    onDataChange();
                    break;
            }
        });
        
        chartPanel.addChartMouseListener(new HighlightChartMouseListener2());
        chartPanel.addKeyListener(revealObs);

        onDataFormatChange();
        onColorSchemeChange();

        chartPanel.setPopupMenu(buildMenu().getPopupMenu());
    }
    
    private JFreeChart createMarginViewChart() {
        JFreeChart result = ChartFactory.createXYLineChart("", "", "", Charts.emptyXYDataset(), PlotOrientation.VERTICAL, false, false, false);
        result.setPadding(TsCharts.CHART_PADDING);

        XYPlot plot = result.getXYPlot();
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        LinesThickness linesThickness = LinesThickness.Thin;

        XYLineAndShapeRenderer factor = new LineRenderer(FACTOR_INDEX);
        plot.setRenderer(FACTOR_INDEX, factor);
        
        XYLineAndShapeRenderer filtered = new LineRenderer(FILTERED_INDEX);
        plot.setRenderer(FILTERED_INDEX, filtered);

        XYDifferenceRenderer difference = new XYDifferenceRenderer();
        difference.setAutoPopulateSeriesPaint(false);
        difference.setAutoPopulateSeriesStroke(false);
        difference.setBaseStroke(TsCharts.getNormalStroke(linesThickness));
        plot.setRenderer(DIFFERENCE_INDEX, difference);

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
    
    public void setData(TsData factor, TsData filtered, TsData lower, TsData upper) {
        this.data = new FactorData(factor, filtered, lower, upper);
        firePropertyChange(DATA_PROPERTY, null, data);
    }
    
    //<editor-fold defaultstate="collapsed" desc="EVENT HANDLERS">
    private void onDataChange() {
        chartPanel.getChart().setNotify(false);

        XYPlot plot = chartPanel.getChart().getXYPlot();

        plot.setDataset(FACTOR_INDEX, TsXYDatasets.from("factor", data.factor));
        plot.setDataset(FILTERED_INDEX, TsXYDatasets.from("filtered", data.filtered));
        plot.setDataset(DIFFERENCE_INDEX, TsXYDatasets.builder().add("lower", data.lower).add("upper", data.upper).build());

        onDataFormatChange();

        chartPanel.getChart().setNotify(true);
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

        XYLineAndShapeRenderer factor = (XYLineAndShapeRenderer) plot.getRenderer(FACTOR_INDEX);
        factor.setBasePaint(themeSupport.getLineColor(FACTOR_COLOR));
        
        XYLineAndShapeRenderer filtered = (XYLineAndShapeRenderer) plot.getRenderer(FILTERED_INDEX);
        filtered.setBasePaint(themeSupport.getLineColor(FILTERED_COLOR));

        XYDifferenceRenderer difference = ((XYDifferenceRenderer) plot.getRenderer(DIFFERENCE_INDEX));
        Color diffArea = SwingColorSchemeSupport.withAlpha(themeSupport.getAreaColor(DIFFERENCE_COLOR), 150);
        difference.setPositivePaint(diffArea);
        difference.setNegativePaint(diffArea);
        difference.setBasePaint(themeSupport.getLineColor(DIFFERENCE_COLOR));
    }
    //</editor-fold>

    @Override
    public ColorScheme getColorScheme() {
        return themeSupport.getLocalColorScheme();
    }

    @Override
    public void setColorScheme(ColorScheme colorScheme) {
        themeSupport.setLocalColorScheme(colorScheme);
    }
    
    private JMenu buildMenu() {
        JMenu result = new JMenu();

        result.add(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TsCollection col = TsFactory.instance.createTsCollection();
                col.add(TsFactory.instance.createTs("factor", null, data.factor));
                col.add(TsFactory.instance.createTs("filtered", null, data.filtered));
                col.add(TsFactory.instance.createTs("lower", null, data.lower));
                col.add(TsFactory.instance.createTs("upper", null, data.upper));
                Transferable t = TssTransferSupport.getInstance().fromTsCollection(col);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        }).setText("Copy all series");

        JMenu export = new JMenu("Export image to");
        export.add(ChartCommand.printImage().toAction(chartPanel)).setText("Printer...");
        export.add(ChartCommand.copyImage().toAction(chartPanel)).setText("Clipboard");
        export.add(ChartCommand.saveImage().toAction(chartPanel)).setText("File...");
        result.add(export);

        return result;
    }

    private static final class FactorData {

        final TsData factor;
        final TsData filtered;
        final TsData lower;
        final TsData upper;

        public FactorData(TsData factor, TsData filtered, TsData lower, TsData upper) {
            this.factor = factor;
            this.filtered = filtered;
            this.lower = lower;
            this.upper = upper;
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

    public final class RevealObs implements KeyListener {

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
    
    private static final Shape ITEM_SHAPE = new Ellipse2D.Double(-3, -3, 6, 6);
    
    private class LineRenderer extends XYLineAndShapeRenderer {

        private final int index;
        
        public LineRenderer(int index) {
            this.index = index;
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
            if (highlight != null && highlight.getDataset().equals(plot.getDataset(index))) {
                return highlight.getSeriesIndex() == series 
                        && highlight.getItem() == item;
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
            return themeSupport.getLineColor(index == FACTOR_INDEX ? FACTOR_COLOR : FILTERED_COLOR);
        }

        @Override
        public Paint getItemPaint(int series, int item) {
            return themeSupport.getLineColor(index == FACTOR_INDEX ? FACTOR_COLOR : FILTERED_COLOR);
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
            Paint fillPaint = themeSupport.getLineColor(index == FACTOR_INDEX ? FACTOR_COLOR : FILTERED_COLOR);
            Stroke outlineStroke = AbstractRenderer.DEFAULT_STROKE;
            Charts.drawItemLabelAsTooltip(g2, x, y, 3d, label, font, paint, fillPaint, paint, outlineStroke);
        }

        private String generateLabel() {
            TsData d = index == FACTOR_INDEX ? data.factor : data.filtered;
            TsPeriod p = new TsPeriod(d.getFrequency(), new Date(highlight.getDataset().getX(0, highlight.getItem()).longValue()));
            String label = (index == FACTOR_INDEX ? "Factor" : "Filtered") + "\nPeriod : " + p.toString() + "\nValue : ";
            label += themeSupport.getDataFormat().numberFormatter().formatAsString(d.get(p));
            return label;
        }
    };
}
