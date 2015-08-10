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
package ec.tss.dfm;

import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.timeseries.simplets.TsData;

/**
 *
 * @author Mats Maggi
 */
public class SimulationResultsDocument {

    private final CompositeResults simulationResults;
    private final DfmResults dfmResults;
    private TsData[] smoothedSeriesStdev;
    
    public SimulationResultsDocument(CompositeResults simulation, DfmResults dfm) {
        this.simulationResults = simulation;
        this.dfmResults = dfm;
    }

    public DfmResults getDfmResults() {
        return dfmResults;
    }

    public CompositeResults getSimulationResults() {
        return simulationResults;
    }

    public TsData[] getSmoothedSeriesStdev() {
        return smoothedSeriesStdev;
    }

    public void setSmoothedSeriesStdev(TsData[] smoothedSeriesStdev) {
        this.smoothedSeriesStdev = smoothedSeriesStdev;
    }    
}
