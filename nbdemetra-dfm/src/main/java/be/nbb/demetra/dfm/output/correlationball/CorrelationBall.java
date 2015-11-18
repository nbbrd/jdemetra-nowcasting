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
package be.nbb.demetra.dfm.output.correlationball;

import be.nbb.demetra.dfm.output.correlationball.ClickableShape.Path;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Mats Maggi
 */
public class CorrelationBall extends JPanel {

    public static final String COLOR_SCALE_PROPERTY = "scale";
    public static final String SHOW_IMPORTANT_PROPERTY = "showImportantOnly";
    public static final String INDEX_SELECTED_PROPERTY = "indexSelected";
    public static final String SPIN_ON_SELECTION_PROPERTY = "spinOnSelection";

    private double min, max;
    private double zoom = 1.0;
    private final float maxStroke = 3f;
    private final float minStroke = .1f;
    private int spinAngle = 0;

    private double colorScale = 0.5;
    private boolean showImportantOnly = true;
    private boolean spinOnSelection = false;

    // Heat map colour settings.
    private final Color highValueColour = new Color(0, 82, 163);
    private final Color lowValueColour = Color.WHITE;

    // How many RGB steps there are between the high and low colours.
    private int colourValueDistance;

    private int indexSelected = -1;
    private int indexHovered = -1;

    private List<String> titles;
    private double[][] matrix;

    private AffineTransform original;

    private final List<ClickableShape> shapes = new ArrayList<>();

    public CorrelationBall() {
        super();
        setDoubleBuffered(true);
        setPreferredSize(new Dimension(300, 300));
        setLayout(null);

        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int rotations = e.getWheelRotation();
                if (rotations > 0 || (rotations < 0 && zoom > 0.2)) {
                    zoom += rotations * 0.1;
                }

                zoom = Math.max(0.00001, zoom);
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    setIndexSelected(-1);
                    Point2D pt = new Point2D.Double();
                    original.inverseTransform((Point2D) e.getPoint(), pt);
                    int i = 0;
                    boolean found = false;
                    while (!found && i < shapes.size()) {
                        if (shapes.get(i).getEllipse().contains(pt)) {
                            setIndexSelected(i);
                            found = true;
                        }
                        i++;
                    }
                    super.mouseClicked(e);
                } catch (NoninvertibleTransformException ex) {
                    Logger.getLogger(CorrelationBall.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                boolean found = false;
                int i = 0;
                try {
                    Point2D pt = new Point2D.Double();
                    original.inverseTransform((Point2D) e.getPoint(), pt);
                    if (indexHovered != -1 && !shapes.get(indexHovered).getEllipse().contains(pt)) {
                        indexHovered = -1;
                        repaint();
                    } else {
                        while (!found && i < shapes.size()) {
                            if (shapes.get(i).getEllipse().contains(pt)) {
                                if (indexHovered != i) {
                                    indexHovered = i;
                                    found = true;
                                    repaint();
                                }

                                break;
                            }
                            i++;
                        }
                    }

                } catch (NoninvertibleTransformException ex) {
                    Logger.getLogger(CorrelationBall.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    public JMenu buildGridMenu() {
        JMenu result = new JMenu();

        JMenu scale = new JMenu("Color Scale");
        final JSlider slider = new JSlider(1, 100, 1);
        slider.setPreferredSize(new Dimension(50, slider.getPreferredSize().height));
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setColorScale((double) slider.getValue() / 10.0);
            }
        });
        addPropertyChangeListener(COLOR_SCALE_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                slider.setValue((int) (getColorScale() * 10.0));
            }
        });
        scale.add(slider);
        for (final double o : new double[]{0.1, 0.5, 1.0, 5.0, 10.0}) {
            scale.add(new JCheckBoxMenuItem(CorrelationBallCommand.applyColorScale(o).toAction(this))).setText(String.valueOf(o));
        }
        result.add(scale);

