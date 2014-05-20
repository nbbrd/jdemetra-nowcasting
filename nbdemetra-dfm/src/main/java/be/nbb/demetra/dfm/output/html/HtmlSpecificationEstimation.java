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

import ec.tss.html.AbstractHtmlElement;
import ec.tss.html.HtmlStream;
import ec.tss.html.HtmlStyle;
import ec.tss.html.HtmlTable;
import ec.tss.html.HtmlTableCell;
import ec.tss.html.HtmlTag;
import ec.tss.html.IHtmlElement;
import ec.tstoolkit.dfm.DfmEstimationSpec;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 *
 * @author Mats Maggi
 */
public class HtmlSpecificationEstimation extends AbstractHtmlElement implements IHtmlElement {

    private final DfmEstimationSpec estimationSpec;
    private final int COL_SIZE = 250;
    private final int VAL_SIZE = 150;
    private DecimalFormat df2 = new DecimalFormat();

    public HtmlSpecificationEstimation(DfmEstimationSpec estimationSpec) {
        this.estimationSpec = estimationSpec;
        
        df2.setMaximumFractionDigits(2);
    }

    @Override
    public void write(HtmlStream stream) throws IOException {
        if (estimationSpec != null) {
            writePrincipalComponents(stream);    
            writePreliminaryEM(stream);
            writeNumericalOptimization(stream);
            writeFinalEM(stream);
        }
    }

    private void writePrincipalComponents(HtmlStream stream) throws IOException {
        stream.write(HtmlTag.HEADER2, h2, "Principal components").newLine();

        stream.open(new HtmlTable(1, COL_SIZE+VAL_SIZE));
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Enabled", COL_SIZE, HtmlStyle.Left));
        boolean enabled = estimationSpec.getPrincipalComponentsSpec().isEnabled();
        stream.write(new HtmlTableCell(enabled ? "Yes" : "No", VAL_SIZE, enabled ? HtmlStyle.Success : HtmlStyle.Red, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);

        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Estimation span", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(estimationSpec.getPrincipalComponentsSpec().getSpan().toString(), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Data availability (min %)", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(df2.format(estimationSpec.getPrincipalComponentsSpec().getMinPartNonMissingSeries()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);

        stream.close(HtmlTag.TABLE);
    }

    private void writePreliminaryEM(HtmlStream stream) throws IOException {
        stream.newLine().write(HtmlTag.HEADER2, h2, "Preliminary EM").newLine();

        stream.open(new HtmlTable(1, COL_SIZE+VAL_SIZE));
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Enabled", COL_SIZE, HtmlStyle.Left));
        boolean enabled = estimationSpec.getPreEmSpec().isEnabled();
        stream.write(new HtmlTableCell(enabled ? "Yes" : "No", VAL_SIZE, enabled ? HtmlStyle.Success : HtmlStyle.Red, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Version", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(String.valueOf(estimationSpec.getPreEmSpec().getVersion()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Max iterations", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(String.valueOf(estimationSpec.getPreEmSpec().getMaxIter()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Max numerical iterations", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(String.valueOf(estimationSpec.getPreEmSpec().getMaxNumIter()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.close(HtmlTag.TABLE);
    }
    
    private void writeNumericalOptimization(HtmlStream stream) throws IOException {
        stream.newLine().write(HtmlTag.HEADER2, h2, "Numerical Optimization").newLine();
        
        stream.open(new HtmlTable(1, COL_SIZE+VAL_SIZE));
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Enabled", COL_SIZE, HtmlStyle.Left));
        boolean enabled = estimationSpec.getNumericalProcessingSpec().isEnabled();
        stream.write(new HtmlTableCell(enabled ? "Yes" : "No", VAL_SIZE, enabled ? HtmlStyle.Success : HtmlStyle.Red, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Max number iterations", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(String.valueOf(estimationSpec.getNumericalProcessingSpec().getMaxIter()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Max number iterations in optimization by block", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(String.valueOf(estimationSpec.getNumericalProcessingSpec().getMaxIntermediateIter()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Simplified model iterations", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(String.valueOf(estimationSpec.getNumericalProcessingSpec().getMaxInitialIter()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Independent VAR shocks", COL_SIZE, HtmlStyle.Left));
        boolean independent = estimationSpec.getNumericalProcessingSpec().isIndependentVarShocks();
        stream.write(new HtmlTableCell(independent ? "Yes" : "No", VAL_SIZE, independent ? HtmlStyle.Success : HtmlStyle.Red, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Iterations by block", COL_SIZE, HtmlStyle.Left));
        boolean iterByBlocks = estimationSpec.getNumericalProcessingSpec().isBlockIterations();
        stream.write(new HtmlTableCell(iterByBlocks ? "Yes" : "No", VAL_SIZE, iterByBlocks ? HtmlStyle.Success : HtmlStyle.Red, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Mixed estimation", COL_SIZE, HtmlStyle.Left));
        boolean mixed = estimationSpec.getNumericalProcessingSpec().isMixedEstimation();
        stream.write(new HtmlTableCell(mixed ? "Yes" : "No", VAL_SIZE, mixed ? HtmlStyle.Success : HtmlStyle.Red, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Optimization method", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(estimationSpec.getNumericalProcessingSpec().getMethod().toString(), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Precision", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(String.valueOf(estimationSpec.getNumericalProcessingSpec().getPrecision()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.close(HtmlTag.TABLE);
    }
    
    private void writeFinalEM(HtmlStream stream) throws IOException {
        stream.newLine().write(HtmlTag.HEADER2, h2, "Final EM").newLine();
        
        stream.open(new HtmlTable(1, COL_SIZE+VAL_SIZE));
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Enabled", COL_SIZE, HtmlStyle.Left));
        boolean enabled = estimationSpec.getPostEmSpec().isEnabled();
        stream.write(new HtmlTableCell(enabled ? "Yes" : "No", VAL_SIZE, enabled ? HtmlStyle.Success : HtmlStyle.Red, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Version", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(String.valueOf(estimationSpec.getPostEmSpec().getVersion()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Max iterations", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(String.valueOf(estimationSpec.getPostEmSpec().getMaxIter()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Max numerical iterations", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(String.valueOf(estimationSpec.getPostEmSpec().getMaxNumIter()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.open(HtmlTag.TABLEROW);
        stream.write(new HtmlTableCell("Precision", COL_SIZE, HtmlStyle.Left));
        stream.write(new HtmlTableCell(String.valueOf(estimationSpec.getPostEmSpec().getPrecision()), VAL_SIZE, HtmlStyle.Left));
        stream.close(HtmlTag.TABLEROW);
        
        stream.close(HtmlTag.TABLE);
    }

}
