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
package ec.tss.dfm;

import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.algorithm.IProcessing;
import ec.tstoolkit.dfm.DfmSpec;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.timeseries.Day;
import ec.tstoolkit.timeseries.forecasts.ITsForecaster;
import ec.tstoolkit.timeseries.information.TsInformationSet;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsPeriod;

/**
 *
 * @author Jean Palate
 */
public class DfmForecaster implements ITsForecaster {
    
    private final DfmSpec spec_;
    private CompositeResults rslts_;
    private TsInformationSet info_;
    private int var_;
    
    public DfmForecaster(DfmSpec spec) {
        spec_ = spec;
    }
    
    @Override
    public boolean process(TsInformationSet info, int var, Day horizon) {
        if (info_ == info) {
            var_ = var;
            int fh = spec_.getModelSpec().getForecastHorizon();
            TsPeriod last = info.getCurrentDomain().getLast();
            if (horizon.isNotAfter(last.lastday())) {
                return rslts_ != null;
            } else {
                TsPeriod np=last.clone();
                np.set(horizon);
                spec_.getModelSpec().setForecastHorizon(np.minus(last));
                rslts_=null;
            }
        } else {
            rslts_ = null;
        }
        IProcessing<TsData[], CompositeResults> processing = DfmProcessingFactory.instance.generateProcessing(spec_, null);
        rslts_ = processing.process(info.toArray());
        return rslts_.isSuccessful();
    }
    
    @Override
    public TsData getForecast() {
        if (rslts_ == null) {
            return null;
        }
        String info = InformationSet.item(DfmProcessingFactory.FINALC, "var" + (var_ + 1));
        return rslts_.getData(info, TsData.class);
    }
    
    @Override
    public TsData getForecastStdev() {
        return null;
    }
    
}
