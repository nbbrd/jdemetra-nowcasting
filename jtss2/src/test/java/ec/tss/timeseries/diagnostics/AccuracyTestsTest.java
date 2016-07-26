/*
 * Copyright 2016 National Bank of Belgium
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
package ec.tss.timeseries.diagnostics;

import com.google.common.base.Stopwatch;
import data.Data;
import ec.tstoolkit.arima.ArimaModel;
import ec.tstoolkit.arima.ArimaModelBuilder;
import ec.tstoolkit.maths.polynomials.Polynomial;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 *
 * @author Mats Maggi
 */
public class AccuracyTestsTest {

    //@Test
    public void testDieboldMariano() {
        List<TsData> rndAirlines = Data.rndAirlines(10, 240, -.6, -.8);
        List<TsData> rndAirlinesBench = Data.rndAirlines(10, 240, -.6, -.8);
        Stopwatch sw = Stopwatch.createStarted();
        String format = "%-20s%-15s%-15s%-15s%-15s%-15s";
        System.out.format(format, "Test", "PValue", "Avg Loss", "Test Stat", "Distribution", "Order").println();
        DecimalFormat df = new DecimalFormat("0.###");
        for (int i = 0; i < rndAirlines.size(); i++) {
            DieboldMarianoTest dmTest = new DieboldMarianoTest(rndAirlines.get(i), rndAirlinesBench.get(i), AccuracyTests.AsymptoticsType.STANDARD);
            dmTest.setDelay(-30);
            dmTest.setAbsolute(true);
            dmTest.getPValue();

            System.out.format(format, "Diebold Mariano", df.format(dmTest.getPValue()),
                    df.format(dmTest.getAverageLoss()), df.format(dmTest.getTestStat()),
                    dmTest.getDistributionType().toString(), dmTest.getOrder()).println();

//            System.out.println("P-Value : " + dmTest.getPValue());
//            System.out.println("Avg Loss : " + dmTest.getAverageLoss());
//            System.out.println("TestStat : " + dmTest.getTestStat());
//            System.out.println("Distribution : " + dmTest.getDistributionType());
//            System.out.println("Order : " + dmTest.getOrder());
        }
        System.out.println("Duration : " + sw.stop().elapsed(TimeUnit.MILLISECONDS) + " ms");

    }

    @Test
    public void firstCase() {
        int nb = 50000;
        ArimaModelBuilder gen = new ArimaModelBuilder();
        ArimaModel model = gen.createModel(Polynomial.valueOf(1, -0.9), Polynomial.ONE, 1);
        TsData[] losses = new TsData[nb];

        for (int i = 0; i < nb; i++) {
            losses[i] = new TsData(TsFrequency.Monthly, 2000, 0, gen.generate(model, 0, 50), false);
        }

        for (int b = 5; b <= 50; b = b + 5) {
            int nbReject = 0;
            for (int i = 0; i < nb; i++) {
                DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.STANDARD);
                test.setLoss(losses[i]);
                test.setBandwith(b);

                if (test.getPValue() < 0.025) {
                    nbReject++;
                }
            }
            System.out.println("Bandwith : " + b + "\t" + (double)nbReject / (double)nb);
        }
    }
}
