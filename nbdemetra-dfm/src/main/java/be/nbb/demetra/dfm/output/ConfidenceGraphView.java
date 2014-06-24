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

import com.google.common.base.Optional;
import ec.tss.Ts;
import ec.tss.dfm.DfmResults;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/**
 *
 * @author Mats Maggi
 */
public class ConfidenceGraphView extends JPanel {
    
    // Properties
    public static final String RESULTS_PROPERTY = "results";

    private final JComboBox comboBox;
    private final ConfidenceGraph graph;
    
    private Ts[] series;
    private Ts[] stdevs;
    private DfmResults results;
    
    public ConfidenceGraphView() {
        super(new BorderLayout());
        this.graph = new ConfidenceGraph();
        this.comboBox = new JComboBox();
        
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
                    case RESULTS_PROPERTY:
                        updateComboBox();
                        updateChart();
                        break;
                }
            }
        });
        
        updateComboBox();
        updateChart();

        add(comboBox, BorderLayout.NORTH);
        add(graph, BorderLayout.CENTER);
    }
    
    private void updateComboBox() {
        if (series != null && series.length != 0) {
            comboBox.setModel(toComboBoxModel(series));
            comboBox.setEnabled(true);
        } else {
            comboBox.setModel(new DefaultComboBoxModel());
            comboBox.setEnabled(false);
        }
    }
    
    public void setResults(Ts[] series, Ts[] stdevs, Optional<DfmResults> results) {
        this.series = series;
        this.stdevs = stdevs;
        this.results = results.isPresent() ? results.get() : null;
        firePropertyChange(RESULTS_PROPERTY, null, null);
    }
    
    private void updateChart() {
        if (series != null && stdevs != null && comboBox.getSelectedIndex() != -1) {
            int selectedIndex = comboBox.getSelectedIndex();
            
            TsData serie = series[selectedIndex].getTsData();
            TsData stdev = stdevs[selectedIndex].getTsData();
            if (results != null) {
                TsFrequency freq = results.getTheData()[selectedIndex].getFrequency();
                if (freq.equals(TsFrequency.Quarterly)) {
                    serie = serie.changeFrequency(TsFrequency.Quarterly, TsAggregationType.Last, false);
                    stdev = stdev.changeFrequency(TsFrequency.Quarterly, TsAggregationType.Last, false);
                }
            }
         
            graph.setData(serie, stdev, null);
        } else {
            graph.setData(null, null, null);
        }
    }
    
    private static DefaultComboBoxModel toComboBoxModel(Ts[] desc) {
        String[] ts = new String[desc.length];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = desc[i].getRawName();
        }
        DefaultComboBoxModel result = new DefaultComboBoxModel(ts);
        return result;
    }
}
