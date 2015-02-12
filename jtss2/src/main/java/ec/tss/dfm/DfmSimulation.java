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
import ec.tstoolkit.timeseries.Day;
import ec.tstoolkit.timeseries.information.TsInformationSet;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jean Palate
 */
public class DfmSimulation {
    
    public static final int DEF_NY=2;

    private Day horizon_;

    private final Map<Day, DfmDocument> rslts_ = new HashMap<>();

    private Day eday_;
    private int mlag_;

    public DfmSimulation(Day horizon) {
        horizon_ = horizon;
    }
    
    public Map<Day, DfmDocument> getResults(){
        return Collections.unmodifiableMap(rslts_);
    }

    public boolean process(DfmDocument refdoc, Day[] ed) {
        rslts_.clear();
        DfmSpec spec = refdoc.getSpecification();
        Ts[] input = refdoc.getInput();
        
        TsInformationSet info = new TsInformationSet(refdoc.getData());
        
        Day neday = eday_;
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
            if (neday == null || ed[i].isNotAfter(neday)) {
                curspec = spec.clone();
            } else {
                curspec = spec.cloneDefinition();
            }
            // update the time horizon
            TsPeriod last = cinfo.getCurrentDomain().getLast();
            TsPeriod end=last.clone();
            end.set(horizon_);
            curspec.getModelSpec().setForecastHorizon(end.minus(last));
            doc.setSpecification(curspec);
            spec = curspec;
            rslts_.put(ed[i], doc);
        }
        return true;
    }

    public boolean process(DfmDocument refdoc, Day start) {
        TsInformationSet info = new TsInformationSet(refdoc.getData());
        
        if (start == null){
            Day last=info.getCurrentDomain().getEnd().firstday();
            GregorianCalendar c = last.toCalendar();
            c.add(GregorianCalendar.YEAR, DEF_NY);
            start=new Day(c.getTime());
        }
        
        Day[] cal = info.generatePublicationCalendar(refdoc.getSpecification().getModelSpec().getPublicationDelays(), start);
        return process(refdoc, cal);
    }


    public void setEstimationPolicy(Day firstEstimationDay, int estimationSpan) {
        eday_ = firstEstimationDay;
        mlag_ = estimationSpan;
    }

}
