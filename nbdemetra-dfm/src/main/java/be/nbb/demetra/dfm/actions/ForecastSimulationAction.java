/*
 * Copyright 2013-2014 National Bank of Belgium
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
package be.nbb.demetra.dfm.actions;

import be.nbb.demetra.dfm.DfmDocumentManager;
import ec.nbdemetra.ui.nodes.SingleNodeAction;
import ec.nbdemetra.ws.WorkspaceItem;
import ec.nbdemetra.ws.nodes.ItemWsNode;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.DfmProcessingFactory;
import ec.tss.dfm.DfmSimulation;
import ec.tss.dfm.VersionedDfmDocument;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.modelling.arima.tramo.TramoSpecification;
import ec.tstoolkit.timeseries.Day;
import ec.tstoolkit.timeseries.forecasts.ArimaForecaster;
import ec.tstoolkit.timeseries.information.TsInformationSet;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataTable;
import ec.tstoolkit.timeseries.simplets.TsDataTableInfo;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.tstoolkit.utilities.Paths;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map;
import javax.swing.JFileChooser;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit",
        id = "be.nbb.demetra.dfm.actions.ForecastSimulationAction"
)
@ActionRegistration(displayName = "#CTL_ForecastSimulationAction", lazy = false)
@ActionReferences({
    //    @ActionReference(path = "Menu/Edit"),
    @ActionReference(path = DfmDocumentManager.ITEMPATH, position = 1500)
})
@Messages("CTL_ForecastSimulationAction=Forecast simulation...")
public final class ForecastSimulationAction extends SingleNodeAction<ItemWsNode> {

    public static JFileChooser chooser = new JFileChooser();

    public static final String RENAME_TITLE = "Please enter the new name",
            NAME_MESSAGE = "New name:";

    public ForecastSimulationAction() {
        super(ItemWsNode.class);
    }

    @Override
    protected void performAction(ItemWsNode context) {
        WorkspaceItem<?> cur = context.getItem();
        if (cur != null && !cur.isReadOnly()) {
            if (cur.getElement() instanceof VersionedDfmDocument) {

                chooser.setDialogTitle("Select the output folder");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                //
                // disable the "All files" option.
                //
                chooser.setAcceptAllFileFilterUsed(false);
                //    
                if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File folder = chooser.getSelectedFile();
                VersionedDfmDocument vdoc = (VersionedDfmDocument) cur.getElement();
                
                //TODO getData() ???
                //TsInformationSet info = new TsInformationSet(vdoc.getCurrent().getData());
                TsData[] data = new TsData[vdoc.getInput().length];
                for (int i = 0; i < vdoc.getInput().length; i++) {
                    data[i] = vdoc.getInput()[i].getTsData();
                }
                TsInformationSet info = new TsInformationSet(data);
                
                TsPeriod last = info.getCurrentDomain().getLast();
                last.move(last.getFrequency().intValue());
                Day horizon = last.lastday();
                DfmSimulation simulation = new DfmSimulation(horizon);
                last.move(-3 * last.getFrequency().intValue());
                simulation.process(vdoc.getCurrent(), last.firstday());
                Map<Day, DfmDocument> results = simulation.getResults();
                Day[] cal = new Day[results.size()];
                cal = results.keySet().toArray(cal);
                Arrays.sort(cal);
                for (int s = 0; s < info.getSeriesCount(); ++s) {
                    TsDataTable tble = new TsDataTable();
                    for (int i = 0; i < cal.length; ++i) {
                        TsData f = results.get(cal[i]).getResults().getData(InformationSet.concatenate(DfmProcessingFactory.FINALC, "var" + (s + 1)), TsData.class);
                        tble.insert(-1, f);
                    }
                    tble.insert(-1, info.series(s));
                    TsDataTable tble2 = new TsDataTable();
                    ArimaForecaster af = new ArimaForecaster(TramoSpecification.TRfull.build());
                    for (int i = 0; i < cal.length; ++i) {
                        af.process(info.generateInformation(null, cal[i]), s, horizon);
                        TsData f = af.getForecast();
                        tble2.insert(-1, f);
                    }
                    tble2.insert(-1, info.series(s));
                    String nfile = Paths.concatenate(folder.getAbsolutePath(), "dfm-" + (s + 1));
                    nfile = Paths.changeExtension(nfile, "txt");
                    try (FileWriter writer = new FileWriter(nfile)) {
                        StringWriter swriter = new StringWriter();
                        write(swriter, tble, cal);
                        writer.append(swriter.toString());
                    } catch (IOException err) {
                    }
                    nfile = Paths.concatenate(folder.getAbsolutePath(), "arima-" + (s + 1));
                    nfile = Paths.changeExtension(nfile, "txt");
                    try (FileWriter writer = new FileWriter(nfile)) {
                        StringWriter swriter = new StringWriter();
                        write(swriter, tble2, cal);
                        writer.append(swriter.toString());
                    } catch (IOException err) {
                    }
                }
                NotifyDescriptor sdesc = new NotifyDescriptor.Message("Simulations ended");
                DialogDisplayer.getDefault().notify(sdesc);
            }
        }
    }

    @Override
    protected boolean enable(ItemWsNode context) {
        WorkspaceItem<?> cur = context.getItem();
        return cur != null;
    }

    @Override
    public String getName() {
        return Bundle.CTL_ForecastSimulationAction();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    private void write(StringWriter writer, TsDataTable table, Day[] cal) {
        TsDomain dom = table.getDomain();
        if (dom == null || dom.isEmpty() || cal == null || cal.length == 0) {
            return;
        }

        NumberFormat fmt = NumberFormat.getNumberInstance();

        // write headers
        for (int i = 0; i < cal.length; ++i) {
            writer.append('\t').append(cal[i].toString());
        }
        writer.append("\tseries");
        // write each rows
        int i0 = dom.search(cal[0]);
        if (i0 < 0) {
            return;
        }
        for (int j = i0; j < dom.getLength(); ++j) {
            writer.append("\r\n").append(dom.get(j).lastday().toString());
            for (int i = 0; i < table.getSeriesCount(); ++i) {
                TsDataTableInfo dataInfo = table.getDataInfo(j, i);
                if (dataInfo == TsDataTableInfo.Valid) {
                    writer.append('\t').append(fmt.format(table.getData(j, i)));
                } else {
                    writer.append('\t');
                }
            }
        }
    }
}
