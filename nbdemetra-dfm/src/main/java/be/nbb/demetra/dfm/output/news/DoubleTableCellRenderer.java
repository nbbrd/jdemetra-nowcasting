/*
 * Copyright 2013 National Bank of Belgium
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
package be.nbb.demetra.dfm.output.news;

import ec.nbdemetra.ui.DemetraUI;
import ec.tss.tsproviders.utils.Formatters;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Table cell renderer for double values
 *
 * @author Mats Maggi
 */
public class DoubleTableCellRenderer extends DefaultTableCellRenderer {

    private JToolTip tooltip;

    public DoubleTableCellRenderer() {
        setHorizontalAlignment(SwingConstants.TRAILING);
        tooltip = super.createToolTip();
    }

    @Override
    public JToolTip createToolTip() {
        tooltip.setBackground(getBackground());
        tooltip.setForeground(getForeground());
        return tooltip;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof Double) {
            DemetraUI demetraUI = DemetraUI.getInstance();
            Formatters.Formatter<Number> format = demetraUI.getDataFormat().numberFormatter();
            setText(format.formatAsString((Double) value));
        }

        return this;
    }
}
