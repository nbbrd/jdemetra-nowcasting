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
import ec.tstoolkit.design.Development;
import ec.tstoolkit.ssf2.ResidualsCumulator;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class MFilteringResults extends ResidualsCumulator implements
        IMFilteringResults {

    private final VarianceMFilter m_var = new VarianceMFilter();
    private final MFilteredData m_fdata = new MFilteredData();

    /**
     *
     */
    public MFilteringResults() {
    }

    /**
     *
     */
    @Override
    public void clear() {
        super.clear();
        m_fdata.clear();
        m_var.clear();
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
    public MFilteredData getFilteredData() {
        return m_fdata;
    }

    /**
     *
     * @return
     */
    public VarianceMFilter getVarianceFilter() {
        return m_var;
    }

    /**
     *
     * @param ssf
     * @param data
     */
    @Override
    public void prepare(final IMSsf ssf, final IMSsfData data) {
        if (m_var.isOpen()) {
            m_fdata.checkSize(data.getCount());
        } else {
            super.clear();
            m_fdata.init(ssf.getStateDim(), data.getVarsCount(), data.getCount());
        }
        m_var.prepare(ssf, data);
    }

    /**
     *
     * @param t
     * @param state
     */
    @Override
    public void save(final int t, final MState state) {
        m_var.save(t, state);
        m_fdata.save(t, state);
        DataBlock diag = state.F.diagonal();
        for (int i = 0; i < state.E.getLength(); ++i) {
            double r = diag.get(i);
            if (r != 0) {
                addStd(state.E.get(i), r);
            }
        }
    }

    /**
     *
     * @param start
     */
    public void setStartSaving(int start) {
        m_var.setStartSaving(start);
    }

    /**
     *
     * @param start
     */
    public void saveAll(int start) {
        m_var.setSavingK(true);
        m_var.setSavingP(true);
        m_var.setStartSaving(start);
    }
}
