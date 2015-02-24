/*
 * Copyright 2015 National Bank of Belgium
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
package be.nbb.demetra.dfm.properties;

import ec.tstoolkit.descriptors.EnhancedPropertyDescriptor;
import ec.tstoolkit.descriptors.IObjectDescriptor;
import ec.tstoolkit.timeseries.Day;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Mats Maggi
 */
public class EstimationDayDescriptor implements IObjectDescriptor<Day> {

    public Day core_;

    public EstimationDayDescriptor() {
        this.core_ = new Day(new Date());
    }
    
    public EstimationDayDescriptor(Day d) {
        this.core_ = d;
    }
    
    @Override
    public Day getCore() {
        return core_;
    }
    
    public void setCore(Day day) {
        if (day != null) {
            core_ = day;
        }
    }

    @Override
    public List<EnhancedPropertyDescriptor> getProperties() {
        ArrayList<EnhancedPropertyDescriptor> descs = new ArrayList<>();
        EnhancedPropertyDescriptor desc = dayDesc();
        if (desc != null) {
            descs.add(desc);
        }

        return descs;
    }
    
    private EnhancedPropertyDescriptor dayDesc() {
        try {
            PropertyDescriptor desc = new PropertyDescriptor("core", this.getClass());
            EnhancedPropertyDescriptor edesc = new EnhancedPropertyDescriptor(desc, 1);
            desc.setDisplayName("Day");
            return edesc;
        } catch (IntrospectionException ex) {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        return "Estimation day";
    }

    @Override
    public String toString() {
        return core_.toString();
    }
}
