/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tss.dfm;

import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.timeseries.simplets.TsData;

/**
 * Quantify measures of predictive accuracy of a forecast "f" and (relative to)
 * a benchmark "fB".
 *
 * @author deanton
 * @since 15-January-2015
 * <ul>
 * <li>Scale Dependent Measures</li>
 * <ul>
 * <li>RMSE</li>
 * <li>MAE</li>
 * <li>MdAE</li>
 * </ul>
 * <li>Percentage Errors</li>
 * <ul>
 * <li>RMSPE</li>
 * <li>sMAPE</li>
 * <li>sMdAPE</li>
 * </ul>
 * <li>Scaled Errors</li>
 * <ul>
 * <li>RMSSE</li>
 * <li>MASE</li>
 * <li>MdASE</li>
 * </ul>
 * <li>Relative Errors </li>
 * <ul>
 * <li>calcPB</li>
 * <li>relRMSE(DM test) </li>
 * <li>relMAE (DM test)</li>
 * <li>relMdAE</li>
 * <li>RMSPE</li>
 * <li>sMAPE</li>
 * <li>sMdAPE</li>
 * <li>RMSSE</li>
 * <li>MASE</li>
 * <li>MdASE</li>
 * </ul>
 * </ul>
 *
 * References
 * <ul>
 * <li>
 * <a href="http://dx.doi.org/10.1016/j.ijforecast.2006.03.001">
 * Rob J. Hyndman and Anne B. Koehler (2006) “Another look at measures of
 * forecast accuracy” International Journal of Forecasting. 22(4), 679-688
 * </a>
 * </li>
 */
public class ForecastEvaluation {

    // Inputs: data, forecast and errors
    private final TsData y; // true data
    private final TsData f; // forecasts for a given horizon 
    private final TsData fB; // forecasts for a given horizon 

    private final TsData e; // forecast errors: Y-F
    private final TsData eB; // forecast errors benchmark: Y-F

    /**
     * Calculates forecast errors for reference model and for benchmark
     *
     * @param f
     * @param fB
     * @param y
     */
    public ForecastEvaluation(TsData f, TsData fB, TsData y) {
        this.f = f;
        this.fB = fB;
        this.y = y;

        e = TsData.subtract(y, f);
        eB = TsData.subtract(y, fB);
    }

    public AccuracyTests createTests(int horizon) {  // method of FE class that creates object of AT class... design?
        return new AccuracyTests(f, fB, y, horizon);
    }

