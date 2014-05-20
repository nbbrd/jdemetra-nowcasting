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
import ec.tstoolkit.dfm.DynamicFactorModel;
import ec.tstoolkit.maths.matrices.Matrix;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

/**
 *
 * @author Mats Maggi
 */
public class HtmlEstimationModel extends AbstractHtmlElement implements IHtmlElement {

    private final Optional<DfmResults> results;
    private final DecimalFormat df5 = new DecimalFormat();

    private final int SERIES_SIZE = 150;
    private final int NB_SIZE = 50;

    public HtmlEstimationModel(Optional<DfmResults> rslts) {
        this.results = rslts;
        df5.setMaximumFractionDigits(5);
        df5.setMinimumFractionDigits(5);
    }

    @Override
    public void write(HtmlStream stream) throws IOException {
        if (results.isPresent() && results.get() != null) {
            writeLoadings(stream);
            writeVARModel(stream);
            writeInnovativeVariance(stream);
        }
    }

    private void writeLoadings(HtmlStream stream) throws IOException {
        DfmResults rslts = results.get();
        int nbFactors = rslts.getModel().getFactorsCount();
        List<DynamicFactorModel.MeasurementDescriptor> descriptors = rslts.getModel().getMeasurements();

        stream.write(HtmlTag.HEADER2, h2, "Loadings").newLine();

        stream.open(new HtmlTable(1, (SERIES_SIZE*2) + ((nbFactors + 1) * NB_SIZE)));

        // HEADERS
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Series", SERIES_SIZE, HtmlStyle.Bold, HtmlStyle.Left));
        for (int i = 0; i < nbFactors; i++) {
            stream.write(new HtmlTableCell("F" + (i+1), NB_SIZE, HtmlStyle.Bold, HtmlStyle.Center));
        }
        stream.write(new HtmlTableCell("Variance", NB_SIZE, HtmlStyle.Bold, HtmlStyle.Center));
        stream.close(HtmlTag.TABLEROW);

        // DATA
        for (int i = 0; i < rslts.getDescriptions().length; i++) {
            stream.open(HtmlTag.TABLEROW);
            stream.write(new HtmlTableCell(rslts.getDescription(i).description, SERIES_SIZE, HtmlStyle.Left));
            for (int j = 0; j < descriptors.get(i).coeff.length; j++) {
                String coeff = (Double.isNaN(descriptors.get(i).coeff[j])) ? "" : df5.format(descriptors.get(i).coeff[j]);
                stream.write(new HtmlTableCell(coeff, NB_SIZE, HtmlStyle.Right));
            }
            stream.write(new HtmlTableCell(df5.format(descriptors.get(i).var), NB_SIZE, HtmlStyle.Right));
            stream.close(HtmlTag.TABLEROW);
        }

        stream.close(HtmlTag.TABLE);
    }

    private void writeVARModel(HtmlStream stream) throws IOException {
        DfmResults rslts = results.get();
        int nbFactors = rslts.getModel().getFactorsCount();
        Matrix m = rslts.getModel().getTransition().varParams;
        int nbLags = rslts.getModel().getTransition().nlags;

        stream.newLine().write(HtmlTag.HEADER2, h2, "VAR Model").newLine();

        stream.open(new HtmlTable(1, (m.getColumnsCount() + 1) * NB_SIZE));

        // HEADERS
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("", NB_SIZE, HtmlStyle.Bold, HtmlStyle.Left));
        for (int i = 0; i < nbFactors; i++) {
            stream.write(new HtmlTableCell("F" + (i+1) + "(-1)", NB_SIZE, HtmlStyle.Bold, HtmlStyle.Center));
        }
        for (int i = 0; i < nbFactors; i++) {
            stream.write(new HtmlTableCell("F" + (i+1) + "(-" + nbLags + ")", NB_SIZE, HtmlStyle.Bold, HtmlStyle.Center));
        }
        stream.close(HtmlTag.TABLEROW);

        // DATA
        for (int i = 0; i < m.getRowsCount(); i++) {
            stream.open(HtmlTag.TABLEROW);
            stream.write(new HtmlTableCell("F" + (i+1), NB_SIZE, HtmlStyle.Left, HtmlStyle.Bold));
            for (int j = 0; j < m.getColumnsCount(); j++) {
                String value = (Double.isNaN(m.get(i, j))) ? "" : df5.format(m.get(i, j));
                stream.write(new HtmlTableCell(value, NB_SIZE, HtmlStyle.Right));
            }
            stream.close(HtmlTag.TABLEROW);
        }

        stream.close(HtmlTag.TABLE);
    }

    private void writeInnovativeVariance(HtmlStream stream) throws IOException {
        DfmResults rslts = results.get();
        int nbFactors = rslts.getModel().getFactorsCount();
        Matrix m = rslts.getModel().getTransition().covar;

        stream.newLine().write(HtmlTag.HEADER2, h2, "Innovative Variance").newLine();

        stream.open(new HtmlTable(1, (nbFactors + 1) * NB_SIZE));

        // HEADERS
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("", NB_SIZE, HtmlStyle.Bold, HtmlStyle.Left));
        for (int i = 0; i < m.getColumnsCount(); i++) {
            stream.write(new HtmlTableCell("F" + (i+1), NB_SIZE, HtmlStyle.Bold, HtmlStyle.Center));
        }
        stream.close(HtmlTag.TABLEROW);

        // DATA
        for (int i = 0; i < m.getRowsCount(); i++) {
            stream.open(HtmlTag.TABLEROW);
            stream.write(new HtmlTableCell("F" + (i+1), NB_SIZE, HtmlStyle.Left, HtmlStyle.Bold));
            for (int j = 0; j < m.getColumnsCount(); j++) {
                String value = (Double.isNaN(m.get(i, j))) ? "" : df5.format(m.get(i, j));
                stream.write(new HtmlTableCell(value, NB_SIZE, HtmlStyle.Right));
            }
            stream.close(HtmlTag.TABLEROW);
        }

        stream.close(HtmlTag.TABLE);
    }

}
