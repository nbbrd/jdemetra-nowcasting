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

import be.nbb.demetra.dfm.output.news.outline.CustomOutlineCellRenderer;
import be.nbb.demetra.dfm.output.news.outline.NewsRenderer;
import be.nbb.demetra.dfm.output.news.outline.NewsRenderer.Type;
import be.nbb.demetra.dfm.output.news.outline.NewsRowModel;
import be.nbb.demetra.dfm.output.news.outline.NewsTreeModel;
import be.nbb.demetra.dfm.output.news.outline.TreeNode.VariableNode;
import be.nbb.demetra.dfm.output.news.outline.XOutline;
import ec.nbdemetra.ui.DemetraUI;
import ec.nbdemetra.ui.NbComponents;
import ec.tss.TsCollection;
import ec.tss.TsFactory;
import ec.tss.datatransfer.TsDragRenderer;
import ec.tss.datatransfer.TssTransferSupport;
import ec.tss.dfm.DfmResults;
import ec.tss.dfm.DfmSeriesDescriptor;
import ec.tss.tsproviders.utils.Formatters;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.dfm.DfmNews;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.information.TsInformationSet;
import ec.tstoolkit.timeseries.information.TsInformationUpdates;
import ec.tstoolkit.timeseries.information.TsInformationUpdates.Update;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataCollector;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.ui.chart.TsXYDatasets;
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
import ec.util.various.swing.JCommand;
import ec.util.various.swing.ModernUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.tree.TreeModel;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.OutlineModel;

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

    private final XOutline outline;
    private final JComboBox combobox;
    private final JSplitPane splitPane;
    private final JTimeSeriesChart chartForecast;
    private TsCollection collection;

    private final DemetraUI demetraUI;
    private Formatters.Formatter<Number> formatter;
    private CustomSwingColorSchemeSupport defaultColorSchemeSupport;

    private final ListSelectionListener outlineListener, chartListener;

    public NewsWeightsView() {
        setLayout(new BorderLayout());

        demetraUI = DemetraUI.getDefault();
        formatter = demetraUI.getDataFormat().numberFormatter();
        defaultColorSchemeSupport = new CustomSwingColorSchemeSupport() {
            @Override
            public ColorScheme getColorScheme() {
                return demetraUI.getColorScheme();
            }
        };

        chartForecast = createChart();
        outline = new XOutline();
        outline.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        combobox = new JComboBox();

        combobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateOutlineModel();
                updateChart();
            }
        });

        chartForecast.setSeriesRenderer(SeriesFunction.always(TimeSeriesChart.RendererType.LINE));
        chartForecast.setPopupMenu(createChartMenu().getPopupMenu());
        chartForecast.setMouseWheelEnabled(true);

        JScrollPane p = ModernUI.withEmptyBorders(new JScrollPane());
        p.setViewportView(outline);

        splitPane = NbComponents.newJSplitPane(JSplitPane.VERTICAL_SPLIT, p, chartForecast);
        splitPane.setResizeWeight(0.5);

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case RESULTS_PROPERTY:
                        updateComboBox();
                        updateOutlineModel();
                        updateChart();
                }
            }
        });

        outlineListener = new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                chartForecast.getSeriesSelectionModel().removeListSelectionListener(chartListener);
                chartForecast.getSeriesSelectionModel().clearSelection();
                ListSelectionModel model = (ListSelectionModel) e.getSource();
                if (!model.isSelectionEmpty()) {
                    for (int i = 0; i < outline.getSelectedRows().length; i++) {
                        int row = outline.getSelectedRows()[i];
                        if (row == outline.getRowCount() - 2) {
                            chartForecast.getSeriesSelectionModel().addSelectionInterval(0, 0);
                        } else if (row == outline.getRowCount() - 1) {
                            chartForecast.getSeriesSelectionModel().addSelectionInterval(1, 1);
                        }
                    }
                }
                chartForecast.getSeriesSelectionModel().addListSelectionListener(chartListener);
            }
        };

        chartListener = new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                outline.getSelectionModel().removeListSelectionListener(outlineListener);
                outline.getSelectionModel().clearSelection();
                ListSelectionModel model = (ListSelectionModel) e.getSource();
                if (!model.isSelectionEmpty()) {
                    for (int i = model.getMinSelectionIndex(); i <= model.getMaxSelectionIndex(); i++) {
                        if (model.isSelectedIndex(i)) {
                            int rowCount = outline.getRowCount();
                            if (i == 0) {
                                outline.getSelectionModel().addSelectionInterval(rowCount - 2, rowCount - 2);
                            } else if (i == 1) {
                                outline.getSelectionModel().addSelectionInterval(rowCount - 1, rowCount - 1);
                            }
                        }
                    }
                }
                outline.getSelectionModel().addListSelectionListener(outlineListener);
            }
        };

        outline.getSelectionModel().addListSelectionListener(outlineListener);
        chartForecast.getSeriesSelectionModel().addListSelectionListener(chartListener);

        updateComboBox();
        updateOutlineModel();
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

        add(combobox, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void onColorSchemeChanged() {
        defaultColorSchemeSupport = new CustomSwingColorSchemeSupport() {
            @Override
            public ColorScheme getColorScheme() {
                return demetraUI.getColorScheme();
            }
        };
        chartForecast.setColorSchemeSupport(defaultColorSchemeSupport);
        outline.setDefaultRenderer(String.class, new CustomOutlineCellRenderer(defaultColorSchemeSupport, Type.WEIGHTS));
    }

    private void onOutlineColorSchemeChanged() {
        outline.setDefaultRenderer(String.class, new CustomOutlineCellRenderer(defaultColorSchemeSupport, Type.WEIGHTS));
    }

    private void onDataFormatChanged() {
        formatter = demetraUI.getDataFormat().numberFormatter();
        try {
            chartForecast.setPeriodFormat(demetraUI.getDataFormat().newDateFormat());
        } catch (IllegalArgumentException ex) {
            // do nothing?
        }
        try {
            chartForecast.setValueFormat(demetraUI.getDataFormat().newNumberFormat());

            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    refreshModel();
                }
            });
        } catch (IllegalArgumentException ex) {
            // do nothing?
        }
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
                        + "\nPeriod : " + collection.get(series).getTsData().getDomain().get(obs).toString()
                        + "\nValue : " + formatter.format(chartForecast.getDataset().getY(series, obs));
            }
        });

        chart.addPropertyChangeListener(JTimeSeriesChart.COLOR_SCHEME_SUPPORT_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                onOutlineColorSchemeChanged();
            }
        });

        chart.setColorSchemeSupport(defaultColorSchemeSupport);
        chart.setNoDataMessage("No data produced");
        chart.setTransferHandler(new TsCollectionTransferHandler());

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

    private void updateOutlineModel() {
        if (doc != null) {
            calculateData();
            refreshModel();
        }
    }

    private void updateChart() {
        if (doc != null) {
            TsInformationUpdates details = doc.newsDetails();
            List<TsInformationUpdates.Update> updates = details.news();
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
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Models">
    private static DefaultComboBoxModel toComboBoxModel(DfmSeriesDescriptor[] data) {
        DefaultComboBoxModel result = new DefaultComboBoxModel(data);
        return result;
    }

    private List<VariableNode> nodes = new ArrayList<>();

    private void refreshModel() {
        TreeModel treeMdl = new NewsTreeModel(nodes);
        OutlineModel mdl = DefaultOutlineModel.createOutlineModel(treeMdl, new NewsRowModel(titles, newPeriods, formatter), true);
        outline.setRenderDataProvider(new NewsRenderer(defaultColorSchemeSupport, Type.WEIGHTS));
        outline.setDefaultRenderer(String.class, new CustomOutlineCellRenderer(defaultColorSchemeSupport, Type.WEIGHTS));
        outline.setModel(mdl);

        outline.setTableHeader(new JTableHeader(outline.getColumnModel()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = 40;
                return d;
            }
        });

        outline.getColumnModel().getColumn(0).setHeaderValue(" ");
        outline.getColumnModel().getColumn(0).setPreferredWidth(200);
        for (int i = 1; i < outline.getColumnCount(); i++) {
            outline.getColumnModel().getColumn(i).setPreferredWidth(60);
        }
    }

    //</editor-fold>
    private List<Title> titles;
    private List<TsPeriod> newPeriods;
    private List<TsPeriod> oldPeriods;
    private List<Double> all_news;
    private List<Double> all_revisions;
    private List<Double> old_forecasts;
    private List<Double> new_forecasts;
    private List<DataBlock> all_news_weights;
    private List<DataBlock> all_revisions_weights;
    private Map<TsPeriod, Double> old_forecasts2;
    private Map<TsPeriod, Double> new_forecasts2;

    private void calculateData() {
        DataBlock n = doc.news();
        DataBlock r = doc.revisions();
        TsInformationSet dataNew = doc.getNewInformationSet();
        TsInformationSet dataOld = doc.getOldInformationSet();
        int selected = combobox.getSelectedIndex();
        TsData sNew = dataNew.series(selected);
        TsData sOld = dataOld.series(selected);
        TsPeriod newsStart = null, revsStart = null;

        TsFrequency freq = doc.getNewsDomain().getFrequency();
        newPeriods = new ArrayList<>();
        oldPeriods = new ArrayList<>();
        all_news = new ArrayList<>();
        all_revisions = new ArrayList<>();
        all_news_weights = new ArrayList<>();
        all_revisions_weights = new ArrayList<>();
        old_forecasts = new ArrayList<>();
        new_forecasts = new ArrayList<>();
        old_forecasts2 = new HashMap<>();
        new_forecasts2 = new HashMap<>();

        double mean = desc[selected].mean;
        double stdev = desc[selected].stdev;

        if (!doc.newsDetails().news().isEmpty()) {
            newsStart = doc.getNewsDomain().getStart();
        }
        if (!doc.newsDetails().revisions().isEmpty()) {
            revsStart = doc.getRevisionsDomain().getStart();
        }

        for (int j = sNew.getLength() - 1; j >= 0; --j) {
            if (sNew.isMissing(j)) {
                TsPeriod p = sNew.getDomain().get(j);
                TsPeriod pN = p.lastPeriod(freq);
                if (pN.isNotBefore(doc.getNewsDomain().getStart())) {
                    newPeriods.add(p);

                    DataBlock news_weights = doc.weights(selected, pN); // Get weights
                    all_news.add(n.dot(news_weights) * stdev);

                    double newValue = (doc.getNewForecast(selected, pN) * stdev) + mean;
                    new_forecasts.add(newValue);
                    new_forecasts2.put(p, newValue);

                    all_news_weights.add(news_weights);

                    if (!doc.newsDetails().revisions().isEmpty()) {
                        DataBlock revisions_weights = doc.weightsRevisions(selected, pN);
                        all_revisions.add(r.dot(revisions_weights) * stdev);

                        all_revisions_weights.add(revisions_weights);
                    }
                }
            } else {
                break;
            }
        }

        for (int j = sOld.getLength() - 1; j >= 0; --j) {
            if (sOld.isMissing(j)) {
                TsPeriod p = sOld.getDomain().get(j);
                TsPeriod pO = p.lastPeriod(freq);
                if (pO.isNotBefore(doc.getNewsDomain().getStart())) {
                    oldPeriods.add(p);

                    double oldValue = (doc.getOldForecast(selected, pO) * stdev) + mean;
                    old_forecasts.add(oldValue);
                    old_forecasts2.put(p, oldValue);
                }
            } else {
                break;
            }
        }

        Collections.reverse(newPeriods);
        Collections.reverse(oldPeriods);
        Collections.reverse(all_news);
        Collections.reverse(all_revisions);
        Collections.reverse(old_forecasts);
        Collections.reverse(new_forecasts);
        Collections.reverse(all_news_weights);
        Collections.reverse(all_revisions_weights);

        createColumnTitles();

        //================================================
        TsInformationUpdates details = doc.newsDetails();
        List<Update> updates = details.news();

        nodes = new ArrayList<>();

        List<VariableNode> newsNodes = new ArrayList<>();
        for (int i = 0; i < updates.size(); i++) {
            TsInformationUpdates.Update updt = updates.get(i);
            TsPeriod p = updt.period;
            if (p.firstPeriod(freq).isNotBefore(newsStart)) {
                DfmSeriesDescriptor descriptor = desc[updt.series];
                String name = descriptor.description;

                Double exp = updt.getForecast() * descriptor.stdev + descriptor.mean;
                Double obs = updt.getObservation() * descriptor.stdev + descriptor.mean;

                Map<TsPeriod, Double> values = new HashMap<>();
                for (int j = 0; j < all_news_weights.size(); j++) {
                    values.put(newPeriods.get(j), all_news_weights.get(j).get(i));
                }

                newsNodes.add(new VariableNode(name, p, exp, obs, values));
            }
        }

        VariableNode allNewsNode = new VariableNode("All News", null, null, null, null);
        allNewsNode.setChildren(newsNodes);
        nodes.add(allNewsNode);

        List<Update> revisions = details.revisions();
        List<VariableNode> revNodes = new ArrayList<>();
        for (int i = 0; i < revisions.size(); i++) {
            TsPeriod p = revisions.get(i).period;
            if (p.firstPeriod(freq).isNotBefore(revsStart)) {
                DfmSeriesDescriptor descriptor = desc[revisions.get(i).series];
                String name = descriptor.description;

                Double exp = revisions.get(i).getForecast() * descriptor.stdev + descriptor.mean;
                Double obs = revisions.get(i).getObservation() * descriptor.stdev + descriptor.mean;

                Map<TsPeriod, Double> values = new HashMap<>();
                for (int j = 0; j < all_revisions_weights.size(); j++) {
                    values.put(newPeriods.get(j), all_revisions_weights.get(j).get(i));
                }

                revNodes.add(new VariableNode(name, p, exp, obs, values));
            }
        }

        if (!revNodes.isEmpty()) {
            VariableNode allRevisionsNode = new VariableNode("All Revisions", null, null, null, null);
            allRevisionsNode.setChildren(revNodes);
            nodes.add(allRevisionsNode);
        }

        nodes.add(new VariableNode("Old Forecasts", null, null, null, old_forecasts2));
        nodes.add(new VariableNode("New Forecasts", null, null, null, new_forecasts2));
    }

    private void createColumnTitles() {
        titles = new ArrayList<>();
        titles.add(new Title("Reference Period", "<html>Reference<br>Period"));
        titles.add(new Title("Expected Value", "<html>Expected<br>Value"));
        titles.add(new Title("Observed Value", "<html>Observed<br>Value"));
        for (TsPeriod p : newPeriods) {
            titles.add(new Title("Weight " + p.toString(), "<html>Weight<br>" + p.toString()));
        }

        outline.setTitles(titles);
    }

    public static class Title {

        private final String title;
        private final String htmlTitle;

        public Title(String title, String htmlTitle) {
            this.title = title;
            this.htmlTitle = htmlTitle;
        }

        public String getTitle() {
            return title;
        }

        public String getHtmlTitle() {
            return htmlTitle;
        }
    }

    private TsCollection toCollection() {
        TsCollection result = TsFactory.instance.createTsCollection();
        int selectedIndex = combobox.getSelectedIndex();
        double stdev = desc[selectedIndex].stdev;
        double mean = desc[selectedIndex].mean;
        TsInformationSet dataOld = doc.getOldInformationSet();
        TsData serieOld = dataOld.series(selectedIndex).times(stdev).plus(mean);
        TsData serieNew = dfmResults.getTheData()[selectedIndex].plus(mean);
        TsDataCollector coll = new TsDataCollector();
        for (int i = 0; i < old_forecasts.size(); i++) {
            coll.addObservation(oldPeriods.get(i).middle(), old_forecasts.get(i));
        }
        TsData oldF = coll.make(serieOld.getFrequency(), TsAggregationType.None);
        coll.clear();
        for (int i = 0; i < new_forecasts.size(); i++) {
            coll.addObservation(newPeriods.get(i).middle(), new_forecasts.get(i));
        }
        TsData newF = coll.make(serieNew.getFrequency(), TsAggregationType.None);
        TsData old_serie = serieOld.update(oldF);
        TsData new_serie = serieNew.update(newF);

        result.quietAdd(TsFactory.instance.createTs("Old Forecasts", null, old_serie));
        result.quietAdd(TsFactory.instance.createTs("New Forecasts", null, new_serie));

        chartForecast.setDashPredicate(new ObsPredicate() {
            @Override
            public boolean apply(int series, int obs) {
                return obs >= chartForecast.getDataset().getItemCount(series) - (series == 0 ? old_forecasts.size() : new_forecasts.size());
            }
        });

        return result;

    }

    //<editor-fold defaultstate="collapsed" desc="JTimeSeriesChart utilities">
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
                Transferable t = TssTransferSupport.getDefault().fromTsCollection(c.collection);
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

    private TsCollection dragSelection = null;

    protected Transferable transferableOnSelection() {
        TsCollection col = TsFactory.instance.createTsCollection();

        ListSelectionModel model = chartForecast.getSeriesSelectionModel();
        if (!model.isSelectionEmpty()) {
            for (int i = model.getMinSelectionIndex(); i <= model.getMaxSelectionIndex(); i++) {
                if (model.isSelectedIndex(i)) {
                    col.quietAdd(collection.get(i));
                }
            }
        }
        dragSelection = col;
        return TssTransferSupport.getDefault().fromTsCollection(dragSelection);

    }

    public class TsCollectionTransferHandler extends TransferHandler {

        @Override
        public int getSourceActions(JComponent c) {
            transferableOnSelection();
            TsDragRenderer r = dragSelection.getCount() < 10 ? TsDragRenderer.asChart() : TsDragRenderer.asCount();
            Image image = r.getTsDragRendererImage(Arrays.asList(dragSelection.toArray()));
            setDragImage(image);
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return transferableOnSelection();
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            return false;
        }
    }

}
