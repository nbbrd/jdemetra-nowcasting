/*
 * Copyright 2014 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or â€“ as soon they will be approved 
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
package ec.tss.dfm;

import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.List;

/**
 *
 * @author Mats Maggi
 */
public class DfmSimulationResults {

    private List<Integer> forecastHorizons;
    private List<Double> trueValues;
    private List<Double> trueValuesYoY;
    private List<Double> trueValuesQoQ;
    private Double[][] forecastsArray;
    private Double[][] forecastsArrayYoY;
    private Double[][] forecastsArrayQoQ;
    private List<TsPeriod> evaluationSample;

    public List<Integer> getForecastHorizons() {
        return forecastHorizons;
    }

    public void setForecastHorizons(List<Integer> forecastHorizons) {
        this.forecastHorizons = forecastHorizons;
    }

    public List<Double> getTrueValues() {
        return trueValues;
    }

    public void setTrueValues(List<Double> trueValues) {
        this.trueValues = trueValues;
    }

    public List<Double> getTrueValuesYoY() {
        return trueValuesYoY;
    }

    public void setTrueValuesYoY(List<Double> trueValues) {
        this.trueValuesYoY = trueValues;
    }

    public List<Double> getTrueValuesQoQ() {
        return trueValuesQoQ;
    }

    public void setTrueValuesQoQ(List<Double> trueValues) {
        this.trueValuesQoQ = trueValues;
    }

    public Double[][] getForecastsArray() {
        return forecastsArray;
    }

    public void setForecastsArray(Double[][] forecastsArray) {
        this.forecastsArray = forecastsArray;
    }

    public Double[][] getForecastsArrayYoY() {
        return forecastsArrayYoY;
    }

    public Double[][] getForecastsArrayQoQ() {
        return forecastsArrayQoQ;
    }

    public void setForecastsArrayQoQ(Double[][] forecastsArray) {
        this.forecastsArrayQoQ = forecastsArray;
    }

    public void setForecastsArrayYoY(Double[][] forecastsArrayYoY) {
        this.forecastsArrayYoY = forecastsArrayYoY;
    }

    public void setEvaluationSample(List<TsPeriod> evaluationSample) {
        this.evaluationSample = evaluationSample;
    }

    public List<TsPeriod> getEvaluationSample() {
        return evaluationSample;
    }

//    @Override
//    public InformationSet write(boolean verbose) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public boolean read(InformationSet info) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
}
