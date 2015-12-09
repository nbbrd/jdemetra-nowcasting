/*
 * Copyright 2013 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
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

import be.nbb.demetra.dfm.output.news.outline.CustomNode;
import be.nbb.demetra.dfm.output.news.outline.CustomOutlineCellRenderer;
import be.nbb.demetra.dfm.output.news.outline.NewsRenderer;
import be.nbb.demetra.dfm.output.news.outline.NewsRenderer.Type;
import be.nbb.demetra.dfm.output.news.outline.NewsRowModel;
import be.nbb.demetra.dfm.output.news.outline.NewsTreeModel;
import be.nbb.demetra.dfm.output.news.outline.VariableNode;
import be.nbb.demetra.dfm.output.news.outline.XOutline;
import ec.nbdemetra.ui.DemetraUI;
import ec.nbdemetra.ui.NbComponents;
import ec.tss.Ts;
import ec.tss.TsCollection;
import ec.tss.TsFactory;
import ec.tss.TsMoniker;
import ec.tss.datatransfer.TsDragRenderer;
import ec.tss.datatransfer.TssTransferSupport;
import ec.tss.dfm.DfmResults;
import ec.tss.dfm.DfmSeriesDescriptor;
import ec.tss.tsproviders.utils.Formatters;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.dfm.DfmNews;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.information.TsInformationUpdates;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataCollector;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.ui.chart.TsXYDatasets;
import ec.util.chart.ColorScheme;
import ec.util.chart.ObsFunction;
import ec.util.chart.ObsIndex;
import ec.util.chart.ObsPredicate;
import ec.util.chart.SeriesFunction;
import ec.util.chart.SeriesPredicate;
import ec.util.chart.TimeSeriesChart;
import ec.util.chart.swing.ColorSchemeIcon;
import ec.util.chart.swing.JTimeSeriesChart;
import static ec.util.chart.swing.JTimeSeriesChartCommand.applyColorSchemeSupport;
import static ec.util.chart.swing.JTimeSeriesChartCommand.applyLineThickness;
import static ec.util.chart.swing.JTimeSeriesChartCommand.copyImage;
import static ec.util.chart.swing.JTimeSeriesChartCommand.printImage;
import static ec.util.chart.swing.JTimeSeriesChartCommand.saveImage;
import ec.util.chart.swing.SwingColorSchemeSupport;
import ec.util.grid.CellIndex;
import ec.util.various.swing.JCommand;
import ec.util.various.swing.ModernUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.tree.TreeModel;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.OutlineModel;

/**
 * View displaying News impacts values in a table and a chart
 *
 * @author Mats Maggi
 */
public class NewsImpactsView extends JPanel {

    public static final String RESULTS_PROPERTY = "results";
    public static final String ALL_REVISIONS = "All Revisions";
    public static final String ALL_NEWS = "All News";

    private DfmNews doc;
    private DfmSeriesDescriptor[] desc;
    private DfmResults dfmResults;

    private final XOutline outline;
    private final JComboBox combobox;
    private final JSplitPane splitPane;
    private final JTimeSeriesChart chartImpacts;
    private TsCollection collection;

    private final DemetraUI demetraUI;
    private Formatters.Formatter<Number> formatter;
    private CustomSwingColorSchemeSupport defaultColorSchemeSupport;
    private NewsImpactsDataExtractor extractor;

    private Map<String, Integer> indexOfSeries;

