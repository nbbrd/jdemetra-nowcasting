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

import be.nbb.demetra.dfm.output.news.outline.CustomNode;
import be.nbb.demetra.dfm.output.news.outline.NewsTreeModel;
import be.nbb.demetra.dfm.output.news.outline.XOutline;
import be.nbb.demetra.dfm.output.news.outline.XOutline.Title;
import be.nbb.demetra.dfm.output.simulation.outline.SimulationNode;
import be.nbb.demetra.dfm.output.simulation.outline.SimulationOutlineCellRenderer;
import be.nbb.demetra.dfm.output.simulation.outline.SimulationRowModel;
import be.nbb.demetra.dfm.output.simulation.utils.FilterEvaluationSamplePanel;
import com.google.common.base.Optional;
import ec.nbdemetra.ui.DemetraUI;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.DfmSeriesDescriptor;
import ec.tss.dfm.DfmSimulation;
import ec.tss.dfm.DfmSimulationResults;
import ec.tss.dfm.ForecastEvaluationResults;
import ec.tss.timeseries.diagnostics.AccuracyTests;
import ec.tss.timeseries.diagnostics.GlobalForecastingEvaluation;
import ec.tss.tsproviders.utils.Formatters;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataCollector;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.util.chart.ColorScheme;
import ec.util.chart.swing.SwingColorSchemeSupport;
import ec.util.various.swing.ModernUI;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.tree.TreeModel;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.OutlineModel;

/**
 *
 * @author Mats Maggi
 */
public class SimulationQuantifiedResultsView extends JPanel {

    public static final String DFM_SIMULATION_PROPERTY = "dfmSimulation";
    public static final String D_M_TEST = "Squared loss";
    public static final String D_M_ABS_TEST = "Absolute loss";
    public static final String ENCOMPASING_TEST = "Encompasing Test";

    // Top bar
    private final JComboBox comboBox;
    private final JPanel comboBoxPanel;
    private final JLabel variableLabel;
    private final JComboBox typeComboBox;
    private final JLabel typeLabel;
    private final JButton filterButton;

    // Center component
    private final XOutline outline;

    private final DemetraUI demetraUI;
    private Formatters.Formatter<Number> formatter;
    private SwingColorSchemeSupport defaultColorSchemeSupport;

    private Optional<DfmSimulation> dfmSimulation;

    private List<CustomNode> nodes;
    private FilterEvaluationSamplePanel filterPanel;

