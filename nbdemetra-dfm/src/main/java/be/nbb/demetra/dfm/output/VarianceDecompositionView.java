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
package be.nbb.demetra.dfm.output;

import com.google.common.base.Optional;
import ec.nbdemetra.ui.DemetraUI;
import ec.tss.datatransfer.TssTransferSupport;
import ec.tss.dfm.DfmResults;
import ec.tss.dfm.DfmSeriesDescriptor;
import ec.tss.tsproviders.utils.Formatters;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.ui.chart.TsXYDatasets;
import ec.util.chart.ColorScheme;
import ec.util.chart.ObsFunction;
import ec.util.chart.SeriesFunction;
import ec.util.chart.TimeSeriesChart;
import ec.util.chart.swing.SwingColorSchemeSupport;
import ec.util.various.swing.JCommand;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 *
 * @author charphi
 */
final class VarianceDecompositionView extends javax.swing.JPanel {

    public static final String DFM_RESULTS_PROPERTY = "dfmResults";

    private final int[] horizon = {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 16, 18,
        20, 24, 28, 32, 36, 40, 48, 60, 72, 84, 96, 120, 240, 1000
    };
    private Optional<DfmResults> dfmResults;
    
    private final DemetraUI demetraUI;
    private Formatters.Formatter<Number> formatter;
    private CustomSwingColorSchemeSupport defaultColorSchemeSupport;

