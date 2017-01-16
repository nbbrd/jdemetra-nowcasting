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

import be.nbb.demetra.dfm.output.news.outline.CustomNode;
import java.util.List;

/**
 *
 * @author Mats Maggi
 */
public class SimulationNode extends CustomNode{

    private final List<Double> values;    
    private final List<Double> pValues;
    
    public SimulationNode(String name) {
        this(name, null);
    }

    public SimulationNode(String name, List<Double> values) {
        this(name, values, null);
    }
    
    public SimulationNode(String name, List<Double> values, List<Double> pValues) {
        super(name);
        this.values = values;
        this.pValues = pValues;
    }

    public List<Double> getValues() {
        return values;
    }

    public List<Double> getPValues() {
        return pValues;
    }    
}
