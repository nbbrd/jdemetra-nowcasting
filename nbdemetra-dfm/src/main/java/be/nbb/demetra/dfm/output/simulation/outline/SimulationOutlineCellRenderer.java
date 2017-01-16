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

import be.nbb.demetra.dfm.output.simulation.SimulationQuantifiedResultsView;
import static be.nbb.demetra.dfm.output.simulation.SimulationQuantifiedResultsView.BENCH_ENC;
import static be.nbb.demetra.dfm.output.simulation.SimulationQuantifiedResultsView.FIXED_SMOOTH_ASYMP;
import static be.nbb.demetra.dfm.output.simulation.SimulationQuantifiedResultsView.MODEL_ENC;
import static be.nbb.demetra.dfm.output.simulation.SimulationQuantifiedResultsView.STD_ASYMP;
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

    private final Color RED_1 = new Color(254, 129, 129);
    private final Color RED_2 = new Color(254, 46, 46);
    private final Color RED_3 = new Color(182, 32, 32);
    private final Color GREEN_1 = new Color(178, 210, 153);
    private final Color GREEN_2 = new Color(89, 191, 86);
    private final Color GREEN_3 = new Color(2, 131, 10);

    private final Formatters.Formatter<Number> numberFormatter;

    public SimulationOutlineCellRenderer(Formatters.Formatter<Number> formatter) {
        this.numberFormatter = formatter;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value == null
                || ((SimulationNode) value).getValues() == null
                || (Double.isNaN(((SimulationNode) value).getValues().get(column - 1)))) {
            label.setText("");
            label.setToolTipText(null);
            return label;
        }
        SimulationNode node = (SimulationNode) value;
        double d = node.getValues().get(column - 1);

        label.setToolTipText(null);
        label.setText(numberFormatter.formatAsString(d));

        if (!node.getName().equals(STD_ASYMP)
                && !node.getName().equals(FIXED_SMOOTH_ASYMP)
                && !node.getName().equals(MODEL_ENC)
                && !node.getName().equals(BENCH_ENC)
                && !node.getName().equals(FIXED_SMOOTH_ASYMP)) {
            return label;
        }

        if (node.getPValues() != null) {
            Font f = label.getFont();
            if (Double.isNaN(node.getPValues().get(column - 1))) {
                label.setFont(f.deriveFont(Font.PLAIN));
                label.setToolTipText("P-Value : NaN");
                setColor(label, Color.WHITE);
            } else {
                double pVal = node.getPValues().get(column - 1);
                label.setToolTipText("P-Value : " + numberFormatter.formatAsString(pVal));
                if (node.getName().equals(SimulationQuantifiedResultsView.MODEL_ENC)) {
                    // Green shades
                    if (pVal <= 0.05) {
                        setColor(label, GREEN_3);
                    } else if (pVal <= 0.1) {
                        setColor(label, GREEN_2);
                    } else if (pVal <= 0.2) {
                        setColor(label, GREEN_1);
                    }
                } else // Red shades
                {
                    if (pVal <= 0.05) {
                        setColor(label, RED_3);
                    } else if (pVal <= 0.1) {
                        setColor(label, RED_2);
                    } else if (pVal <= 0.2) {
                        setColor(label, RED_1);
                    }
                }
            }
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
