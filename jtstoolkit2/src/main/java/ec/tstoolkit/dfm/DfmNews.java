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

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockIterator;
import ec.tstoolkit.dfm.DfmInformationUpdates.Update;
import ec.tstoolkit.maths.matrices.LowerTriangularMatrix;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import ec.tstoolkit.mssf2.IMSsf;
import ec.tstoolkit.mssf2.MFilter;
import ec.tstoolkit.mssf2.MFilteringResults;
import ec.tstoolkit.mssf2.MSmoother;
import ec.tstoolkit.mssf2.MSmoothingResults;
import ec.tstoolkit.mssf2.MultivariateSsfData;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.List;

/**
 * Computation of the news (See Banbura an Modagno, appendix D of the reference
 * paper for further details)
 *
 * @author Jean Palate
 */
public class DfmNews {

    private MSmoothingResults srslts0_, srslts1_;
    private final DynamicFactorModel model_;
    private DynamicFactorModel modelex_;
    private final IMSsf ssf_;
    private DfmInformationSet oldset_, newset_;
    private DfmInformationUpdates updates_;
    private Matrix mcov_, lcov_;
    private TsPeriod first_, last_;
    /**
     * "Complete" domain, which is the union of the domains of the old
     * information set and of the new information set
     */
    private TsDomain fullDomain_;
    /**
     * domain containing all the news
     */
    private TsDomain nDomain_;

    /**
     *
     * @param model
     */
    public DfmNews(DynamicFactorModel model) {
        model_ = model;
        ssf_ = model_.ssfRepresentation();
    }

    public DfmInformationSet getOldInformationSet() {
        return this.oldset_;
    }

    public DfmInformationSet getNewInformationSet() {
        return this.newset_;
    }

    public DynamicFactorModel getModel() {
        return model_;
    }

    /**
     * Computes the news between two consecutive information set
     *
     * @param oldSet The old information set
     * @param newSet The new information set
     * @return True if the news have been successfully computed
     */
    public boolean process(DfmInformationSet oldSet, DfmInformationSet newSet) {
        oldset_ = oldSet;
        newset_ = newSet;
        updates_ = oldset_.updates(newset_);
        if (updates_.updates().isEmpty()) {
            return false;
        }
        computeDomains();
        Matrix M = oldset_.generateMatrix(fullDomain_);
        if (!smoothOldData(M)) {
            return false;
        }
        updateNews();
        if (!smoothNewDataEx(M)) {
            return false;
        }
        computeNewsCovariance();
        return true;
    }

    public double getOldForecast(int series, TsPeriod p) {
        int pos = p.lastPeriod(fullDomain_.getFrequency()).minus(fullDomain_.getStart());
        return ssf_.ZX(pos, series, srslts0_.A(pos));
    }

    public double getNewForecast(int series, TsPeriod p) {
        int pos = p.lastPeriod(fullDomain_.getFrequency()).minus(fullDomain_.getStart());
        return ssf_.ZX(pos, series, getNewSmoothingResults().A(pos));
    }

    private void computeDomains() {
        TsDomain idomain = oldset_.getCurrentDomain();
        nDomain_ = updates_.updatesDomain(idomain.getFrequency());
        for (int i=0; i<oldset_.getSeriesCount(); ++i){
            TsData s=oldset_.series(i);
            int j=s.getLength();
            while (j>0 && s.isMissing(j-1))
                --j;
            TsPeriod last=s.getStart().plus(j).lastPeriod(nDomain_.getFrequency());
            int n=nDomain_.getStart().minus(last);
            if (n > 0)
                nDomain_=nDomain_.extend(n, 0);
        }
        fullDomain_ = idomain.union(newset_.getCurrentDomain());
        last_ = fullDomain_.getLast();
        first_ = fullDomain_.getStart();
    }

    public TsDomain getDomain() {
        return nDomain_;
    }

    /**
     * Computes the smoothed states corresponding to the old data set. The
     * variance of the smoothed states are not computed. The states are saved
     * till the period corresponding to the domain of the news.
     *
     * @param M
     */
    private boolean smoothOldData(Matrix M) {
        MultivariateSsfData ssfData = new MultivariateSsfData(M.subMatrix().transpose(), null);
        MSmoother smoother = new MSmoother();
        srslts0_ = new MSmoothingResults();
        int last = fullDomain_.search(nDomain_.getStart());
        srslts0_.setSavingStart(last);
        smoother.setStopPosition(last);
        smoother.setCalcVariance(false);
        return smoother.process(ssf_, ssfData, srslts0_);
    }

    private boolean smoothNewData(Matrix M) {
        MultivariateSsfData ssfData = new MultivariateSsfData(M.subMatrix().transpose(), null);
        MSmoother smoother = new MSmoother();
        srslts1_ = new MSmoothingResults();
        int last = fullDomain_.search(nDomain_.getStart());
        srslts1_.setSavingStart(last);
        smoother.setStopPosition(last);
        smoother.setCalcVariance(false);
        return smoother.process(ssf_, ssfData, srslts1_);
    }

    /**
     * Updates the news with the forecasts computed on the old data
     */
    private void updateNews() {
        TsFrequency freq = first_.getFrequency();
        for (Update update : updates_.updates()) {

            update.y = newset_.series(update.series).get(update.period);
            int pos = update.period.lastPeriod(freq).minus(first_);
            update.fy = ssf_.ZX(pos, update.series, srslts0_.A(pos));
        }
    }