    public SimulationQuantifiedResultsView(DfmDocument doc) {
        setLayout(new BorderLayout());

        // Top panel
        comboBoxPanel = new JPanel();
        comboBoxPanel.setLayout(new BoxLayout(comboBoxPanel, BoxLayout.LINE_AXIS));
        variableLabel = new JLabel("Variable :");
        variableLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 10));
        comboBoxPanel.add(variableLabel);

        comboBox = new JComboBox();
        comboBox.setRenderer(new ComboBoxRenderer());
        comboBoxPanel.add(comboBox);

        typeLabel = new JLabel("Type :");
        typeLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 10));
        comboBoxPanel.add(typeLabel);
        typeComboBox = new JComboBox(new DefaultComboBoxModel(new String[]{"Level", "Year On Year", "Quarter On Quarter"}));
        comboBoxPanel.add(typeComboBox);

        filterButton = new JButton("Filter sample");
        typeLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 0, 0));
        filterButton.addActionListener((ActionEvent evt) -> {
            filterButtonActionPerformed(evt);
        });
        comboBoxPanel.add(filterButton);

        demetraUI = DemetraUI.getDefault();
        formatter = demetraUI.getDataFormat().numberFormatter();
        defaultColorSchemeSupport = new SwingColorSchemeSupport() {
            @Override
            public ColorScheme getColorScheme() {
                return demetraUI.getColorScheme();
            }
        };

        this.dfmSimulation = Optional.absent();

        outline = new XOutline();
        outline.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        typeComboBox.addItemListener((ItemEvent e) -> {
            filterPanel = null;
            updateOutlineModel();
        });

        comboBox.addItemListener((ItemEvent e) -> {
            filterPanel = null;
            updateOutlineModel();
        });

        JScrollPane p = ModernUI.withEmptyBorders(new JScrollPane());
        p.setViewportView(outline);

        addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case DFM_SIMULATION_PROPERTY:
                    updateComboBox();
                    updateOutlineModel();
            }
        });

        updateComboBox();
        updateOutlineModel();

        demetraUI.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case DemetraUI.DATA_FORMAT_PROPERTY:
                    onDataFormatChanged();
                    break;
                case DemetraUI.COLOR_SCHEME_NAME_PROPERTY:
                    onColorSchemeChanged();
                    break;
            }
        });

        add(comboBoxPanel, BorderLayout.NORTH);
        add(p, BorderLayout.CENTER);
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

    private void updateOutlineModel() {
        if (dfmSimulation != null
                && dfmSimulation.isPresent()
                && comboBox.getSelectedIndex() != -1
                && typeComboBox.getSelectedIndex() != -1) {
            calculateData();
            refreshModel();
        }
    }

    private void filterButtonActionPerformed(ActionEvent evt) {
        int r = JOptionPane.showConfirmDialog(outline, filterPanel, "Select evaluation sample", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r == JOptionPane.OK_OPTION) {
            updateOutlineModel();
        }
    }

    private void refreshModel() {
        TreeModel treeMdl = new NewsTreeModel(nodes);
        OutlineModel mdl = DefaultOutlineModel.createOutlineModel(treeMdl, new SimulationRowModel(titles), true);
        outline.setDefaultRenderer(String.class, new SimulationOutlineCellRenderer(formatter));
        outline.setModel(mdl);

        outline.getColumnModel().getColumn(0).setHeaderValue(" ");
        for (int i = 1; i < outline.getColumnCount(); i++) {
            outline.getColumnModel().getColumn(i).setPreferredWidth(60);
        }
        outline.expandAll();
    }

    private List<TsPeriod> periods;
    private List<Integer> horizons;
    private List<Title> titles;
    private List<TsPeriod> filteredPeriods;

    private void createTitles(List<Integer> data) {
        titles = new ArrayList<>();
        data.stream().forEach((i) -> {
            titles.add(new Title(String.valueOf(i)));
        });
        outline.setTitles(titles);
    }

    Map<String, List<Double>> map = new HashMap<>();

    private void calculateData() {
        nodes = new ArrayList<>();
        int selectedIndex = comboBox.getSelectedIndex();
        int type = typeComboBox.getSelectedIndex();

        DfmSimulationResults dfm = dfmSimulation.get().getDfmResults().get(selectedIndex);
        DfmSimulationResults arima = dfmSimulation.get().getArimaResults().get(selectedIndex);
        periods = dfm.getEvaluationSample();
        horizons = dfm.getForecastHorizons();

        List<Double> trueValues = type == 1 ? dfm.getTrueValuesYoY() : type == 2 ? dfm.getTrueValuesQoQ() : dfm.getTrueValues();
        Double[][] dfmFcts = type == 1 ? dfm.getForecastsArrayYoY() : type == 2 ? dfm.getForecastsArrayQoQ() : dfm.getForecastsArray();
        Double[][] arimaFcts = type == 1 ? arima.getForecastsArrayYoY() : type == 2 ? arima.getForecastsArrayQoQ() : arima.getForecastsArray();
        Map<Integer, TsData> dfmTs = new HashMap<>();
        Map<Integer, TsData> arimaTs = new HashMap<>();

        // Remove periods of evaluation sample not in true values domain
        filteredPeriods = filterEvaluationSample(trueValues);

        if (filterPanel == null) {
            filterPanel = new FilterEvaluationSamplePanel(filteredPeriods);
        }

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

        List<Integer> filteredHorizons = new ArrayList<>();
        filteredHorizons.addAll(dfmTs.keySet());
        Collections.sort(filteredHorizons);

        createTitles(filteredHorizons);

        TsPeriod start = filteredPeriods.get(filterPanel.getStart());
        TsPeriod end = filteredPeriods.get(filterPanel.getEnd());
        TsDomain dom = new TsDomain(start, end.minus(start) + 1);

        // Base
        for (Integer horizon : filteredHorizons) {
            TsData fcts = dfmTs.get(horizon) == null ? null : dfmTs.get(horizon).fittoDomain(dom);
            TsData fctsBench = arimaTs.get(horizon) == null ? null : arimaTs.get(horizon).fittoDomain(dom);
            TsData trueData = trueTsData.fittoDomain(dom);
            ForecastEvaluationResults rslt = new ForecastEvaluationResults(fcts, fctsBench, trueData);
            addToMap("RMSE", rslt.calcRMSE());
            addToMap("MAE", rslt.calcMAE());
            addToMap("MDAE", rslt.calcMdAE());
            addToMap("RMSPE", rslt.calcRMSPE());
            addToMap("SMAPE", rslt.calc_sMAPE());
            addToMap("MDAPE", rslt.calc_sMdAPE());
            addToMap("RMSSE", rslt.calcRMSSE());
            addToMap("MASE", rslt.calcMASE());
            addToMap("MDASE", rslt.calcMdASE());
            addToMap("REL_RMSE", rslt.calcRelRMSE());
            addToMap("REL_MAE", rslt.calcRelMAE());
            addToMap("REL_MDAE", rslt.calcRelMdAE());
            addToMap("REL_RMSPE", rslt.calcRelRMSPE());
            addToMap("REL_SMAPE", rslt.calcRel_sMAPE());
            addToMap("REL_SMDAPE", rslt.calcRel_sMdAPE());
            addToMap("REL_RMSSE", rslt.calcRelRMSSE());
            addToMap("REL_MASE", rslt.calcRelMASE());
            addToMap("REL_MDASE", rslt.calcRelMdASE());

            addToMap("PB", rslt.calcPB());

            GlobalForecastingEvaluation test = new GlobalForecastingEvaluation(
                    fcts, fctsBench, trueTsData, ec.tss.timeseries.diagnostics.AccuracyTests.AsymptoticsType.STANDARD);
            test.setDelay(horizon);
            boolean twoSided = true;
            addToMap("DM", test.getDieboldMarianoTest().getPValue(twoSided));
            addToMap("DM_ABS", test.getDieboldMarianoAbsoluteTest().getPValue(twoSided));
            addToMap("MDL_ENC_BENCH", test.getModelEncompassesBenchmarkTest().getPValue(twoSided));
            addToMap("BIAS", test.getBiasTest().getPValue(twoSided));
            addToMap("EFFICIENCY", test.getEfficiencyTest().getPValue(twoSided));

            test.setAsymptoticsType(AccuracyTests.AsymptoticsType.HYBRID);
            addToMap("H_DM", test.getDieboldMarianoTest().getPValue(twoSided));
            addToMap("H_DM_ABS", test.getDieboldMarianoAbsoluteTest().getPValue(twoSided));
            addToMap("H_MDL_ENC_BENCH", test.getModelEncompassesBenchmarkTest().getPValue(twoSided));
            addToMap("H_BIAS", test.getBiasTest().getPValue(twoSided));
            addToMap("H_EFFICIENCY", test.getEfficiencyTest().getPValue(twoSided));
        }

        nodes.add(new SimulationNode("Scale dependent")
                .addChild(new SimulationNode("RMSE", map.get("RMSE")))
                .addChild(new SimulationNode("MAE", map.get("MAE")))
                .addChild(new SimulationNode("MdAE", map.get("MDAE"))));

        nodes.add(new SimulationNode("Percentage errors")
                .addChild(new SimulationNode("RMSPE", map.get("RMSPE")))
                .addChild(new SimulationNode("sMAPE", map.get("SMAPE")))
                .addChild(new SimulationNode("sMdAPE", map.get("SMDAPE"))));

        nodes.add(new SimulationNode("Scaled errors")
                .addChild(new SimulationNode("RMSSE", map.get("RMSSE")))
                .addChild(new SimulationNode("MASE", map.get("MASE")))
                .addChild(new SimulationNode("MdASE", map.get("MDASE"))));

        // Relative
        nodes.add(new SimulationNode("Relative")
                .addChild(new SimulationNode("Scale dependent")
                        .addChild(new SimulationNode("RMSE", map.get("REL_RMSE")))
                        .addChild(new SimulationNode("MAE", map.get("REL_MAE")))
                        .addChild(new SimulationNode("MdAE", map.get("REL_MDAE"))))
                .addChild(new SimulationNode("Percentage error")
                        .addChild(new SimulationNode("RMSPE", map.get("REL_RMSPE")))
                        .addChild(new SimulationNode("sMAPE", map.get("REL_SMAPE")))
                        .addChild(new SimulationNode("sMdAPE", map.get("REL_SMDAPE"))))
                .addChild(new SimulationNode("Scaled errors")
                        .addChild(new SimulationNode("RMSSE", map.get("RMSSE")))
                        .addChild(new SimulationNode("MASE", map.get("MASE")))
                        .addChild(new SimulationNode("MdASE", map.get("MDASE"))))
                .addChild(new SimulationNode("Percentage better", map.get("PB")))
        );

        // Tests
        nodes.add(new SimulationNode("Diebold Mariano")
                .addChild(new SimulationNode("Standard")
                        .addChild(new SimulationNode(D_M_TEST, map.get("DM")))
                        .addChild(new SimulationNode(D_M_ABS_TEST, map.get("DM_ABS"))))
        );

        nodes.add(new SimulationNode(ENCOMPASING_TEST)
                .addChild(new SimulationNode("Standard", map.get("MDL_ENC_BENCH"))));

        nodes.add(new SimulationNode("Bias")
                .addChild(new SimulationNode("Standard", map.get("BIAS"))));

        nodes.add(new SimulationNode("Efficiency Test")
                .addChild(new SimulationNode("Standard", map.get("EFFICIENCY"))));
    }

    private void addToMap(String key, Double value) {
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<>());
        }
        map.get(key).add(value);
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
}
