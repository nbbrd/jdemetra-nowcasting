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

import be.nbb.demetra.dfm.Arrays3;
import ec.tss.dfm.DfmDocument;
import ec.tss.dfm.DfmResults;
import ec.tss.html.AbstractHtmlElement;
import ec.tss.html.HtmlStream;
import ec.tss.html.HtmlStyle;
import ec.tss.html.HtmlTable;
import ec.tss.html.HtmlTableCell;
import ec.tss.html.HtmlTag;
import ec.tss.html.IHtmlElement;
import ec.tstoolkit.ParameterType;
import ec.tstoolkit.dfm.DfmModelSpec;
import ec.tstoolkit.dfm.MeasurementSpec;
import java.io.IOException;

/**
 *
 * @author Mats Maggi
 */
public class HtmlSpecificationModel extends AbstractHtmlElement implements IHtmlElement {

    private final DfmDocument doc;
    
    public HtmlSpecificationModel(DfmDocument doc) {
        this.doc = doc;
    }
    
    @Override
    public void write(HtmlStream stream) throws IOException {
        if (doc != null) {
            DfmResults rslts = doc.getDfmResults();
            if (rslts != null) {
                // Inputs :
                int nbFactors = rslts.getModel().getFactorsCount();
                final int SERIES_SIZE = 120;
                final int TRANSF_SIZE = 100;
                final int FACTOR_SIZE = 20;
                
                stream.open(new HtmlTable(1, SERIES_SIZE+TRANSF_SIZE + (FACTOR_SIZE*20)));
                
                // Headers
                stream.open(HtmlTag.TABLEROW);
                stream.write(new HtmlTableCell("Series", SERIES_SIZE, HtmlStyle.Bold, HtmlStyle.Left));
                stream.write(new HtmlTableCell("Series trans.", TRANSF_SIZE, HtmlStyle.Bold, HtmlStyle.Center));
                stream.write(new HtmlTableCell("Factor trans.", TRANSF_SIZE, HtmlStyle.Bold, HtmlStyle.Center));
                for (int i = 0; i < nbFactors; i++) {
                    stream.write(new HtmlTableCell("F" + (i+1), FACTOR_SIZE, HtmlStyle.Bold, HtmlStyle.Center));
                }
                stream.close(HtmlTag.TABLEROW);
                
                DfmModelSpec spec = doc.getSpecification().getModelSpec();
                for (int i = 0; i < rslts.getDescriptions().length; i++) {
                    stream.open(HtmlTag.TABLEROW);
                    
                    stream.write(new HtmlTableCell(rslts.getDescription(i).description, SERIES_SIZE, HtmlStyle.Left));
                    MeasurementSpec ms = spec.getMeasurements().get(i);
                    stream.write(new HtmlTableCell(Arrays3.enumToString(ms.getSeriesTransformations()), TRANSF_SIZE, HtmlStyle.Center));
                    stream.write(new HtmlTableCell(ms.getFactorsTransformation().toString(), TRANSF_SIZE, HtmlStyle.Center));
                    for (int j = 0; j < nbFactors; j++) {
                        boolean selected = ms.getCoefficients()[j].getType() != ParameterType.Fixed;
                        stream.write(new HtmlTableCell(selected ? "X" : "", FACTOR_SIZE, HtmlStyle.Center));
                    }
                    
                    stream.close(HtmlTag.TABLEROW);
                }
                
                stream.close(HtmlTag.TABLE);
                
                stream.newLines(2);
                
                // Parameters :
                stream.write("Lags count : ", HtmlStyle.Bold);
                stream.write(spec.getVarSpec().getLagsCount()).newLine();
                stream.write("Initialization : ", HtmlStyle.Bold);
                stream.write(spec.getVarSpec().getInitialization().toString()).newLine();
                stream.write("Forecast horizon : ", HtmlStyle.Bold);
                stream.write(spec.getForecastHorizon()).newLine();
                
            }
        }
    }

}
