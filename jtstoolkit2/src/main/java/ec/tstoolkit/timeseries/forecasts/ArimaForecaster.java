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
package ec.tstoolkit.timeseries.forecasts;

import ec.tstoolkit.arima.estimation.Forecasts;
import ec.tstoolkit.modelling.arima.IPreprocessor;
import ec.tstoolkit.modelling.arima.ModellingContext;
import ec.tstoolkit.modelling.arima.PreprocessingModel;
import ec.tstoolkit.timeseries.Day;
import ec.tstoolkit.timeseries.information.TsInformationSet;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import ec.tstoolkit.timeseries.simplets.TsPeriod;

/**
 *
 * @author Jean Palate
 */
public class ArimaForecaster implements ITsForecaster {

    private final IPreprocessor processor_;
    private PreprocessingModel model_;
    private TsDomain fdom_;

    public ArimaForecaster(IPreprocessor processor) {
        processor_ = processor;
    }

    @Override
    public boolean process(TsInformationSet info, int var, Day horizon) {
        TsData series = info.series(var);
        TsPeriod start = series.getEnd();
        if (horizon.isBefore(start.lastday())) {
            return false;
        }
        if (model_ != null && model_.description.getOriginal().equals(series)) {
            return true;
        }
        TsPeriod last = start.clone();
        last.set(horizon);
        ModellingContext context = new ModellingContext();
        model_ = processor_.process(series, context);
        fdom_ = new TsDomain(start, last.minus(start));
        return model_ != null;
    }

    @Override
    public TsData getForecast() {
        try {
            return model_.forecast(fdom_.getLength(), false);
        } catch (Exception err) {
            return null;
        }
    }

    @Override
    public TsData getForecastStdev() {
        try {
            Forecasts forecasts = model_.forecasts(fdom_.getLength());
            double[] ef = forecasts.getForecastStdevs();
            TsData r = new TsData(fdom_.getStart(), ef, false);
            if (model_.isMultiplicative()) {
                TsData f = new TsData(fdom_.getStart(), forecasts.getForecasts(), false).exp();
                return r.exp().plus(-1).times(f);
            } else {
                return r;
            }
        } catch (Exception err) {
            return null;
        }
    }

}