    /**
     * Scale dependent measures: RMSE (squared root of the mean squared error)
     *
     * @return RMSE
     */
    public double calcRMSE() {
        try {
            double temp = 0;
            int count = 0;
            for (int i = 0; i < e.getLength(); i++) {
                if (!e.isMissing(i)) {
                    temp += Math.pow(e.get(i), 2);
                    count++;
                }
            }
            return Math.sqrt(temp / count);
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Scale dependent measures: RMSE for benchmark (squared root of the mean
     * squared error)
     *
     * @return RMSE for Benchmark
     */
    public double calcRMSE_Benchmark() {
        try {
            double temp = 0;
            int count = 0;
            for (int i = 0; i < eB.getLength(); i++) {
                if (!eB.isMissing(i)) {
                    temp += Math.pow(eB.get(i), 2);
                    count++;
                }
            }
            return Math.sqrt(temp / count);
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Scale dependent measures: MAE (mean of the absolute error)
     *
     * @return MAE
     */
    public double calcMAE() {
        try {
            double temp = 0;
            int count = 0;
            for (int i = 0; i < e.getLength(); i++) {
                if (!e.isMissing(i)) {
                    temp += Math.abs(e.get(i));
                    count++;
                }
            }
            return temp / count;
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Scale dependent measures: MAE for the benchmark (mean of the absolute
     * error)
     *
     * @return MAE for Benchmark
     */
    public double calcMAE_Benchmark() {
        try {
            double temp = 0;
            int count = 0;
            for (int i = 0; i < eB.getLength(); i++) {
                if (!eB.isMissing(i)) {
                    temp += Math.abs(eB.get(i));
                    count++;
                }
            }
            return temp / count;
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Scale dependent measures: MdAE (Median of the Absolute Error)
     *
     * @return MdAE
     */
    public double calcMdAE() {
        TsData eAbs = e.abs();
        DescriptiveStatistics MdAE = new DescriptiveStatistics(eAbs);
        return MdAE.getMedian();
    }

    /**
     * Scale dependent measures: MdAE for benchmark (Median of the Absolute
     * Error)
     *
     * @return MdAE for Benchmark
     */
    public double calcMdAE_Benchmark() {
        TsData eAbs = eB.abs();
        DescriptiveStatistics MdAE = new DescriptiveStatistics(eAbs);
        return MdAE.getMedian();
    }

    /**
     * Percentage Errors: RMSPE (squared root of the mean of squared "percentage
     * errors"). Problems: - Heavier penalty on positive errors; - Can take
     * negative values; - Useless for variables that take values that are close
     * to zero;
     *
     * @return RMSPE
     */
    public double calcRMSPE() {
        try {
            double temp = 0;
            int count = 0;
            for (int i = 0; i < e.getLength(); i++) {
                if (!e.isMissing(i)) {
                    temp += Math.pow(e.get(i) / y.get(i), 2);
                    count++;
                }
            }
            return 100 * Math.sqrt(temp / count);
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Percentage Errors: RMSPE for Benchmark (squared root of the mean of
     * squared "percentage errors").
     *
     * @return RMSPE_Benchmark
     */
    public double calcRMSPE_Benchmark() {
        try {
            double temp = 0;
            int count = 0;
            for (int i = 0; i < eB.getLength(); i++) {
                if (!eB.isMissing(i)) {
                    temp += Math.pow(eB.get(i) / y.get(i), 2);
                    count++;
                }
            }
            return 100 * Math.sqrt(temp / count);
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Percentage Errors: sMAPE (Symmetric Mean Absolute Percentage Error). -
     * Heavier penalty when forecasts are low - Symmetric in the sense that
     * positive or negative errors are equally penalized - Useless for variables
     * that take values that are close to zero
     *
     * @return sMAPE
     */
    public double calc_sMAPE() {
        try {
            double temp = 0;
            int count = 0;
            for (int i = 0; i < e.getLength(); i++) {
                if (!e.isMissing(i)) {
                    temp += Math.abs(Math.abs(e.get(i)) / (y.get(i) + f.get(i)));
                    count++;
                }
            }
            return 200 * temp / count;
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Percentage Errors: sMAPE for benchmark (Symmetric Mean Absolute
     * Percentage Error).
     *
     * @return sMAPE for Benchmark
     */
    public double calc_sMAPE_Benchmark() {
        try {
            double temp = 0;
            int count = 0;
            for (int i = 0; i < eB.getLength(); i++) {
                if (!eB.isMissing(i)) {
                    temp += Math.abs(Math.abs(eB.get(i)) / (y.get(i) + f.get(i)));
                    count++;
                }
            }
            return 200 * temp / count;
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Percentage Errors: sMdAPE (Symmetric Median Absolute Percentage Error). -
     * Heavier penalty when forecasts are low - Symmetric in the sense that
     * positive or negative errors are equally penalized - Useless for variables
     * that take values that are close to zero
     *
     * @return sMdAPE
     */
    public double calc_sMdAPE() {
        TsData eAbs = e.abs();
        double[] eAbs_double = new double[eAbs.getLength()];
        for (int i = 0; i < eAbs.getLength(); i++) {
            eAbs_double[i] = Math.abs(eAbs.get(i) / (y.get(i) + f.get(i)));
        }
        DescriptiveStatistics sMdAE = new DescriptiveStatistics(eAbs_double);
        return 200 * sMdAE.getMedian();
    }

    /**
     * Percentage Errors: sMdAPE (Symmetric Median Absolute Percentage Error). -
     * Heavier penalty when forecasts are low - Symmetric in the sense that
     * positive or negative errors are equally penalized - Useless for variables
     * that take values that are close to zero
     *
     * @return sMdAPE for Benchmark
     */
    public double calc_sMdAPE_Benchmark() {
        TsData eAbs = eB.abs();
        double[] eAbs_double = new double[eAbs.getLength()];
        for (int i = 0; i < eAbs.getLength(); i++) {
            eAbs_double[i] = Math.abs(eAbs.get(i) / (y.get(i) + f.get(i)));
        }
        DescriptiveStatistics sMdAE = new DescriptiveStatistics(eAbs_double);
        return 200 * sMdAE.getMedian();
    }

    /**
     * Scaled Errors: RMS(S)E (Root Mean Squared (Scaled) Error). A scaled error
     * is less than one if it arises from a better forecast than the average
     * one-step naive forecast computed in-sample. Conversely, it is greater
     * than one if the forecast is worse than the average one-step naive
     * forecast computed in-sample. - They have a meaningful scale, are widely
     * applicable - Not subject to the degeneracy problems
     *
     * @return RMS(S)E
     */
    public double calcRMSSE() {
        try {
            // calculate scaling factor first
            double scaling;
            double cumsum = 0;
            int count = 0;
            for (int i = 1; i < y.getLength(); i++) {
                if (!e.isMissing(i) && !y.isMissing(i) && !y.isMissing(i - 1)) {
//                  cumsum += Math.abs(y.get(i) - y.get(i - 1));
                    cumsum += Math.pow(y.get(i) - y.get(i - 1), 2);

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
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Scaled Errors: RMS(S)E for Benchmark (Root Mean Squared (Scaled) Error).
     *
     * @return return RMS(S)E for Benchmark
     */
    public double calcRMSSE_Benchmark() {
        try {
            // calculate scaling factor first
            double scaling;
            double cumsum = 0;
            int count = 0;
            for (int i = 1; i < y.getLength(); i++) {
                if (!eB.isMissing(i) && !y.isMissing(i) && !y.isMissing(i - 1)) {
//                  cumsum += Math.abs(y.get(i) - y.get(i - 1));
                    cumsum += Math.pow(y.get(i) - y.get(i - 1), 2);
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
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Scaled Errors: MA(S)E (Mean Absolute (Scaled) Error). A scaled error is
     * less than one if it arises from a better forecast than the average
     * one-step naive forecast computed in-sample. Conversely, it is greater
     * than one if the forecast is worse than the average one-step naive
     * forecast computed in-sample. - They have a meaningful scale, are widely
     * applicable - Not subject to the degeneracy problems - Less sensitive to
     * outliers than RMS(S)E - Less variable on small samples than MdA(S)E.
     *
     * @return MA(S)E
     */
    public double calcMASE() {
        try {
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
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Scaled Errors: MA(S)E for benchmark (Mean Absolute (Scaled) Error).
     *
     * @return MA(S)E for Benchmark
     */
    public double calcMASE_Benchmark() {
        try {
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
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Scaled Errors: MdA(S)E (Median Absolute (Scaled) Error) . A scaled error
     * is less than one if it arises from a better forecast than the average
     * one-step naive forecast computed in-sample. Conversely, it is greater
     * than one if the forecast is worse than the average one-step naive
     * forecast computed in-sample. - They have a meaningful scale, are widely
     * applicable - Not subject to the degeneracy problems - May be too volatile
     * in small samples, but robust to outliers.
     *
     * @return MdA(S)E
     */
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
        scaling = cumsum / count; // better use median??
        TsData eAbs = e.abs();
        double[] eAbs_double = new double[eAbs.getLength()];
        for (int i = 0; i < eAbs.getLength(); i++) {
            eAbs_double[i] = Math.abs(e.get(i) / scaling);
        }
        DescriptiveStatistics sMdASE = new DescriptiveStatistics(eAbs_double);
        return sMdASE.getMedian();
    }

    /**
     * Scaled Errors: MdA(S)E for benchmark (Median Absolute (Scaled) Error) .
     *
     * @return MdA(S)E for Benchmark
     */
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

    /**
     * Relative Measures: PB (Percentage of periods with smaller absolute error
     * than a benchmark). Different from Hyndman and Koehler's measure, which
     * corresponds to the number of variables for which a forecasting method is
     * better.
     *
     * @return PB
     */
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
        if (count == 0) {
            return Double.NaN;
        } else {
            return 100 * temp / count;
        }
    }

    /**
     * Relative Measures: RelRMSE (RMSE divided by RMSE of benchmark).
     *
     * @return RelRMSE
     */
    public double calcRelRMSE() {
        try {
            return calcRMSE() / calcRMSE_Benchmark();
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Relative Measures: RelRAE (RMAE divided by RMAE of benchmark).
     *
     * @return RelRAE
     */
    public double calcRelMAE() {
        try {
            return calcMAE() / calcMAE_Benchmark();
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Relative Measures: RelRMdAE (RMdAE divided by RMdAE of benchmark).
     *
     * @return RelRMdAE
     */
    public double calcRelMdAE() {
        try {
            return calcMdAE() / calcMdAE_Benchmark();
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Relative Measures: RelRMSPE (RMSPE divided by RMSPE of benchmark).
     *
     * @return RelRMSPE
     */
    public double calcRelRMSPE() {
        try {
            return calcRMSPE() / calcRMSPE_Benchmark();
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Relative Measures: Rel_sMAPE (sMAPE divided by sMAPE of benchmark).
     *
     * @return Rel_sMAPE
     */
    public double calcRel_sMAPE() {
        try {
            return calc_sMAPE() / calc_sMAPE_Benchmark();
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Relative Measures: Rel_sMdAPE (sMdAPE divided by sMdAPE of benchmark).
     *
     * @return Rel_sMdAPE
     */
    public double calcRel_sMdAPE() {
        try {
            return calc_sMdAPE() / calc_sMdAPE_Benchmark();
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Relative Measures: RelRMSSE (RelRMSSE divided by RelRMSSE of benchmark).
     *
     * @return RelRMSSE
     */
    public double calcRelRMSSE() {
        try {
            return calcRMSSE() / calcRMSSE_Benchmark();
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Relative Measures: RelMASE (RelMASE divided by RelMASE of benchmark).
     *
     * @return RelMASE
     */
    public double calcRelMASE() {
        try {
            return calcMASE() / calcMASE_Benchmark();
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    /**
     * Relative Measures: RelMdASE (RelMdASE divided by RelMdASE of benchmark).
     *
     * @return RelMdASE
     */
    public double calcRelMdASE() {
        try {
            return calcMdASE() / calcMdASE_Benchmark();
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }
}
