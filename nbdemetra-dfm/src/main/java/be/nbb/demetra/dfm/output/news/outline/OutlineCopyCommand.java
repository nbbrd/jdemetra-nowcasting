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

import be.nbb.demetra.dfm.output.simulation.outline.SimulationNode;
import ec.nbdemetra.ui.DemetraUI;
import ec.tss.datatransfer.TssTransferSupport;
import ec.tstoolkit.data.Table;
import ec.util.various.swing.JCommand;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.text.NumberFormat;
import javax.annotation.Nonnull;
import javax.swing.table.TableModel;

/**
 *
 * @author Mats Maggi
 */
public class OutlineCopyCommand extends JCommand<XOutline> {

    private final NumberFormat format = DemetraUI.getDefault().getDataFormat().newNumberFormat();

    @Override
    public void execute(XOutline component) throws Exception {
        Table<?> table = toTable(component);
        Transferable t = TssTransferSupport.getDefault().fromTable(table);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
    }

    @Nonnull
    public static OutlineCopyCommand copyAll() {
        return new OutlineCopyCommand();
    }

    public Table<?> toTable(@Nonnull XOutline outline) {
        TableModel model = outline.getModel();
        if (model.getRowCount() == 0 || model.getColumnCount() == 0) {
            return new Table<>(0, 0);
        }

        int cols = outline.getColumnCount();
        int rows = outline.getRowCount();
        Table<Object> result = new Table<>(model.getRowCount() + 1, model.getColumnCount() + 1);
        if (outline.getTitles() != null) {
            for (int i = 0; i < outline.getTitles().size(); i++) {
                result.set(0, i + 1, outline.getTitles().get(i).getTitle());
            }
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Object r = model.getValueAt(i, j);
                if (r instanceof SimulationNode) {
                    SimulationNode n = (SimulationNode) r;
                    if (j == 0) {
                        result.set(i + 1, j, n.getName());
                    } else if (n.getValues() != null) {
                        Double val = n.getValues().get(j - 1);
                        if (!Double.isNaN(val)) {
                            result.set(i + 1, j, format.format(val));
                        }
                    }
                }
            }
        }

        return result;
    }
}
