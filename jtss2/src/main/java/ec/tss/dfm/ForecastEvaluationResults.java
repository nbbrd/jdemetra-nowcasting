/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tss.dfm;

import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.timeseries.simplets.TsData;

/**
 *
 * @author deanton
 */
public class ForecastEvaluationResults {

    // Inputs: data, forecast and errors
    private final TsData y; // true data
    private final TsData f; // forecasts for a given horizon 
    private final TsData e; // forecast errors: Y-F

    private final TsData eB; // forecast errors: Y-F

    public ForecastEvaluationResults(TsData f, TsData fB, TsData y) {
        this.f = f;
        //this.fB = fB;
        this.y = y;

        e = TsData.subtract(y, f);
        eB = TsData.subtract(y, fB);
    }

    // Scale dependent measures ///////////////////////////////////////////////
    //__________________________///////////////////////////////////////////////
    //__________________________///////////////////////////////////////////////
    public double calcRMSE() {
        double temp = 0;
        int count = 0;
        for (int i = 0; i < e.getLength(); i++) {
            if (!e.isMissing(i)) {
                temp += Math.pow(e.get(i), 2);
                count++;
            }
        }
        return Math.sqrt(temp / count);
    }

    public double calcRMSE_Benchmark() {
        double temp = 0;
        int count = 0;
        for (int i = 0; i < eB.getLength(); i++) {
            if (!eB.isMissing(i)) {
                temp += Math.pow(eB.get(i), 2);
                count++;
            }
        }
        return Math.sqrt(temp / count);
    }

    public double calcMAE() {
        double temp = 0;
        int count = 0;
        for (int i = 0; i < e.getLength(); i++) {
            if (!e.isMissing(i)) {
                temp += Math.abs(e.get(i));
                count++;
            }
        }
        return temp / count;
    }

    public double calcMAE_Benchmark() {
        double temp = 0;
        int count = 0;
        for (int i = 0; i < eB.getLength(); i++) {
            if (!eB.isMissing(i)) {
                temp += Math.abs(eB.get(i));
                count++;
            }
        }
        return temp / count;
    }

    public double calcMdAE() {
        TsData eAbs = e.abs();
        DescriptiveStatistics MdAE = new DescriptiveStatistics(eAbs);
        return MdAE.getMedian();
    }

    public double calcMdAE_Benchmark() {
        TsData eAbs = eB.abs();
        DescriptiveStatistics MdAE = new DescriptiveStatistics(eAbs);
        return MdAE.getMedian();
    }

    // Percentage Errors        ///////////////////////////////////////////////
    //__________________________///////////////////////////////////////////////
    //__________________________///////////////////////////////////////////////
    public double calcRMSPE() {
        double temp = 0;
        int count = 0;
        for (int i = 0; i < e.getLength(); i++) {
            if (!e.isMissing(i)) {
                temp += Math.pow(e.get(i) / y.get(i), 2);
                count++;
            }
        }
        return 100 * Math.sqrt(temp / count);
    }

    public double calcRMSPE_Benchmark() {
        double temp = 0;
        int count = 0;
        for (int i = 0; i < eB.getLength(); i++) {
            if (!eB.isMissing(i)) {
                temp += Math.pow(eB.get(i) / y.get(i), 2);
                count++;
            }
        }
        return 100 * Math.sqrt(temp / count);
    }

    public double calc_sMAPE() {
        double temp = 0;
        int count = 0;
        for (int i = 0; i < e.getLength(); i++) {
            if (!e.isMissing(i)) {
                temp += Math.abs(Math.abs(e.get(i)) / (y.get(i) + f.get(i)));
                count++;
            }
        }
        return 200 * temp / count;
    }

    public double calc_sMAPE_Benchmark() {
        double temp = 0;
        int count = 0;
        for (int i = 0; i < eB.getLength(); i++) {
            if (!eB.isMissing(i)) {
                temp += Math.abs(Math.abs(eB.get(i)) / (y.get(i) + f.get(i)));
                count++;
            }
        }
        return 200 * temp / count;
    }

