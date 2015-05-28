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

import ec.ui.commands.GridCommand;
import ec.ui.interfaces.IZoomableGrid;
import ec.util.grid.swing.JGrid;
import ec.util.grid.swing.ext.TableGridCommand;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Mats Maggi
 */
public class JZoomableGrid implements IZoomableGrid {

    private final JGrid grid;
    
    private int zoomRatio = 100;
    private double colorScale = 1.0;
    private Font originalFont;

    public JZoomableGrid() {
        super();
        
        grid = new JGrid();
        
        grid.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case ZOOM_PROPERTY :
                        onZoomChange();
                        break;
                }
            }
        });
    }
    
    public JMenu buildGridMenu() {
        JMenu result = new JMenu();
        
        result.add(TableGridCommand.copyAll(true, true).toAction(grid)).setText("Copy");

        JMenu zoom = new JMenu("Zoom");
        final JSlider slider = new JSlider(10, 200, 100);
        {
            slider.setPreferredSize(new Dimension(50, slider.getPreferredSize().height));
            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    setZoomRatio(slider.getValue());
                }
            });
            grid.addPropertyChangeListener(ZOOM_PROPERTY, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    slider.setValue(getZoomRatio());
                }
            });
        }
        zoom.add(slider);
        for (final int o : new int[]{200, 100, 75, 50, 25}) {
            zoom.add(new JCheckBoxMenuItem(GridCommand.applyZoomRatio(o).toAction(this))).setText(o + "%");
        }
        result.add(zoom);
        
        //------------------------------------------------------
        
        JMenu scale = new JMenu("Color Scale");
        final JSlider slider2 = new JSlider(1, 100, 1);
        {
            slider2.setPreferredSize(new Dimension(50, slider.getPreferredSize().height));
            slider2.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    setColorScale((double)slider2.getValue()/10.0);
                }
            });
            grid.addPropertyChangeListener(COLOR_SCALE_PROPERTY, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    slider2.setValue((int)(getColorScale()*10.0));
                }
            });
        }
        scale.add(slider2);
        for (final double o : new double[]{0.1, 0.5, 1.0, 5.0, 10.0}) {
            scale.add(new JCheckBoxMenuItem(GridCommand.applyColorScale(o).toAction(this))).setText(String.valueOf(o));
        }
        result.add(scale);

        return result;
    }
    
    @Override
    public int getZoomRatio() {
        return zoomRatio;
    }

    @Override
    public void setZoomRatio(int zoomRatio) {
        int old = this.zoomRatio;
        this.zoomRatio = zoomRatio >= 10 && zoomRatio <= 200 ? zoomRatio : 100;
        grid.firePropertyChange(ZOOM_PROPERTY, old, this.zoomRatio);
    }

    
    protected void onZoomChange() {
        if (originalFont == null) {
            originalFont = grid.getFont();
        }

        Font font = originalFont;

        if (this.zoomRatio != 100) {
            float floatRatio = ((float) this.zoomRatio) / 100.0f;
            float scaledSize = originalFont.getSize2D() * floatRatio;
            font = originalFont.deriveFont(scaledSize);
        }

        grid.setFont(font);
    }

    @Override
    public double getColorScale() {
        return colorScale;
    }

    @Override
    public void setColorScale(double scale) {
        double old = this.colorScale;
        this.colorScale = scale >= 0.1 && scale <= 10.0 ? scale : 1.0;
        grid.firePropertyChange(COLOR_SCALE_PROPERTY, old, this.colorScale);
    }

    public JGrid getGrid() {
        return grid;
    }
}
