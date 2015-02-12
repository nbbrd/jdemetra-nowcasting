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

import be.nbb.demetra.dfm.output.news.NewsWeightsView.Title;
import be.nbb.demetra.dfm.output.news.outline.TreeNode.VariableNode;
import ec.tss.tsproviders.utils.Formatters;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.List;
import org.netbeans.swing.outline.RowModel;

/**
 *
 * @author Mats Maggi
 */
public class NewsRowModel implements RowModel {

    private final List<Title> cNames;
    private final Formatters.Formatter<Number> numberFormatter;
    private final List<TsPeriod> periods;

    public NewsRowModel(List<Title> columns, List<TsPeriod> periods, Formatters.Formatter<Number> format) {
        this.cNames = columns;
        this.numberFormatter = format;
        this.periods = periods;
    }

    @Override
    public int getColumnCount() {
        return cNames.size();
    }

    @Override
    public Object getValueFor(Object o, int i) {
        if (o instanceof VariableNode) {
            VariableNode n = (VariableNode) o;
            switch (i) {
                case 0:
                    return formatPeriod(n.getRefPeriod());
                case 1:
                    return formatValue(n.getExpected());
                case 2:
                    return formatValue(n.getObserved());
                default:
                    int index = i - 3;
                    if (index >= 0 && n.getNews() != null && index < n.getNews().size()) {
                        TsPeriod p = periods.get(index);
                        if (n.getNews().containsKey(p)) {
                            return formatValue(n.getNews().get(p));
                        }
                        return "";
                    } else {
                        return "";
                    }
            }
        }
        return null;
    }

    private String formatPeriod(TsPeriod p) {
        if (p == null) {
            return "";
        } else {
            return p.toString();
        }
    }

    private String formatValue(Double v) {
        if (v == null || v.isNaN()) {
            return "";
        } else {
            return numberFormatter.formatAsString(v);
        }
    }

    @Override
    public Class getColumnClass(int i) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(Object o, int i) {
        return false;
    }

    @Override
    public void setValueFor(Object o, int i, Object o1) {

    }

    @Override
    public String getColumnName(int i) {
        return cNames.get(i).getHtmlTitle();
    }
}