    /**
     * Computes the smoothed states of the extended model
     *
     * @return
     */
    private boolean smoothNewDataEx(Matrix M) {
        // extends the model
        modelex_ = model_.clone();
        modelex_.setBlockLength(model_.getBlockLength() + last_.minus(nDomain_.getStart()) + 1);
        int last = fullDomain_.getLength() - 1;
        IMSsf ssf = modelex_.ssfRepresentation();
        MFilteringResults frslts = new MFilteringResults();
        frslts.saveAll(last);
        MFilter filter = new MFilter();
        MultivariateSsfData ssfData = new MultivariateSsfData(M.subMatrix().transpose(), null);
        filter.process(ssf, ssfData, frslts);
        MSmoother smoother = new MSmoother();
        smoother.setStopPosition(last);
        smoother.setCalcVariance(true);
        MSmoothingResults srslts = new MSmoothingResults();
        srslts.setSavingStart(last);
        smoother.process(ssf, ssfData, frslts, srslts);
        mcov_ = new Matrix(srslts.P(last));
        return true;
    }

    /**
     *
     * @return
     */
    public MSmoothingResults getOldSmoothingResults() {
        return srslts0_;
    }

    /**
     *
     * @return
     */
    public MSmoothingResults getNewSmoothingResults() {
        if (srslts1_ == null) {
            Matrix M = newset_.generateMatrix(fullDomain_);
            smoothNewData(M);
        }
        return srslts1_;
    }

    /**
     *
     * @return
     */
    public IMSsf getSsf() {
        return ssf_;
    }

    private void computeNewsCovariance() {
        TsFrequency freq = last_.getFrequency();
        List<Update> updates = updates_.updates();
        int n = updates.size();
        int c = model_.getBlockLength();
        int xc = modelex_.getBlockLength();
        int nb = model_.getFactorsCount();
        lcov_ = new Matrix(n, n);
        int d = ssf_.getStateDim();
        Matrix V = new Matrix(d, d);
        DataBlockIterator vcols = V.columns();
        DataBlock vcol = vcols.getData();
        DataBlock tmp = new DataBlock(d);
        for (int i = 0; i < n; ++i) {
            Update iupdate = updates.get(i);
            int istart = last_.minus(iupdate.period.lastPeriod(freq));
            for (int j = 0; j <= i; ++j) {
                Update jupdate = updates.get(j);
                // copy the right covariance
                int jstart = last_.minus(jupdate.period.lastPeriod(freq));
                V.set(0);
                for (int r = 0; r < nb; ++r) {
                    for (int s = 0; s < nb; ++s) {
                        V.subMatrix(r * c, r * c + c, s * c, s * c + c).copy(
                                mcov_.subMatrix(r * xc + istart, r * xc + istart + c,
                                        s * xc + jstart, s * xc + jstart + c));
                    }
                }

                tmp.set(0);
                vcols.begin();
                do {
                    tmp.set(vcols.getPosition(), ssf_.ZX(0, iupdate.series, vcol));
                } while (vcols.next());
                double q = ssf_.ZX(0, jupdate.series, tmp);
                if (i == j) {
                    q += model_.getMeasurements().get(iupdate.series).var;
                }
                lcov_.set(i, j, q);
            }
        }
        SymmetricMatrix.fromLower(lcov_);
        SymmetricMatrix.lcholesky(lcov_, MFilter.Zero);
    }

    /**
     *
     * @return
     */
    public Matrix getStateCovariance() {
        return mcov_;
    }

    /**
     *
     * @return
     */
    public DfmInformationUpdates newsDetails() {
        return updates_;
    }

    /**
     *
     * @return
     */
    public DataBlock news() {
        List<Update> updates = updates_.updates();
        int n = updates.size();
        DataBlock a = new DataBlock(n);
        for (int i = 0; i < n; ++i) {
            a.set(i, updates.get(i).getNews());
        }
        return a;
    }

    /**
     *
     * @param series
     * @param p
     * @return
     */
    public DataBlock weights(int series, TsPeriod p) {
        List<Update> updates = updates_.updates();
        int n = updates.size();
        DataBlock a = new DataBlock(n);
        int c = model_.getBlockLength();
        int xc = modelex_.getBlockLength();
        int nb = model_.getFactorsCount();
        int d = ssf_.getStateDim();
        Matrix V = new Matrix(d, d);
        DataBlockIterator vcols = V.columns();
        DataBlock vcol = vcols.getData();
        DataBlock tmp = new DataBlock(d);
        int istart = last_.minus(p);
        TsFrequency freq = last_.getFrequency();
        for (int j = 0; j < n; ++j) {
            Update jupdate = updates.get(j);
            int jstart = last_.minus(jupdate.period.lastPeriod(freq));
            V.set(0);
            for (int r = 0; r < nb; ++r) {
                for (int s = 0; s < nb; ++s) {
                    V.subMatrix(r * c, r * c + c, s * c, s * c + c).copy(
                            mcov_.subMatrix(r * xc + istart, r * xc + istart + c,
                                    s * xc + jstart, s * xc + jstart + c));
                }
            }
            tmp.set(0);
            vcols.begin();
            do {
                tmp.set(vcols.getPosition(), ssf_.ZX(0, series, vcol));
            } while (vcols.next());
            double q = ssf_.ZX(0, jupdate.series, tmp);
            a.set(j, q);
        }
        // w = A * (LL')^-1 <-> w(LL')=A
        // B = wL, BL' = A <-> LB'=A'
        LowerTriangularMatrix.rsolve(lcov_, a, MFilter.Zero); // B
        LowerTriangularMatrix.lsolve(lcov_, a, MFilter.Zero);
        return a;
    }
}
