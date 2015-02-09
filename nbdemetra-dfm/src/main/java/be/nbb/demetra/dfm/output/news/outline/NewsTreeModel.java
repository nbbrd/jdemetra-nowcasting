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
package be.nbb.demetra.dfm.output.news.outline;

import be.nbb.demetra.dfm.output.news.outline.TreeNode.CustomNode;
import be.nbb.demetra.dfm.output.news.outline.TreeNode.RootNode;
import be.nbb.demetra.dfm.output.news.outline.TreeNode.VariableNode;
import java.util.List;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author Mats Maggi
 */
public class NewsTreeModel implements TreeModel {

    private final RootNode root;

    public NewsTreeModel(List<VariableNode> nodes) {
        this.root = new RootNode(nodes);
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return getChildren(parent).get(index);
    }

    protected List<VariableNode> getChildren(Object node) {
        return ((CustomNode) node).getChildren();
    }

    @Override
    public int getChildCount(Object parent) {
        List<VariableNode> children = getChildren(parent);
        return children == null ? 0 : children.size();
    }

    @Override
    public boolean isLeaf(Object node) {
        if (node instanceof CustomNode) {
            return ((CustomNode) node).isLeaf();
        }
        return false;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // Selection managed here ???
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        CustomNode par = (CustomNode) parent;
        CustomNode ch = (CustomNode) child;
        return par.getChildren().indexOf(ch);
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
    }

}
