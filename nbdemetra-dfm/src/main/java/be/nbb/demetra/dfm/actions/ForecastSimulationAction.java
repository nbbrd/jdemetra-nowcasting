/*
 * Copyright 2013-2014 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or â€“ as soon they will be approved 
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
import com.google.common.base.Stopwatch;
import ec.nbdemetra.ui.nodes.SingleNodeAction;
import ec.nbdemetra.ui.notification.MessageType;
import ec.nbdemetra.ui.notification.NotifyUtil;
import ec.nbdemetra.ws.WorkspaceItem;
import ec.nbdemetra.ws.nodes.ItemWsNode;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.DfmProcessingFactory;
import ec.tss.dfm.DfmSimulation;
import ec.tss.dfm.DfmSimulationResults;
import ec.tss.dfm.VersionedDfmDocument;
import ec.tstoolkit.dfm.MeasurementSpec;
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
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Cancellable;
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
    private ProgressHandle progressHandle;
    private SimulationSwingWorker worker;
    //private Map<Day, DfmDocument> results;

    public static final String RENAME_TITLE = "Please enter the new name",
            NAME_MESSAGE = "New name:";

    public ForecastSimulationAction() {
        super(ItemWsNode.class);
    }

    private final class SimulationSwingWorker extends SwingWorker<Object, String> {

        WorkspaceItem<?> cur;
        Stopwatch watch;

        public SimulationSwingWorker(WorkspaceItem<?> item) {
            cur = item;
        }

        @Override
        protected Object doInBackground() throws Exception {
            watch = Stopwatch.createStarted();

            File folder = chooser.getSelectedFile();
            VersionedDfmDocument vdoc = (VersionedDfmDocument) cur.getElement();

            TsInformationSet info = new TsInformationSet(vdoc.getCurrent().getData());

            TsPeriod last = info.getCurrentDomain().getLast();
            last.move(last.getFrequency().intValue());
            Day horizon = last.lastday();
            DfmSimulation simulation = new DfmSimulation(horizon);
            last.move(-3 * last.getFrequency().intValue());
            publish("Processing simulation of DFM...");
            simulation.process(vdoc.getCurrent(), new ArrayList<>(Arrays.asList(vdoc.getCurrent().getSpecification().getSimulationSpec().getEstimationDays())));
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

                List<Integer> delays = new ArrayList<>();
                for (MeasurementSpec m : vdoc.getCurrent().getSpecification().getModelSpec().getMeasurements()) {
                    delays.add(m.getDelay());
                }

                publish("Processing simulation of Arima...");
                for (int i = 0; i < cal.length; ++i) {
                    af.process(info.generateInformation(delays, cal[i]), s, horizon);
                    TsData f = af.getForecast();
                    tble2.insert(-1, f);
                }
                tble2.insert(-1, info.series(s));

                publish("Writing result's files to selected folder...");
                String nfile = Paths.concatenate(folder.getAbsolutePath(), "dfm-" + (s + 1));
                nfile = Paths.changeExtension(nfile, "txt");
                try (FileWriter writer = new FileWriter(nfile)) {
                    StringWriter swriter = new StringWriter();
                    write(swriter, tble, cal);
                    writer.append(swriter.toString());
                } catch (IOException err) {
                }

                nfile = Paths.concatenate(folder.getAbsolutePath(), "dfm-test-" + (s + 1));
                nfile = Paths.changeExtension(nfile, "txt");
                try (FileWriter writer = new FileWriter(nfile)) {
                    StringWriter swriter = new StringWriter();
                    simulation.getDfmResults().add(createFHTable(swriter, tble, cal));
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

                nfile = Paths.concatenate(folder.getAbsolutePath(), "arima-test-" + (s + 1));
                nfile = Paths.changeExtension(nfile, "txt");
                try (FileWriter writer = new FileWriter(nfile)) {
                    StringWriter swriter = new StringWriter();
                    simulation.getArimaResults().add(createFHTable(swriter, tble2, cal));
                    writer.append(swriter.toString());
                } catch (IOException err) {
                }
                
            }
            publish("Done !");
            return null;
        }

        @Override
        protected void done() {
            super.done();
            DateFormat df = new SimpleDateFormat("mm:ss");
            if (isCancelled()) {
                progressHandle.finish();
                NotifyUtil.show("Cancelled !", "Simulation has been cancelled after " + df.format(watch.stop().elapsed(TimeUnit.MILLISECONDS)), MessageType.WARNING, null, null, null);
            } else {
                try {
                    get();
                    if (progressHandle != null) {
                        progressHandle.finish();
                    }
                    NotifyUtil.show("Simulations done !", "Simulations ended in " + df.format(watch.stop().elapsed(TimeUnit.MILLISECONDS)), MessageType.SUCCESS, null, null, null);
                } catch (InterruptedException | ExecutionException ex) {
                    NotifyDescriptor desc = new NotifyDescriptor(ex.getMessage(), "Error", NotifyDescriptor.DEFAULT_OPTION, NotifyDescriptor.ERROR_MESSAGE, null, null);
                    DialogDisplayer.getDefault().notify(desc);
                }
            }
        }

        @Override
        protected void process(List<String> chunks) {
            super.process(chunks);
            progressHandle.progress(chunks.get(chunks.size() - 1));
        }

    }

    @Override
    protected void performAction(ItemWsNode context) {
        WorkspaceItem<?> cur = context.getItem();
        try {
            if (cur != null && !cur.isReadOnly()) {
                if (cur.getElement() instanceof VersionedDfmDocument) {

                    chooser.setDialogTitle("Select the output folder");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setAcceptAllFileFilterUsed(false);

                    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                        return;
                    }

                    worker = new SimulationSwingWorker(cur);
                    progressHandle = ProgressHandleFactory.createHandle("Forecast Simulations", new Cancellable() {
                        @Override
                        public boolean cancel() {
                            worker.cancel(false);
                            return true;
                        }
                    });

                    progressHandle.start();
                    progressHandle.switchToIndeterminate();
                    worker.execute();
                }
            }
        } catch (IllegalArgumentException ex) {
            NotifyDescriptor desc = new NotifyDescriptor(ex.getMessage(), "Error", NotifyDescriptor.DEFAULT_OPTION, NotifyDescriptor.ERROR_MESSAGE, null, null);
            DialogDisplayer.getDefault().notify(desc);
        }
    }

    @Override
    protected boolean enable(ItemWsNode context) {
        WorkspaceItem<?> cur = context.getItem();
        return cur != null && (worker == null || !worker.getState().equals(SwingWorker.StateValue.STARTED));
    }

    @Override
    public String getName() {
        return Bundle.CTL_ForecastSimulationAction();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    private DfmSimulationResults createFHTable(StringWriter writer, TsDataTable table, Day[] cal) {
        TsDomain dom = table.getDomain();
        if (dom == null || dom.isEmpty() || cal == null || cal.length == 0) {
            return null;
        }
        
        DfmSimulationResults r = new DfmSimulationResults();

        NumberFormat fmt = NumberFormat.getNumberInstance();

        int i0 = dom.search(cal[0]);    // Start of table
        int i1 = dom.search(cal[cal.length - 1]); // End of table

        int nbHeaders = i1 - i0 + 1;

        if (i0 < 0 || i1 < 0) {
            return null;
        }

        List<TsPeriod> evaluationSample = new ArrayList<>();
        for (int i = i0; i <= i1; i++) {    // headers
            evaluationSample.add(dom.get(i));
            writer.append('\t').append(dom.get(i).toString());
        }
        
        r.setEvaluationSample(evaluationSample);

        Map<Integer, Double[]> map = new TreeMap<>();

        for (int i = i0; i <= i1; i++) {    // parcours des headers
            for (int j = 0; j < cal.length; j++) {  // parcours du calendrier
                int diff = cal[j].difference(dom.get(i).lastday());
                if (!map.containsKey(diff)) {
                    map.put(diff, new Double[nbHeaders]);
                }

                TsDataTableInfo dataInfo = table.getDataInfo(i, j);
                if (dataInfo == TsDataTableInfo.Valid) {
                    map.get(diff)[i - i0] = table.getData(i, j);
                }
            }
        }

        Double[][] array = new Double[map.keySet().size()][];
        Iterator<Integer> keys = map.keySet().iterator();
        int iArray = 0;
        List<Integer> fctsHorizons = new ArrayList<>();
        while (keys.hasNext()) {
            int index = keys.next();
            Double[] values = map.get(index);
            array[iArray] = new Double[nbHeaders];
            System.arraycopy(values, 0, array[iArray++], 0, values.length);
            
            fctsHorizons.add(index);
        }
        r.setForecastHorizons(fctsHorizons);    // Set forecast horizons (keys)

        // Remplissage des missing values
        for (int col = 0; col < array[0].length; col++) {
            // search for 
            int start = 0;
            while (start < array.length && array[start][col] == null) {
                start++;
            }

            int end = array.length - 1;
            while (end >= 0 && array[end][col] == null) {
                end--;
            }

            if (start < array.length && start <= end && end >= 0) {
                double valueToCopy = array[start][col];
                for (int i = start + 1; i <= end; i++) {
                    if (array[i][col] != null) {
                        valueToCopy = array[i][col];
                    } else {
                        array[i][col] = valueToCopy;
                    }
                }
            }
        }
        
        r.setForecastsArray(array);
        
        Integer[] keysArray = map.keySet().toArray(new Integer[map.keySet().size()]);
        for (int i = keysArray.length - 1; i >= 0; i--) {
            writer.append("\r\n").append(String.valueOf(keysArray[i]));
            for (Double val : array[i]) {
                if (val != null) {
                    writer.append('\t').append(fmt.format(val));
                } else {
                    writer.append('\t');
                }
            }
        }

        List<Double> trueValues = new ArrayList<>();
        writer.append("\r\n").append("REAL");
        for (int i = i0; i <= i1; i++) {
            TsDataTableInfo dataInfo = table.getDataInfo(i, table.getSeriesCount() - 1);
            if (dataInfo == TsDataTableInfo.Valid) {
                Double val = table.getData(i, table.getSeriesCount() - 1);
                writer.append('\t').append(fmt.format(table.getData(i, table.getSeriesCount() - 1)));
                trueValues.add(val);
            } else {
                writer.append('\t');
                trueValues.add(null);
            }
        }
        
        r.setTrueValues(trueValues);
        
        return r;
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
