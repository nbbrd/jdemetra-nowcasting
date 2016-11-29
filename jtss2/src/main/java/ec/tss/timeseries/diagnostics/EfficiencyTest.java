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

import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.timeseries.simplets.TsData;

/**
 *
 * @author Mats Maggi
 */
public class EfficiencyTest extends AccuracyTests {

    private boolean yearly = false;

    public EfficiencyTest(TsData fcts, TsData y, AsymptoticsType asympType) {
        super(fcts, y, asympType);
    }

    public EfficiencyTest(TsData error, AsymptoticsType asympType) {
        super(error, null, asympType);
    }

    public EfficiencyTest(TsData fcts, TsData y, AsymptoticsType asympType, Integer delay, Integer horizon) {
        super(fcts, y, asympType, delay, horizon);
    }

    public boolean isYearly() {
        return yearly;
    }

    public void setYearly(boolean yearly) {
        this.yearly = yearly;
    }

    @Override
    public TsData calcLoss() {
        double e0_bar = (e.drop(1, 0)).average();
        double e1_bar = (e.drop(0, 1)).average();

        TsData e0 = (e.drop(1, 0));
        TsData e1 = (e.drop(0, 1)).lag(-1);

        TsData e0e1 = e0.minus(e0_bar).times(e1.minus(e1_bar));

        return e0e1;
    }

    public double calcCorrelation() {
        double e0_bar = (e.drop(1, 0)).average();
        double e1_bar = (e.drop(0, 1)).average();

        TsData e0 = (e.drop(1, 0));
        TsData e1 = (e.drop(0, 1)).lag(-1);

        TsData e0e1 = e0.minus(e0_bar).times(e1.minus(e1_bar));
        double e0e1Cov = e0e1.average();

        DescriptiveStatistics desc0 = new DescriptiveStatistics(e0);
        DescriptiveStatistics desc1 = new DescriptiveStatistics(e1);
        double std0 = desc0.getStdev();
        double std1 = desc1.getStdev();

        return e0e1Cov / (std0 * std1);
    }

}
