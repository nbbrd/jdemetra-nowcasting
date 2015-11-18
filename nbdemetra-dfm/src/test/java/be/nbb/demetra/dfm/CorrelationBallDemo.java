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
package be.nbb.demetra.dfm;

import be.nbb.demetra.dfm.output.correlationball.CorrelationBall;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;

/**
 *
 * @author Mats Maggi
 */
public class CorrelationBallDemo {

    public static void main(String[] args) {
        JFrame f = new JFrame("Correlation Ball");
        f.setLayout(new BorderLayout());
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final CorrelationBall correlationBall = new CorrelationBall();
        double[][] matrix = createMatrix();
        List<String> titles = createTitles(matrix.length);
        correlationBall.setMatrix(titles, matrix);
        correlationBall.setBackground(Color.WHITE);
        correlationBall.setForeground(Color.BLACK);
        
        f.add(correlationBall, BorderLayout.CENTER);
        JButton b = new JButton(new AbstractAction("Random data") {

            @Override
            public void actionPerformed(ActionEvent e) {
                double[][] matrix = createMatrix();
                List<String> titles = createTitles(matrix.length);
                correlationBall.setMatrix(titles, matrix);
            }
        });
        
        f.add(b, BorderLayout.SOUTH);

        f.pack();
        f.setVisible(true);
    }
    
    private static double[][] createMatrix() {
        Random r = new Random();
        int size = r.nextInt(50) + 6;
        
        double [][] matrix = new double[size][size];
        
        for (int i = 0; i < matrix.length; i++) {
            for (int j = i; j < matrix[i].length; j++) {
                if (i==j) {
                    matrix[i][j] = 1;
                } else {
                    double nb = (r.nextDouble()+1)*100;
                    double weight = (r.nextDouble()+1)*100;
                    matrix[i][j] = (nb > 97) ? weight : 0;
                }
                
            }
        }
        
        return matrix;
    }
    
    private static List<String> createTitles(int nb) {
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < nb; i++) {
            titles.add("Variable number " + i);
        }
        
        return titles;
    }
}
