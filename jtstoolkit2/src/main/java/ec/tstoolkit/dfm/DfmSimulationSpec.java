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
package ec.tstoolkit.dfm;

import ec.tstoolkit.algorithm.IProcSpecification;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.timeseries.Day;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;

/**
 *
 * @author Mats Maggi
 */
public class DfmSimulationSpec implements IProcSpecification, Cloneable {

    public static final String EST_DAYS = "estimationDays", DEF_NY = "numYears";

    private Day[] estimationDays;
    private int numberOfYears;

    public DfmSimulationSpec() {
        estimationDays = new Day[]{};
        numberOfYears = 2;
    }

    public Day[] getEstimationDays() {
        return estimationDays;
    }

    public void setEstimationDays(Day[] estimationDays) {
        this.estimationDays = estimationDays;
    }

    public int getNumberOfYears() {
        return numberOfYears;
    }

    public void setNumberOfYears(int numberOfYears) {
        this.numberOfYears = numberOfYears;
    }

    @Override
    public DfmSimulationSpec clone() {
        try {
            DfmSimulationSpec spec = (DfmSimulationSpec) super.clone();
            spec.estimationDays = estimationDays.clone();
            spec.setNumberOfYears(numberOfYears);

            return spec;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof DfmSimulationSpec && equals((DfmSimulationSpec) obj));
    }

    public boolean equals(DfmSimulationSpec obj) {
        return Arrays.deepEquals(obj.estimationDays, estimationDays) && obj.numberOfYears == numberOfYears;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public InformationSet write(boolean bln) {
        InformationSet info = new InformationSet();

        int i = 0;
        String[] days = new String[estimationDays.length];
        for (Day d : estimationDays) {
            days[i++] = d.toString();
        }

        if (days != null && days.length != 0) {
            info.add(EST_DAYS, days);
        }
        
        info.add(DEF_NY, numberOfYears);

        return info;
    }

    @Override
    public boolean read(InformationSet info) {
        if (info == null) {
            return true;
        }
        
        String[] days = info.get(EST_DAYS, String[].class);
        if (days != null) {
            estimationDays = new Day[days.length];
            for (int i = 0; i < estimationDays.length; i++) {
                try {
                    estimationDays[i] = Day.fromString(days[i]);
                } catch (ParseException ex) {
                }
            }
        }
        
        Integer ny = info.get(DEF_NY, Integer.class);
        if (ny != null) {
            numberOfYears = ny;
        }

        return true;
    }

    public static void fillDictionary(String prefix, Map<String, Class> dic) {
        dic.put(InformationSet.item(prefix, EST_DAYS), String[].class);
        dic.put(InformationSet.item(prefix, DEF_NY), Integer.class);
    }

}
