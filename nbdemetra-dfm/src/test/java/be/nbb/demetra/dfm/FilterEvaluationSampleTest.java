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
package be.nbb.demetra.dfm;

import be.nbb.demetra.dfm.output.simulation.utils.FilterEvaluationSamplePanel;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 *
 * @author Mats Maggi
 */
public class FilterEvaluationSampleTest {
    public static void main(String[] args) {
        //JOptionPane.showMessageDialog(null, new FilterHorizonsPanel(), "Filter horizons", JOptionPane.PLAIN_MESSAGE);
        List<TsPeriod> periods = new ArrayList<>();
        TsPeriod p = new TsPeriod(TsFrequency.Monthly, 1972, 0);
        for (int i = 0; i < 200; i++) {
            periods.add(p.plus(i));
        }
        
        FilterEvaluationSamplePanel filterPanel = new FilterEvaluationSamplePanel(periods);
        
        int response = JOptionPane.showConfirmDialog(null, filterPanel, "Filter evaluation sample", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (response == JOptionPane.OK_OPTION) {
            System.out.println("Ok");
        }
    }
}
