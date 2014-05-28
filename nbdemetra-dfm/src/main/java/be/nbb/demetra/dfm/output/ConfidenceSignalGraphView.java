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
import ec.tss.dfm.DfmResults;
import ec.tss.dfm.DfmSeriesDescriptor;
import ec.tstoolkit.timeseries.simplets.TsData;
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
public class ConfidenceSignalGraphView extends JPanel {
    
    // Properties
    public static final String DFM_RESULTS_PROPERTY = "dfmResults";

    private final JComboBox comboBox;
    private final ConfidenceGraph graph;
    
    private Optional<DfmResults> dfmResults;
    
    public ConfidenceSignalGraphView() {
        super(new BorderLayout());
        this.graph = new ConfidenceGraph();
        this.dfmResults = Optional.absent();
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
        add(graph, BorderLayout.CENTER);
    }
    
    //<editor-fold defaultstate="collapsed" desc="Getters/Setters">
    public Optional<DfmResults> getDfmResults() {
        return dfmResults;
    }

    public void setDfmResults(Optional<DfmResults> dfmResults) {
        Optional<DfmResults> old = this.dfmResults;
        this.dfmResults = dfmResults != null ? dfmResults : Optional.<DfmResults>absent();
        firePropertyChange(DFM_RESULTS_PROPERTY, old, this.dfmResults);
    }
    //</editor-fold>
    
    private void updateComboBox() {
        if (dfmResults.isPresent()) {
            comboBox.setModel(toComboBoxModel(dfmResults.get().getDescriptions()));
            comboBox.setEnabled(true);
        } else {
            comboBox.setModel(new DefaultComboBoxModel());
            comboBox.setEnabled(false);
        }
    }
    
    private void updateChart() {
        if (dfmResults.isPresent() && comboBox.getSelectedIndex() != -1) {
            DfmResults rslts = dfmResults.get();
            int selectedIndex = comboBox.getSelectedIndex();
            TsData signalMean = rslts.getSignalProjections()[selectedIndex];
            TsData smoothedSignalUncertainly = rslts.getSignalUncertainty()[selectedIndex].pow(0.5);
            
            graph.setData(signalMean, smoothedSignalUncertainly);
        } else {
            graph.setData(null, null);
        }
    }
    
    private static DefaultComboBoxModel toComboBoxModel(DfmSeriesDescriptor[] data) {
        DefaultComboBoxModel result = new DefaultComboBoxModel(data);
        return result;
    }
}
