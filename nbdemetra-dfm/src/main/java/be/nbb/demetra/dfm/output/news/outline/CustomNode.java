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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Mats Maggi
 */
public class CustomNode {

    private String name;
    private String fullName;
    private List<CustomNode> children;
    private CustomNode parent;

    public CustomNode() {
        name = "CustomNode";
    }

    public CustomNode(String name) {
        this.name = name;
    }
    
    public CustomNode(List<CustomNode> children) {
        this("Root");
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<CustomNode> getChildren() {
        return children;
    }

    public void setChildren(List<CustomNode> children) {
        this.children = children;
        for (CustomNode child : children) {
            child.parent = this;
        }
    }
    
    public void addChild(CustomNode node) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(node);
    }

    public boolean isLeaf() {
        return (getChildren() == null || getChildren().isEmpty());
    }

    public CustomNode getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return name;
    }
}
