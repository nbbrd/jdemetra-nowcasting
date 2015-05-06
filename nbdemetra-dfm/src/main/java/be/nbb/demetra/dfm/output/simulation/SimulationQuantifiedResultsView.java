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
import com.google.common.base.Optional;
import ec.nbdemetra.ui.DemetraUI;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.DfmSeriesDescriptor;
import ec.tss.dfm.DfmSimulation;
import ec.tss.dfm.DfmSimulationResults;
import ec.tss.dfm.ForecastEvaluationResults;
import ec.tss.dfm.ForecastEvaluationResults.AccuracyTests;
import ec.tss.tsproviders.utils.Formatters;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataCollector;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.util.chart.ColorScheme;
import ec.util.chart.swing.SwingColorSchemeSupport;
import ec.util.various.swing.ModernUI;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.tree.TreeModel;
import be.nbb.demetra.dfm.output.simulation.utils.FilterHorizonsPanel;
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

    private final XOutline outline;
    private final JComboBox comboBox;
    private final DemetraUI demetraUI;
    private Formatters.Formatter<Number> formatter;
    private SwingColorSchemeSupport defaultColorSchemeSupport;

    private DfmDocument document;
    private Optional<DfmSimulation> dfmSimulation;

    private List<CustomNode> nodes;

    private FilterHorizonsPanel filterPanel;

    public SimulationQuantifiedResultsView(DfmDocument doc) {
        setLayout(new BorderLayout());

        this.document = doc;

        filterPanel = new FilterHorizonsPanel();

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

        comboBox = new JComboBox();

        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateOutlineModel();
            }
        });

        JScrollPane p = ModernUI.withEmptyBorders(new JScrollPane());
        p.setViewportView(outline);

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case DFM_SIMULATION_PROPERTY:
                        updateComboBox();
                        updateOutlineModel();
                }
            }
        });

        updateComboBox();
        updateOutlineModel();

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

        add(comboBox, BorderLayout.NORTH);
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

    private void updateOutlineModel() {
        if (document != null && dfmSimulation.isPresent()) {
            calculateData();
            refreshModel();
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

    private void createTitles() {
        titles = new ArrayList<>();
        for (int i = 0; i < horizons.size(); i++) {
            titles.add(new Title(String.valueOf(horizons.get(i))));
        }
        outline.setTitles(titles);
    }

    private void calculateData() {
        nodes = new ArrayList<>();
        int selectedIndex = comboBox.getSelectedIndex();

        DfmSimulationResults dfm = dfmSimulation.get().getDfmResults().get(selectedIndex);
        DfmSimulationResults arima = dfmSimulation.get().getArimaResults().get(selectedIndex);
        periods = dfm.getEvaluationSample();
        horizons = dfm.getForecastHorizons();

        filterPanel.setSelectedElements(horizons);

        createTitles();
        List<Double> trueValues = dfm.getTrueValues();
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

        // Base
        SimulationNode scale = new SimulationNode("Scale dependent", null);

        List<Double> valuesRMSE = new ArrayList<>();
        List<Double> valuesMAE = new ArrayList<>();
        List<Double> valuesMdAE = new ArrayList<>();
        for (Integer horizon : horizons) {
            ForecastEvaluationResults rslt = new ForecastEvaluationResults(dfmTs.get(horizon), arimaTs.get(horizon), trueTsData);
            valuesRMSE.add(rslt.calcRMSE());
            valuesMAE.add(rslt.calcMAE());
            valuesMdAE.add(rslt.calcMdAE());
        }
        scale.addChild(new SimulationNode("RMSE", valuesRMSE));
        scale.addChild(new SimulationNode("MAE", valuesMAE));
        scale.addChild(new SimulationNode("MdAE", valuesMdAE));

        nodes.add(scale);

        SimulationNode percentage = new SimulationNode("Percentage errors", null);

        List<Double> valuesRMSPE = new ArrayList<>();
        List<Double> values_sMAPE = new ArrayList<>();
        List<Double> values_sMdAPE = new ArrayList<>();
        for (Integer horizon : horizons) {
            ForecastEvaluationResults rslt = new ForecastEvaluationResults(dfmTs.get(horizon), arimaTs.get(horizon), trueTsData);
            valuesRMSPE.add(rslt.calcRMSPE());
            values_sMAPE.add(rslt.calc_sMAPE());
            values_sMdAPE.add(rslt.calc_sMdAPE());
        }
        percentage.addChild(new SimulationNode("RMSPE", valuesRMSPE));
        percentage.addChild(new SimulationNode("sMAPE", values_sMAPE));
        percentage.addChild(new SimulationNode("sMdAPE", values_sMdAPE));

        nodes.add(percentage);

        SimulationNode errors = new SimulationNode("Scaled errors", null);

        List<Double> valuesRMSSE = new ArrayList<>();
        List<Double> valuesMASE = new ArrayList<>();
        List<Double> values_MdASE = new ArrayList<>();
        for (Integer horizon : horizons) {
            ForecastEvaluationResults rslt = new ForecastEvaluationResults(dfmTs.get(horizon), arimaTs.get(horizon), trueTsData);
            valuesRMSSE.add(rslt.calcRMSSE());
            valuesMASE.add(rslt.calcMASE());
            values_MdASE.add(rslt.calcMdASE());
        }
        errors.addChild(new SimulationNode("RMSSE", valuesRMSSE));
        errors.addChild(new SimulationNode("MASE", valuesMASE));
        errors.addChild(new SimulationNode("MdASE", values_MdASE));

        nodes.add(errors);

        // Relative
        SimulationNode relative = new SimulationNode("Relative", null);
        scale = new SimulationNode("Scale dependent", null);

        valuesRMSE = new ArrayList<>();
        valuesMAE = new ArrayList<>();
        valuesMdAE = new ArrayList<>();
        for (Integer horizon : horizons) {
            ForecastEvaluationResults rslt = new ForecastEvaluationResults(dfmTs.get(horizon), arimaTs.get(horizon), trueTsData);
            valuesRMSE.add(rslt.calcRelRMSE());
            valuesMAE.add(rslt.calcRelMAE());
            valuesMdAE.add(rslt.calcRelMdAE());
        }
        scale.addChild(new SimulationNode("RMSE", valuesRMSE));
        scale.addChild(new SimulationNode("MAE", valuesMAE));
        scale.addChild(new SimulationNode("MdAE", valuesMdAE));

        relative.addChild(scale);

        percentage = new SimulationNode("Percentage errors", null);

        valuesRMSPE = new ArrayList<>();
        values_sMAPE = new ArrayList<>();
        values_sMdAPE = new ArrayList<>();
        for (Integer horizon : horizons) {
            ForecastEvaluationResults rslt = new ForecastEvaluationResults(dfmTs.get(horizon), arimaTs.get(horizon), trueTsData);
            valuesRMSPE.add(rslt.calcRelRMSPE());
            values_sMAPE.add(rslt.calcRel_sMAPE());
            values_sMdAPE.add(rslt.calcRel_sMdAPE());
        }
        percentage.addChild(new SimulationNode("RMSPE", valuesRMSPE));
        percentage.addChild(new SimulationNode("sMAPE", values_sMAPE));
        percentage.addChild(new SimulationNode("sMdAPE", values_sMdAPE));

        relative.addChild(percentage);

        errors = new SimulationNode("Scaled errors", null);

        valuesRMSSE = new ArrayList<>();
        valuesMASE = new ArrayList<>();
        values_MdASE = new ArrayList<>();
        for (Integer horizon : horizons) {
            ForecastEvaluationResults rslt = new ForecastEvaluationResults(dfmTs.get(horizon), arimaTs.get(horizon), trueTsData);
            valuesRMSSE.add(rslt.calcRelRMSSE());
            valuesMASE.add(rslt.calcRelMASE());
            values_MdASE.add(rslt.calcRelMdASE());
        }
        errors.addChild(new SimulationNode("RMSSE", valuesRMSSE));
        errors.addChild(new SimulationNode("MASE", valuesMASE));
        errors.addChild(new SimulationNode("MdASE", values_MdASE));

        relative.addChild(errors);

        List<Double> pbValues = new ArrayList<>();
        for (Integer horizon : horizons) {
            ForecastEvaluationResults rslt = new ForecastEvaluationResults(dfmTs.get(horizon), arimaTs.get(horizon), trueTsData);
            pbValues.add(rslt.calcPB());
        }
        relative.addChild(new SimulationNode("Percentage better", pbValues));

        nodes.add(relative);

        // Diebold Mariano + Encompassing Test
        SimulationNode dm = new SimulationNode("Diebold Mariano", null);

        List<Double> dmSqValues = new ArrayList<>();
        List<Double> dmAbsValues = new ArrayList<>();
        for (Integer horizon : horizons) {
            ForecastEvaluationResults rslt = new ForecastEvaluationResults(dfmTs.get(horizon), arimaTs.get(horizon), trueTsData);
            AccuracyTests test = rslt.new AccuracyTests();
            dmSqValues.add(test.getDM());
            dmAbsValues.add(test.getDMabs());
        }
        dm.addChild(new SimulationNode(D_M_TEST, dmSqValues));
        dm.addChild(new SimulationNode(D_M_ABS_TEST, dmAbsValues));

        nodes.add(dm);

        List<Double> encValues = new ArrayList<>();
        for (Integer horizon : horizons) {
            ForecastEvaluationResults rslt = new ForecastEvaluationResults(dfmTs.get(horizon), arimaTs.get(horizon), trueTsData);
            AccuracyTests test = rslt.new AccuracyTests();
            encValues.add(test.getDM_e());
        }
        SimulationNode enc = new SimulationNode(ENCOMPASING_TEST, encValues);

        nodes.add(enc);
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
            map.put(horizons.get(i), coll.make(freq, TsAggregationType.None));
        }
    }
}