        final JCheckBoxMenuItem show = new JCheckBoxMenuItem(CorrelationBallCommand.showAllCommand().toAction(this));
        show.setText("Show important relations only");
        result.add(show);
        
        final JCheckBoxMenuItem spin = new JCheckBoxMenuItem(CorrelationBallCommand.spinOnSelectionCommand().toAction(this));
        spin.setText("Spin on selection");
        result.add(spin);

        return result;
    }

    public double getColorScale() {
        return colorScale;
    }

    public void setColorScale(double scale) {
        double old = this.colorScale;
        this.colorScale = scale >= 0.1 && scale <= 10.0 ? scale : 1.0;
        firePropertyChange(COLOR_SCALE_PROPERTY, old, this.colorScale);
    }

    public boolean isImportantOnlyShown() {
        return this.showImportantOnly;
    }

    public void setImportantOnlyShown(boolean showAll) {
        this.showImportantOnly = showAll;
        repaint();
    }

    public boolean isSpinOnSelection() {
        return spinOnSelection;
    }

    public void setSpinOnSelection(boolean spinOnSelection) {
        this.spinOnSelection = spinOnSelection;
    }

    public void spin(int value) {
        spinAngle = value;
        repaint();
    }

    public final void setMatrix(List<String> titles, double[][] matrix) {
        shapes.clear();
        removeAll();
        revalidate();
        indexSelected = -1;

        this.matrix = matrix;
        this.titles = titles;

        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;

        for (int i = 0; i < matrix.length; i++) {
            ClickableShape cc = new ClickableShape(i);
            shapes.add(cc);
            for (int j = 0; j < matrix[i].length; j++) {
                double abs = Math.abs(matrix[i][j]);
                if (abs != 0) {
                    if (abs < min) {
                        min = abs;
                    } else if (abs > max) {
                        max = abs;
                    }
                }
            }
        }

        updateColourDistance();
        updateUI();
    }

    public void setIndexSelected(int index) {
        int old = this.indexSelected;
        if (indexSelected == index) {
            index = -1;
        }
        this.indexSelected = index;
        repaint();
        firePropertyChange(INDEX_SELECTED_PROPERTY, old, this.indexSelected);
        
        if (spinOnSelection) {
            double step = 360.0 / shapes.size();
            firePropertyChange(SPIN_ON_SELECTION_PROPERTY, null, 360.0-(step*index));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (matrix == null) {
            return;
        }
        int space = getWidth() / 5;
        int radius = (getWidth() - space * 2) / 2;

        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(getWidth() / 2, getHeight() / 2);
        g2d.scale(zoom, zoom);
        g2d.rotate(Math.toRadians(spinAngle));

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_SPEED);

        g2d.setColor(getForeground());
        g2d.setStroke(new BasicStroke(1f));

        original = g2d.getTransform();

        int length = matrix.length;

        double step = 360.0 / length;

        List<Point.Double> points = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            double angle = Math.toRadians(step * i);

            double newX = (radius * Math.cos(angle)) - 5;
            double newY = (radius * Math.sin(angle)) - 5;

            points.add(new Point.Double(newX + 5, newY + 5));

            Point2D pt = new Point2D.Double();
            try {
                original.inverseTransform(shapes.get(i).getEllipse().getBounds().getLocation(), pt);
            } catch (NoninvertibleTransformException ex) {
                Logger.getLogger(CorrelationBall.class.getName()).log(Level.SEVERE, null, ex);
            }

            shapes.get(i).setBounds((int) pt.getX(), (int) pt.getY(), 10, 10);
            shapes.get(i).setLocation((int) pt.getX(), (int) pt.getY());
            shapes.get(i).setCoords(newX, newY);
            shapes.get(i).getPaths().clear();
        }
        
        allPaths = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < i; j++) {
                if (!Double.isNaN(matrix[i][j]) && matrix[i][j] != 0) {
                    Path2D.Double path = new Path2D.Double();

                    double minAngle = (double) Math.min(i, j) * step;
                    double maxAngle = (double) Math.max(i, j) * step;

                    double angleFinal;
                    double diff;
                    if (maxAngle - minAngle > 180) {
                        diff = (360.0 - (maxAngle - minAngle)) / 2.0;
                        angleFinal = maxAngle + diff;
                    } else {
                        diff = ((maxAngle - minAngle) / 2.0);
                        angleFinal = minAngle + diff;
                    }

                    double angleRad = Math.toRadians(angleFinal);

                    double newX = ((radius / 3) * Math.cos(angleRad));
                    double newY = ((radius / 3) * Math.sin(angleRad));

                    path.moveTo(points.get(i).x, points.get(i).y);
                    path.curveTo(newX, newY + 5, newX, newY, points.get(j).x, points.get(j).y);

                    double normValue = ((double) Math.abs(matrix[i][j]) - min) / (max - min);
                    float strokeValue = ((float) normValue * (maxStroke - minStroke)) + minStroke;

                    shapes.get(i).addPath(path, strokeValue, i, j);
                    shapes.get(j).addPath(path, strokeValue, j, i);
                }
            }
        }
        
        for (ClickableShape shape : shapes) {
            allPaths.addAll(shape.getPaths());
        }
        Collections.sort(allPaths);
        int size = allPaths.size();
        allPaths = allPaths.subList((int)(size*0.75), size-1);

        paintLines(g2d, radius, step);
    }
    
    private List<Path> allPaths;

    private void paintLines(Graphics2D g2d, int radius, double step) {
        for (int i = 0; i < shapes.size(); i++) {
            for (Path p : shapes.get(i).getPaths()) {
                double normalized = normalize(p.weight);
                g2d.setColor(getCellColour(normalized));
                g2d.setStroke(new BasicStroke(p.weight));
                if (indexSelected == -1) {
                    if (showImportantOnly) {
                        if (allPaths.contains(p)) {
                            g2d.draw(p.path);
                        }
                    } else {
                        g2d.draw(p.path);
                    }
                } else if (indexSelected == i) {
                    g2d.draw(p.path);
                }
            }
        }

        Color offLedColor = new Color(86, 0, 0);
        for (int i = 0; i < shapes.size(); i++) {
            double angle = Math.toRadians(step * i);
            if (i == indexSelected || i == indexHovered) {
                g2d.setColor(new Color(250, 70, 71));
            } else {
                if (indexSelected != -1) {
                    Set<Integer> connected = shapes.get(indexSelected).getConnectedShapes();
                    if (connected.contains(i)) {
                        g2d.setColor(new Color(33, 150, 243));
                    } else {
                        g2d.setColor(new Color(211, 234, 253));
                    }
                    drawString(g2d, titles.get(i), radius, angle);
                } else {
                    g2d.setColor(offLedColor);
                }
            }
            drawString(g2d, titles.get(i), radius, angle);

            add(shapes.get(i));
            g2d.fill(shapes.get(i).getEllipse());
        }
    }

    private void drawString(Graphics2D g2d, String label, int radius, double angle) {
        // Draw labels
        int h = (int) Math.floor(g2d.getFontMetrics().getStringBounds(label, g2d).getHeight() / 2);
        double X = (radius * Math.cos(0)) - 5;
        double Y = (radius * Math.sin(0)) - 5;
        AffineTransform rotated = (AffineTransform) g2d.getTransform().clone();
        rotated.rotate(angle);
        g2d.setTransform(rotated);

        g2d.drawString(label, (int) X + 15, (int) Y + h + 3);

        g2d.setTransform(original);
    }

    private double normalize(double value) {
        return (value - minStroke) / (maxStroke - minStroke);
    }

    private Color getCellColour(double data) {
        double range = max - min;
        double position = data - min;

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
        return (int) Math.round(colourValueDistance * Math.pow(percentPosition, getColorScale()));
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
}
