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
package be.nbb.demetra.dfm.properties;

import ec.tstoolkit.timeseries.Day;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Mats Maggi
 */
public class DaysEditor extends PropertyEditorSupport {

    private DaysPropertyEditor<EstimationDayDescriptor> editor;

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }

    @Override
    public Component getCustomEditor() {
        editor = new DaysPropertyEditor<>(getDescriptors(), EstimationDayDescriptor.class);

        editor.addPropertyChangeListener(DaysPropertyEditor.ELEMENTS_PROPERTY, (PropertyChangeEvent evt) -> {
            setDescriptors(editor.getElements());
        });
        return editor;
    }

    @Override
    public String getAsText() {
        Day[] days = (Day[]) getValue();
        if (days == null || days.length == 0) {
            return "";
        } else {
            String s = days[0].toString();
            for (int i = 1; i < days.length; i++) {
                s += ", " + days[i].toString();
            }
            return s;
        }
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.isEmpty()) {
            setValue(new Day[]{});
        } else {
            String[] splitted = text.split(",");
            List<Day> d = new ArrayList<>();
            for (String splitted1 : splitted) {
                try {
                    d.add(Day.fromString(splitted1.trim()));
                } catch (ParseException ex) {
                }
            }
            setValue(d.toArray(new Day[d.size()]));
        }
    }

    private EstimationDayDescriptor[] getDescriptors() {
        Day[] days = (Day[]) getValue();
        EstimationDayDescriptor[] descs = new EstimationDayDescriptor[days.length];
        for (int i = 0; i < descs.length; ++i) {
            descs[i] = new EstimationDayDescriptor(days[i]);
        }
        return descs;
    }

    private void setDescriptors(List<EstimationDayDescriptor> elements) {
        Day[] days = new Day[elements.size()];
        for (int i = 0; i < days.length; ++i) {
            days[i] = elements.get(i).getCore();
        }
        setValue(days);
    }
}
