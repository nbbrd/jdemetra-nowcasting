/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import ec.tss.timeseries.diagnostics.AccuracyTests;
import ec.tss.timeseries.diagnostics.ForecastEvaluation;
import ec.tss.timeseries.diagnostics.GlobalForecastingEvaluation;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import org.junit.Test;

/**
 *
 * @author deanton
 */
public class SurveyPaperData {

    // fh(-30)     
    private static final double[] dfmPre30
            = {
                0.51977544, 0.525696385, 0.438855874, 0.415859878, 0.38540503, 0.302901957, 0.08804077, -0.110632114, -0.931359482, 0.566867627, 1.002253088, 0.761475564, -0.196546175, 0.360739577, -0.026382431, 0.077125145, 1.038341463, 0.737435487, 0.029691459, -0.282322399, -0.212052768, -0.307245663, -0.391997912, -0.777139541, 0.012921099, -0.054855253, 0.072696322, 0.085317222, 0.327379059, 0.307126205, 0.086418996, 0.140291001, -0.11048533

            };

// fh(0)  
    private static final double[] dfm0
            = {
                0.557192331, 0.561699324, 0.461222832, 0.431914412, 0.40365345, 0.291732632, 0.07868605, -0.183463352, -1.349880496, 0.00401673, 0.587567124, 0.525247569, 0.361100579, 0.525719293, 0.201151641, 0.401027952, 0.846145264, 0.610796084, 0.213944461, -0.381234445, -0.241583192, -0.283852578, -0.212013099, -0.880331377, -0.001581687, 0.156607943, -0.085924794, 0.040053786, 0.254789185, 0.231923816, 0.112576029, 0.082097104, 0.016951904
            };

    // fh(30)   
    private static final double[] dfmPost30
            = {
                0.571434422, 0.557018585, 0.453128527, 0.482666811, 0.404785595, 0.285586864, 0.067744184, -1.415254337, -1.548280049, -0.221086126, 0.427391035, 0.288510356, 0.557747482, 0.947419229, 0.4429614, 0.726322452, 0.781355897, 0.551171378, 0.387589234, -0.367917044, -0.167567487, -0.261485008, -0.019875784, -0.698495077, 0.00993869, 0.252518013, 0.017238834, 0.172592034, 0.206503354, 0.058023913, 0.118256273, 0.119354109, 0.168851517
            };

    // fh(42)        
    private static final double[] dfmPost42
            = {
                0.814127989, 0.733036016, 0.806207342, 0.482229078, 0.405174293, 0.28619083, 0.068642109, -1.514980384, -1.80444319, -0.390459238, 0.340514691, 0.299687747, 0.793563523, 1.125639737, 0.549135358, 0.680942834, 0.74132965, 0.483672909, 0.361425421, -0.434190236, -0.105429106, -0.1756787, 0.011778163, -0.605338137, 0.126393109, 0.303086233, 0.03564331, 0.163192652, 0.215708629, 0.149867978, 0.170972118, 0.11840683, 0.19715997
            };

    private static final double[] bloombergN
            = {
                0.5, 0.5, 0.6, 0.3, 0.5, -0.2, -0.2, -1.3, -2, -0.5, 0.5, 0.3, 0.1, 0.7, 0.5, 0.4, 0.6, 0.3, 0.2, -0.4, 0.2, -0.2, -0.1, -0.4, -0.1, 0.2, 0.1, 0.2, 0.4, 0.1, 0.1, 0.2, 0.4
            };

    private static final double[] flashGdp
            = {
                0.6, 0.3, 0.7, 0.4, 0.7, -0.2, -0.2, -1.5, -2.5, -0.1, 0.4, 0.1, 0.2, 1, 0.4, 0.3, 0.8, 0.2, 0.2, -0.3, 0, -0.2, -0.1, -0.6, -0.2, 0.3, 0.1, 0.3, 0.2, 0, 0.2, 0.3, 0.4
            };

    private static final double[] revisedGdp
            = {
                0.77054188, 0.627352053, 0.472204071, 0.555473748, 0.517015547, -0.332247425, -0.549365773, -1.752738526, -2.974592541, -0.241562446, 0.300537533, 0.509810972, 0.439509895, 0.953616662, 0.429673062, 0.554135945, 0.841388145, 0.008661258, 0.011959749, -0.332771155, -0.179559954, -0.310857635, -0.127225405, -0.426706409, -0.28178789, 0.400397463, 0.283545192, 0.182388287, 0.206995449, 0.098721187, 0.299600946, 0.365222852, 0.553250346
            };

