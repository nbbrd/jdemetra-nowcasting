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
package be.nbb.demetra.dfm.output.news.outline;

import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.Map;

/**
 *
 * @author Mats Maggi
 */
public class VariableNode extends CustomNode {

    private final TsPeriod refPeriod;
    private Double expected = Double.NaN;
    private Double observed = Double.NaN;
    private final Map<TsPeriod, Double> news;

    public VariableNode(String name, TsPeriod refPeriod, Double expected, Double observed, Map<TsPeriod, Double> news) {
        super(name);
        this.refPeriod = refPeriod;
        this.expected = expected;
        this.observed = observed;
        this.news = news;
    }

    public TsPeriod getRefPeriod() {
        return refPeriod;
    }

    public Double getExpected() {
        return expected;
    }

    public Double getObserved() {
        return observed;
    }

    public Map<TsPeriod, Double> getNews() {
        return news;
    }
}
