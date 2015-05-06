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
package be.nbb.demetra.dfm.output.news.outline;

import be.nbb.demetra.dfm.output.news.ColorIcon;
import be.nbb.demetra.dfm.output.news.outline.NewsRenderer.Type;
import ec.util.chart.ColorScheme;
import ec.util.chart.swing.SwingColorSchemeSupport;
import java.awt.Color;
import java.awt.Component;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import org.netbeans.swing.outline.DefaultOutlineCellRenderer;

/**
 *
 * @author Mats Maggi
 */
public class CustomOutlineCellRenderer extends DefaultOutlineCellRenderer {

    private final SwingColorSchemeSupport defaultColorSchemeSupport;
    private final Color red;
    private final Color blue;
    private final Type type;

    private final ColorIcon icon;
    private final List<Integer> colors;

    public CustomOutlineCellRenderer(SwingColorSchemeSupport support, Type type) {
        this.defaultColorSchemeSupport = support;
        red = SwingColorSchemeSupport.withAlpha(defaultColorSchemeSupport.getLineColor(ColorScheme.KnownColor.RED), 50);
        blue = SwingColorSchemeSupport.withAlpha(defaultColorSchemeSupport.getLineColor(ColorScheme.KnownColor.BLUE), 50);
        this.type = type;

        icon = new ColorIcon();
        colors = support.getColorScheme().getLineColors();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        //JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        JLabel l = new JLabel();
        if (value != null) {
            l.setText(String.valueOf(value));
            l.setToolTipText(String.valueOf(value));
        }
        l.setBackground(null);
        l.setForeground(null);
        l.setOpaque(true);
        l.setIcon(null);
        l.setBorder(new EmptyBorder(0, 2, 0, 2));

        VariableNode n = (VariableNode) table.getValueAt(row, 0);
        if (!isSelected) {
            l.setForeground(table.getForeground());
            if (type.equals(Type.WEIGHTS)) {
                DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) table.getCellRenderer(row, 0);
                switch (n.getName()) {
                    case "Old Forecasts":
                        l.setBackground(red);
                        renderer.setForeground(Color.BLACK);
                        break;
                    case "New Forecasts":
                        l.setBackground(blue);
                        renderer.setForeground(Color.BLACK);
                        break;
                }
            } else {
                switch (n.getName()) {
                    case "All News":
                        l.setBackground(red);
                        break;
                    case "All Revisions":
                        l.setBackground(blue);
                        break;
                }

            }
        } else {
            l.setForeground(table.getSelectionForeground());
            l.setBackground(table.getSelectionBackground());
        }

        l.setHorizontalAlignment(SwingConstants.RIGHT);

        if (type.equals(Type.IMPACTS)) {
            if (column == 1 && n.getParent() != null) {
                VariableNode p = (VariableNode) n.getParent();
                int index = p.getChildren().indexOf(n);
                if (p.getName().equals("All Revisions")) {
                    VariableNode pNews = (VariableNode) table.getValueAt(0, 0);
                    index += pNews.getChildren() == null ? 0 : pNews.getChildren().size();
                }
                l.setHorizontalAlignment(SwingConstants.LEADING);
                icon.setColor(new Color(colors.get(index % colors.size())));
                l.setIcon(icon);
            }
        }

        return l;
    }
}
