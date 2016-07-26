/*
 * Copyright 2016 National Bank of Belgium
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
package ec.tss.timeseries.diagnostics;

import ec.tstoolkit.timeseries.simplets.TsData;

/**
 *
 * @author Mats Maggi
 */
public class EfficiencyTest extends AccuracyTests {

    public EfficiencyTest(TsData fcts, TsData y, AsymptoticsType asympType) {
        super(fcts, y, asympType);
    }

    public EfficiencyTest(TsData error, AsymptoticsType asympType) {
        super(error, null, asympType);
    }
    
    public EfficiencyTest(TsData fcts, TsData y, AsymptoticsType asympType, Integer delay, Integer horizon) {
        super(fcts, y, asympType, delay, horizon);
    }

    @Override
    public TsData calcLoss() {
        return e.drop(1, 0).times(e.drop(0, 1));
    }
}
