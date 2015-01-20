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

import ec.tstoolkit.modelling.arima.tramo.TramoSpecification;
import ec.tstoolkit.modelling.arima.x13.RegArimaSpecification;
import ec.tstoolkit.timeseries.Day;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataTable;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jean Palate
 */
public class ArimaForecasterTest {
    
    public ArimaForecasterTest() {
    }

    @Test
    public void demoTramo() {
        TsData s=data.Data.P;
        
        ArimaForecaster arima=new ArimaForecaster(TramoSpecification.TRfull.build());
        arima.process(s.drop(0, 12), null, 12);
        TsData f=arima.getForecast();
        TsData ef = arima.getForecastStdev();
        TsDataTable tmp=new TsDataTable();
        tmp.insert(-1, s.drop(s.getLength()-24, 0));
        tmp.insert(-1, f);
        tmp.insert(-1, ef);
        System.out.println("Tramo");
        System.out.println(tmp);
    }
    
    @Test
    public void demoAirline() {
        TsData s=data.Data.P;
        
        ArimaForecaster arima=new ArimaForecaster(TramoSpecification.TR0.build());
        arima.process(s.drop(0, 12), null, 12);
        TsData f=arima.getForecast();
        TsData ef = arima.getForecastStdev();
        TsDataTable tmp=new TsDataTable();
        tmp.insert(-1, s.drop(s.getLength()-24, 0));
        tmp.insert(-1, f);
        tmp.insert(-1, ef);
        System.out.println("airline");
        System.out.println(tmp);
    }
    
    @Test
    public void demoX13() {
        TsData s=data.Data.P;
        
        ArimaForecaster arima=new ArimaForecaster(RegArimaSpecification.RG5.build());
        arima.process(s.drop(0, 12), null, 12);
        TsData f=arima.getForecast();
        TsData ef = arima.getForecastStdev();
        TsDataTable tmp=new TsDataTable();
        tmp.insert(-1, s.drop(s.getLength()-24, 0));
        tmp.insert(-1, f);
        tmp.insert(-1, ef);
        System.out.println("X13");
        System.out.println(tmp);
    }
}
