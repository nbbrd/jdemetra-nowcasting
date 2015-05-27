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

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.netbeans.swing.outline.Outline;

/**
 * Custom Outline component used to display news data.
 *
 * @author Mats Maggi
 */
public class XOutline extends Outline {

    private List<Title> titles;

    public XOutline() {
        super();

        setRootVisible(false);
        getTableHeader().setReorderingAllowed(false);
        setColumnHidingAllowed(true);
        setRowSorter(null);
        setFillsViewportHeight(true);
        setDragEnabled(false);
        getTableHeader().setReorderingAllowed(false);
        setComponentPopupMenu(createPopupMenu());
        setColumnSelectionOn(MouseEvent.BUTTON3, ColumnSelection.DIALOG);
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
            for (Object n : node.getChildren()) {
                TreePath path = p.pathByAddingChild(n);
                expandAll(path);
            }
        }
        expandPath(p);
    }

    private void collapseAll(TreePath p) {
        CustomNode node = (CustomNode) p.getLastPathComponent();
        if (node.getChildren() != null && node.getChildren().size() >= 0) {
            for (Object n : node.getChildren()) {
                TreePath path = p.pathByAddingChild(n);
                collapseAll(path);
            }
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
}
