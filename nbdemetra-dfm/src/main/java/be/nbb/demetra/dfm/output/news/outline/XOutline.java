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
package be.nbb.demetra.dfm.output.news.outline;

import ec.util.grid.CellIndex;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.util.EventObject;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.netbeans.swing.outline.Outline;

/**
 * Custom Outline component used to display news data.
 *
 * @author Mats Maggi
 */
public class XOutline extends Outline {

    public static final String SELECTED_CELL_PROPERTY = "selectedCell";
    public static final String HOVERED_CELL_PROPERTY = "hoveredCell";
    
    private final CellIndex DEFAULT_SELECTED_CELL = CellIndex.NULL;
    private final CellIndex DEFAULT_HOVERED_CELL = CellIndex.NULL;
    private final CellSelectionListener cellSelectionListener;

    private List<Title> titles;
    protected CellIndex hoveredCell = DEFAULT_HOVERED_CELL;
    private CellIndex selectedCell = DEFAULT_SELECTED_CELL;

    public XOutline() {
        super();

        setRootVisible(false);
        getTableHeader().setReorderingAllowed(false);
        setColumnHidingAllowed(true);
        setRowSorter(null);
        setFillsViewportHeight(true);
        setDragEnabled(false);
        setComponentPopupMenu(createPopupMenu());
        setColumnSelectionOn(MouseEvent.BUTTON3, ColumnSelection.DIALOG);
        cellSelectionListener = new CellSelectionListener();

        addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case SELECTED_CELL_PROPERTY:
                    onSelectedCellChange();
                case HOVERED_CELL_PROPERTY:
                    onHoveredCellChange();
            }
        });
    }

    public void setSelectedCell(@Nullable CellIndex selectedCell) {
        CellIndex old = this.selectedCell;
        this.selectedCell = selectedCell != null ? selectedCell : DEFAULT_SELECTED_CELL;
        firePropertyChange(SELECTED_CELL_PROPERTY, old, this.selectedCell);
    }

    public CellIndex getSelectedCell() {
        return selectedCell;
    }
    
    public void setHoveredCell(@Nullable CellIndex hoveredCell) {
        CellIndex old = this.hoveredCell;
        this.hoveredCell = hoveredCell != null ? hoveredCell : DEFAULT_HOVERED_CELL;
        firePropertyChange(HOVERED_CELL_PROPERTY, old, this.hoveredCell);
    }
    
    @Nonnull
    public CellIndex getHoveredCell() {
        return hoveredCell;
    }

    private void onSelectedCellChange() {
        if (cellSelectionListener.enabled) {
            cellSelectionListener.enabled = false;
            CellIndex index = getSelectedCell();
            if (CellIndex.NULL.equals(index)) {
                getSelectionModel().clearSelection();
            } else {
                addRowSelectionInterval(index.getRow(), index.getRow());
                addColumnSelectionInterval(index.getColumn(), index.getColumn());
            }
            cellSelectionListener.enabled = true;
        }
        repaint();
    }
    
    private void onHoveredCellChange() {
        repaint();
    }

    public void enableCellSelection() {
        getSelectionModel().addListSelectionListener(cellSelectionListener);
    }

    private CellIndex getIndex(MouseEvent e) {
        if (e.getSource() instanceof XOutline) {
            XOutline table = (XOutline) e.getSource();
            Point point = e.getPoint();
            return CellIndex.valueOf(table.rowAtPoint(point), table.columnAtPoint(point));
        }
        return CellIndex.NULL;
    }

    public void enableCellHovering() {
        MouseMotionListener cellFocus = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHoveredCell(getIndex(e));
            }

        };
        addMouseMotionListener(cellFocus);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setHoveredCell(getIndex(e));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHoveredCell(getIndex(e));
            }
        });
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu result = new JPopupMenu();

        JMenuItem expand = new JMenuItem(new AbstractAction("Expand All") {
            @Override
            public void actionPerformed(ActionEvent e) {
                expandAll();
            }
        });

        JMenuItem collapse = new JMenuItem(new AbstractAction("Collapse All") {
            @Override
            public void actionPerformed(ActionEvent e) {
                collapseAll();
            }
        });

        result.add(expand);
        result.add(collapse);
        result.add(OutlineCommand.copyAll().toAction(this)).setText("Copy All");

        return result;
    }

    public void expandAll() {
        TreePath parent = new TreePath(((TreeModel) getModel()).getRoot());
        expandAll(parent);
    }

    public void collapseAll() {
        TreePath parent = new TreePath(((TreeModel) getModel()).getRoot());
        collapseAll(parent);
    }

    private void expandAll(TreePath p) {
        CustomNode node = (CustomNode) p.getLastPathComponent();
        if (node.getChildren() != null && node.getChildren().size() >= 0) {
            node.getChildren().stream().map((n) -> p.pathByAddingChild(n)).forEach((path) -> {
                expandAll(path);
            });
        }
        expandPath(p);
    }

    private void collapseAll(TreePath p) {
        CustomNode node = (CustomNode) p.getLastPathComponent();
        if (node.getChildren() != null && node.getChildren().size() >= 0) {
            node.getChildren().stream().map(p::pathByAddingChild).forEach((p1) -> this.collapseAll(p1));
        }

        if (p.getParentPath() != null) {
            collapsePath(p);
        }
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        if (getRowCount() == 0 || row == -1) {
            return false;
        } else {
            return super.editCellAt(row, column, e);
        }
    }

    public List<Title> getTitles() {
        return titles;
    }

    public void setTitles(List<Title> titles) {
        this.titles = titles;
    }

    public static class Title {

        private final String title;
        private final String htmlTitle;

        public Title(String title) {
            this.title = title;
            this.htmlTitle = title;
        }

        public Title(String title, String htmlTitle) {
            this.title = title;
            this.htmlTitle = htmlTitle;
        }

        public String getTitle() {
            return title;
        }

        public String getHtmlTitle() {
            return htmlTitle;
        }
    }

    private final class CellSelectionListener implements ListSelectionListener {

        boolean enabled = true;

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (enabled && !e.getValueIsAdjusting()) {
                enabled = false;
                ListSelectionModel model = getSelectionModel();
                if (model.isSelectionEmpty()) {
                    setSelectedCell(CellIndex.NULL);
                } else {
                    int row = getSelectedRow();
                    int col = getSelectedColumn();
                    if (!getSelectedCell().equals(row, col)) {
                        setSelectedCell(CellIndex.valueOf(row, col));
                    }
                }
                enabled = true;
            }
        }
    }
}