    public double calc_sMdAPE() {
        TsData eAbs = e.abs();
        double[] eAbs_double = new double[eAbs.getLength()];
        for (int i = 0; i < eAbs.getLength(); i++) {
            eAbs_double[i] = Math.abs(eAbs.get(i) / (y.get(i) + f.get(i)));
        }
        DescriptiveStatistics sMdAE = new DescriptiveStatistics(eAbs_double);
        return 200 * sMdAE.getMedian();
    }

    public double calc_sMdAPE_Benchmark() {
        TsData eAbs = eB.abs();
        double[] eAbs_double = new double[eAbs.getLength()];
        for (int i = 0; i < eAbs.getLength(); i++) {
            eAbs_double[i] = Math.abs(eAbs.get(i) / (y.get(i) + f.get(i)));
        }
        DescriptiveStatistics sMdAE = new DescriptiveStatistics(eAbs_double);
        return 200 * sMdAE.getMedian();
    }

    // Scaled Errors            ///////////////////////////////////////////////
    //__________________________///////////////////////////////////////////////
    //__________________________///////////////////////////////////////////////
    public double calcRMSSE() {
        // calculate scaling factor first
        double scaling;
        double cumsum = 0;
        int count = 0;
        for (int i = 1; i < y.getLength(); i++) {
            if (!e.isMissing(i) && !y.isMissing(i) && !y.isMissing(i - 1)) {
                cumsum += Math.abs(y.get(i) - y.get(i - 1));
                count++;
            }
        }
        scaling = cumsum / count;
        double temp = 0;
        count = 0;
        for (int i = 0; i < e.getLength(); i++) {
            if (!e.isMissing(i)) {
                temp += Math.pow(e.get(i) / scaling, 2);
                count++;
            }
        }
        return Math.sqrt(temp / count);
    }

    public double calcRMSSE_Benchmark() {
        // calculate scaling factor first
        double scaling;
        double cumsum = 0;
        int count = 0;
        for (int i = 1; i < y.getLength(); i++) {
            if (!eB.isMissing(i) && !y.isMissing(i) && !y.isMissing(i - 1)) {
                cumsum += Math.abs(y.get(i) - y.get(i - 1));
                count++;
            }
        }
        scaling = cumsum / count;
        double temp = 0;
        count = 0;
        for (int i = 0; i < eB.getLength(); i++) {
            if (!e.isMissing(i)) {
                temp += Math.pow(eB.get(i) / scaling, 2);
                count++;
            }
        }
        return Math.sqrt(temp / count);
    }

    public double calcMASE() {
        // calculate scaling factor first
        double scaling;
        double cumsum = 0;
        int count = 0;
        for (int i = 1; i < y.getLength(); i++) {
            if (!e.isMissing(i) && !y.isMissing(i) && !y.isMissing(i - 1)) {
                cumsum += Math.abs(y.get(i) - y.get(i - 1));
                count++;
            }
        }
        scaling = cumsum / count;
        double temp = 0;
        count = 0;
        for (int i = 0; i < e.getLength(); i++) {
            if (!e.isMissing(i)) {
                temp += Math.abs(Math.abs(e.get(i)) / scaling);
                count++;
            }
        }
        return temp / count;
    }

    public double calcMASE_Benchmark() {
        // calculate scaling factor first
        double scaling;
        double cumsum = 0;
        int count = 0;
        for (int i = 1; i < y.getLength(); i++) {
            if (!eB.isMissing(i) && !y.isMissing(i) && !y.isMissing(i - 1)) {
                cumsum += Math.abs(y.get(i) - y.get(i - 1));
                count++;
            }
        }
        scaling = cumsum / count;
        double temp = 0;
        count = 0;
        for (int i = 0; i < eB.getLength(); i++) {
            if (!eB.isMissing(i)) {
                temp += Math.abs(Math.abs(eB.get(i)) / scaling);
                count++;
            }
        }
        return temp / count;
    }

