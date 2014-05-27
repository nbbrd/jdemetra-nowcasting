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

import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.timeseries.Day;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataTable;
import ec.tstoolkit.timeseries.simplets.TsDataTableInfo;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import ec.tstoolkit.timeseries.simplets.TsPeriod;

/**
 *
 * @author Jean Palate
 */
public class DfmInformationSet {

    /**
     *
     * @param input
     */
    public DfmInformationSet(TsData[] input) {
        for (int i = 0; i < input.length; ++i) {
            table_.insert(-1, input[i]);
        }
    }

    // 
    /**
     * Creates a new information set with only the revised data in comparison
     * with this data set (the domains of the series of this data set are
     * identical to the domains of the series of the returned information set
     *
     * @param newdata
     * @return
     */
    public DfmInformationSet revisedData(DfmInformationSet newdata) {
        TsData[] ndata = new TsData[table_.getSeriesCount()];
        for (int i = 0; i < ndata.length; ++i) {
            TsData cur = table_.series(i);
            TsData ncur = newdata.table_.series(i);
            ndata[i] = ncur.fittoDomain(cur.getDomain());
            for (int j = 0; j < cur.getLength(); ++j) {
                if (cur.getValues().isMissing(j)) {
                    ncur.getValues().setMissing(j);
                }
            }
        }
        return new DfmInformationSet(ndata);
    }

    public DfmInformationSet actualData() {
        TsData[] ndata = new TsData[table_.getSeriesCount()];
        for (int i = 0; i < ndata.length; ++i) {
            ndata[i] = table_.series(i).cleanExtremities();
        }
        return new DfmInformationSet(ndata);
    }

    public DfmInformationSet extendTo(final Day end) {
        TsData[] ndata = new TsData[table_.getSeriesCount()];
        for (int i = 0; i < ndata.length; ++i) {
            ndata[i] = table_.series(i).extendTo(end);
        }
        return new DfmInformationSet(ndata);
    }

    /**
     *
     * @return
     */
    public TsDomain getCurrentDomain() {
        return table_.getDomain();
    }

    public int getSeriesCount() {
        return table_.getSeriesCount();
    }

    public int getDataCount() {
        int n = 0;
        for (int i = 0; i < table_.getSeriesCount(); ++i) {
            TsData series = table_.series(i);
            n += series.getObsCount();
        }
        return n;
    }

    /**
     *
     * @param idx
     * @return
     */
    public TsData series(int idx) {
        return table_.series(idx);
    }

    /**
     *
     * @param domain
     * @return
     */
    public Matrix generateMatrix(final TsDomain domain) {
        TsDomain tdomain = table_.getDomain();
        if (tdomain == null) {
            return null;
        }
        if (domain == null) {
            return generateMatrix(tdomain);
        }
        if (domain.getFrequency() != tdomain.getFrequency()) {
            return null;
        }
        Matrix m = new Matrix(domain.getLength(), table_.getSeriesCount());
        m.set(Double.NaN);
        TsDomain common = tdomain.intersection(domain);
        for (int i = 0, j = common.getStart().minus(domain.getStart()),
                k = common.getStart().minus(tdomain.getStart()); i < common.getLength(); ++i, ++j, ++k) {
            for (int s = 0; s < m.getColumnsCount(); ++s) {
                TsDataTableInfo dataInfo = table_.getDataInfo(k, s);
                if (dataInfo == TsDataTableInfo.Valid) {
                    m.set(j, s, table_.getData(k, s));
                }
            }
        }
        return m;
    }

    DfmInformationUpdates updates(DfmInformationSet ndata) {
        int n = table_.getSeriesCount();
        if (n != ndata.table_.getSeriesCount()) {
            return null;
        }
        DfmInformationUpdates updates = new DfmInformationUpdates();
        for (int i = 0; i < n; ++i) {
            TsData olds = table_.series(i), news = ndata.table_.series(i);
            int del = news.getStart().minus(olds.getStart());
            TsPeriod start = news.getStart();
            for (int j = 0; j < news.getLength(); ++j) {
                if (!news.getValues().isMissing(j)) {
                    int k = j + del;
                    if (k < 0 || k >= olds.getLength() || olds.getValues().isMissing(k)) {
                        updates.add(start.plus(j), i);
                    }
                }

            }
        }
        return updates;
    }
    private final TsDataTable table_ = new TsDataTable();
}
