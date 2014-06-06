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
import ec.nbdemetra.ui.awt.PopupListener;
import ec.tss.dfm.DfmResults;
import ec.tss.tsproviders.utils.Formatters.Formatter;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.ui.interfaces.IZoomableGrid;
import ec.util.grid.swing.AbstractGridModel;
import ec.util.grid.swing.GridModel;
import ec.util.grid.swing.GridRowHeaderRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Mats Maggi
 */
public class ResidualsMatrixView extends JPanel {

    // Properties
    public static final String DFM_RESULTS_PROPERTY = "dfmResults";
    public static final String ZOOM_PROPERTY = "zoom";
    public static final String HEAT_MAP_PROPERTY = "heatMap";
    private int zoomRatio = 100;
    private Font originalFont;
    private boolean heatMapVisible = true;

    // Heat map colour settings.
    private final Color highValueColour = new Color(0, 82, 163);
    private final Color lowValueColour = Color.WHITE;

    // How many RGB steps there are between the high and low colours.
    private int colourValueDistance;

    private double lowValue;
    private double highValue;

    private static final DecimalFormat df3 = new DecimalFormat("0.000");

    private final JZoomableGrid matrix;
    private Optional<DfmResults> results;

    public ResidualsMatrixView(Optional<DfmResults> r) {
        setLayout(new BorderLayout());

        this.matrix = createMatrix();
        this.results = r;

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case ZOOM_PROPERTY:
                        onZoomChange();
                        break;
                    case HEAT_MAP_PROPERTY:
                    case DFM_RESULTS_PROPERTY:
                        updateMatrix();
                        break;
                }
            }
        });

        matrix.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case IZoomableGrid.COLOR_SCALE_PROPERTY:
                        updateMatrix();
                        break;
                }
            }
        });

        matrix.addMouseListener(new PopupListener.PopupAdapter(buildGridMenu().getPopupMenu()));
        add(matrix, BorderLayout.CENTER);

        updateMatrix();
    }

    private JMenu buildGridMenu() {
        JMenu menu = matrix.buildGridMenu();
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(new AbstractAction("Show heat map") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setHeatMapVisible(!heatMapVisible);
            }
        });
        item.setSelected(heatMapVisible);
        menu.add(item);
        return menu;
    }

    protected void onZoomChange() {
        if (originalFont == null) {
            originalFont = getFont();
        }

        Font font = originalFont;

        if (this.zoomRatio != 100) {
            float floatRatio = ((float) this.zoomRatio) / 100.0f;
            float scaledSize = originalFont.getSize2D() * floatRatio;
            font = originalFont.deriveFont(scaledSize);
        }

        matrix.setFont(font);
    }

    public int getZoomRatio() {
        return zoomRatio;
    }

    public void setZoomRatio(int zoomRatio) {
        int old = this.zoomRatio;
        this.zoomRatio = zoomRatio >= 10 && zoomRatio <= 200 ? zoomRatio : 100;
        firePropertyChange(ZOOM_PROPERTY, old, this.zoomRatio);
    }

    public Optional<DfmResults> getDfmResults() {
        return results;
    }

    public void setDfmResults(Optional<DfmResults> dfmResults) {
        Optional<DfmResults> old = this.results;
        this.results = dfmResults != null ? dfmResults : Optional.<DfmResults>absent();
        firePropertyChange(DFM_RESULTS_PROPERTY, old, this.results);
    }

    public boolean isHeatMapVisible() {
        return heatMapVisible;
    }

    public void setHeatMapVisible(boolean heatMapVisible) {
        boolean old = this.heatMapVisible;
        this.heatMapVisible = heatMapVisible;
        firePropertyChange(HEAT_MAP_PROPERTY, old, this.heatMapVisible);
    }

    private void updateMatrix() {
        if (results == null || !results.isPresent()) {
            matrix.setModel(null);
        } else {
            matrix.setModel(createModel());
        }
    }

    private JZoomableGrid createMatrix() {
        final JZoomableGrid result = new JZoomableGrid();
        result.setOddBackground(null);
        result.setRowRenderer(new GridRowHeaderRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel result = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                result.setToolTipText(result.getText());
                return result;
            }
        });

        HeatMapCellRenderer cellRenderer = new HeatMapCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.CENTER);
        result.setDefaultRenderer(Object.class, cellRenderer);

        return result;
    }

    private List<String> titles;

    private Matrix filterMatrix() {
        List<Integer> indexes = new ArrayList<>();
        titles = new ArrayList<>();
        DfmResults rslts = results.get();
        List<DynamicFactorModel.MeasurementDescriptor> measurements = rslts.getModel().getMeasurements();
        Matrix data = rslts.getIdiosyncratic();
        for (int i = 0; i < data.getColumnsCount(); i++) {
            if (measurements.get(i).getUsedFactorsCount() > 0) {
                indexes.add(i);
                titles.add(rslts.getDescription(i).description);
            }
        }

        Matrix result = new Matrix(indexes.size(), indexes.size());
        for (int i = 0; i < indexes.size(); i++) {
            for (int j = 0; j < indexes.size(); j++) {
                result.set(i, j, data.get(indexes.get(i), indexes.get(j)));
            }
        }
        return result;
    }

    // <editor-fold defaultstate="collapsed" desc="Table Model & Adapater">
    private GridModel createModel() {

        final Matrix data = filterMatrix();
        lowValue = min(data);
        highValue = max(data);
        updateColourDistance();

        final int nbSeries = titles.size();

        return new AbstractGridModel() {
            @Override
            public int getRowCount() {
                return nbSeries;
            }

            @Override
            public int getColumnCount() {
                return nbSeries;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return columnIndex <= rowIndex ? data.get(rowIndex, columnIndex) : null;
            }

            @Override
            public String getRowName(int rowIndex) {
                return titles.get(rowIndex);
            }

            @Override
            public String getColumnName(int column) {
                return getRowName(column);
            }
        };
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="HeatMap Color Management">
    private Color getCellColour(double data) {
        double range = highValue - lowValue;
        double position = data - lowValue;

        // What proportion of the way through the possible values is that.
        double percentPosition = position / range;

        // Which colour group does that put us in.
        int colourPosition = getColourPosition(percentPosition);

        int r = lowValueColour.getRed();
        int g = lowValueColour.getGreen();
        int b = lowValueColour.getBlue();

        // Make n shifts of the colour, where n is the colourPosition.
        for (int i = 0; i < colourPosition; i++) {
            int rDistance = r - highValueColour.getRed();
            int gDistance = g - highValueColour.getGreen();
            int bDistance = b - highValueColour.getBlue();

            if ((Math.abs(rDistance) >= Math.abs(gDistance))
                    && (Math.abs(rDistance) >= Math.abs(bDistance))) {
                // Red must be the largest.
                r = changeColourValue(r, rDistance);
            } else if (Math.abs(gDistance) >= Math.abs(bDistance)) {
                // Green must be the largest.
                g = changeColourValue(g, gDistance);
            } else {
                // Blue must be the largest.
                b = changeColourValue(b, bDistance);
            }
        }

        return new Color(r, g, b);
    }

    private int getColourPosition(double percentPosition) {
        return (int) Math.round(colourValueDistance * Math.pow(percentPosition, matrix.getColorScale()));
    }

    private int changeColourValue(int colourValue, int colourDistance) {
        if (colourDistance < 0) {
            return colourValue + 1;
        } else if (colourDistance > 0) {
            return colourValue - 1;
        } else {
            // This shouldn't actually happen here.
            return colourValue;
        }
    }

    private void updateColourDistance() {
        int r1 = lowValueColour.getRed();
        int g1 = lowValueColour.getGreen();
        int b1 = lowValueColour.getBlue();
        int r2 = highValueColour.getRed();
        int g2 = highValueColour.getGreen();
        int b2 = highValueColour.getBlue();

        colourValueDistance = Math.abs(r1 - r2);
        colourValueDistance += Math.abs(g1 - g2);
        colourValueDistance += Math.abs(b1 - b2);
    }

    /**
     * Calculate the relative luminance of a given color
     *
     * @see http://en.wikipedia.org/wiki/Luminance_(relative)
     * @param c Color
     * @return Relative luminance value
     */
    private double getLuminance(Color c) {
        double r, g, b;
        r = c.getRed();
        g = c.getGreen();
        b = c.getBlue();
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private Color getForegroundColor(double luminance) {
        if (luminance > 127) {
            return Color.BLACK;
        } else {
            return Color.WHITE;
        }
    }

    private double min(Matrix values) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < values.getRowsCount(); i++) {
            for (int j = 0; j < values.getColumnsCount(); j++) {
                min = (Math.abs(values.get(i, j)) < min) ? Math.abs(values.get(i, j)) : min;
            }
        }
        return min;
    }

    private double max(Matrix values) {
        double max = Double.MIN_VALUE;
        for (int i = 0; i < values.getRowsCount(); i++) {
            for (int j = 0; j < values.getColumnsCount(); j++) {
                max = (Math.abs(values.get(i, j)) > max) ? Math.abs(values.get(i, j)) : max;
            }
        }
        return max;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Cell Renderer">
    private class HeatMapCellRenderer extends DefaultTableCellRenderer {

        private JToolTip tooltip;

        public HeatMapCellRenderer() {
            setHorizontalAlignment(TRAILING);
            setOpaque(true);

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

            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setForeground(null);
            l.setBackground(null);
            l.setToolTipText(null);
            if (value == null) {
                if (isSelected) {
                    l.setBackground(table.getSelectionBackground());
                    l.setForeground(table.getSelectionForeground());
                }
            } else if (value instanceof Double) {
                DemetraUI demetraUI = DemetraUI.getInstance();
                Formatter<Number> format = demetraUI.getDataFormat().numberFormatter();
                Number number = (Double) value;

                if (heatMapVisible) {
                    Color c = getCellColour(Math.abs(number.doubleValue()));
                    l.setBackground(c);
                    l.setForeground(getForegroundColor(getLuminance(c)));
                } else {
                    l.setBackground(table.getBackground());
                    l.setForeground(table.getForeground());
                }
                
                if (isSelected) {
                    l.setBackground(table.getSelectionBackground());
                    l.setForeground(table.getSelectionForeground());
                }
                l.setText(Double.isNaN(number.doubleValue()) ? "" : format.formatAsString(number));
                setToolTipText("<html>" + titles.get(row) + "<br>"
                        + titles.get(column) + "<br>"
                        + "Value : " + l.getText());
            }

            return l;
        }
    }
    // </editor-fold>
}
