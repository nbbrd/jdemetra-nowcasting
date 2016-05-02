/*
 * Copyright 2014 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or Ã¢â‚¬â€œ as soon they will be approved 
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

import be.nbb.demetra.dfm.DfmController.DfmState;
import static be.nbb.demetra.dfm.DfmController.SIMULATION_STATE_PROPERTY;
import com.google.common.base.Stopwatch;
import ec.nbdemetra.ui.DemetraUiIcon;
import ec.nbdemetra.ui.NbComponents;
import ec.nbdemetra.ui.notification.MessageType;
import ec.nbdemetra.ui.notification.NotifyUtil;
import ec.nbdemetra.ui.properties.OpenIdePropertySheetBeanEditor;
import ec.nbdemetra.ws.WorkspaceItem;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.DfmSimulation;
import ec.tss.dfm.DfmSimulationResults;
import ec.tss.dfm.SimulationResultsDocument;
import ec.tss.dfm.VersionedDfmDocument;
import ec.tstoolkit.dfm.DfmSimulationSpec;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.dfm.MeasurementSpec;
import ec.tstoolkit.modelling.arima.tramo.TramoSpecification;
import ec.tstoolkit.timeseries.Day;
import ec.tstoolkit.timeseries.TsException;
import ec.tstoolkit.timeseries.forecasts.ArimaForecaster;
import ec.tstoolkit.timeseries.information.TsInformationSet;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataTable;
import ec.tstoolkit.timeseries.simplets.TsDataTableInfo;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.tstoolkit.utilities.Paths;
import ec.ui.view.tsprocessing.DefaultProcessingViewer;
import static ec.ui.view.tsprocessing.DefaultProcessingViewer.Type.NONE;
import ec.util.various.swing.JCommand;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import static org.openide.util.ImageUtilities.createDisabledIcon;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 *
 * @author Mats Maggi
 */
