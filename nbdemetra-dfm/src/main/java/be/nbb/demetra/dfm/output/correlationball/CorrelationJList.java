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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author Mats Maggi
 */
public class CorrelationJList extends JList<String> {

    public static final String SELECTED_ITEM_PROPERTY = "itemSelected";
    
    private int selectedItem = -1;
    private CellSelectionListener selectionListener;
    
    public CorrelationJList() {
        super();
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionListener = new CellSelectionListener();
        getSelectionModel().addListSelectionListener(selectionListener);
        
        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case SELECTED_ITEM_PROPERTY :
                        onSelectedItemChange();
                        break;
                }
            }
        });
    }
    
    public void setSelectedItem(int index) {
        int old = this.selectedItem;
        this.selectedItem = index;
        firePropertyChange(SELECTED_ITEM_PROPERTY, old, this.selectedItem);
    }
    
    private void onSelectedItemChange() {
        if (selectionListener.enabled) {
            selectionListener.enabled = false;
            if (selectedItem < 0) {
                getSelectionModel().clearSelection();
            } else {
                getSelectionModel().addSelectionInterval(selectedItem, selectedItem);
            }
            selectionListener.enabled = true;
        }
        repaint();
    }
    
    private final class CellSelectionListener implements ListSelectionListener {

        boolean enabled = true;
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (enabled && !e.getValueIsAdjusting()) {
                enabled = false;
                ListSelectionModel model = getSelectionModel();
                if (model.isSelectionEmpty()) {
                    setSelectedItem(-1);
                } else {
                    setSelectedItem(getSelectedIndex());
                }
                enabled = true;
            }
        }
    }
}
