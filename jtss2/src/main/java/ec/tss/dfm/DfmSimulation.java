/*
 * Copyright 2013-2014 National Bank of Belgium
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

import ec.tss.Ts;
import ec.tss.TsFactory;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.dfm.MeasurementSpec;
import ec.tstoolkit.timeseries.Day;
import ec.tstoolkit.timeseries.information.TsInformationSet;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jean Palate
 */
public class DfmSimulation {

    private final Day horizon_;
    private Map<Day, DfmDocument> rslts_ = new HashMap<>(); // Results of the simulation process
    private final List<DfmSimulationResults> arimaResults;    // built results for arima
    private final List<DfmSimulationResults> dfmResults;  // built results for dfm

    public DfmSimulation(Day horizon) {
        horizon_ = horizon;
        arimaResults = new ArrayList<>();
        dfmResults = new ArrayList<>();
    }

    public Map<Day, DfmDocument> getResults() {
        return rslts_;
    }

    public List<DfmSimulationResults> getArimaResults() {
        return arimaResults;
    }

    public List<DfmSimulationResults> getDfmResults() {
        return dfmResults;
    }

    /**
     * Processes the simulation of the the given document
     *
     * @param refdoc Reference document containing inputs, specs, etc...
     * @param ed All generated publication days
     * @param estimationDays Days where a re-estimation is requested
     * @return True if the process has finished
     */
    public boolean process(DfmDocument refdoc, Day[] ed, List<Day> estimationDays) {
        rslts_ = new HashMap<>();
        DfmSpec spec = refdoc.getSpecification();
        Ts[] input = refdoc.getInput();

        TsInformationSet info = new TsInformationSet(refdoc.getData());

        for (int i = 0; i < ed.length; ++i) {
            DfmDocument doc = new DfmDocument();
            // current information
            TsInformationSet cinfo = info.generateInformation(spec.getModelSpec().getPublicationDelays(), ed[i]);
            Ts[] curinput = new Ts[input.length];
            for (int j = 0; j < input.length; ++j) {
                curinput[j] = TsFactory.instance.createTs(input[j].getRawName(), null, cinfo.series(j));
            }
            doc.setInput(curinput);
            // update the specification
            // 
            DfmSpec curspec;
            if (mustBeEstimated(ed[i], estimationDays)) {
                curspec = spec.cloneDefinition();
            } else {
                curspec = spec.clone();
            }
            // update the time horizon
            TsPeriod last = cinfo.getCurrentDomain().getLast();
            TsPeriod end = last.clone();
            end.set(horizon_);
            curspec.getModelSpec().setForecastHorizon(end.minus(last));
            doc.setSpecification(curspec);
            doc.getResults();
            spec = doc.getSpecification();
            //spec = curspec;
            rslts_.put(ed[i], doc);
        }

        return true;
    }

    private boolean mustBeEstimated(Day day, List<Day> estimationDays) {
        if (estimationDays == null || estimationDays.isEmpty()) {
            return false;
        }

        int i = 0;
        boolean found = false;
        while (!found && i < estimationDays.size()) {
            if (day.isNotBefore(estimationDays.get(i))) {
                found = true;
                estimationDays.remove(i);
            } else {
                i++;
            }
        }

        return found;
    }

    public boolean process(DfmDocument refdoc, List<Day> estimationDays) {
        TsInformationSet info = new TsInformationSet(refdoc.getData());

        Day last = info.getCurrentDomain().getEnd().firstday();
        GregorianCalendar c = last.toCalendar();
        c.add(GregorianCalendar.YEAR, -refdoc.getSpecification().getSimulationSpec().getNumberOfYears());
        Day start = new Day(c.getTime());

        List<TsData> data = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        List<MeasurementSpec> measurements = refdoc.getSpecification().getModelSpec().getMeasurements();
        for (int i = 0; i < measurements.size(); i++) {
            if (measurements.get(i).isUsedForGeneration()) {
                data.add(refdoc.getData()[i]);
                delays.add(measurements.get(i).getDelay());
            }
        }

        if (estimationDays != null) {
            Collections.sort(estimationDays);
        }

        if (data.isEmpty()) {
            throw new IllegalArgumentException("You must select at least one "
                    + "reference series to generate the publication calendar !");
        }

        TsInformationSet infoCal = new TsInformationSet(data.toArray(new TsData[data.size()]));

        Day[] cal = infoCal.generatePublicationCalendar(delays, start);
        return process(refdoc, cal, estimationDays);
    }

}
