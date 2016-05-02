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
package be.nbb.demetra.dfm.properties;

import com.google.common.collect.Lists;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTable;
import com.l2fprod.common.propertysheet.PropertySheetTableModel;
import ec.nbdemetra.ui.DemetraUiIcon;
import ec.nbdemetra.ui.NbComponents;
import ec.nbdemetra.ui.properties.l2fprod.CustomPropertyEditorRegistry;
import ec.nbdemetra.ui.properties.l2fprod.CustomPropertyRendererFactory;
import ec.nbdemetra.ui.properties.l2fprod.PropertiesPanelFactory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import org.openide.util.Exceptions;

/**
 *
 * @author Mats Maggi
 */
public class DaysPropertyEditor<T> extends JPanel {

    public static final String ELEMENTS_PROPERTY = "elements";
    private Class<T> c_;
    private ArrayList<T> elements = new ArrayList<>();
    private T current_;
    private boolean dirty_;

    public DaysPropertyEditor(T[] elems, Class<T> c) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        
        c_ = c;
        elements = Lists.newArrayList(elems);

        final JList list = new JList(new ArrayEditorListModel(elements));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setPreferredSize(new Dimension(150, 200));
        add(NbComponents.newJScrollPane(list), BorderLayout.WEST);

        final PropertySheetTableModel model = new PropertySheetTableModel();
        final PropertySheetPanel psp = new PropertySheetPanel(new PropertySheetTable(model));
        psp.setToolBarVisible(false);
        psp.setEditorFactory(CustomPropertyEditorRegistry.INSTANCE.getRegistry());
        psp.setRendererFactory(CustomPropertyRendererFactory.INSTANCE.getRegistry());
        psp.setDescriptionVisible(true);
        psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
        add(psp, BorderLayout.CENTER);
        psp.setPreferredSize(new Dimension(250, 200));
        psp.setBorder(BorderFactory.createEtchedBorder());

        list.addListSelectionListener((ListSelectionEvent e) -> {
            if (list.getSelectedValue() != null) {
                model.setProperties(PropertiesPanelFactory.INSTANCE.createProperties(list.getSelectedValue()));
                current_ = (T) list.getSelectedValue();
            } else {
                current_ = null;
                model.setProperties(new Property[]{});
            }
        });

        psp.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (dirty_) {
                    firePropertyChange(ELEMENTS_PROPERTY, null, elements);
                    list.setModel(new ArrayEditorListModel(elements));
                    list.invalidate();
                }
            }
        });

        model.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            setDirty(true);
            firePropertyChange(ELEMENTS_PROPERTY, null, elements);
        });

        final JPanel buttonPane = new JPanel();
        BoxLayout layout = new BoxLayout(buttonPane, BoxLayout.LINE_AXIS);
        buttonPane.setLayout(layout);
        final JButton addButton = new JButton(DemetraUiIcon.LIST_ADD_16);
        addButton.setPreferredSize(new Dimension(30, 30));
        addButton.setFocusPainted(false);
        addButton.addActionListener((ActionEvent e) -> {
            setDirty(true);
            try {
                Constructor<T> constructor = c_.getConstructor(new Class[]{});
                final T o = constructor.newInstance(new Object[]{});
                elements.add(o);
                firePropertyChange(ELEMENTS_PROPERTY, null, elements);
                SwingUtilities.invokeLater(() -> {
                    list.setModel(new ArrayEditorListModel(elements));
                    list.setSelectedValue(o, true);
                    list.invalidate();
                });
                
            } catch (SecurityException | IllegalArgumentException ex) {
                System.err.println(ex.getMessage());
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
        });
        buttonPane.add(addButton);
        final JButton deleteButton = new JButton(DemetraUiIcon.LIST_REMOVE_16);
        deleteButton.setPreferredSize(new Dimension(30, 30));
        deleteButton.setFocusPainted(false);
        deleteButton.addActionListener((ActionEvent e) -> {
            try {
                if (current_ == null) {
                    return;
                }
                setDirty(true);
                elements.remove(current_);
                firePropertyChange(ELEMENTS_PROPERTY, null, elements);
                SwingUtilities.invokeLater(() -> {
                    list.setModel(new ArrayEditorListModel(elements));
                    list.invalidate();
                });
                
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        });
        buttonPane.add(deleteButton);
        buttonPane.add(Box.createGlue());
        buttonPane.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        add(buttonPane, BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        setMinimumSize(new Dimension(400, 200));
        setPreferredSize(new Dimension(400, 200));
    }

    public ArrayList<T> getElements() {
        return elements;
    }

    public boolean isDirty() {
        return dirty_;
    }

    public void setDirty(boolean dirty) {
        this.dirty_ = dirty;
    }

    class ArrayEditorListModel<T> extends DefaultListModel {

        private final List<T> elementsList_;

        public ArrayEditorListModel(List<T> elements) {
            super();
            if (elements == null) {
                elementsList_ = new ArrayList<>();
            } else {
                elementsList_ = elements;
            }
        }

        @Override
        public int getSize() {
            return elementsList_.size();
        }

        @Override
        public Object getElementAt(int index) {
            return elementsList_.get(index);
        }
    }
}
