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

import be.nbb.demetra.dfm.output.correlationball.CorrelationBall;
import static be.nbb.demetra.dfm.output.correlationball.CorrelationBall.COLOR_SCALE_PROPERTY;
import be.nbb.demetra.dfm.output.correlationball.CorrelationJList;
import com.google.common.base.Optional;
import ec.nbdemetra.ui.NbComponents;
import ec.tss.dfm.DfmResults;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.maths.matrices.Matrix;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Mats Maggi
 */
public class CorrelationBallView extends JPanel {

    // Properties
    public static final String DFM_RESULTS_PROPERTY = "dfmResults";
    
    private Optional<DfmResults> results;
    private CorrelationBall ball;
    private List<String> titles;
    private final CorrelationJList list;  
    
    public CorrelationBallView(Optional<DfmResults> r) {
        setLayout(new BorderLayout());
        
        this.results = r;
        createBall();
        
        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case DFM_RESULTS_PROPERTY:
                        updateBall();
                        break;
                }
            }
        });
        
        final JSlider slider = new JSlider(0, 360, 0);
        slider.setBackground(Color.WHITE);
        Dictionary<Integer, JComponent> dic = new Hashtable<>();
        for (int i = 0; i <= 360; i = i + 30) {
            dic.put(i, new JLabel(String.valueOf(i) + "°"));
        }
        slider.setLabelTable(dic);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(30);
        slider.setMinorTickSpacing(5);
        slider.setFocusable(false);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                ball.spin(source.getValue());
            }
        });
        
        ball.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case COLOR_SCALE_PROPERTY:
                        updateBall();
                        break;
                    case CorrelationBall.SPIN_ON_SELECTION_PROPERTY :
                        slider.setValue((int)((double)evt.getNewValue()));
                        break;
                }
            }
        });
        
        ball.setComponentPopupMenu(ball.buildGridMenu().getPopupMenu());
        list = new CorrelationJList();
      
        JScrollPane scrollPane = new JScrollPane(list, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(150, 500));
        JSplitPane splitPane = NbComponents.newJSplitPane(JSplitPane.HORIZONTAL_SPLIT, ball, scrollPane);
        splitPane.setResizeWeight(0.9);
        
        enableSync();
        
        add(slider, BorderLayout.SOUTH);
        add(splitPane, BorderLayout.CENTER);
        
        updateBall();
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
                    case CorrelationBall.INDEX_SELECTED_PROPERTY:
                        int index = (int) evt.getNewValue();
                        list.setSelectedItem(index);
                        break;
                    case CorrelationJList.SELECTED_ITEM_PROPERTY:
                        ball.setIndexSelected((int) evt.getNewValue());
                        break;
                }
                updating = false;
            }
        };
        
        ball.addPropertyChangeListener(listener);
        list.addPropertyChangeListener(listener);
    }
    
    private void createBall() {
        ball = new CorrelationBall();
        ball.setBackground(Color.WHITE);
        ball.setForeground(Color.BLACK);
    }
    
    private double[][] filterMatrix() {
        List<Integer> indexes = new ArrayList<>();
        titles = new ArrayList<>();
        DfmResults rslts = results.get();
        List<DynamicFactorModel.MeasurementDescriptor> measurements = rslts.getModel().getMeasurements();
        Matrix data = rslts.getIdiosyncratic();
        for (int i = 0; i < data.getColumnsCount(); i++) {
            if (measurements.get(i).getUsedFactorsCount() > 0) {
                indexes.add(i);
                titles.add(rslts.getDescription(i).description);
            }
        }
        
        double[][] result = new double[indexes.size()][indexes.size()];
        for (int i = 0; i < indexes.size(); i++) {
            for (int j = 0; j < indexes.size(); j++) {
                result[i][j] = data.get(indexes.get(i), indexes.get(j));
            }
        }
        return result;
    }
    
    public Optional<DfmResults> getDfmResults() {
        return results;
    }
    
    public void setDfmResults(Optional<DfmResults> dfmResults) {
        Optional<DfmResults> old = this.results;
        this.results = dfmResults != null ? dfmResults : Optional.<DfmResults>absent();
        firePropertyChange(DFM_RESULTS_PROPERTY, old, this.results);
    }
    
    private void updateBall() {
        ball.removeAll();
        if (results != null && results.isPresent()) {
            double[][] matrix = filterMatrix();
            ball.setMatrix(titles, matrix);
            
            list.setListData(titles.toArray(new String[titles.size()]));
        }
    }
    
    
    
}
