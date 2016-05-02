/*
 * Copyright 2013 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
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

import ec.util.completion.swing.XPopup;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author Philippe Charles
 */
class PopupTableCellEditor extends AbstractCellEditor implements TableCellEditor {

    private final EditorDelegate customEditor;
    private final JPanel main;
    private final JLabel label;
    private final JButton button;
    private final XPopup popup;

    public PopupTableCellEditor(final EditorDelegate customEditor) {
        this.customEditor = customEditor;
        this.main = new JPanel();
        this.label = new JLabel();
        this.button = new JButton("...");
        this.popup = new XPopup();
        
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener((ActionEvent e) -> {
            popup.show(label, customEditor.getCustomComponent(), XPopup.Anchor.BOTTOM_LEADING, new Dimension());
        });
        main.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                popup.hide();
            }
        });
        main.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                popup.hide();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                popup.hide();
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                popup.hide();
            }
        });
        main.setLayout(new BorderLayout());
        main.add(label, BorderLayout.CENTER);
        main.add(button, BorderLayout.EAST);
    }

    @Override
    public Object getCellEditorValue() {
        return customEditor.getValue();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        customEditor.setValue(value);
        label.setText(customEditor.getValueAsString());
        return main;
    }

    interface EditorDelegate {

        Component getCustomComponent();

        String getValueAsString();

        Object getValue();

        void setValue(Object value);
    }

}
