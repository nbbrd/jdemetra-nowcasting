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
public class EncompassingTest extends AccuracyTests {

    private boolean benchmarkEncompassesModel = false;
    private final TsData eBench;

    public EncompassingTest(TsData fcts, TsData fctsBench, TsData y, AsymptoticsType asympType) {
        this(TsData.subtract(y, fcts), TsData.subtract(y, fctsBench), asympType);
    }

    public EncompassingTest(TsData error, TsData errorBench, AsymptoticsType asympType) {
        super(error, asympType);
        this.eBench = errorBench;
    }

    public EncompassingTest(TsData fcts, TsData fctsBench, TsData y, AsymptoticsType asympType, Integer delay, Integer horizon) {
        super(fcts, y, asympType, delay, horizon);
        this.eBench = TsData.subtract(y, fctsBench);
    }

    public void setBenchmarkEncompassesModel(boolean benchmarkEncompasses) {
        this.benchmarkEncompassesModel = benchmarkEncompasses;
    }

    @Override
    public TsData calcLoss() {
        if (benchmarkEncompassesModel) {
            return eBench.times(eBench.minus(e));
        } else {
            return e.times(e.minus(eBench));
        }
    }

    // deanton: regression based calculation of beta
    //  weigthed average=(1-beta) f1 + beta fbench 
    //  Forecast 1 encompases Forecast 2
    //  y = ((e1-e2).*e1); 
    //  y = y-nanmean(y);
    //  teststat_e1(ii) = sqrt(n)*mean(y)/sqrt(nw(y,qn)); 
    //  X=[e1-e2];
    //  Y=e1; % center to make sure intercept does not play
    //  beta2=inv(X'*X)*X'*Y; 
    public double calcWeights() {
        if (benchmarkEncompassesModel) { // weigthed average=(beta) f1 + (1-beta) fbench 

            TsData Y = eBench.minus(eBench.average());
            TsData X = eBench.minus(e);
            double invXX = Math.pow((X.times(X)).sum(), -1);
            double XY = (X.times(Y)).sum();
            double beta = invXX * XY;
            return beta;

        } else {  // weigthed average=(1-beta) f1 + beta fbench 

            TsData Y = e.minus(e.average());
            TsData X = e.minus(eBench);
            double invXX = Math.pow((X.times(X)).sum(), -1);
            double XY = (X.times(Y)).sum();
            double beta = invXX * XY;
            return beta;

        }
    }
}