    /**
     * Creates new form VarianceDecompositionView
     */
    public VarianceDecompositionView() {
        initComponents();

        this.dfmResults = Optional.absent();
        
        demetraUI = DemetraUI.getInstance();
        formatter = demetraUI.getDataFormat().numberFormatter();
        defaultColorSchemeSupport = new CustomSwingColorSchemeSupport() {
            @Override
            public ColorScheme getColorScheme() {
                return demetraUI.getColorScheme();
            }
        };

        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateChart();
            }
        });

        chart.setPopupMenu(createChartMenu().getPopupMenu());
        chart.setSeriesRenderer(SeriesFunction.always(TimeSeriesChart.RendererType.STACKED_COLUMN));
        chart.setSeriesFormatter(new SeriesFunction<String>() {
            @Override
            public String apply(int series) {
                return chart.getDataset().getSeriesKey(series).toString();
            }
        });
        chart.setObsFormatter(new ObsFunction<String>() {
            @Override
            public String apply(int series, int obs) {
                return chart.getSeriesFormatter().apply(series)
                        + " - " + chart.getPeriodFormat().format(chart.getDataset().getX(series, obs))
                        + "\n" + formatter.format(chart.getDataset().getY(series, obs));
            }
        });
        chart.setValueFormat(new DecimalFormat("#.###"));
        chart.setPeriodFormat(new DateFormat() {
            final Calendar cal = Calendar.getInstance();
            final DecimalFormat f = new DecimalFormat("###0");

            @Override
            public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
                cal.setTime(date);
                int year = cal.get(Calendar.YEAR);
                int index = year - 2000;
                if (index >= 0 && index < horizon.length) {
                    return toAppendTo.append(f.format(horizon[index]));
                }
                return toAppendTo.append(index);
            }

            @Override
            public Date parse(String source, ParsePosition pos) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
        
        chart.setColorSchemeSupport(defaultColorSchemeSupport);

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case DFM_RESULTS_PROPERTY:
                        updateComboBox();
                        updateChart();
                }
            }
        });

        updateComboBox();
        updateChart();
        
        demetraUI.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case DemetraUI.DATA_FORMAT_PROPERTY:
                        onDataFormatChanged();
                        break;
                    case DemetraUI.COLOR_SCHEME_NAME_PROPERTY:
                        onColorSchemeChanged();
                        break;
                }
            }
        });
    }
    
    private void onDataFormatChanged() {
        formatter = demetraUI.getDataFormat().numberFormatter();
    }

    private void onColorSchemeChanged() {
        defaultColorSchemeSupport = new CustomSwingColorSchemeSupport() {
            @Override
            public ColorScheme getColorScheme() {
                return demetraUI.getColorScheme();
            }
        };
        chart.setColorSchemeSupport(defaultColorSchemeSupport);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        chart = new ec.util.chart.swing.JTimeSeriesChart();
        comboBox = new javax.swing.JComboBox();

        setLayout(new java.awt.BorderLayout());
        add(chart, java.awt.BorderLayout.CENTER);

        add(comboBox, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ec.util.chart.swing.JTimeSeriesChart chart;
    private javax.swing.JComboBox comboBox;
    // End of variables declaration//GEN-END:variables

    //<editor-fold defaultstate="collapsed" desc="Getters/Setters">
    public Optional<DfmResults> getDfmResults() {
        return dfmResults;
    }

    public void setDfmResults(Optional<DfmResults> dfmResults) {
        Optional<DfmResults> old = this.dfmResults;
        this.dfmResults = dfmResults != null ? dfmResults : Optional.<DfmResults>absent();
        firePropertyChange(DFM_RESULTS_PROPERTY, old, this.dfmResults);
    }
    //</editor-fold>

    private void updateComboBox() {
        if (dfmResults.isPresent()) {
            comboBox.setModel(toComboBoxModel(dfmResults.get().getDescriptions()));
            comboBox.setEnabled(true);
        } else {
            comboBox.setModel(new DefaultComboBoxModel());
            comboBox.setEnabled(false);
        }
    }

    private void updateChart() {
        if (dfmResults.isPresent() && comboBox.getSelectedIndex() != -1) {
            Matrix matrix = toMatrix(dfmResults.get(), comboBox.getSelectedIndex());
            TsPeriod start = new TsPeriod(TsFrequency.Yearly, 2000, 0);
            TsXYDatasets.Builder b = TsXYDatasets.builder();
            int i = 0;
            for (DataBlock o : matrix.rowList()) {
                double[] data = new double[o.getLength()];
                o.copyTo(data, 0);
                b.add(i == matrix.getRowsCount() - 1 ? "Noise" : ("F" + ++i), new TsData(start, data, true));
            }
            chart.setDataset(b.build());
        } else {
            chart.setDataset(null);
        }
    }

    private Matrix toMatrix(DfmResults results, int selectedItem) {
        return results.getVarianceDecompositionIdx(horizon, selectedItem);
    }

    private static DefaultComboBoxModel toComboBoxModel(DfmSeriesDescriptor[] data) {
        DefaultComboBoxModel result = new DefaultComboBoxModel(data);
        return result;
    }

    private JMenu createChartMenu() {
        JMenu menu = new JMenu();
        JMenuItem item;

        item = menu.add(CopyCommand.INSTANCE.toAction(this));
        item.setText("Copy");

        return menu;
    }

    private static final class CopyCommand extends JCommand<VarianceDecompositionView> {

        public static final CopyCommand INSTANCE = new CopyCommand();

        @Override
        public void execute(VarianceDecompositionView c) throws Exception {
            Optional<DfmResults> dfmResults = c.getDfmResults();
            if (dfmResults.isPresent()) {
                Transferable t = TssTransferSupport.getInstance().fromMatrix(c.toMatrix(dfmResults.get(), c.comboBox.getSelectedIndex()));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
            }
        }

        @Override
        public boolean isEnabled(VarianceDecompositionView c) {
            return c.getDfmResults().isPresent();
        }

        @Override
        public JCommand.ActionAdapter toAction(VarianceDecompositionView c) {
            return super.toAction(c).withWeakPropertyChangeListener(c, DFM_RESULTS_PROPERTY);
        }
    }
    
    private abstract class CustomSwingColorSchemeSupport extends SwingColorSchemeSupport {
       
        @Override
        public Color getLineColor(int series) {
            return super.getLineColor(series);
        }
    }

}
