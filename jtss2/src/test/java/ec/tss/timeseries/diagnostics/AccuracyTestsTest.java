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
import ec.tstoolkit.data.DataBlock;
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
        boolean twoSided = false;
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
            dmTest.getPValue(twoSided);

            System.out.format(format, "Diebold Mariano", df.format(dmTest.getPValue(twoSided)),
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

    @Test ////CORRECT
    public void standardCase() {
        boolean twoSided = false;
        int nb = 50000;
        ArimaModelBuilder gen = new ArimaModelBuilder();
        //   ArimaModel model = gen.createModel(Polynomial.valueOf(1, -0.8), Polynomial.valueOf(1, -0.4), 1);

        ArimaModel model = gen.createModel(Polynomial.ONE, Polynomial.ONE, 1);

        TsData[] losses = new TsData[nb];

        for (int i = 0; i < nb; i++) {
            losses[i] = new TsData(TsFrequency.Monthly, 2000, 0, gen.generate(model, 0, 50), false);
        }

        // for (int b = 5; b <= 50; b = b + 5) {
        for (int b = 0; b <= 50; b = b + 1) {
            int nbReject = 0;
            int negValues = 0;
            for (int i = 0; i < nb; i++) {
                //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.STANDARD);
                DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.STANDARD_FIXED_B);
                //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.HAR_FIXED_B);
                test.setLoss(losses[i]);
                test.setBandwith(b);

                if (test.getPValue(twoSided) <= 0.05) {
                    nbReject++;
                }

                if (test.getDenomPositive() == false) {
                    negValues++;
                }

            }
            System.out.println("*Bandwith : " + b + "\t" + (double) nbReject / (double) nb + "%,  failure : " + (double) negValues / nb + "%");
        }   // I multiply by 0.5 because the null in the paper by KV is mu<=0 with alpha=0.05, 
        // and our pvalues are for for the 2 sided test (mu=0 vs mu=/=0) with alpha=0.10;
        // Half of our tstats that fall outside the confidence interval are on the left (symmetry of the IID process)
    }

    @Test //CORRECT
    public void harCase() {
        boolean twoSided = false;
        int nb = 50000;
        ArimaModelBuilder gen = new ArimaModelBuilder();
        ArimaModel model = gen.createModel(Polynomial.valueOf(1, -0.8), Polynomial.valueOf(1, -0.4), 1);
        // ArimaModel model = gen.createModel(Polynomial.ONE, Polynomial.ONE, 1);

        TsData[] losses = new TsData[nb];

        for (int i = 0; i < nb; i++) {
            losses[i] = new TsData(TsFrequency.Monthly, 2000, 0, gen.generate(model, 0, 50), false);
        }

        // for (int b = 5; b <= 50; b = b + 5) {
        for (int b = 0; b <= 50; b = b + 1) {
            int nbReject = 0;
            int negValues = 0;
            for (int i = 0; i < nb; i++) {
                //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.STANDARD);
                DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.HAR_FIXED_B);
                //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.HAR_FIXED_B);
                test.setLoss(losses[i]);
                test.setBandwith(b);

                if (test.getPValue(twoSided) <= 0.05) {
                    nbReject++;
                }

                if (test.getDenomPositive() == false) {
                    negValues++;
                }

                //  System.out.println("***  pVal : " + test.getPValue());
            }
//            System.out.println("**Bandwith : " + b + "\t" + (double)nbReject / (double)nb+ "%,  failure : " + (double)negValues/nb+"%");
            System.out.println("**Bandwith : " + b + "\t" + (double) nbReject / (double) nb + "%,  failure : " + (double) negValues);
            //System.out.println("***Bandwith : " + b + "\t" + (double)nbReject / (double)nb+ "%,  pVal : " + test.getPValue());
        }   // I multiply by 0.5 because the null in the paper by KV is mu<=0 with alpha=0.05, 
        // and our pvalues are for for the 2 sided test (mu=0 vs mu=/=0) with alpha=0.10;
        // Half of our tstats that fall outside the confidence interval are on the left (symmetry of the IID process)
    }

    @Test //   test.setBandwith(b); It will be automatic b=T^(1/3)
    public void harCaseFixedBandwidth() {
        boolean twoSided = false;
        int nb = 50000;
        ArimaModelBuilder gen = new ArimaModelBuilder();
        ArimaModel model = gen.createModel(Polynomial.valueOf(1, -0.8), Polynomial.valueOf(1, -0.4), 1);
        // ArimaModel model = gen.createModel(Polynomial.ONE, Polynomial.ONE, 1);

        TsData[] losses = new TsData[nb];

        for (int i = 0; i < nb; i++) {
            losses[i] = new TsData(TsFrequency.Monthly, 2000, 0, gen.generate(model, 0, 50), false);
        }

        // for (int b = 5; b <= 50; b = b + 5) {
        for (int b = 0; b <= 50; b = b + 1) { // not used at all
            int nbReject = 0;
            int negValues = 0;
            for (int i = 0; i < nb; i++) {
                //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.STANDARD);
                DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.HAR_FIXED_B);
                //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.HAR_FIXED_B);
                test.setLoss(losses[i]);
                //test.setBandwith(b); It will be automatic b=T^(1/3)

                if (test.getPValue(twoSided) <= 0.05) {
                    nbReject++;
                }

                if (test.getDenomPositive() == false) {
                    negValues++;
                }

                //  System.out.println("***  pVal : " + test.getPValue());
            }
//            System.out.println("**Bandwith : " + b + "\t" + (double)nbReject / (double)nb+ "%,  failure : " + (double)negValues/nb+"%");
            System.out.println("***Bandwith : " + b + "\t" + 0.5 * (double) nbReject / (double) nb + "%,  failure : " + (double) negValues);
            //System.out.println("***Bandwith : " + b + "\t" + (double)nbReject / (double)nb+ "%,  pVal : " + test.getPValue());
        }   // I multiply by 0.5 because the null in the paper by KV is mu<=0 with alpha=0.05, 
        // and our pvalues are for for the 2 sided test (mu=0 vs mu=/=0) with alpha=0.10;
        // Half of our tstats that fall outside the confidence interval are on the left (symmetry of the IID process)
    }

    @Test // strange results: check how critical values are calculated!!!!
    public void hlnCase() {
        boolean twoSided = false;
        int nb = 50000;
        ArimaModelBuilder gen = new ArimaModelBuilder();
        // ArimaModel model = gen.createModel(Polynomial.valueOf(1, -0.8), Polynomial.valueOf(1, -0.4), 1);
        ArimaModel model = gen.createModel(Polynomial.ONE, Polynomial.ONE, 1);

        TsData[] losses = new TsData[nb];

        for (int i = 0; i < nb; i++) {
            losses[i] = new TsData(TsFrequency.Monthly, 2000, 0, gen.generate(model, 0, 50), false);
        }

        // for (int b = 5; b <= 50; b = b + 5) {
        for (int b = 0; b <= 50; b = b + 1) { // not used at all
            int nbReject = 0;
            int negValues = 0;
            int orderAc = 0;
            for (int i = 0; i < nb; i++) {
                //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.STANDARD);
                DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.HLN);
                //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.HAR_FIXED_B);
                test.setLoss(losses[i]);
                test.setBandwith(0); //It will be automatic equal to order

                if (test.getPValue(twoSided) <= 0.05) {
                    nbReject++;
                }

                if (test.getDenomPositive() == false) {
                    negValues++;
                }

                orderAc = test.getOrder() + orderAc;
                //  System.out.println("***  pVal : " + test.getPValue());

            }
//            System.out.println("**Bandwith : " + b + "\t" + (double)nbReject / (double)nb+ "%,  failure : " + (double)negValues/nb+"%");
            System.out.println("****Bandwith : " + b + "\t" + 0.5 * (double) nbReject / (double) nb + "%,  failure : " + (double) negValues + " Order:" + (double) orderAc / (double) nb);
            //System.out.println("***Bandwith : " + b + "\t" + (double)nbReject / (double)nb+ "%,  pVal : " + test.getPValue());
        }   // I multiply by 0.5 because the null in the paper by KV is mu<=0 with alpha=0.05, 
        // and our pvalues are for for the 2 sided test (mu=0 vs mu=/=0) with alpha=0.10;
        // Half of our tstats that fall outside the confidence interval are on the left (symmetry of the IID process)
    }

    @Test
    public void power() {
        boolean twoSided = false;
        int nb = 50000;
        int nbObs = 40;

        AccuracyTests.AsymptoticsType asympType = AccuracyTests.AsymptoticsType.HAR_FIXED_B;
        // AccuracyTests.AsymptoticsType asympType = AccuracyTests.AsymptoticsType.STANDARD_FIXED_B;

        // AccuracyTests.AsymptoticsType asympType = AccuracyTests.AsymptoticsType.STANDARD_FIXED_B;
        AccuracyTests.AsymptoticsType asympTypeB = AccuracyTests.AsymptoticsType.UNFEASIBLE;

        double bandwith = Math.sqrt(nbObs);
        //  double bandwith = Math.pow(nbObs,(1.0/3));
        ArimaModelBuilder gen = new ArimaModelBuilder();
        // ArimaModel model = gen.createModel(Polynomial.valueOf(1, -0.8), Polynomial.valueOf(1, -0.4), 1);
        ArimaModel model = gen.createModel(Polynomial.ONE, Polynomial.ONE, 1);
        double[] acf = model.getAutoCovarianceFunction().values(nbObs + 1); // FROM variance until autocov of order (arg)-1
        double denomIdeal = new DataBlock(acf).sum() * 2.0 - model.getAutoCovarianceFunction().get(0);

//         ArimaModel model = gen.createModel(Polynomial.ONE, Polynomial.ONE, 1);
        for (double c = 0; c <= 7; c += 0.05) {
            double mu = c * Math.pow((double) nbObs, -0.5);
            TsData[] losses = new TsData[nb];

            int nbReject = 0;
            int nbRejectB = 0;
            for (int i = 0; i < nb; i++) {
                losses[i] = new TsData(TsFrequency.Monthly, 2000, 0, gen.generate(model, mu, nbObs), false);

                DieboldMarianoTest test = new DieboldMarianoTest(null, null, asympType);
                test.setLoss(losses[i]);
                test.setBandwith((int) bandwith);

                DieboldMarianoTest testB = new DieboldMarianoTest(null, null, asympTypeB);
                testB.setLoss(losses[i]);
                // testB.setDenomIdeal(denomIdeal/Math.sqrt(nbObs));
                testB.setDenomIdeal(denomIdeal / nbObs);
                if (test.getPValue(twoSided) <= 0.05) {
                    nbReject++;
                }

                if (testB.getPValue(twoSided) <= 0.05) {
                    nbRejectB++;

                }

            }

            System.out.println(bandwith + "\t" + c + "\t" + (double) nbReject / (double) nb + "\t" + (double) nbRejectB / (double) nb);
        }

    }

    @Test
    public void powerOptim() {

        boolean twoSided = false;
        int nb = 50000;
        int nbObs = 40;

        AccuracyTests.AsymptoticsType asympType = AccuracyTests.AsymptoticsType.HAR_FIXED_B;
        //    AccuracyTests.AsymptoticsType asympType = AccuracyTests.AsymptoticsType.STANDARD_FIXED_B;

        // AccuracyTests.AsymptoticsType asympType = AccuracyTests.AsymptoticsType.STANDARD_FIXED_B;
        AccuracyTests.AsymptoticsType asympTypeB = AccuracyTests.AsymptoticsType.UNFEASIBLE;

        //  double bandwith = Math.pow(nbObs,(1.0/3));
        ArimaModelBuilder gen = new ArimaModelBuilder();
        ArimaModel model = gen.createModel(Polynomial.valueOf(1, -0.8), Polynomial.valueOf(1, -0.4), 1);
        // ArimaModel model = gen.createModel(Polynomial.ONE, Polynomial.ONE, 1);
        double[] acf = model.getAutoCovarianceFunction().values(nbObs + 1); // FROM variance until autocov of order (arg)-1
        double denomIdeal = new DataBlock(acf).sum() * 2.0 - model.getAutoCovarianceFunction().get(0);

//         ArimaModel model = gen.createModel(Polynomial.ONE, Polynomial.ONE, 1);
        for (int b = 0; b <= 40; b = b + 1) {
            //  double mu = c * Math.pow((double) nbObs, -0.5);
            double mu = 0.158;
            TsData[] losses = new TsData[nb];

            int nbReject = 0;
            int nbRejectB = 0;
            for (int i = 0; i < nb; i++) {
                losses[i] = new TsData(TsFrequency.Monthly, 2000, 0, gen.generate(model, mu, nbObs), false);

                DieboldMarianoTest test = new DieboldMarianoTest(null, null, asympType);
                test.setLoss(losses[i]);
                test.setBandwith((int) b);

                DieboldMarianoTest testB = new DieboldMarianoTest(null, null, asympTypeB);
                testB.setLoss(losses[i]);
                // testB.setDenomIdeal(denomIdeal/Math.sqrt(nbObs));
                testB.setDenomIdeal(denomIdeal / nbObs);
                if (test.getPValue(twoSided) <= 0.05) {
                    nbReject++;
                }

                if (testB.getPValue(twoSided) <= 0.05) {
                    nbRejectB++;

                }

            }

            System.out.println(b + "\t" + mu + "\t" + (double) nbReject / (double) nb + "\t" + (double) nbRejectB / (double) nb);
        }

    }

    @Test //CORRECT
    public void harCase3D() {

        boolean twoSided = false;
        int nb = 50000;
        int T = 50;
        int b = 0;
        ArimaModelBuilder gen = new ArimaModelBuilder();
        // ArimaModel model = gen.createModel(Polynomial.valueOf(1, -0.8), Polynomial.valueOf(1, -0.4), 1);
        ArimaModel model = gen.createModel(Polynomial.ONE, Polynomial.ONE, 1);

        TsData[] losses = new TsData[nb];

        for (int i = 0; i < nb; i++) {
            losses[i] = new TsData(TsFrequency.Monthly, 2000, 0, gen.generate(model, 0, T), false);
        }

        // for (int b = 5; b <= 50; b = b + 5) {
        for (double d = 0; d <= 1; d = d + 0.025) {
            int nbReject = 0;
            int negValues = 0;
            for (int i = 0; i < nb; i++) {
                //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.STANDARD);
                DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.HAR_FIXED_B);
                //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.HAR_FIXED_B);
                test.setLoss(losses[i]);
                b = (int) Math.pow(T, d);
                test.setBandwith(b);

                if (test.getPValue(twoSided) <= 0.05) {
                    nbReject++;
                }

                if (test.getDenomPositive() == false) {
                    negValues++;
                }

                //  System.out.println("***  pVal : " + test.getPValue());
            }
//            System.out.println("**Bandwith : " + b + "\t" + (double)nbReject / (double)nb+ "%,  failure : " + (double)negValues/nb+"%");
            System.out.println("**Bandwith : " + b + "\t" + (double) nbReject / (double) nb + "%,  failure : " + (double) negValues);
            //System.out.println("***Bandwith : " + b + "\t" + (double)nbReject / (double)nb+ "%,  pVal : " + test.getPValue());
        }   // I multiply by 0.5 because the null in the paper by KV is mu<=0 with alpha=0.05, 
        // and our pvalues are for for the 2 sided test (mu=0 vs mu=/=0) with alpha=0.10;
        // Half of our tstats that fall outside the confidence interval are on the left (symmetry of the IID process)
    }
}
