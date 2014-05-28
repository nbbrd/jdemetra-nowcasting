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
package be.nbb.demetra.dfm.output.html;

import com.google.common.base.Optional;
import ec.tss.dfm.DfmResults;
import ec.tss.html.AbstractHtmlElement;
import ec.tss.html.HtmlStream;
import ec.tss.html.HtmlStyle;
import ec.tss.html.HtmlTable;
import ec.tss.html.HtmlTableCell;
import ec.tss.html.HtmlTag;
import ec.tss.html.IHtmlElement;
import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.timeseries.simplets.TsData;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Mats Maggi
 */
public class HtmlFitResiduals extends AbstractHtmlElement implements IHtmlElement {

    private final Optional<DfmResults> results;

    private final int SERIES_SIZE = 500;
    private final int NB_SIZE = 50;

    public HtmlFitResiduals(Optional<DfmResults> rslts) {
        this.results = rslts;
    }

    @Override
    public void write(HtmlStream stream) throws IOException {
        if (results.isPresent() && results.get() != null) {
            writeTables(stream);
        }
    }

    private void writeTables(HtmlStream stream) throws IOException {
        DfmResults rslts = results.get();
        List<DynamicFactorModel.MeasurementDescriptor> descriptors = rslts.getModel().getMeasurements();
        int nbSeries = rslts.getDescriptions().length;

        stream.open(new HtmlTable(0, SERIES_SIZE + (NB_SIZE * 2)));

        // HEADERS
        stream.open(HtmlTag.TABLEROW);
        HtmlTableCell serieHeader = new HtmlTableCell("", SERIES_SIZE);
        serieHeader.rowspan = 2;
        stream.write(serieHeader);

        HtmlTableCell stdDevHeader = new HtmlTableCell("Stdev", SERIES_SIZE, HtmlStyle.Bold, HtmlStyle.Center);
        stdDevHeader.rowspan = 2;
        stream.write(stdDevHeader);

        stream.write(new HtmlTableCell("Autocorrelation", SERIES_SIZE, HtmlStyle.Bold, HtmlStyle.Center));

        stream.close(HtmlTag.TABLEROW);
        stream.open(HtmlTag.TABLEROW);

        HtmlTableCell estimateHeader = new HtmlTableCell("Estimate", SERIES_SIZE, HtmlStyle.Bold, HtmlStyle.Center);
        stream.write(estimateHeader);

        stream.close(HtmlTag.TABLEROW);

        // DATA
        for (int i = 0; i < nbSeries; i++) {
            stream.open(HtmlTag.TABLEROW);
            stream.write(new HtmlTableCell(rslts.getDescription(i).description, SERIES_SIZE, HtmlStyle.Left));

            double stdev = Math.sqrt(descriptors.get(i).var);
            stream.write(new HtmlTableCell(Double.isNaN(stdev) ? "" : df2.format(stdev), SERIES_SIZE, HtmlStyle.Center));

            int obsCount = rslts.getTheData()[i].getObsCount();

            double rho = calculateRho(rslts.getNoise()[i]);
            stream.write(new HtmlTableCell(Double.isNaN(rho) ? "" : df2.format(rho), SERIES_SIZE, HtmlStyle.Center, getRhoColor(rho, obsCount)));

            stream.close(HtmlTag.TABLEROW);
        }

        stream.close(HtmlTag.TABLE);
    }
    
    private HtmlStyle getRhoColor(double rho, int obsCount) {
        double confidence95 = 1.96/Math.sqrt(obsCount);
        double confidence90 = 1.645/Math.sqrt(obsCount);
        double confidence80 = 1.28/Math.sqrt(obsCount);
        
        if (Math.abs(rho) >= confidence95) {
            return HtmlStyle.Red;
        } else if (Math.abs(rho) >= confidence90) {
            return HtmlStyle.DarkOrange;
        } else if (Math.abs(rho) >= confidence80) {
            return HtmlStyle.Yellow;
        } else {
            return HtmlStyle.Black;
        }
    }

    private double calculateRho(TsData d) {
        double[] rawData = new double[d.getLength()];
        d.copyTo(rawData, 0);
        DescriptiveStatistics ds = new DescriptiveStatistics(rawData);
        double var = ds.getVar();
        if (var == 0) {
            return Double.NaN;
        }
        return DescriptiveStatistics.cov(rawData, rawData, 1) / var;
    }
}
