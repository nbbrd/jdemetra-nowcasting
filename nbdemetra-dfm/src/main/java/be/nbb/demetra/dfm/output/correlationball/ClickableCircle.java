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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/**
 *
 * @author Mats Maggi
 */
public class ClickableCircle extends JComponent {

    private int index;
    private String title;
    private final List<Path> paths;
    public final static String HIGHLIGHT = "HIGHLIGHT";

    public ClickableCircle(final int index) {
        setPreferredSize(new Dimension(8, 8));
        this.index = index;
        paths = new ArrayList<>();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                foreground = Color.RED;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                foreground = getForeground();
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                fire();
            }
        });
    }

    public void fire() {
        Container c = getParent();
        if (c instanceof CorrelationBall) {
            ((CorrelationBall) c).changeHighlight(index);
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        setToolTipText(this.title);
    }

    public List<Path> getPaths() {
        return paths;
    }

    public void addPath(Path2D.Double p, float w, int from, int to) {
        for (Path path : paths) {
            if ((path.from == from && path.to == to) 
                    || (path.to == from && path.from == to)) {
                return;
            }
        }
        paths.add(new Path(p, w, from, to));
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    private Color foreground = getForeground();

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(foreground);
        g2d.setStroke(new BasicStroke(1f));
        g2d.fillOval(0, 0, getWidth(), getHeight());
    }
    
    public class Path {
        public Path2D.Double path;
        public float weight;
        public int from;
        public int to;

        public Path(Path2D.Double path, float weight, int from, int to) {
            this.path = path;
            this.weight = weight;
            this.from = from;
            this.to = to;
        }
    }
}
