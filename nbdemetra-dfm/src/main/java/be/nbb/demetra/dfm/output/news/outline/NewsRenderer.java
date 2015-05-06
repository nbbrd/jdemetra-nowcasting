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

import ec.util.chart.ColorScheme;
import ec.util.chart.swing.SwingColorSchemeSupport;
import ec.util.various.swing.FontAwesome;
import java.awt.Color;
import javax.swing.Icon;
import org.netbeans.swing.outline.RenderDataProvider;

/**
 *
 * @author Mats Maggi
 */
public class NewsRenderer implements RenderDataProvider {
    
    public static enum Type {
        WEIGHTS, IMPACTS
    }

    private final SwingColorSchemeSupport defaultColorSchemeSupport;
    private final Color red;
    private final Color blue;
    private final Type type;
    
    public NewsRenderer(SwingColorSchemeSupport support, Type type) {
        this.defaultColorSchemeSupport = support;
        red = SwingColorSchemeSupport.withAlpha(defaultColorSchemeSupport.getLineColor(ColorScheme.KnownColor.RED), 50);
        blue = SwingColorSchemeSupport.withAlpha(defaultColorSchemeSupport.getLineColor(ColorScheme.KnownColor.BLUE), 50);
        this.type = type;
    }
    
    @Override
    public String getDisplayName(Object o) {
        if (o instanceof VariableNode) {
            return ((VariableNode)o).getName();
        }
        return String.valueOf(o);
    }

    @Override
    public boolean isHtmlDisplayName(Object o) {
        return false;
    }

    @Override
    public Color getBackground(Object o) {
        if (o instanceof VariableNode) {
            String name = ((VariableNode)o).getName();
            if (type == Type.WEIGHTS) {
                switch (name) {
                    case "Old Forecasts" : return red;
                    case "New Forecasts" : return blue;
                }
            } else {
                switch (name) {
                    case "All News" : return red;
                    case "All Revisions" : return blue;
                }
            }
        }
        return null;
    }

    @Override
    public java.awt.Color getForeground(Object o) {
        if (o instanceof VariableNode) {
            String name = ((VariableNode)o).getName();
            if (type == Type.WEIGHTS) {
                switch (name) {
                    case "Old Forecasts" : return Color.BLACK;
                    case "New Forecasts" : return Color.BLACK;
                }
            } else {
                switch (name) {
                    case "All News" : return Color.BLACK;
                    case "All Revisions" : return Color.BLACK;
                }
            }
        }
        return null;
    }

    @Override
    public String getTooltipText(Object o) {
        if (o instanceof VariableNode) {
            return ((VariableNode)o).getName();
        }
        return String.valueOf(o);
    }

    @Override
    public Icon getIcon(Object o) {
        if (o instanceof VariableNode) {
            VariableNode n = (VariableNode)o;
            if (n.getParent() != null) {
                Color fg = getForeground(o) == null ? Color.BLACK : Color.WHITE;
                return FontAwesome.FA_BAR_CHART_O.getIcon(fg, 12);
            }
        }
        return null;
    }

}
