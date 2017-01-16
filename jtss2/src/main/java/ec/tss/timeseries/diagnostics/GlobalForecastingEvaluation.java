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
public class GlobalForecastingEvaluation {

    private DieboldMarianoTest dmTest, dmAbsTest;
    private EncompassingTest modelEncompassesBenchmarkTest, benchmarkEncompassesModelTest;
    private BiasTest biasTest, biasBenchmarkTest;
    private EfficiencyTest efficiencyTest, efficiencyBenchmarkTest, efficiencyYearlyTest, efficiencyYearlyBenchmarkTest;

    private final TsData fcts, fctsBench, y;
    private AccuracyTests.AsymptoticsType asymptoticsType;

    private Integer delay = null;
    private Integer fctHorizon = null;

    public GlobalForecastingEvaluation(TsData fcts, TsData fctsBench, TsData y, AccuracyTests.AsymptoticsType asympType) {
        this.fcts = fcts;
        this.fctsBench = fctsBench;
        this.y = y;
        this.asymptoticsType = asympType;
    }

    public void clearTests() {
        dmTest = null;
        dmAbsTest = null;
        modelEncompassesBenchmarkTest = null;
        benchmarkEncompassesModelTest = null;
        biasTest = null;
        biasBenchmarkTest = null;
        efficiencyTest = null;
        efficiencyBenchmarkTest = null;
        efficiencyYearlyTest = null;
        efficiencyYearlyBenchmarkTest = null;
    }

    public void setAsymptoticsType(AccuracyTests.AsymptoticsType asymptoticsType) {
        if (this.asymptoticsType != asymptoticsType) {
            clearTests();
            this.asymptoticsType = asymptoticsType;
        }
    }

    public void setForecastHorizon(Integer fctHorizon) {
        if (!this.fctHorizon.equals(fctHorizon)) {
            clearTests();
            this.fctHorizon = fctHorizon;
        }
    }

    public void setDelay(Integer delay) {
        if (this.delay == null || !this.delay.equals(delay)) {
            clearTests();
            this.delay = delay;
        }
    }

    public DieboldMarianoTest getDieboldMarianoTest() {
        if (dmTest == null) {
            dmTest = new DieboldMarianoTest(fcts, fctsBench, y, asymptoticsType, delay, fctHorizon);
        }
        return dmTest;
    }

    public DieboldMarianoTest getDieboldMarianoAbsoluteTest() {
        if (dmAbsTest == null) {
            dmAbsTest = new DieboldMarianoTest(fcts, fctsBench, y, asymptoticsType, delay, fctHorizon);
            dmAbsTest.setAbsolute(true);
        }
        return dmAbsTest;
    }

    public BiasTest getBiasTest() {
        if (biasTest == null) {
            biasTest = new BiasTest(fcts, y, asymptoticsType, delay, fctHorizon);
        }
        return biasTest;
    }

    public BiasTest getBiasBenchmarkTest() {
        if (biasBenchmarkTest == null) {
            biasBenchmarkTest = new BiasTest(fctsBench, y, asymptoticsType, delay, fctHorizon);
        }
        return biasBenchmarkTest;
    }

    public EfficiencyTest getEfficiencyTest() {
        if (efficiencyTest == null) {
            efficiencyTest = new EfficiencyTest(fcts, y, asymptoticsType, delay, fctHorizon);
        }
        return efficiencyTest;
    }
    
    public EfficiencyTest getEfficiencyYearlyTest() {
        if (efficiencyYearlyTest == null) {
            efficiencyYearlyTest = new EfficiencyTest(fcts, y, asymptoticsType, delay, fctHorizon);
            efficiencyYearlyTest.setYearly(true);
        }
        return efficiencyYearlyTest;
    }

    public EfficiencyTest getEfficiencyBenchmarkTest() {
        if (efficiencyBenchmarkTest == null) {
            efficiencyBenchmarkTest = new EfficiencyTest(fctsBench, y, asymptoticsType, delay, fctHorizon);
        }
        return efficiencyBenchmarkTest;
    }

    public EfficiencyTest getEfficiencyYearlyBenchmarkTest() {
        if (efficiencyYearlyBenchmarkTest == null) {
            efficiencyYearlyBenchmarkTest = new EfficiencyTest(fctsBench, y, asymptoticsType, delay, fctHorizon);
            efficiencyYearlyBenchmarkTest.setYearly(true);
        }
        return efficiencyYearlyBenchmarkTest;
    }
    
    public EncompassingTest getModelEncompassesBenchmarkTest() {
        if (modelEncompassesBenchmarkTest == null) {
            modelEncompassesBenchmarkTest = new EncompassingTest(fcts, fctsBench, y, asymptoticsType, delay, fctHorizon);
            modelEncompassesBenchmarkTest.setBenchmarkEncompassesModel(false);
        }
        return modelEncompassesBenchmarkTest;
    }

    public EncompassingTest getBenchmarkEncompassesModelTest() {
        if (benchmarkEncompassesModelTest == null) {
            benchmarkEncompassesModelTest = new EncompassingTest(fcts, fctsBench, y, asymptoticsType, delay, fctHorizon);
            benchmarkEncompassesModelTest.setBenchmarkEncompassesModel(true);
        }
        return benchmarkEncompassesModelTest;
    }

}
