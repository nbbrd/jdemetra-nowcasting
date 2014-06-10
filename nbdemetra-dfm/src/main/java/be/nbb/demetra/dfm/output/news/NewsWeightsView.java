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
package be.nbb.demetra.dfm.output.news;

import ec.nbdemetra.ui.DemetraUI;
import ec.nbdemetra.ui.NbComponents;
import ec.nbdemetra.ui.awt.PopupListener;
import ec.tss.TsCollection;
import ec.tss.TsFactory;
import ec.tss.datatransfer.TssTransferSupport;
import ec.tss.dfm.DfmResults;
import ec.tss.dfm.DfmSeriesDescriptor;
import ec.tss.tsproviders.utils.Formatters;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.dfm.DfmInformationSet;
import ec.tstoolkit.dfm.DfmInformationUpdates;
import ec.tstoolkit.dfm.DfmNews;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataCollector;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.ui.chart.TsXYDatasets;
import ec.ui.list.TsPeriodTableCellRenderer;
import ec.util.chart.ColorScheme;
import ec.util.chart.ObsFunction;
import ec.util.chart.ObsPredicate;
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
import ec.util.grid.swing.AbstractGridModel;
import ec.util.grid.swing.GridModel;
import ec.util.grid.swing.GridRowHeaderRenderer;
import ec.util.grid.swing.JGrid;
import ec.util.grid.swing.ext.TableGridCommand;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * View displaying News weights values in a table and a chart
 *
 * @author Mats Maggi
 */
public class NewsWeightsView extends JPanel {

    private enum SeriesType {

        OLD_FORECASTS, NEW_FORECASTS
    }

    public static final String RESULTS_PROPERTY = "results";

    private DfmNews doc;
    private DfmSeriesDescriptor[] desc;
    private DfmResults dfmResults;

    private final JGrid grid;
    private final JComboBox combobox;
    private final JSplitPane splitPane;
    private final JTimeSeriesChart chartForecast;
    private TsCollection collection;

