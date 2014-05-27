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
import com.google.common.base.Optional;
import ec.tss.dfm.DfmResults;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.maths.matrices.Matrix;
import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 *
 * @author Mats Maggi
 */
public class CorrelationBallView extends JPanel {

    // Properties
    public static final String DFM_RESULTS_PROPERTY = "dfmResults";
    
    private Optional<DfmResults> results;
    private CorrelationBall ball;
    
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
        
        add(ball, BorderLayout.CENTER);
        
        updateBall();
    }
    
    private void createBall() {
        ball = new CorrelationBall();
        ball.setBackground(Color.WHITE);
        ball.setForeground(Color.BLACK);
    }
   
    private List<String> titles;
    
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
        removeAll();
        if (results != null && results.isPresent()) {
            double[][] matrix = filterMatrix();
            ball.setMatrix(titles, matrix);
            add(ball, BorderLayout.CENTER);
        }
    }
    
}
