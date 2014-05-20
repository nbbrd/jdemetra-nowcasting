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
import ec.ui.view.MarginView;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
    private MarginView marginView;
    private final JComboBox comboBox;

    // Results
    private Optional<DfmResults> dfmResults;

    public FactorView() {
        setLayout(new BorderLayout());
        this.dfmResults = Optional.absent();
        this.marginView = new MarginView();
        comboBox = new JComboBox();

        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateChart();
            }
        });

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case DFM_RESULTS_PROPERTY:
                        updateComboBox();
                        updateChart();
                        break;
                }
            }
        });

        updateComboBox();
        updateChart();

        add(comboBox, BorderLayout.NORTH);
        add(marginView, BorderLayout.CENTER);
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
            TsData o = null, l = null, u = null;
            int index = comboBox.getSelectedIndex();
            DfmResults rslts = dfmResults.get();
            if (rslts != null) {
                o = rslts.getFactor(index);
                TsData e = rslts.getFactorStdev(index);
                if (e != null) {
                    e = e.times(2);
                    u = TsData.add(o, e);
                    l = TsData.subtract(o, e);
                }
            }

            Information info = new Information(o, null, l, u);

            marginView.setData(info.original.update(info.fcasts), info.lfcasts, info.ufcasts, info.markers);
        } else {
            marginView = new MarginView();
        }
    }

    private DefaultComboBoxModel createComboBoxModel() {
        int nbFactors = dfmResults.get().getModel().getFactorsCount();
        String[] factors = new String[nbFactors];
        for (int i = 1; i <= nbFactors; i++) {
            factors[i-1] = "F" + i;
        }

        return new DefaultComboBoxModel(factors);
    }

    public static class Information {

        public Information(TsData o, TsData f, TsData l, TsData u) {
            original = o;
            fcasts = f;
            lfcasts = l;
            ufcasts = u;
        }

        public Information(TsData o, TsData f, TsData ef, double c) {
            original = o;
            fcasts = f;
            TsData e = ef.times(c);
            lfcasts = TsData.subtract(f, e);
            ufcasts = TsData.add(f, e);
        }

        final TsData original, fcasts, lfcasts, ufcasts;

        public Date[] markers;
    }
}
