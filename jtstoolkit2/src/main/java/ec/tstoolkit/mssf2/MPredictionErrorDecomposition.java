/*
 * Copyright 2013 National Bank of Belgium
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
package ec.tstoolkit.mssf2;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockStorage;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.LowerTriangularMatrix;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.ssf2.ResidualsCumulator;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class MPredictionErrorDecomposition extends ResidualsCumulator implements
        IMFilteringResults, IArrayFilteringResults {

    private DataBlockStorage m_res;
    private boolean m_bres;

    /**
     *
     * @param bres
     */
    public MPredictionErrorDecomposition(final boolean bres) {
        m_bres = bres;
    }

    /**
     *
     */
    @Override
    public void close() {
    }

    /**
     *
     * @return
     */
    public boolean hasResiduals() {
        return m_bres;
    }
    
    public double[] allResiduals(){
        return m_res.storage();
    }

    /**
     *
     * @param ssf
     * @param data
     */
    @Override
    public void prepare(final IMSsf ssf, final IMSsfData data) {
        clear();
        if (m_bres) {
            m_res = new DataBlockStorage(data.getVarsCount(), data.getCount());
        }
    }

    /**
     *
     * @param pos 
     * @return
     */
    public DataBlock residuals(int pos) {
        return m_res.block(pos);
    }

    /**
     *
     * @param t
     * @param state
     */
    @Override
    public void save(final int t, final ArrayState state) {
        bsave(t,state);
    }
    
   @Override
    public void save(final int t, final MState state) {
        bsave(t,state);
    }

   private void bsave(final int t, final BaseOrdinaryMState state) {
        m_res.save(t, state.E);
        DataBlock diag = state.F.diagonal();
        for (int i = 0; i < state.E.getLength(); ++i) {
            double r = diag.get(i);
            if (r != 0) {
                addStd(state.E.get(i), r);
            }
        }
    }

 }