    public double calcMdASE() {
        // calculate scaling factor first
        double scaling;
        double cumsum = 0;
        int count = 0;
        for (int i = 1; i < y.getLength(); i++) {
            if (!e.isMissing(i) && !y.isMissing(i) && !y.isMissing(i - 1)) {
                cumsum += Math.abs(y.get(i) - y.get(i - 1));
                count++;
            }
        }
        scaling = cumsum / count;
        TsData eAbs = e.abs();
        double[] eAbs_double = new double[eAbs.getLength()];
        for (int i = 0; i < eAbs.getLength(); i++) {
            eAbs_double[i] = Math.abs(e.get(i) / scaling);
        }
        DescriptiveStatistics sMdASE = new DescriptiveStatistics(eAbs_double);
        return sMdASE.getMedian();
    }

    public double calcMdASE_Benchmark() {
        // calculate scaling factor first
        double scaling;
        double cumsum = 0;
        int count = 0;
        for (int i = 1; i < y.getLength(); i++) {
            if (!eB.isMissing(i) && !y.isMissing(i) && !y.isMissing(i - 1)) {
                cumsum += Math.abs(y.get(i) - y.get(i - 1));
                count++;
            }
        }
        scaling = cumsum / count;
        TsData eAbs = eB.abs();
        double[] eAbs_double = new double[eAbs.getLength()];
        for (int i = 0; i < eAbs.getLength(); i++) {
            eAbs_double[i] = Math.abs(eB.get(i) / scaling);
        }
        DescriptiveStatistics sMdASE = new DescriptiveStatistics(eAbs_double);
        return sMdASE.getMedian();
    }

    // Relative measures        ///////////////////////////////////////////////
    //__________________________///////////////////////////////////////////////
    //__________________________/////////////////////////////////////////////// 
    public double calcPB() {
        int temp = 0;
        int count = 0;

        for (int i = 0; i < e.getLength(); i++) {
            if (!eB.isMissing(i) && !e.isMissing(i)) {

                if (Math.abs(e.get(i)) > Math.abs(eB.get(i))) {
                    temp++;
                }
                count++;
            }
        }
        return 100 * temp / count;
    }

    public double calcRelRMSE() {
        return calcRMSE() / calcRMSE_Benchmark();
    }

    public double calcRelMAE() {
        return calcMAE() / calcMAE_Benchmark();
    }

    public double calcRelMdAE() {
        return calcMdAE() / calcMdAE_Benchmark();
    }

    public double calcRelRMSPE() {
        return calcRMSPE() / calcRMSPE_Benchmark();
    }

    public double calcRel_sMAPE() {
        return calc_sMAPE() / calc_sMAPE_Benchmark();
    }

    public double calcRel_sMdAPE() {
        return calc_sMdAPE() / calc_sMdAPE_Benchmark();
    }

    public double calcRelRMSSE() {
        return calcRMSSE() / calcRMSSE_Benchmark();
    }

    public double calcRelMASE() {
        return calcMASE() / calcMASE_Benchmark();
    }

    public double calcRelMdASE() {
        return calcMdASE() / calcMdASE_Benchmark();
    }

// DIEBOLD-MARIANO AND ENCOMPASING TESTS (I will put them in a different class) !!!!!!!!!!!!!!!!!!!!!!!!
    public class AccuracyTests { // inner class to make sure it can access elements of the outer class

        //Test Statistics: based on squared losses, absolute losses, and "e"ncompassing       
        private double DM, DMabs, DM_e;
        // Loss differentials (demeaned!) and means
        private TsData d, dAbs, d_e; // squared and absolute
        private double dBar, dAbsBar, d_eBar;

        public AccuracyTests() {

        }

        public TsData get_d_e() {
            d_e = e.times(e.minus(eB));
            return d_e;
        }

        public TsData get_d() {
            d = e.pow(2).minus(eB.pow(2));
            return d;
        }

        public TsData get_dAbs() {
            dAbs = e.abs().minus(eB.abs());
            return dAbs;
        }

