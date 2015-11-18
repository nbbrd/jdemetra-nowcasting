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
package be.nbb.demetra.dfm.output.correlationball;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;

/**
 *
 * @author Mats Maggi
 */
public class ClickableShape extends JComponent {

    private final Ellipse2D.Double ellipse;
    private Color foreground;
    private Point2D position;
    private final int index;
    private final List<Path> paths;

    public ClickableShape(int index) {
        setPreferredSize(new Dimension(10, 10));
        this.ellipse = new Ellipse2D.Double(0, 0, 10, 10);
        this.index = index;
        this.paths = new ArrayList<>();
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }

    public void setCoords(double x, double y) {
        this.ellipse.x = x;
        this.ellipse.y = y;
        repaint();
    }

    public List<Path> getPaths() {
        return paths;
    }
    
    public Set<Integer> getConnectedShapes() {
        Set<Integer> connected = new HashSet<>();
        for (Path p : paths) {
            connected.add(p.to);
        }
        
        return connected;
    }

    public Ellipse2D.Double getEllipse() {
        return ellipse;
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

    public class Path implements Comparable<Path> {

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

        @Override
        public int compareTo(Path o) {
            if (this.weight < o.weight) {
                return -1;
            } else if (this.weight > o.weight) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
