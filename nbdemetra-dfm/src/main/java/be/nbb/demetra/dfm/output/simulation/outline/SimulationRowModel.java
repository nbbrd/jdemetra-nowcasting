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
package be.nbb.demetra.dfm.output.simulation.outline;

import be.nbb.demetra.dfm.output.news.outline.XOutline.Title;
import java.util.List;
import org.netbeans.swing.outline.RowModel;

/**
 *
 * @author Mats Maggi
 */
public class SimulationRowModel implements RowModel {

    private final List<Title> cNames;

    public SimulationRowModel(List<Title> columns) {
        this.cNames = columns;
    }

    @Override
    public Object getValueFor(Object o, int i) {
        if (o instanceof SimulationNode) {
            SimulationNode n = (SimulationNode) o;
            if (n.getValues() != null) {
                return n.getValues().get(i);
            }
        }
        return null;
    }

    @Override
    public int getColumnCount() {
        return cNames.size();
    }
    
    @Override
    public Class getColumnClass(int i) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(Object o, int i) {
        return false;
    }

    @Override
    public void setValueFor(Object o, int i, Object o1) {

    }

    @Override
    public String getColumnName(int i) {
        return cNames.get(i).getTitle();
    }
}
