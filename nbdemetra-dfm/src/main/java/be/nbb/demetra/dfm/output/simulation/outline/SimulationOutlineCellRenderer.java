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
package be.nbb.demetra.dfm.output.simulation.outline;

import static be.nbb.demetra.dfm.output.simulation.SimulationQuantifiedResultsView.D_M_ABS_TEST;
import static be.nbb.demetra.dfm.output.simulation.SimulationQuantifiedResultsView.D_M_TEST;
import static be.nbb.demetra.dfm.output.simulation.SimulationQuantifiedResultsView.ENCOMPASING_TEST;
import ec.tss.tsproviders.utils.Formatters;
import ec.util.chart.swing.SwingColorSchemeSupport;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JTable;
import org.netbeans.swing.outline.DefaultOutlineCellRenderer;

/**
 *
 * @author Mats Maggi
 */
public class SimulationOutlineCellRenderer extends DefaultOutlineCellRenderer {
    
    private final double CONF_80 = 0.845;
    private final double CONF_90 = 1.29;
    private final double CONF_95 = 1.65;
    
    private final Color RED_1 = new Color(254,129,129);
    private final Color RED_2 = new Color(254,46,46);
    private final Color RED_3 = new Color(182,32,32);
    private final Color GREEN_1 = new Color(178,210,153);
    private final Color GREEN_2 = new Color(89,191,86);
    private final Color GREEN_3 = new Color(2,131,10);
    
    private final Formatters.Formatter<Number> numberFormatter;
    
    public SimulationOutlineCellRenderer(Formatters.Formatter<Number> formatter) {
        this.numberFormatter = formatter;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (value == null 
                || Double.isNaN((double) value)) {
            label.setText("");
            label.setToolTipText(null);
            return label;
        }
        
        double d = (double) value;
        
        label.setToolTipText(null);
        label.setText(numberFormatter.formatAsString(d));
        
        SimulationNode n = (SimulationNode) table.getValueAt(row, 0);
        if (!n.getName().equals(D_M_TEST)
                && !n.getName().equals(D_M_ABS_TEST)
                && !n.getName().equals(ENCOMPASING_TEST)) {
            return label;
        }
        
        Font f = label.getFont();
        
        if (d < -CONF_80) {
            label.setFont(f.deriveFont(Font.BOLD));
            if (d < -CONF_95) {
                setColor(label, GREEN_3);
                label.setToolTipText("Hypothesis of \"equal forecast accuracy\" is rejected at"
                        + " 95% significance level.");
            } else if (d < -CONF_90) {
                setColor(label, GREEN_2);
                label.setToolTipText("Hypothesis of \"equal forecast accuracy\" is rejected at"
                        + " 90% significance level.");
            } else {
                setColor(label, GREEN_1);
                label.setToolTipText("Hypothesis of \"equal forecast accuracy\" is rejected at"
                        + " 80% significance level.");
            }
        } else if (d > CONF_80) {
            label.setFont(f.deriveFont(Font.BOLD));
            if (d > CONF_95) {
                setColor(label, RED_3);
                label.setToolTipText("Hypothesis of \"equal forecast accuracy\" is rejected at"
                        + " 95% significance level.");
            } else if (d > CONF_90) {
                setColor(label, RED_2);
                label.setToolTipText("Hypothesis of \"equal forecast accuracy\" is rejected at"
                        + " 90% significance level.");
            } else {
                setColor(label, RED_1);
                label.setToolTipText("Hypothesis of \"equal forecast accuracy\" is rejected at"
                        + " 80% significance level.");
            }
        } else {
            label.setFont(f.deriveFont(Font.PLAIN));
            setColor(label, Color.WHITE);
            label.setToolTipText("Hypothesis of \"equal forecast accuracy\" is not rejected.");
        }
        
        return label;
    }
    
    private void setColor(JLabel l, Color c) {
        l.setBackground(c);
        l.setForeground(getForegroundColor(c));
    }
    
    private static Color getForegroundColor(Color color) {
        return SwingColorSchemeSupport.isDark(color) ? Color.WHITE : Color.BLACK;
    }
}