    public NewsWeightsView() {
        setLayout(new BorderLayout());

        this.grid = createGrid();
        chartForecast = createChart();

        this.combobox = new JComboBox();

        combobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateGridModel();
                updateChart();
            }
        });

        grid.addMouseListener(new PopupListener.PopupAdapter(buildGridMenu().getPopupMenu()));

        chartForecast.setSeriesRenderer(SeriesFunction.always(TimeSeriesChart.RendererType.LINE));
        chartForecast.setPopupMenu(createChartMenu().getPopupMenu());

        splitPane = NbComponents.newJSplitPane(JSplitPane.VERTICAL_SPLIT, grid, chartForecast);
        splitPane.setResizeWeight(0.5);

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case RESULTS_PROPERTY:
                        updateComboBox();
                        updateGridModel();
                        updateChart();
                }
            }
        });

        updateComboBox();
        updateGridModel();
        updateChart();

        add(combobox, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    //<editor-fold defaultstate="collapsed" desc="Getters / Setters">
    public void setResults(DfmResults results, DfmNews doc) {
        this.dfmResults = results;
        this.doc = doc;
        this.desc = dfmResults.getDescriptions();
        firePropertyChange(RESULTS_PROPERTY, null, results);
    }
    //</editor-fold>    

    //<editor-fold defaultstate="collapsed" desc="Components creation">
    private JGrid createGrid() {
        final JGrid result = new JGrid();
        result.setOddBackground(null);
        result.setRowRenderer(new GridRowHeaderRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel result = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                result.setToolTipText(result.getText());
                return result;
            }
        });

        result.setDefaultRenderer(TsPeriod.class, new TsPeriodTableCellRenderer());
        result.setDefaultRenderer(Double.class, new DoubleTableCellRenderer());

        return result;
    }

    private JTimeSeriesChart createChart() {
        JTimeSeriesChart chart = new JTimeSeriesChart();
        chart.setValueFormat(new DecimalFormat("#.###"));
        chart.setSeriesFormatter(new SeriesFunction<String>() {
            @Override
            public String apply(int series) {
                return collection.get(series).getName();
            }
        });

        chart.setObsFormatter(new ObsFunction<String>() {
            @Override
            public String apply(int series, int obs) {
                return chartForecast.getSeriesFormatter().apply(series)
                        + " - " + chartForecast.getPeriodFormat().format(chartForecast.getDataset().getX(series, obs))
                        + "\n" + chartForecast.getValueFormat().format(chartForecast.getDataset().getY(series, obs));
            }
        });

        chart.setColorSchemeSupport(defaultColorSchemeSupport);
        chart.setNoDataMessage("No data produced");
        return chart;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Components updates">
    private void updateComboBox() {
        if (desc != null && desc.length != 0) {
            combobox.setModel(toComboBoxModel(desc));
            combobox.setEnabled(true);
        } else {
            combobox.setModel(new DefaultComboBoxModel());
            combobox.setEnabled(false);
        }
    }

    private void updateGridModel() {
        if (doc != null) {
            DfmInformationUpdates details = doc.newsDetails();
            List<DfmInformationUpdates.Update> updates = details.updates();
            if (updates.isEmpty()) {
                grid.setModel(null);
            } else {
                grid.setModel(createModel());
            }
        } else {
            grid.setModel(null);
        }
    }

    private void updateChart() {
        if (doc != null) {
            DfmInformationUpdates details = doc.newsDetails();
            List<DfmInformationUpdates.Update> updates = details.updates();
            if (updates.isEmpty()) {
                chartForecast.setDataset(null);
            } else {
                collection = toCollection();
                chartForecast.setDataset(TsXYDatasets.from(collection));
            }
        } else {
            chartForecast.setDataset(null);
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Menus">
    private JMenu newColorSchemeMenu() {
        JMenu item = new JMenu("Color scheme");
        item.add(new JCheckBoxMenuItem(applyColorSchemeSupport(defaultColorSchemeSupport).toAction(chartForecast))).setText("Default");
        item.addSeparator();
        for (final ColorScheme o : DemetraUI.getInstance().getColorSchemes()) {
            final CustomSwingColorSchemeSupport colorSchemeSupport = new CustomSwingColorSchemeSupport() {
                @Override
                public ColorScheme getColorScheme() {
                    return o;
                }
            };
            JMenuItem subItem = item.add(new JCheckBoxMenuItem(applyColorSchemeSupport(colorSchemeSupport).toAction(chartForecast)));
            subItem.setText(o.getDisplayName());
            subItem.setIcon(new ColorSchemeIcon(o));
        }
        return item;
    }

    private JMenu newLineThicknessMenu() {
        JMenu item = new JMenu("Line thickness");
        item.add(new JCheckBoxMenuItem(applyLineThickness(1f).toAction(chartForecast))).setText("Thin");
        item.add(new JCheckBoxMenuItem(applyLineThickness(2f).toAction(chartForecast))).setText("Thick");
        return item;
    }

    private JMenu createChartMenu() {
        JMenu menu = new JMenu();
        JMenuItem item;

        item = menu.add(CopyCommand.INSTANCE.toAction(this));
        item.setText("Copy");

        menu.addSeparator();
        menu.add(newColorSchemeMenu());
        menu.add(newLineThicknessMenu());
        menu.addSeparator();
        menu.add(newExportMenu());

        return menu;
    }

    private JMenu newExportMenu() {
        JMenu menu = new JMenu("Export image to");
        menu.add(printImage().toAction(chartForecast)).setText("Printer...");
        menu.add(copyImage().toAction(chartForecast)).setText("Clipboard");
        menu.add(saveImage().toAction(chartForecast)).setText("File...");
        return menu;
    }

    private JMenu buildGridMenu() {
        JMenu result = new JMenu();
        result.add(TableGridCommand.copyAll(true, true).toAction(grid)).setText("Copy All");
        return result;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Models">
    private static DefaultComboBoxModel toComboBoxModel(DfmSeriesDescriptor[] data) {
        DefaultComboBoxModel result = new DefaultComboBoxModel(data);
        return result;
    }

    private GridModel createModel() {

        calculateData();

        return new AbstractGridModel() {
            @Override
            public int getRowCount() {
                return rows.size();
            }

            @Override
            public int getColumnCount() {
                return titles.size();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {

                if (rowIndex >= rows.size() - 3) {
                    int nbRows = rows.size();
                    // All revisions, old and new forecasts
                    if (columnIndex > 2) {
                        if (rowIndex == nbRows - 3) {
                            return all_revisions.get(columnIndex - 3);
                        } else if (rowIndex == nbRows - 2) {
                            return old_forecasts.get(columnIndex - 3);
                        } else {
                            return new_forecasts.get(columnIndex - 3);
                        }
                    } else {
                        return null;
                    }
                } else {
                    // Normal series
                    switch (columnIndex) {
                        case 0:
                            return ref_periods.get(rowIndex);
                        case 1:
                            return expected.get(rowIndex);
                        case 2:
                            return observations.get(rowIndex);
                        default:
                            return all_weights.get(columnIndex - 3).get(rowIndex);
                    }
                }
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return TsPeriod.class;
                    default:
                        return Double.class;
                }
            }

            @Override
            public String getRowName(int rowIndex) {
                return rows.get(rowIndex);
            }

            @Override
            public String getColumnName(int column) {
                return titles.get(column);
            }
        };
    }
    //</editor-fold>

    private List<String> titles;
    private List<String> rows;
    private List<TsPeriod> periods;
    private List<TsPeriod> ref_periods;
    private List<Double> all_revisions;
    private List<Double> old_forecasts;
    private List<Double> new_forecasts;
    private List<Double> expected;
    private List<Double> observations;
    private List<DataBlock> all_weights;

    private void calculateData() {
        DataBlock n = doc.news();
        DfmInformationSet data = doc.getNewInformationSet();
        int selected = combobox.getSelectedIndex();
        TsData s = data.series(selected);
        TsFrequency freq = doc.getDomain().getFrequency();
        periods = new ArrayList<>();
        all_revisions = new ArrayList<>();
        all_weights = new ArrayList<>();
        old_forecasts = new ArrayList<>();
        new_forecasts = new ArrayList<>();
        for (int j = s.getLength() - 1; j >= 0; --j) {
            if (s.isMissing(j)) {
                TsPeriod p = s.getDomain().get(j).lastPeriod(freq);
                if (p.isNotBefore(doc.getDomain().getStart())) {
                    periods.add(p);

                    DataBlock weights = doc.weights(selected, p); // Get weights
                    all_revisions.add(n.dot(weights));
                    old_forecasts.add(doc.getOldForecast(selected, p));
                    new_forecasts.add(doc.getNewForecast(selected, p));
                    all_weights.add(weights);
                }
            } else {
                break;
            }
        }

        Collections.reverse(periods);
        Collections.reverse(all_revisions);
        Collections.reverse(old_forecasts);
        Collections.reverse(new_forecasts);
        Collections.reverse(all_weights);

        createColumnTitles();

        //================================================
        DfmInformationUpdates details = doc.newsDetails();
        List<DfmInformationUpdates.Update> updates = details.updates();
        rows = new ArrayList<>();
        ref_periods = new ArrayList<>();
        expected = new ArrayList<>();
        observations = new ArrayList<>();
        for (DfmInformationUpdates.Update updt : updates) {
            DfmSeriesDescriptor description = desc[updt.series];
            rows.add(description.description);
            ref_periods.add(updt.period);
            expected.add(updt.getForecast() * description.stdev + description.mean);
            observations.add(updt.getObservation() * description.stdev + description.mean);
        }
        rows.add("All revisions");
        rows.add("Old Forecast");
        rows.add("New Forecast");
    }

    private void createColumnTitles() {
        titles = new ArrayList<>();
        titles.add("Reference Period");
        titles.add("Expected Value");
        titles.add("Observated Value");
        for (TsPeriod p : periods) {
            titles.add("Weight " + p.toString());
        }
    }

    private TsCollection toCollection() {
        TsCollection result = TsFactory.instance.createTsCollection();
        int selectedIndex = combobox.getSelectedIndex();
        double stdev = desc[selectedIndex].stdev;
        double mean = desc[selectedIndex].mean;

        TsData serie = dfmResults.getTheData()[selectedIndex].plus(mean);
        TsDataCollector coll = new TsDataCollector();
        for (int i = 0; i < old_forecasts.size(); i++) {
            coll.addObservation(periods.get(i).middle(), old_forecasts.get(i));
        }
        TsData oldF = coll.make(serie.getFrequency(), TsAggregationType.None).times(stdev).plus(mean);
        coll.clear();
        for (int i = 0; i < new_forecasts.size(); i++) {
            coll.addObservation(periods.get(i).middle(), new_forecasts.get(i));
        }
        TsData newF = coll.make(serie.getFrequency(), TsAggregationType.None).times(stdev).plus(mean);
        TsData old_serie = serie.update(oldF);
        TsData new_serie = serie.update(newF);

        result.quietAdd(TsFactory.instance.createTs("Old Forecasts", null, old_serie));
        result.quietAdd(TsFactory.instance.createTs("New Forecasts", null, new_serie));

        chartForecast.setDashPredicate(new ObsPredicate() {
            @Override
            public boolean apply(int series, int obs) {
                return obs >= chartForecast.getDataset().getItemCount(series) - old_forecasts.size();
            }
        });

        return result;
    }

    //<editor-fold defaultstate="collapsed" desc="JTimeSeriesChart utilities">
    private final CustomSwingColorSchemeSupport defaultColorSchemeSupport = new CustomSwingColorSchemeSupport() {
        @Override
        public ColorScheme getColorScheme() {
            return DemetraUI.getInstance().getColorScheme();
        }
    };

    private abstract class CustomSwingColorSchemeSupport extends SwingColorSchemeSupport {

        @Override
        public Color getLineColor(int series) {
            switch (getType(series)) {
                case OLD_FORECASTS:
                    return getLineColor(ColorScheme.KnownColor.RED);
                case NEW_FORECASTS:
                    return getLineColor(ColorScheme.KnownColor.BLUE);
                default:
                    throw new RuntimeException();
            }
        }
    }

    private SeriesType getType(int series) {
        switch (collection.get(series).getName()) {
            case "Old Forecasts":
                return SeriesType.OLD_FORECASTS;
            case "New Forecasts":
                return SeriesType.NEW_FORECASTS;
            default:
                throw new RuntimeException();
        }
    }
    //</editor-fold>

    private static final class CopyCommand extends JCommand<NewsWeightsView> {

        public static final CopyCommand INSTANCE = new CopyCommand();

        @Override
        public void execute(NewsWeightsView c) throws Exception {
            if (c.collection != null) {
                Transferable t = TssTransferSupport.getInstance().fromTsCollection(c.collection);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        }

        @Override
        public boolean isEnabled(NewsWeightsView c) {
            return c.collection != null;
        }

        @Override
        public JCommand.ActionAdapter toAction(NewsWeightsView c) {
            return super.toAction(c).withWeakPropertyChangeListener(c, RESULTS_PROPERTY);
        }
    }

    private class DoubleTableCellRenderer extends DefaultTableCellRenderer {

        private JToolTip tooltip;
        private Color colorOld;
        private Color colorNew;

        public DoubleTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.TRAILING);
            tooltip = super.createToolTip();
            colorOld = defaultColorSchemeSupport.withAlpha(defaultColorSchemeSupport.getLineColor(ColorScheme.KnownColor.RED), 50);
            colorNew = defaultColorSchemeSupport.withAlpha(defaultColorSchemeSupport.getLineColor(ColorScheme.KnownColor.BLUE), 50);
        }

        @Override
        public JToolTip createToolTip() {
            tooltip.setBackground(getBackground());
            tooltip.setForeground(getForeground());
            return tooltip;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(null);
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof Double) {
                DemetraUI demetraUI = DemetraUI.getInstance();
                Formatters.Formatter<Number> format = demetraUI.getDataFormat().numberFormatter();
                setText(format.formatAsString((Double) value));
            }

            if (column > 2 && !isSelected) {
                if (row == rows.size() - 2) {
                    setBackground(colorOld);
                } else if (row == rows.size() - 1) {
                    setBackground(colorNew);
                }
            }

            return this;
        }
    }

}