        public double get_d_eBar() {  // d has to be calculated!!! fix
            DescriptiveStatistics d_e_ = new DescriptiveStatistics(get_d_e());
            d_eBar = d_e_.getAverage(); // excluding missing values I suppose
            return d_eBar;
        }

        public double get_dBar() {  // d has to be calculated!!! fix
            DescriptiveStatistics d_ = new DescriptiveStatistics(get_d());
            dBar = d_.getAverage(); // excluding missing values I suppose
            return dBar;
        }

        public double get_dAbsBar() { // d has to be calculated!!! fix
            DescriptiveStatistics dAbs_ = new DescriptiveStatistics(get_dAbs());
            dAbsBar = dAbs_.getAverage(); // excluding missing values I suppose
            return dAbsBar;
        }

        public double getDM_e() {
            DM_e = get_d_eBar() / calcDM(get_d_e());
            return DM_e;
        }

        public double getDM() {
            DM = get_dBar() / calcDM(get_d());
            return DM;
        }

        public double getDMabs() {
            DMabs = get_dAbsBar() / calcDM(get_dAbs());
            return DMabs;
        }

        public double calcDM(TsData lossDiff) {
            lossDiff.removeMean();

            // Determine largest significant order of serial autocorrelation in "d"
            int h = 2; // let's set it to 5 for the moment, but later on we should test it

            // calculate 2πf(0)= ∑_{τ=-(T-1))^(T-1)}〖w(τ) γ(τ) 〗 where γ(τ) is a "modified" 
            // autocovariance of the loss differential 
            int[] w = new int[2 * (lossDiff.getLength() - 1) + 1];
            double[] gamma = new double[2 * (lossDiff.getLength() - 1) + 1];
            //int[] w = null;
            //double[] gamma = null;
            double gammaw = 0;
            double TwoPIf = 0;

            int count = 0;
            int T = 0;
            for (int tau = -(lossDiff.getLength() - 1); tau <= lossDiff.getLength() - 1; tau++) {

                // w
                if (Math.abs(tau / (h - 1)) <= 1) {
                    w[count] = 1;
                }

                // gamma
                gamma[count] = ForecastEvaluationResults.cov(lossDiff.getValues().internalStorage(), lossDiff.getValues().internalStorage(), tau);

                // T
                if (gamma[count] != 0 && !Double.isNaN(gamma[count])) {
                    T++;
                    gammaw += gamma[count] * w[count]; // sum w*gamma
                }
                
                count++;
            }

            // DM = dBar/(sqrt(2pif/T)), where 2pif = gammaw/T
            double DM_denom = Math.sqrt((gammaw / T) / T);
            return DM_denom;
        }
    }

    /**
     * David: I copy and adjust this method from the DescriptiveStatistics class
     * Computes the covariance between two arrays of doubles, which are supposed
     * to have zero means; the arrays might contain missing values (Double.NaN);
     * those values are omitted in the computation the covariance (and the
     * number of observations are adjusted).
     *
     * @param x The first array
     * @param y The second array
     * @param t The delay between the two arrays
     * @return The covariance (suitably modified by David to calculate the
     * Diebold-Mariano statistic is: cov = sum((x(i)*y(i+t)) and NOT cov =
     * sum((x(i)*y(i+t)/(n-t))
     */
    public static double cov(double[] x, double[] y, int t) {
        // x and y must have the same Length...
        if (t < 0) {
            return cov(y, x, -t);
        }
        double v = 0;

        //     DescriptiveStatistics MdAE = new DescriptiveStatistics(eAbs_double); 
        //     return MdAE.getMedian();  
        int n = x.length - t;
        int nm = 0;
        for (int i = 0; i < n; ++i) {
            double xcur = x[i];
            double ycur = y[i + t];
            if (DescriptiveStatistics.isFinite(xcur) && DescriptiveStatistics.isFinite(ycur)) {
                v += xcur * ycur;
            } else {
                ++nm;
            }
        }
        int m = x.length - nm;
        if (m == 0) {
            return 0;
        }
        //return v / m;
        return v;
        //return v / x.length;
    }

}