@ConvertAsProperties(
        dtd = "-//be.nbb.demetra.dfm//DfmSimulationView/EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "DfmSimulationTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@NbBundle.Messages({
    "CTL_DfmSimulationViewAction=DfmSimulationView",
    "CTL_DfmSimulationTopComponent=DfmSimulation Window",
    "HINT_DfmSimulationTopComponent=This is a DfmSimulation window"
})
public class DfmSimulationTopComponent extends AbstractDfmDocumentTopComponent {

    private final DefaultProcessingViewer<DfmDocument> processingViewer;

    private DfmSimulation simulation;
    public static JFileChooser chooser = new JFileChooser();
    private ProgressHandle progressHandle;
    private SimulationSwingWorker swingWorker;

    public DfmSimulationTopComponent() {
        this(null, new DfmController());
    }

    public DfmSimulationTopComponent(WorkspaceItem<VersionedDfmDocument> document, DfmController controller) {
        super(document, controller);
        setToolTipText(Bundle.HINT_DfmSimulationTopComponent());
        this.processingViewer = new DefaultProcessingViewer<DfmDocument>(NONE) {
        };
        processingViewer.setHeaderVisible(false);
    }

    @Override
    public JComponent getToolbarRepresentation() {
        JToolBar toolBar = NbComponents.newInnerToolbar();
        toolBar.addSeparator();
        toolBar.add(Box.createRigidArea(new Dimension(5, 0)));

        JButton edit = toolBar.add(EditSpecCommand.INSTANCE.toAction(this));
        edit.setIcon(DemetraUiIcon.PREFERENCES);
        edit.setDisabledIcon(createDisabledIcon(edit.getIcon()));
        edit.setToolTipText("Specification");

        JToggleButton startStop = (JToggleButton) toolBar.add(new JToggleButton(StartStopCommand.INSTANCE.toAction(this)));
        startStop.setIcon(DemetraUiIcon.COMPILE_16);
        startStop.setDisabledIcon(createDisabledIcon(startStop.getIcon()));
        startStop.setToolTipText("Start/Stop");

        return toolBar;
    }

    @Override
    protected void onSimulationStateChange() {
        switch (controller.getSimulationState()) {
            case CANCELLED:
                progressHandle.finish();
                break;
            case DONE:
                if (progressHandle != null) {
                    progressHandle.finish();
                }
                break;
            case FAILED:
                if (progressHandle != null) {
                    progressHandle.finish();
                }
                break;
            case READY:
                break;
            case STARTED:
                swingWorker = new SimulationSwingWorker(getDocument());
                progressHandle = ProgressHandleFactory.createHandle(getName(), () -> {
                    swingWorker.cancel(false);
                    return true;
                }, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        DfmSimulationTopComponent.this.open();
                        DfmSimulationTopComponent.this.requestActive();
                    }
                });

                progressHandle.start();
                progressHandle.switchToIndeterminate();
                swingWorker.execute();
                break;
            case CANCELLING:
                swingWorker.cancel(false);
                break;
        }
        super.onSimulationStateChange();
    }

    void writeProperties(java.util.Properties p) {
    }

    void readProperties(java.util.Properties p) {
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
            simulation = new DfmSimulation(horizon);

            simulation.addPropertyChangeListener((PropertyChangeEvent evt) -> {
                if (evt.getPropertyName().equals(DfmSimulation.CALENDAR_RESULTS)) {
                    Day value = (Day) evt.getNewValue();
                    publish("Generating data of publication date : " + value.toString() + "...");
                }
            });

            publish("Processing simulation of DFM...");
            simulation.process(vdoc.getCurrent(), new ArrayList<>(Arrays.asList(vdoc.getCurrent().getSpecification().getSimulationSpec().getEstimationDays())));
            Map<Day, SimulationResultsDocument> results = simulation.getResults();
            Day[] cal = new Day[results.size()];
            cal = results.keySet().toArray(cal);
            Arrays.sort(cal);

            List<Integer> delays = new ArrayList<>();
            vdoc.getCurrent().getSpecification().getModelSpec().getMeasurements().stream().forEach((m) -> {
                delays.add(m.getDelay());
            });

            for (int s = 0; s < info.getSeriesCount(); ++s) {
                boolean isWatched = vdoc.getCurrent().getSpecification().getModelSpec().getMeasurements().get(s).isWatched();

                if (isWatched) {
                    publish("Generating results of series #" + s);
                    TsDataTable tble = new TsDataTable();
                    for (int i = 0; i < cal.length; ++i) {
                        if (results.get(cal[i]) != null) {
                            if (results.get(cal[i]).getSimulationResults() != null) {
                                if (results.get(cal[i]).getSimulationResults().contains("var" + (s + 1))) {
                                    tble.insert(-1, results.get(cal[i]).getSimulationResults().getData("var" + (s + 1), TsData.class));
                                } else {
                                    System.out.println("Simulation results for calendar " + cal[i].toString() + " don't contain variable " + (s + 1));
                                }
                            } else {
                                System.out.println("No simulation results for calendar " + cal[i].toString() + " - Variable " + (s + 1));
                            }
                        } else {
                            System.out.println("No results for calendar " + cal[i].toString() + " - Variable " + (s + 1));
                        }
                    }
                    tble.insert(-1, info.series(s));

                    TsDataTable tble2 = new TsDataTable();
                    ArimaForecaster af = new ArimaForecaster(TramoSpecification.TRfull.build());

                    publish("Processing simulation of Arima for series #" + s + "...");
                    for (int i = 0; i < cal.length; ++i) {
                        af.process(info.generateInformation(delays, cal[i]), s, horizon);
                        tble2.insert(-1, af.getForecast());
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
                        simulation.getDfmResults().add(createFHTable(swriter, tble, cal, delays.get(s)));
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
                        simulation.getArimaResults().add(createFHTable(swriter, tble2, cal, delays.get(s)));
                        writer.append(swriter.toString());
                    } catch (IOException err) {
                    }
                }
            }
            publish("Done !");

            vdoc.getCurrent().setSimulation(simulation);
            return null;
        }

        @Override
        protected void done() {
            super.done();
            DateFormat df = new SimpleDateFormat("mm:ss");
            if (isCancelled()) {
                progressHandle.finish();
                NotifyUtil.show("Cancelled !", "Simulation has been cancelled after " + df.format(watch.stop().elapsed(TimeUnit.MILLISECONDS)), MessageType.WARNING, null, null, null);
                controller.setSimulationState(DfmState.CANCELLED);
            } else {
                try {
                    get();
                    if (progressHandle != null) {
                        progressHandle.finish();
                    }
                    NotifyUtil.show("Simulations done !", "Simulations ended in " + df.format(watch.stop().elapsed(TimeUnit.MILLISECONDS)), MessageType.SUCCESS, null, null, null);
                    controller.setSimulationState(DfmState.DONE);
                } catch (InterruptedException | ExecutionException ex) {
                    NotifyDescriptor desc = new NotifyDescriptor(ex.getMessage(), "Error", NotifyDescriptor.DEFAULT_OPTION, NotifyDescriptor.ERROR_MESSAGE, null, null);
                    DialogDisplayer.getDefault().notify(desc);
                    controller.setSimulationState(DfmState.FAILED);
                }
            }
        }

        @Override
        protected void process(List<String> chunks) {
            super.process(chunks);
            progressHandle.progress(chunks.get(chunks.size() - 1));
        }
    }

    private DfmSimulationResults createFHTable(StringWriter writer, TsDataTable table, Day[] cal, int delay) {
        TsDomain dom = table.getDomain();
        if (dom == null || dom.isEmpty() || cal == null || cal.length == 0) {
            return null;
        }

        DfmSimulationResults r = new DfmSimulationResults();

        NumberFormat fmt = NumberFormat.getNumberInstance();

        int i0 = dom.search(cal[0]);    // Start of table
        int i1 = dom.search(cal[cal.length - 1]); // End of table
        int frequency = dom.getFrequency().intValue();

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
        Map<Integer, Double[]> mapYoY = new TreeMap<>();
        Map<Integer, Double[]> mapQoQ = new TreeMap<>();

        for (int i = i0; i <= i1; i++) {    // parcours des headers
            for (int j = 0; j < cal.length; j++) {  // parcours du calendrier
                int diff = cal[j].difference(dom.get(i).lastday());
                if (!map.containsKey(diff)) {
                    map.put(diff, new Double[nbHeaders]);
                    mapYoY.put(diff, new Double[nbHeaders]);
                    mapQoQ.put(diff, new Double[nbHeaders]);
                }

                TsDataTableInfo dataInfo = table.getDataInfo(i, j);
                if (dataInfo == TsDataTableInfo.Valid) {
                    Double current = table.getData(i, j);

                    Double growth;

                    try {
                        TsDataTableInfo infoLastYear = table.getDataInfo(i - frequency, j);
                        if (infoLastYear == TsDataTableInfo.Valid) {
                            Double lastYear = table.getData(i - frequency, j); // /!\
                            if (lastYear == 0) {
                                growth = Double.NaN;
                            } else {
                                growth = ((current - lastYear) / lastYear) * 100.00;
                            }
                        } else {
                            growth = Double.NaN;
                        }
                    } catch (TsException ex) {
                        growth = Double.NaN;
                    }

                    map.get(diff)[i - i0] = current;
                    mapYoY.get(diff)[i - i0] = growth;

                    try {
                        TsDataTableInfo infoLastQuarter = table.getDataInfo(i - (frequency / 4), j); // /!\
                        if (infoLastQuarter == TsDataTableInfo.Valid) {
                            Double lastQuarter = table.getData(i - (frequency / 4), j); // /!\
                            if (lastQuarter == 0) {
                                growth = Double.NaN;
                            } else {
                                growth = ((current - lastQuarter) / lastQuarter) * 100;
                            }
                        } else {
                            growth = Double.NaN;
                        }
                    } catch (TsException ex) {
                        growth = Double.NaN;
                    }

                    mapQoQ.get(diff)[i - i0] = growth;
                } else {
                    TsDataTableInfo dataInfoReal = table.getDataInfo(i, table.getSeriesCount() - 1);
                    if (dataInfoReal == TsDataTableInfo.Valid) {
                        Double current = table.getData(i, table.getSeriesCount() - 1);

                        Double growth;
                        try {
                            TsDataTableInfo infoLastYear = table.getDataInfo(i - frequency, table.getSeriesCount() - 1);
                            if (infoLastYear == TsDataTableInfo.Valid) {
                                Double lastYear = table.getData(i - frequency, table.getSeriesCount() - 1);
                                if (lastYear == 0) {
                                    growth = Double.NaN;
                                } else {
                                    growth = ((current - lastYear) / lastYear) * 100.00;
                                }
                            } else {
                                growth = Double.NaN;
                            }
                        } catch (TsException ex) {
                            growth = Double.NaN;
                        }

                        map.get(diff)[i - i0] = current;
                        mapYoY.get(diff)[i - i0] = growth;

                        try {
                            TsDataTableInfo infoLastQuarter = table.getDataInfo(i - (frequency / 4), table.getSeriesCount() - 1);
                            if (infoLastQuarter == TsDataTableInfo.Valid) {
                                Double lastQuarter = table.getData(i - (frequency / 4), table.getSeriesCount() - 1);
                                if (lastQuarter == 0) {
                                    growth = Double.NaN;
                                } else {
                                    growth = ((current - lastQuarter) / lastQuarter) * 100;
                                }
                            } else {
                                growth = Double.NaN;
                            }
                        } catch (TsException ex) {
                            growth = Double.NaN;
                        }

                        mapQoQ.get(diff)[i - i0] = growth;
                    }
                }
            }
        }

        Double[][] array = new Double[map.keySet().size()][];
        Double[][] arrayYoY = new Double[mapYoY.keySet().size()][];
        Double[][] arrayQoQ = new Double[mapQoQ.keySet().size()][];
        Iterator<Integer> keys = map.keySet().iterator();
        int iArray = 0;
        List<Integer> fctsHorizons = new ArrayList<>();

        while (keys.hasNext()) {
            int index = keys.next();
            Double[] values = map.get(index);
            Double[] valuesYoY = mapYoY.get(index);
            Double[] valuesQoQ = mapQoQ.get(index);

            array[iArray] = new Double[nbHeaders];
            arrayYoY[iArray] = new Double[nbHeaders];
            arrayQoQ[iArray] = new Double[nbHeaders];
            System.arraycopy(values, 0, array[iArray], 0, values.length);
            System.arraycopy(valuesYoY, 0, arrayYoY[iArray], 0, valuesYoY.length);
            System.arraycopy(valuesQoQ, 0, arrayQoQ[iArray], 0, valuesQoQ.length);

            iArray++;
            fctsHorizons.add(index);
        }

        List<Integer> filteredHorizons = filterHorizons(fctsHorizons, delay);

        r.setForecastHorizons(filteredHorizons);    // Set forecast horizons (keys)

        // Remplissage des missing values
        fillMissingValues(array);
        fillMissingValues(arrayYoY);
        fillMissingValues(arrayQoQ);

        Double[][] filteredArray = new Double[filteredHorizons.size()][];
        Double[][] filteredArrayYoY = new Double[filteredHorizons.size()][];
        Double[][] filteredArrayQoQ = new Double[filteredHorizons.size()][];
        for (int i = 0; i < filteredHorizons.size(); i++) {
            filteredArray[i] = array[fctsHorizons.indexOf(filteredHorizons.get(i))];
            filteredArrayYoY[i] = arrayYoY[fctsHorizons.indexOf(filteredHorizons.get(i))];
            filteredArrayQoQ[i] = arrayQoQ[fctsHorizons.indexOf(filteredHorizons.get(i))];
        }

        r.setForecastsArray(filteredArray);
        r.setForecastsArrayYoY(filteredArrayYoY);
        r.setForecastsArrayQoQ(filteredArrayQoQ);

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
        List<Double> trueValuesYoY = new ArrayList<>();
        List<Double> trueValuesQoQ = new ArrayList<>();
        writer.append("\r\n").append("REAL");
        for (int i = i0; i <= i1; i++) {
            TsDataTableInfo dataInfo = table.getDataInfo(i, table.getSeriesCount() - 1);
            if (dataInfo == TsDataTableInfo.Valid) {
                Double val = table.getData(i, table.getSeriesCount() - 1);

                try {
                    TsDataTableInfo infoLastYear = table.getDataInfo(i - frequency, table.getSeriesCount() - 1);
                    if (infoLastYear == TsDataTableInfo.Valid) {
                        Double valLastYear = table.getData(i - frequency, table.getSeriesCount() - 1);
                        trueValuesYoY.add(valLastYear == 0 ? Double.NaN : ((val - valLastYear) / valLastYear) * 100.0);
                    } else {
                        trueValuesYoY.add(Double.NaN);
                    }
                } catch (TsException ex) {
                    trueValuesYoY.add(Double.NaN);
                }

                try {
                    TsDataTableInfo infoLastQuarter = table.getDataInfo(i - (frequency / 4), table.getSeriesCount() - 1);
                    if (infoLastQuarter == TsDataTableInfo.Valid) {
                        Double valLastQuarter = table.getData(i - (frequency / 4), table.getSeriesCount() - 1);
                        trueValuesQoQ.add(valLastQuarter == 0 ? Double.NaN : ((val - valLastQuarter) / valLastQuarter) * 100.0);
                    } else {
                        trueValuesQoQ.add(Double.NaN);
                    }
                } catch (TsException ex) {
                    trueValuesQoQ.add(Double.NaN);
                }

                writer.append('\t').append(fmt.format(val));
                trueValues.add(val);

            } else {
                writer.append('\t');
                trueValues.add(null);
                trueValuesYoY.add(null);
                trueValuesQoQ.add(null);
            }
        }

        r.setTrueValues(trueValues);
        r.setTrueValuesYoY(trueValuesYoY);
        r.setTrueValuesQoQ(trueValuesQoQ);

        return r;
    }

    private void fillMissingValues(Double[][] array) {
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
    }

    private List<Integer> filterHorizons(List<Integer> h, int delay) {
        int pos = h.indexOf(delay) - 1;
        if (pos < 0) {
            int i = 0;
            // While absolute diff is <= previous diff
            int diff = Math.abs(h.get(i) - delay);
            while (i < h.size() && Math.abs(h.get(i) - delay) <= diff) {
                diff = Math.abs(h.get(i) - delay);
                i++;
            }
            pos = i - 1;
        }

        List<Integer> newHorizons = new ArrayList<>();
        while (pos >= 0 && h.get(pos) >= -365) {
            newHorizons.add(h.get(pos));
            pos--;
        }
        Collections.sort(newHorizons);
        return newHorizons;
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
                } else if (i != table.getSeriesCount() - 1) {
                    TsDataTableInfo dataInfoReal = table.getDataInfo(j, table.getSeriesCount() - 1);
                    if (dataInfoReal == TsDataTableInfo.Valid) {
                        writer.append('\t').append(fmt.format(table.getData(j, table.getSeriesCount() - 1)));
                    } else {
                        writer.append('\t');
                    }
                } else {
                    writer.append('\t');
                }
            }
        }
    }

    private static final class EditSpecCommand extends JCommand<DfmSimulationTopComponent> {

        public static final EditSpecCommand INSTANCE = new EditSpecCommand();

        @Override
        public boolean isEnabled(DfmSimulationTopComponent c) {
            return c.controller.getSimulationState() != DfmState.STARTED
                    && c.controller.getSimulationState() != DfmState.CANCELLING;
        }

        @Override
        public void execute(DfmSimulationTopComponent c) throws Exception {
            DfmDocument doc = c.getDocument().getElement().getCurrent();
            DfmSpec spec = doc.getSpecification();

            DfmSimulationSpec newValue = spec.getSimulationSpec().clone();
            if (OpenIdePropertySheetBeanEditor.editSheet(DfmSheets.onSimulationSpec(newValue), "Edit spec", null)) {
                doc.getSpecification().setSimulationSpec(newValue);
            }
        }
    }

    private static abstract class DfmExecCommand extends JCommand<DfmSimulationTopComponent> {

        @Override
        public JCommand.ActionAdapter toAction(DfmSimulationTopComponent c) {
            return super.toAction(c).withWeakPropertyChangeListener(c, SIMULATION_STATE_PROPERTY);
        }
    }

    private static final class StartStopCommand extends DfmExecCommand {

        public static final StartStopCommand INSTANCE = new StartStopCommand();

        @Override
        public void execute(DfmSimulationTopComponent c) throws Exception {
            if (c.controller.getSimulationState() == DfmState.STARTED) {
                c.controller.setSimulationState(DfmState.CANCELLING);
            } else if (c.getDocument() != null && !c.getDocument().isReadOnly()) {
                if (c.getDocument().getElement() instanceof VersionedDfmDocument) {
                    if (c.controller.getDfmState() != DfmState.DONE) {
                        JOptionPane.showMessageDialog(null, "DFM processing must be done before the simulation !", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    VersionedDfmDocument vd = c.getDocument().getElement();
                    List<MeasurementSpec> m = vd.getSpecification().getModelSpec().getMeasurements();
                    int count = 0;
                    int i = 0;
                    while (count == 0 && i < m.size()) {
                        if (m.get(i).isWatched()) {
                            count++;
                        }
                        i++;
                    }

                    if (count == 0) {
                        JOptionPane.showMessageDialog(null, "You must select at least one series for data generation !\nRight click on a series to enable/disable data generation.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    chooser.setDialogTitle("Select the output folder");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setAcceptAllFileFilterUsed(false);

                    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                        return;
                    }

                    c.controller.setSimulationState(DfmState.STARTED);
                }
            }
        }

        @Override
        public boolean isEnabled(DfmSimulationTopComponent c) {
            return c.controller.getSimulationState() != DfmState.CANCELLING;
        }

        @Override
        public boolean isSelected(DfmSimulationTopComponent c) {
            return c.controller.getSimulationState() == DfmState.STARTED;
        }
    }
}
