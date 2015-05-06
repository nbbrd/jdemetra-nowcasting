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

import be.nbb.demetra.dfm.output.simulation.utils.FilterHorizonsPanel;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 *
 * @author Mats Maggi
 */
public class FilterHorizonsTest {
    public static void main(String[] args) {
        //JOptionPane.showMessageDialog(null, new FilterHorizonsPanel(), "Filter horizons", JOptionPane.PLAIN_MESSAGE);
        List<Integer> ints = new ArrayList<>();
        ints.add(-300);
        ints.add(-155);
        ints.add(-75);
        ints.add(0);
        ints.add(7);
        FilterHorizonsPanel panel = new FilterHorizonsPanel(null, null);
        panel.setSelectedElements(ints);
        
        int response = JOptionPane.showConfirmDialog(null, panel, "Filter horizons", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (response == JOptionPane.OK_OPTION) {
            System.out.println("Ok");
            System.out.println(panel.getSelectedElements());
        }
    }
}
