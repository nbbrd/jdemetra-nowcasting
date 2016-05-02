/*
 * Copyright 2013-2014 National Bank of Belgium
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

import com.google.common.base.Optional;
import ec.tss.dfm.DfmResults;
import ec.tstoolkit.timeseries.simplets.TsData;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.util.Date;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/**
 *
 * @author Mats Maggi
 */
public class FactorView extends JPanel {

    // Properties
    public static final String DFM_RESULTS_PROPERTY = "dfmResults";

    // Views
    private FactorChart factorChart;
    private final JComboBox comboBox;

    // Results
    private Optional<DfmResults> dfmResults;

    public FactorView() {
        setLayout(new BorderLayout());
        dfmResults = Optional.absent();
        factorChart = new FactorChart();

        comboBox = new JComboBox();

        comboBox.addItemListener((ItemEvent e) -> {
            updateChart();
        });

        addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case DFM_RESULTS_PROPERTY:
                    updateComboBox();
                    updateChart();
                    break;
            }
        });

        updateComboBox();
        updateChart();

        add(comboBox, BorderLayout.NORTH);
        add(factorChart, BorderLayout.CENTER);
    }

    public Optional<DfmResults> getDfmResults() {
        return dfmResults;
    }

    public void setDfmResults(Optional<DfmResults> dfmResults) {
        Optional<DfmResults> old = this.dfmResults;
        this.dfmResults = dfmResults != null ? dfmResults : Optional.<DfmResults>absent();
        firePropertyChange(DFM_RESULTS_PROPERTY, old, this.dfmResults);
    }

    private void updateComboBox() {
        if (dfmResults.isPresent()) {
            comboBox.setModel(createComboBoxModel());
            comboBox.setEnabled(true);
        } else {
            comboBox.setModel(new DefaultComboBoxModel());
            comboBox.setEnabled(false);
        }
    }

    private void updateChart() {
        if (dfmResults.isPresent() && comboBox.getSelectedIndex() != -1) {
            TsData o = null, f = null, l = null, u = null;
            int index = comboBox.getSelectedIndex();
            DfmResults rslts = dfmResults.get();
            if (rslts != null) {
                o = rslts.getFactor(index);
                f = rslts.getFactor_Filtered(index);
                TsData e = rslts.getFactorStdev(index);
                if (e != null) {
                    e = e.times(2);
                    u = TsData.add(o, e);
                    l = TsData.subtract(o, e);
                }
            }

            Information info = new Information(o, f, l, u);

            factorChart.setData(info.original, info.filtered, info.lfcasts, info.ufcasts);
        } else {
            factorChart = new FactorChart();
        }
    }

    private DefaultComboBoxModel createComboBoxModel() {
        int nbFactors = dfmResults.get().getModel().getFactorsCount();
        String[] factors = new String[nbFactors];
        for (int i = 1; i <= nbFactors; i++) {
            factors[i - 1] = "F" + i;
        }

        return new DefaultComboBoxModel(factors);
    }

    public static class Information {

        public Information(TsData o, TsData filtered, TsData l, TsData u) {
            original = o;
            this.filtered = filtered;
            lfcasts = l;
            ufcasts = u;
        }

        final TsData original, filtered, lfcasts, ufcasts;

        public Date[] markers;
    }

}
