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
package ec.tstoolkit.dfm;

import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.mssf2.IMSsf;
import ec.tstoolkit.mssf2.MFilteringResults;
import ec.tstoolkit.mssf2.MSmoother;
import ec.tstoolkit.mssf2.MSmoothingResults;
import ec.tstoolkit.mssf2.MultivariateSsfData;
import ec.tstoolkit.timeseries.simplets.TsData;

/**
 *
 * @author Jean Palate
 */
public class DfmMonitor {

    private final DynamicFactorModel model_;
    private MSmoothingResults srslts_;
    private MFilteringResults frslts_;
    private DfmInformationSet input_;
    private boolean bvar_; // false by default
    private boolean normalize_ = true;

    /**
     * Creates a new monitor for a given model
     *
     * @param model The model used by the monitor.
     * @throws DfmException An exception is thrown when the given model is null
     * or invalid
     */
    public DfmMonitor(DynamicFactorModel model) throws DfmException {
        if (model == null) {
            throw new DfmException(DfmException.INVALID_MODEL);
        }
        model_ = model;
    }
    
    public boolean isNormalizingVariance(){
        return normalize_;
    }

    public void setNormalizingVariance(boolean n){
        normalize_=n;
    }
    /**
     * Retrieves the current data used by the monitor
     *
     * @return The Current data. May be null.
     */
    public DfmInformationSet getInput() {
        return input_;
    }

    /**
     * Retrieves the model of the monitor
     *
     * @return The model of the monitor. Never null.
     */
    public DynamicFactorModel getModel() {
        return model_;
    }

    /**
     * /**
     * Processes the model with the given data. This method uses the current
     * parameters of the model
     *
     * @param data The new data. The number of series should correspond to the
     * number of measurement equations.
     * @return True if the data have been successfully processed, false
     * otherwise.
     * @throws DfmException An exception is thrown when the number of series
     * doesn't correspond to the number of measurement equations in the model.
     */
    public boolean process(TsData[] data) throws DfmException {
        try {
            clear();
            input_ = new DfmInformationSet(data);
            Matrix M = input_.generateMatrix(null);
            if (M.getColumnsCount() != model_.getMeasurementsCount()) {
                throw new DfmException(DfmException.INCOMPATIBLE_DATA);
            }
            MSmoother smoother = new MSmoother();
            srslts_ = new MSmoothingResults();
            smoother.setCalcVariance(bvar_);
            IMSsf ssf = model_.ssfRepresentation();
            smoother.process(ssf, new MultivariateSsfData(M.subMatrix().transpose(), null), srslts_);
            frslts_=smoother.getFilteringResults();
            if (normalize_) {
                srslts_.setStandardError(1);
            }
            return true;
        } catch (Exception err) {
            srslts_ = null;
            return false;
        }
    }

    /**
     * Retrieves the smoothing results
     *
     * @return The Smoothing results. May by null.
     */
    public MSmoothingResults getSmoothingResults() {
        return srslts_;
    }
    
    public MFilteringResults getFilteringResults(){
        return frslts_;
    }

    /**
     * Flag indicating that the variance of the smoothed states has been
     * estimated.
     *
     * @return True if the variance of the smoothed states has been estimated,
     * false otherwise. False by default
     */
    public boolean isCalcVariance() {
        return bvar_;
    }

    /**
     * Specifies the computation or not of the variance of the smoothed states.
     * By default, the computation of the variance is not enabled. In the case
     * of large models, the computation of the variance may be expensive.
     *
     * @param var True if the variance of the smoothed states has to be
     * estimated, false otherwise.
     */
    public void setCalcVariance(boolean var) {
        bvar_ = var;
    }

    private void clear() {
        srslts_ = null;
        input_ = null;
    }
}
