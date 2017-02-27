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
import static be.nbb.demetra.dfm.output.simulation.SimulationQuantifiedResultsView.AVG_FCTS_ERROR;
import static be.nbb.demetra.dfm.output.simulation.SimulationQuantifiedResultsView.BENCH_ENC;
import static be.nbb.demetra.dfm.output.simulation.SimulationQuantifiedResultsView.FIRST_ORDER_AUTOCORR;
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
    private final Color BLUE_1 = new Color(193, 215, 221);
    private final Color BLUE_2 = new Color(149, 187, 198);
    private final Color BLUE_3 = new Color(90, 150, 166);

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
            label.setToolTipText("P-Value : > " + numberFormatter.formatAsString(0.2));
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
                && !node.getName().equals(AVG_FCTS_ERROR)
                && !node.getName().equals(FIRST_ORDER_AUTOCORR)
                && !node.getName().equals(FIXED_SMOOTH_ASYMP)) {
            return label;
        }

        if (node.getPValues() != null) {
            Font f = label.getFont();
            if (Double.isNaN(node.getPValues().get(column - 1))) {
                label.setFont(f.deriveFont(Font.PLAIN));
                label.setToolTipText("P-Value : > " + numberFormatter.formatAsString(0.2));
                setColor(label, Color.WHITE);
            } else {
                double pVal = node.getPValues().get(column - 1);
                if (!node.getName().equals(STD_ASYMP)) {
                    label.setToolTipText("P-Value : \u2264 " + numberFormatter.formatAsString(pVal));
                }

                if (node.getParent() != null) {
                    switch (node.getParent().getName()) {
                        case SimulationQuantifiedResultsView.ENC_TITLE:
                            switch (node.getName()) {
                                case MODEL_ENC:
                                    setRed(label, pVal);
                                    break;
                                case BENCH_ENC:
                                    setGreen(label, pVal);
                                    break;
                            }
                            break;
                        case SimulationQuantifiedResultsView.BIAS_TITLE:
                        case SimulationQuantifiedResultsView.EFFICIENCY_TITLE:
                            setRed(label, pVal);
                            break;
                        case SimulationQuantifiedResultsView.DM_TITLE:
                            setBlue(label, pVal);
                            break;
                    }
                }
            }
        }

        return label;
    }

    private void setRed(JLabel l, double pVal) {
        if (pVal <= 0.05) {
            setColor(l, RED_3);
        } else if (pVal <= 0.1) {
            setColor(l, RED_2);
        } else if (pVal <= 0.2) {
            setColor(l, RED_1);
        }
    }

    private void setBlue(JLabel l, double pVal) {
        if (pVal <= 0.05) {
            setColor(l, BLUE_3);
        } else if (pVal <= 0.1) {
            setColor(l, BLUE_2);
        } else if (pVal <= 0.2) {
            setColor(l, BLUE_1);
        }
    }

    private void setGreen(JLabel l, double pVal) {
        if (pVal <= 0.05) {
            setColor(l, GREEN_3);
        } else if (pVal <= 0.1) {
            setColor(l, GREEN_2);
        } else if (pVal <= 0.2) {
            setColor(l, GREEN_1);
        }
    }

    private void setColor(JLabel l, Color c) {
        l.setBackground(c);
        l.setForeground(getForegroundColor(c));
    }

    private static Color getForegroundColor(Color color) {
        return SwingColorSchemeSupport.isDark(color) ? Color.WHITE : Color.BLACK;
    }
}