    public NewsImpactsView() {
        setLayout(new BorderLayout());

        demetraUI = DemetraUI.getDefault();
        formatter = demetraUI.getDataFormat().numberFormatter();
        defaultColorSchemeSupport = new CustomSwingColorSchemeSupport() {
            @Override
            public ColorScheme getColorScheme() {
                return demetraUI.getColorScheme();
            }
        };

        chartImpacts = createChart();
        outline = new XOutline();
        outline.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        outline.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        outline.setCellSelectionEnabled(true);
        outline.setColumnHidingAllowed(false);
        outline.getTableHeader().setReorderingAllowed(false);

        combobox = new JComboBox();

        combobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateOutlineModel();
                updateChart();
            }
        });

        chartImpacts.setSeriesRenderer(new SeriesFunction<TimeSeriesChart.RendererType>() {
            @Override
            public TimeSeriesChart.RendererType apply(int series) {
                switch (getType(series)) {
                    case REVISION:
                        return TimeSeriesChart.RendererType.LINE;
                    case NEWS:
                        return TimeSeriesChart.RendererType.LINE;
                    default:
                        return TimeSeriesChart.RendererType.STACKED_COLUMN;
                }
            }
        });

        chartImpacts.setComponentPopupMenu(createChartMenu().getPopupMenu());
        chartImpacts.setMouseWheelEnabled(true);

        JScrollPane p = ModernUI.withEmptyBorders(new JScrollPane());
        p.setViewportView(outline);

        splitPane = NbComponents.newJSplitPane(JSplitPane.VERTICAL_SPLIT, p, chartImpacts);
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

        outline.enableCellSelection();
        outline.enableCellHovering();

        chartImpacts.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case JTimeSeriesChart.COLOR_SCHEME_SUPPORT_PROPERTY:
                        changeOutlineColorScheme();
                        break;
                }

            }
        });

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

        enableSync();

        add(combobox, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void enableSync() {
        PropertyChangeListener listener = new PropertyChangeListener() {
            boolean updating = false;

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (updating) {
                    return;
                }
                updating = true;
                switch (evt.getPropertyName()) {
                    case XOutline.HOVERED_CELL_PROPERTY:
                        chartImpacts.setHoveredObs(toObsIndex(outline.getHoveredCell()));
                    case JTimeSeriesChart.HOVERED_OBS_PROPERTY:
                        CellIndex index = toCellIndex(chartImpacts.getHoveredObs());
                        outline.setHoveredCell(index);
                        scrollToCell(index);

                        break;
                    case XOutline.SELECTED_CELL_PROPERTY:
                        chartImpacts.setSelectedObs(toObsIndex(outline.getSelectedCell()));
                        break;
                    case JTimeSeriesChart.SELECTED_OBS_PROPERTY:
                        outline.setSelectedCell(toCellIndex(chartImpacts.getSelectedObs()));
                        break;
                }
                updating = false;
            }

            private ObsIndex toObsIndex(CellIndex index) {
                if (index.equals(CellIndex.NULL)) {
                    return ObsIndex.NULL;
                } else {
                    int row = index.getRow();
                    int col = index.getColumn();

                    VariableNode n = (VariableNode) outline.getOutlineModel().getValueAt(row, 0);
                    if (n != null && n.getFullName() != null) {
                        int idx = indexOfSeries.get(n.getFullName());
                        return ObsIndex.valueOf(idx, col - 4);
                    } else {
                        return ObsIndex.NULL;
                    }
                }
            }

            private CellIndex toCellIndex(ObsIndex index) {
                outline.expandAll();
                return CellIndex.valueOf(index.getSeries(),
                        index.equals(ObsIndex.NULL) ? index.getObs() : index.getObs() + 4);
            }

            private void scrollToCell(CellIndex index) {
                if (index.equals(CellIndex.NULL)) {
                    return;
                }
                JViewport viewport = (JViewport) outline.getParent();
                Rectangle rect = outline.getCellRect(index.getRow(), index.getColumn(), true);
                Point pt = viewport.getViewPosition();
                rect.setLocation(rect.x - pt.x, rect.y - pt.y);
                boolean visible = new Rectangle(viewport.getExtentSize()).contains(rect);

                if (!visible) {
                    outline.scrollRectToVisible(
                            outline.getCellRect(index.getRow(), index.getColumn(), true));
                }
            }
        };

        outline.addPropertyChangeListener(listener);
        chartImpacts.addPropertyChangeListener(listener);
    }

    public void setResults(DfmResults results, DfmNews doc) {
        this.dfmResults = results;
        this.doc = doc;
        this.desc = dfmResults.getDescriptions();
        firePropertyChange(RESULTS_PROPERTY, null, results);
    }

    private JTimeSeriesChart createChart() {
        JTimeSeriesChart chart = new JTimeSeriesChart();
        chart.setValueFormat(new DecimalFormat("#.###"));
        chart.setSeriesFormatter(new SeriesFunction<String>() {
            @Override
            public String apply(int series) {
                TsMoniker key = (TsMoniker) chartImpacts.getDataset().getSeriesKey(series);
                return collection.search(key).getName();
            }
        });

        chart.setObsFormatter(new ObsFunction<String>() {
            @Override
            public String apply(int series, int obs) {
                TsMoniker key = (TsMoniker) chartImpacts.getDataset().getSeriesKey(series);
                Ts ts = collection.search(key);

                return chartImpacts.getSeriesFormatter().apply(series)
                        + "\nImpact for : " + ts.getTsData().getDomain().get(obs).toString()
                        + "\nContribution : " + formatter.format(chartImpacts.getDataset().getY(series, obs));
            }
        });

        chart.setObsHighlighter(new ObsPredicate() {
            @Override
            public boolean apply(int series, int obs) {
                return chartImpacts.getHoveredObs().equals(series, obs);
            }
        });

        chart.setColorSchemeSupport(defaultColorSchemeSupport);
        chart.setNoDataMessage("No data produced");

        chart.setLegendVisibilityPredicate(SeriesPredicate.alwaysFalse());

        chart.setTransferHandler(new TsCollectionTransferHandler());
        return chart;
    }

    private void onColorSchemeChanged() {
        defaultColorSchemeSupport = new CustomSwingColorSchemeSupport() {
            @Override
            public ColorScheme getColorScheme() {
                return demetraUI.getColorScheme();
            }
        };
        chartImpacts.setColorSchemeSupport(defaultColorSchemeSupport);
        changeOutlineColorScheme();
    }

    private void changeOutlineColorScheme() {
        if (outline != null) {
            CustomSwingColorSchemeSupport newColorScheme = (CustomSwingColorSchemeSupport) chartImpacts.getColorSchemeSupport();
            outline.setDefaultRenderer(String.class, new CustomOutlineCellRenderer(newColorScheme, Type.IMPACTS, outline));
            outline.setRenderDataProvider(new NewsRenderer(newColorScheme, Type.IMPACTS));
            outline.setDefaultRenderer(TsPeriod.class, new TsPeriodTableCellRenderer(newColorScheme.getColorScheme()));
            outline.repaint();
        }

    }

    private void onDataFormatChanged() {
        formatter = demetraUI.getDataFormat().numberFormatter();
        try {
            chartImpacts.setPeriodFormat(demetraUI.getDataFormat().newDateFormat());
        } catch (IllegalArgumentException ex) {
            // do nothing?
        }
        try {
            chartImpacts.setValueFormat(demetraUI.getDataFormat().newNumberFormat());

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

    //<editor-fold defaultstate="collapsed" desc="Menus">
    private JMenu newColorSchemeMenu() {
        JMenu item = new JMenu("Color scheme");
        item.add(new JCheckBoxMenuItem(applyColorSchemeSupport(defaultColorSchemeSupport).toAction(chartImpacts))).setText("Default");
        item.addSeparator();
        for (final ColorScheme o : DemetraUI.getInstance().getColorSchemes()) {
            final CustomSwingColorSchemeSupport colorSchemeSupport = new CustomSwingColorSchemeSupport() {
                @Override
                public ColorScheme getColorScheme() {
                    return o;
                }
            };
            JMenuItem subItem = item.add(new JCheckBoxMenuItem(applyColorSchemeSupport(colorSchemeSupport).toAction(chartImpacts)));
            subItem.setText(o.getDisplayName());
            subItem.setIcon(new ColorSchemeIcon(o));
        }
        return item;
    }

    private JMenu newLineThicknessMenu() {
        JMenu item = new JMenu("Line thickness");
        item.add(new JCheckBoxMenuItem(applyLineThickness(1f).toAction(chartImpacts))).setText("Thin");
        item.add(new JCheckBoxMenuItem(applyLineThickness(2f).toAction(chartImpacts))).setText("Thick");
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
        menu.add(printImage().toAction(chartImpacts)).setText("Printer...");
        menu.add(copyImage().toAction(chartImpacts)).setText("Clipboard");
        menu.add(saveImage().toAction(chartImpacts)).setText("File...");
        return menu;
    }

    //</editor-fold>
    
    private void refreshModel() {
        List<CustomNode> nodes = extractor.getNodes();
        TreeModel treeMdl = new NewsTreeModel(nodes);
        OutlineModel mdl = DefaultOutlineModel.createOutlineModel(treeMdl, new NewsRowModel(extractor.getTitles(), extractor.getNewPeriods(), formatter), true);
        outline.setRenderDataProvider(new NewsRenderer(defaultColorSchemeSupport, Type.IMPACTS));
        outline.setDefaultRenderer(String.class, new CustomOutlineCellRenderer(defaultColorSchemeSupport, Type.IMPACTS, outline));
        outline.setDefaultRenderer(TsPeriod.class, new TsPeriodTableCellRenderer(chartImpacts.getColorSchemeSupport().getColorScheme()));
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
        ((JLabel) outline.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void updateOutlineModel() {
        if (doc != null && combobox.getSelectedIndex() > -1) {
            extractor = new NewsImpactsDataExtractor(doc, dfmResults);
            extractor.calculateData(combobox.getSelectedIndex());
            indexOfSeries = extractor.getIndexOfSeries();
            outline.setTitles(extractor.getTitles());
            refreshModel();
            outline.expandAll();
        }
    }

    private void updateComboBox() {
        if (desc != null && desc.length != 0) {
            combobox.setModel(toComboBoxModel(desc));
            combobox.setEnabled(true);
        } else {
            combobox.setModel(new DefaultComboBoxModel());
            combobox.setEnabled(false);
        }
    }

    private static DefaultComboBoxModel toComboBoxModel(DfmSeriesDescriptor[] data) {
        DefaultComboBoxModel result = new DefaultComboBoxModel(data);
        return result;
    }

    private void updateChart() {
        if (doc != null) {
            TsInformationUpdates details = doc.newsDetails();
            List<TsInformationUpdates.Update> updates = details.news();
            if (updates.isEmpty()) {
                chartImpacts.setDataset(null);
            } else {
                collection = toCollection();
                chartImpacts.setDataset(TsXYDatasets.from(collection));
            }
        } else {
            chartImpacts.setDataset(null);
        }
    }

    private TsCollection toCollection() {
        TsCollection result = TsFactory.instance.createTsCollection();
        int selectedIndex = combobox.getSelectedIndex();
        TsFrequency freq = dfmResults.getTheData()[selectedIndex].getFrequency();

        List<TsPeriod> newPeriods = extractor.getNewPeriods();
        // News
        List<Double> all_news = extractor.getAllNews();
        TsDataCollector coll = new TsDataCollector();
        for (int i = 0; i < all_news.size(); i++) {
            coll.addObservation(newPeriods.get(i).middle(), all_news.get(i));
        }
        TsData news = coll.make(freq, TsAggregationType.None);
        result.quietAdd(TsFactory.instance.createTs(ALL_NEWS, null, news));

        TsInformationUpdates details = doc.newsDetails();
        List<TsInformationUpdates.Update> updates = details.news();
        List<DataBlock> news_impacts = extractor.getNewsImpacts();
        for (int i = 0; i < updates.size(); i++) {
            coll.clear();
            DfmSeriesDescriptor description = desc[updates.get(i).series];
            for (int j = 0; j < newPeriods.size(); j++) {
                coll.addObservation(newPeriods.get(j).middle(), news_impacts.get(j).get(i));
            }
            TsData data = coll.make(freq, TsAggregationType.None);
            result.quietAdd(TsFactory.instance.createTs(description.description
                    + " (N) [" + updates.get(i).period.toString() + "]", null, data));
        }

        // Revisions
        coll.clear();
        List<Double> all_revisions = extractor.getAllRevisions();
        for (int i = 0; i < all_revisions.size(); i++) {
            coll.addObservation(newPeriods.get(i).middle(), all_revisions.get(i));
        }
        TsData revisions = coll.make(freq, TsAggregationType.None);
        result.quietAdd(TsFactory.instance.createTs(ALL_REVISIONS, null, revisions));

        updates = details.revisions();
        List<DataBlock> revisions_impacts = extractor.getRevisionsImpacts();
        for (int i = 0; i < updates.size(); i++) {
            coll.clear();
            DfmSeriesDescriptor description = desc[updates.get(i).series];
            for (int j = 0; j < newPeriods.size(); j++) {
                coll.addObservation(newPeriods.get(j).middle(), revisions_impacts.get(j).get(i));
            }
            TsData data = coll.make(freq, TsAggregationType.None);
            result.quietAdd(TsFactory.instance.createTs(description.description
                    + " (R) [" + updates.get(i).period.toString() + "]", null, data));
        }

        return result;
    }

    private abstract class CustomSwingColorSchemeSupport extends SwingColorSchemeSupport {

        @Override
        public Color getLineColor(int series) {
            switch (getType(series)) {
                case NEWS:
                    return getLineColor(ColorScheme.KnownColor.RED);
                case REVISION:
                    return getLineColor(ColorScheme.KnownColor.BLUE);
                default:
                    TsMoniker moniker = (TsMoniker) chartImpacts.getDataset().getSeriesKey(series);
                    return super.getLineColor(collection.indexOf(collection.search(moniker)));
            }
        }
    }

    private SeriesType getType(int series) {
        TsMoniker key = (TsMoniker) chartImpacts.getDataset().getSeriesKey(series);
        String name = collection.search(key).getName();
        switch (name) {
            case ALL_NEWS:
                return SeriesType.NEWS;
            case ALL_REVISIONS:
                return SeriesType.REVISION;
            default:
                return SeriesType.IMPACTS;
        }
    }

    private enum SeriesType {

        NEWS, REVISION, IMPACTS
    }

    private static final class CopyCommand extends JCommand<NewsImpactsView> {

        public static final CopyCommand INSTANCE = new CopyCommand();

        @Override
        public void execute(NewsImpactsView c) throws Exception {
            if (c.collection != null) {
                Transferable t = TssTransferSupport.getDefault().fromTsCollection(c.collection);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        }

        @Override
        public boolean isEnabled(NewsImpactsView c) {
            return c.collection != null;
        }

        @Override
        public JCommand.ActionAdapter toAction(NewsImpactsView c) {
            return super.toAction(c).withWeakPropertyChangeListener(c, RESULTS_PROPERTY);
        }
    }

    private class TsPeriodTableCellRenderer extends DefaultTableCellRenderer {

        private final ColorIcon icon;
        private final List<Integer> colors;

        public TsPeriodTableCellRenderer(ColorScheme colorScheme) {
            setHorizontalAlignment(SwingConstants.LEADING);
            icon = new ColorIcon();
            colors = colorScheme.getLineColors();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setIcon(null);
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof TsPeriod) {
                setText(((TsPeriod) value).toString());

                icon.setColor(new Color(colors.get(row % colors.size())));
                setIcon(icon);
            }

            return this;
        }
    }

    private TsCollection dragSelection = null;

    protected Transferable transferableOnSelection() {
        TsCollection col = TsFactory.instance.createTsCollection();

        ListSelectionModel model = chartImpacts.getSeriesSelectionModel();
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
        public boolean canImport(TransferSupport support) {
            return false;
        }
    }
}