    public static final TsData TRUEDATA = new TsData(TsFrequency.Quarterly, 2007, 0, revisedGdp, false);
    public static final TsData FLASH = new TsData(TsFrequency.Quarterly, 2007, 0, flashGdp, false);
    public static final TsData BLOOM = new TsData(TsFrequency.Quarterly, 2007, 0, bloombergN, false);
    public static final TsData DFMPOST42 = new TsData(TsFrequency.Quarterly, 2007, 0, dfmPost42, false);
    public static final TsData DFMPOST30 = new TsData(TsFrequency.Quarterly, 2007, 0, dfmPost30, false);
    public static final TsData DFM0 = new TsData(TsFrequency.Quarterly, 2007, 0, dfm0, false);
    public static final TsData DFMPRE30 = new TsData(TsFrequency.Quarterly, 2007, 0, dfmPre30, false);

    @Test //CORRECT
    public void dmTest() {
        // TsData[] series = {DFMPRE30,DFM0,DFMPOST30, DFMPOST42, BLOOM};
        TsData[] series = {DFMPRE30, DFM0, DFMPOST30, DFMPOST42, BLOOM, FLASH};
        boolean twoSided = true;
        for (int i = 0; i < series.length; i++) {

            //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.STANDARD);
            //DieboldMarianoTest test = new DieboldMarianoTest(series[i], BLOOM, TRUEDATA, AccuracyTests.AsymptoticsType.HAR_FIXED_B);
            int bandwith = (int) Math.sqrt(series[i].getObsCount());

            // GlobalForecastingEvaluation eval = new GlobalForecastingEvaluation(series[i], FLASH, TRUEDATA, AccuracyTests.AsymptoticsType.STANDARD_FIXED_B);         
            //GlobalForecastingEvaluation eval = new GlobalForecastingEvaluation(series[i], BLOOM, TRUEDATA, AccuracyTests.AsymptoticsType.HAR_FIXED_B);         
            GlobalForecastingEvaluation eval = new GlobalForecastingEvaluation(series[i], FLASH, TRUEDATA, AccuracyTests.AsymptoticsType.HAR_FIXED_B);
            eval.getDieboldMarianoTest().setBandwith(bandwith);
            double pValue2S;
            pValue2S = eval.getDieboldMarianoTest().getPValue(twoSided);

            ForecastEvaluation feval = new ForecastEvaluation(series[i], FLASH, TRUEDATA);
            double relRMSE;
            relRMSE = feval.calcRMSE();

            System.out.println(bandwith + "\t" + "relRMSE" + "\t" + relRMSE + "\t" + "pValue" + "\t" + pValue2S);

        }
    }

    @Test
    public void bias_and_ar_Test() {
        // TsData[] series = {DFMPRE30,DFM0,DFMPOST30, DFMPOST42, BLOOM};
        TsData[] series = {DFMPRE30, DFM0, DFMPOST30, DFMPOST42, BLOOM, FLASH};
        boolean twoSided = true;

        double[] bias = new double[6];
        double[] biasPval = new double[6];

        double[] ar = new double[6];
        double[] arPval = new double[6];

        double[] m_enc_bloom = new double[6];
        double[] m_enc_bloom_Pval = new double[6];
        double[] bloom_enc_m = new double[6];
        double[] bloom_enc_m_Pval = new double[6];

        for (int i = 0; i < series.length; i++) {

            //DieboldMarianoTest test = new DieboldMarianoTest(null, null, AccuracyTests.AsymptoticsType.STANDARD);
            //DieboldMarianoTest test = new DieboldMarianoTest(series[i], BLOOM, TRUEDATA, AccuracyTests.AsymptoticsType.HAR_FIXED_B);
            int bandwith = (int) Math.sqrt(series[i].getObsCount());

            GlobalForecastingEvaluation eval = new GlobalForecastingEvaluation(series[i], FLASH, TRUEDATA, AccuracyTests.AsymptoticsType.STANDARD_FIXED_B);

            eval.getBiasTest().setBandwith(bandwith);
            bias[i] = eval.getBiasTest().getAverageLoss();
            biasPval[i] = eval.getBiasTest().getPValue(twoSided);
            eval.getEfficiencyTest().setBandwith(bandwith);
            ar[i] = eval.getEfficiencyTest().getAverageLoss();
            arPval[i] = eval.getEfficiencyTest().getPValue(twoSided);
            eval.getModelEncompassesBenchmarkTest().setBandwith(bandwith);
            m_enc_bloom[i] = eval.getModelEncompassesBenchmarkTest().calcWeights();
            m_enc_bloom_Pval[i] = eval.getModelEncompassesBenchmarkTest().getPValue(twoSided);
            bloom_enc_m[i] = eval.getBenchmarkEncompassesModelTest().calcWeights();
            bloom_enc_m_Pval[i] = eval.getBenchmarkEncompassesModelTest().getPValue(twoSided);

            System.out.println("Bias" + "\t" + bias[i] + "\t" + biasPval[i] + "\t" + "AR" + "\t" + ar[i] + "\t" + arPval[i] + "\t" + "Weight on Model" + "\t" + (1 - m_enc_bloom[i]) + "\t" + "m_enc_bloom Pval" + "\t" + m_enc_bloom_Pval[i] + "\t" + "bloom_enc_m Pval" + "\t" + bloom_enc_m_Pval[i]);

        }
    }

}
