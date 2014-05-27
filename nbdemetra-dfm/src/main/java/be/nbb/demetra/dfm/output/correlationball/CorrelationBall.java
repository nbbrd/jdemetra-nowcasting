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

import be.nbb.demetra.dfm.output.correlationball.ClickableCircle.Path;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 *
 * @author Mats Maggi
 */
public class CorrelationBall extends JPanel {

    private double min, max;
    private final float maxStroke = 4f;
    private final float minStroke = .1f;
    
    // Heat map colour settings.
    private final Color highValueColour = new Color(0, 82, 163);
    private final Color lowValueColour = Color.WHITE;

    // How many RGB steps there are between the high and low colours.
    private int colourValueDistance;

    private int indexToHighlight = -1;
    private final List<ClickableCircle> circles = new ArrayList<>();

    public CorrelationBall() {
        super();
        setPreferredSize(new Dimension(500, 500));
        setLayout(null);

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                indexToHighlight = -1;
                repaint();
            }

        });

    }

    private double[][] matrix;
    private List<String> titles;

    public final void setMatrix(List<String> titles, double[][] matrix) {
        circles.clear();
        removeAll();
        revalidate();
        indexToHighlight = -1;

        this.matrix = matrix;
        this.titles = titles;

        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;

        for (int i = 0; i < matrix.length; i++) {
            ClickableCircle c = new ClickableCircle(i);
            circles.add(c);
            add(c);
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

    public void changeHighlight(int index) {
        if (indexToHighlight == index) {
            indexToHighlight = -1;
        } else {
            indexToHighlight = index;
        }
        repaint();
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
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(11f));
        g2d.drawOval(space, space, getWidth()-space*2, getWidth()-space*2);

        g2d.setColor(getForeground());
        g2d.setStroke(new BasicStroke(1f));

        AffineTransform original = g2d.getTransform();

        int length = matrix.length;

        double step = 360.0 / length;

        List<Point.Double> points = new ArrayList<>();
        Color offLedColor = new Color(86, 0, 0);

        for (int i = 0; i < length; i++) {
            double angle = Math.toRadians(step * i);

            double newX = (radius * Math.cos(angle)) + radius;
            double newY = (radius * Math.sin(angle)) + radius;
            points.add(new Point.Double(newX + space, newY + space));

            AffineTransform orig = (AffineTransform) g2d.getTransform().clone();
            orig.rotate(angle, newX + space, newY + space);
            g2d.setTransform(orig);

            ClickableCircle c = circles.get(i);
            c.setForeground(offLedColor);
            c.getPaths().clear();
            c.setLocation((int) newX + space - 5, (int) newY + space - 5);
            c.setBounds((int) newX + space - 5, (int) newY + space - 5, 10, 10);

            newX = ((radius) * Math.cos(angle)) + radius + 10;
            newY = ((radius) * Math.sin(angle)) + radius;

            g2d.drawString(titles.get(i), (int) newX + space + 5, (int) newY + space + 5);
            g2d.setTransform(original);
        }

        for (int i = 0; i < length; i++) {
            for (int j = 0; j <= i; j++) {
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

                    double newX = ((radius / 3) * Math.cos(angleRad)) + radius;
                    double newY = ((radius / 3) * Math.sin(angleRad)) + radius;

                    path.moveTo(points.get(i).x, points.get(i).y);
                    path.curveTo(newX + space, newY + space, newX + space, newY + space, points.get(j).x, points.get(j).y);

                    double normValue = ((double) Math.abs(matrix[i][j]) - min) / (max - min);
                    float strokeValue = ((float)normValue * (maxStroke - minStroke)) + minStroke;

                    circles.get(i).addPath(path, strokeValue, i, j);
                    circles.get(j).addPath(path, strokeValue, j, i);
                }
            }
        }

        Paint paint;
        int w = getWidth() - (space * 2) - 10;
        paint = new RadialGradientPaint(new Point2D.Double(getWidth() / 2.0,
                getWidth() / 2.0), radius,
                new float[]{0.8f, 1.0f},
                new Color[]{new Color(255, 255, 255, 0),
                    new Color(1.0f, 1.0f, 1.0f, 0.5f)});
        g2d.setPaint(paint);
        g2d.fillOval(space + 5, space + 5, w, w);

        paintLines(g2d);
    }

    private void paintLines(Graphics2D g2d) {
        
        for (int i = 0; i < circles.size(); i++) {
            for (Path p : circles.get(i).getPaths()) {
                g2d.setColor(getCellColour(normalize(p.weight)));
                if (indexToHighlight == -1 || i == indexToHighlight) {
                    g2d.setStroke(new BasicStroke(p.weight));
                    g2d.draw(p.path);
                }
            }
        }
    }

    private double normalize(double value) {
        return (value - minStroke) / (maxStroke - minStroke);
    }
    
    // <editor-fold defaultstate="collapsed" desc="HeatMap Color Management">
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
        return (int) Math.round(colourValueDistance * Math.pow(percentPosition, 1.0));
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
