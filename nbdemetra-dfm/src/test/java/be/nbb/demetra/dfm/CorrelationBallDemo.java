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
package be.nbb.demetra.dfm;

import be.nbb.demetra.dfm.output.correlationball.CorrelationBall;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.TransferHandler;

/**
 *
 * @author Mats Maggi
 */
public class CorrelationBallDemo {

    public static void main(String[] args) {
        JFrame f = new JFrame("Correlation Ball");
        f.setLayout(new BorderLayout());
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final CorrelationBall correlationBall = new CorrelationBall();
        double[][] matrix = createMatrix();
        List<String> titles = createTitles(matrix.length);
        correlationBall.setMatrix(titles, matrix);
        correlationBall.setBackground(Color.WHITE);
        correlationBall.setForeground(Color.BLACK);
        correlationBall.setTransferHandler(new ImageSelection());

        f.add(correlationBall, BorderLayout.CENTER);
        JButton b = new JButton(new AbstractAction("Random data") {

            @Override
            public void actionPerformed(ActionEvent e) {
                double[][] matrix = createMatrix();
                List<String> titles = createTitles(matrix.length);
                correlationBall.setMatrix(titles, matrix);
            }
        });

        f.add(b, BorderLayout.SOUTH);

        JButton save = new JButton(new AbstractAction("Save Image") {

            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                correlationBall.getTransferHandler().exportToClipboard(correlationBall, cb, TransferHandler.COPY);
            }
        });
        f.add(save, BorderLayout.NORTH);
        f.pack();
        f.setVisible(true);
    }

    private static double[][] createMatrix() {
        Random r = new Random();
        int size = r.nextInt(50) + 6;

        double[][] matrix = new double[size][size];

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j <= i; j++) {
                if (i == j) {
                    matrix[i][j] = 1;
                } else {
                    double nb = r.nextDouble();
                    double weight = r.nextDouble();
                    matrix[i][j] = nb;
                }

            }
        }

        return matrix;
    }

    private static List<String> createTitles(int nb) {
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < nb; i++) {
            titles.add("Variable number " + i);
        }

        return titles;
    }
}

class ImageSelection extends TransferHandler implements Transferable {

    private static final DataFlavor flavors[] = {DataFlavor.imageFlavor};
    private CorrelationBall source;
    private BufferedImage image;

    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY;
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor flavor[]) {
        if (!(comp instanceof CorrelationBall)) {
            return false;
        }
        for (int i = 0, n = flavor.length; i < n; i++) {
            for (int j = 0, m = flavors.length; j < m; j++) {
                if (flavor[i].equals(flavors[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Transferable createTransferable(JComponent comp) {
        // Clear
        source = null;
        image = null;

        if (comp instanceof CorrelationBall) {
            source = (CorrelationBall) comp;

            Insets insets = source.getInsets();
            int w = source.getWidth() - insets.left - insets.right;
            int h = source.getHeight() - insets.top - insets.bottom;

            image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            source.paint(g2d);
            return this;
        }
        return null;
    }

    // Transferable
    @Override
    public Object getTransferData(DataFlavor flavor) {
        if (isDataFlavorSupported(flavor)) {
            return image;
        }
        return null;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DataFlavor.imageFlavor);
    }
}
