/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tss.dfm;

import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.dstats.Normal;
import ec.tstoolkit.dstats.ProbabilityType;
import ec.tstoolkit.dstats.T;
import ec.tstoolkit.stats.LjungBoxTest;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataBlock;

/**
 * Quantify measures of predictive accuracy for a forecast "f" and statistical
 * significance with respect to a benchmark "fB". The HAC estimation of the
 * long-run variance is used to construct the test statistics throughout the
 * multiple tests. For small samples, we use the HAR-Diebold-Mariano test
 * proposed by Coroneo and Iacone (2015). We opt for the fixed-b asymptotics and
 * make use of the tabulation of Kiefer and Vogelsang(2005).
 *
 * @author deanton
 * @since 15-January-2015
 *
 * References
 * <ul>
 * <li>  <a href="http://dx.doi.org/10.1080/07350015.1995.10524599">
 * Diebold, F.X. and R.S. Mariano (1995), “Comparing Predictive Accuracy"
 * Journal of Business and Economic Statistics, 13, 253-263.
 * </a>
 * </li>
 * <li>   <a href="http://dx.doi.org/10.1080/07350015.2014.983236">
 * Francis X. Diebold (2015) Comparing Predictive Accuracy, Twenty Years Later:
 * A Personal Perspective on the Use and Abuse of Diebold–Mariano Tests, Journal
 * of Business & Economic Statistics, 33:1, 1-1
 * </a>
 * </li>
 * <li>  <a href="http://www.jstor.org/stable/1392581">
 * David I. Harvey, Stephen J. Leybourne and Paul Newbold (1998) “Tests for
 * Forecast Encompassing” Journal of Business & Economic Statistics, 16, 254-259
 * </a>
 * </li>
 * <li>  <a href= "https://ideas.repec.org/p/yor/yorken/15-15.html">
 * Laura Coroneo and Fabrizio Iacone (2015) “Comparing Predictive Accuracy in
 * Small Sapmles” University of York Discussion Papers in Economics
 * </a>
 * </li>
 * <li>  <a href="http://dx.doi.org/10.1017/S0266466605050565">
 * Nicholas M. Kiefer and Timothy J. Vogelsang (2005) “A New Asymptotic Theory
 * for Heteroskedasticity-Autocorrelation Robust Tests” Econometric Theory, 21,
 * 1130-1164
 * </a>
 * </li>
 * </ul>
 */
public class AccuracyTests { // inner class to make sure it can access elements of the outer class

    //Test Statistics: based on squared losses, absolute losses, and "e"ncompassing       
    private double DM_d, DM_dAbs, DM_d_e, DM_d_e2, DM_bias, DM_biasB, DM_ar, DM_arB;
    private double pDM, pDMabs, pDM_e, pDM_e2, pBias, pBiasB, pAR, pAR_B;

    private int ntotal = -1;   // how does the get work?
    private int order = -1;   // how does the get work? // This is automatically computed?

    // Loss differentials  and means
    private TsData d, dAbs, d_e, d_e2, bias, biasB, ar, arB; // squared and absolute
    private double dBar, dAbsBar, d_eBar, d_e2Bar;
    private final int horizon; // exact horizon in days
    private int hK; // forecast horizon (theoretical)

    // Inputs: data, forecast and errors
    private final TsData f; // forecasts for a given horizon 
    private final TsData fB; // forecasts for a given horizon 

    private final TsData e; // forecast errors: Y-F
    private final TsData eB; // forecast errors benchmark: Y-F

    private DistributionType dType;// this is automatically computed 

    public AccuracyTests(TsData f, TsData fB, TsData y, int h) {
        this.horizon = h;
        this.f = f;
        this.fB = fB;

        e = TsData.subtract(y, f);
        eB = TsData.subtract(y, fB);
    }

    public int getOrder() {
        return order;  // what if it has not been yet calculated?
    }

    public int getNtotal() {
        if (ntotal == -1) {
            ntotal = d.getObsCount();
        }
        return ntotal; // what if it has not been yet calculated?
    }
    
    public DistributionType getDistributionType() {
        return dType;
    }

    public double getTestStat(AsymptoticsType asympType, TestHypothesisType testType) {
        double TestStat = Double.NaN;
        // firt define loss function
        try {
            TestStat = get_AverageLoss(Loss(testType)) / calcHAC_SE(Loss(testType), asympType);
            return TestStat;
        } catch (ArithmeticException ex) {
            return TestStat;
        }
    }

    public double get_AverageLoss(TsData Loss) {
        double AverageLoss = Double.NaN;
        try {
            DescriptiveStatistics LossDesc = new DescriptiveStatistics(Loss);
            AverageLoss = LossDesc.getAverage(); // excluding missing values I suppose
            return AverageLoss;
        } catch (ArithmeticException ex) {
            return AverageLoss;
        }
    }

    public TsData Loss(TestHypothesisType type) {
        switch (type) {
            case EQUAL_FA_quadratic:
                d = e.pow(2).minus(eB.pow(2));
                return d;
            case EQUAL_FA_absolute:
                dAbs = e.abs().minus(eB.abs());
                return dAbs;
            case ENCOMPASSING_BENCHMARK:
                d_e = e.times(e.minus(eB));
                return d_e;
            case ENCOMPASSED_BY_BENCHMARK:
                d_e2 = eB.times(eB.minus(e));
                return d_e2;
            case BIAS:
                bias = e;
                return bias;
            case AR:
                ar = e.drop(1, 0).times(e.drop(0, 1));
                return ar;
            case BIAS_B:
                biasB = eB;
                return biasB;
            case AR_B:
                arB = eB.drop(1, 0).times(eB.drop(0, 1));
                return arB;
        }
        throw new IllegalArgumentException("Invalid type: choose the loss function depending on what do you want to test");
    }

    public double getPvalue(TestHypothesisType testType, AsymptoticsType type) {

        try {
            switch (testType) {

                case EQUAL_FA_quadratic:
                    //   pDM=calcPvalue(testType.EQUAL_FA_quadratic,type);
                    pDM = calcPvalue(testType, type);
                    return pDM;
                case EQUAL_FA_absolute:
                    //   pDM=calcPvalue(testType.EQUAL_FA_quadratic,type);
                    pDMabs = calcPvalue(testType, type);
                    return pDMabs;
                case ENCOMPASSING_BENCHMARK:
                    //   pDM=calcPvalue(testType.EQUAL_FA_quadratic,type);
                    pDM_e = calcPvalue(testType, type);
                    return pDM_e;
                case ENCOMPASSED_BY_BENCHMARK:
                    //   pDM=calcPvalue(testType.EQUAL_FA_quadratic,type);
                    pDM_e2 = calcPvalue(testType, type);
                    return pDM_e2;
                case BIAS:
                    //   pDM=calcPvalue(testType.EQUAL_FA_quadratic,type);
                    pBias = calcPvalue(testType, type);
                    return pBias;
                case BIAS_B:
                    //   pDM=calcPvalue(testType.EQUAL_FA_quadratic,type);
                    pBiasB = calcPvalue(testType, type);
                    return pBiasB;
                case AR:
                    //   pDM=calcPvalue(testType.EQUAL_FA_quadratic,type);
                    pAR = calcPvalue(testType, type);
                    return pAR;
                case AR_B:
                    //   pDM=calcPvalue(testType.EQUAL_FA_quadratic,type);
                    pAR_B = calcPvalue(testType, type);
                    return pAR_B;
            }
            throw new IllegalArgumentException("Invalid type");

        } catch (ArithmeticException ex) {
            return Double.NaN;
        }

    }

    /**
     * P-value associated the null of the encompassing test: the reference model
     * encompasses the benchmark. The p-value is defined under the Null
     * hypothesis that the Model encompasses the benchmark. Values smaller than
     * 0.10 will imply that the benchmark may have some added value at a
     * confidence level larger than 90%
     *
     * @param testType
     * @param type: AsymptoticsType
     * @return pDM_e p-value the Forecast Encompassing Test under the Null.
     */
    public double calcPvalue(TestHypothesisType testType, AsymptoticsType type) {

        dType = DistributionType.NORMAL; // initialization
        double TestStat = Double.NaN;
        double Pvalue = Double.NaN;

        TestStat = getTestStat(type, testType); // only one time is needed now
        // this calculation will automatically redefine the DistributionType 

        switch (type) {
            case STANDARD:
                try {

//                  DM_e = getDM_e(AsymptoticsType.STANDARD); // not needed anymore
                    Normal x = new Normal();
                    Pvalue = 2 * x.getProbability(Math.abs(TestStat), ProbabilityType.Upper);
                    return Pvalue;

                } catch (ArithmeticException ex) {
                    return Double.NaN;
                }

            case HYBRID:

                try {

                    switch (dType) {

                        case NORMAL: // it will never be the case in principle
                            Normal x = new Normal();
                            Pvalue = 2 * x.getProbability(Math.abs(TestStat), ProbabilityType.Upper);
                            return Pvalue;
                        case T_STUDENT:
                            int n = Loss(testType).getObsCount();
                            T y = new T();
                            y.setDegreesofFreedom(n - 1);
                            Pvalue = 2 * y.getProbability(Math.abs(TestStat), ProbabilityType.Upper); // double tail
                            return Pvalue;
                        case KIEFERVOGELSANG:

                            n = Loss(testType).getObsCount(); // already declared in previous case
                            double b = Math.pow(n, 0.5) / n;
                            double cval2 = 1.2816 + 1.3040 * b + 0.5135 * Math.pow(b, 2) - 0.3386 * Math.pow(b, 3);
                            double cval5 = 1.6449 + 2.1859 * b + 0.3142 * Math.pow(b, 2) - 0.3427 * Math.pow(b, 3);
                            double cval10 = 1.9600 + 2.9694 * b + 0.4160 * Math.pow(b, 2) - 0.5324 * Math.pow(b, 3);
                            double cval20 = 2.3263 + 4.1618 * b + 0.5368 * Math.pow(b, 2) - 0.9060 * Math.pow(b, 3);

                            if (TestStat < cval20) {
                                return Pvalue = 1.00;

                            } else if (TestStat < cval10) {
                                return Pvalue = 0.20;

                            } else if (TestStat < cval5) {
                                return Pvalue = 0.10;

                            } else if (TestStat < cval2) {
                                return Pvalue = 0.05;

                            } else {
                                return Pvalue = 0.02;
                            }
                    }
                    throw new IllegalArgumentException("Invalid DistributionType: the distribution assumed for the test is undefined");
                } catch (ArithmeticException ex) {
                    return Double.NaN;
                }
        }
        throw new IllegalArgumentException("Invalid AsymptoticsType: choose asymptotic theory for inference");

    }

    /**
     * Calculates the order of autocorrelation by using Ljung-Box test where H1
     * corresponds to increasingly large orders of autocorrelation until H0
     * (iid) is not rejected. We do not test orders of autocorrelation larger
     * than 5 in order to avoid spurious results.
     *
     * @param lossDiff The time series with the difference of the loss
     * functions.
     * @param MaxAR number of Ljung-Box tests that will be run with increasing
     * orders of autocorrelation
     * @param SignificanceLevel if pval is smaller than SignificanceLevel then
     * we reject the null.
     * @return Integer order of autocorrelation.
     */
    private int calcOrderAutoCorr(TsData lossDiff, int MaxAr, double SignificanceLevel) {

        lossDiff.removeMean();

        hK = (int) getH();
        //    int K = (int) 12*(horizon/lossDiff.getFrequency().intValue());// we will not consider higher orders of autocorrelation than K to avoid spurious results
        LjungBoxTest acTest = new LjungBoxTest();

        IReadDataBlock lossDiffCopy = lossDiff.rextract(0, lossDiff.getLength());

        acTest.usePositiveAc(true); // true: only positive autocorr are considered
        //                                   Even if negative, it should not be discarded if significant

        acTest.setLag(1);

        int h = hK - 1; // MINIMUM order of autocorrelation IN THEORY, it should enter as input!!!!
        double pval = 0;

        // includes a check for autocorrelation of order K  (it shouldn't happen, in theory) 
        //while (pval <= SignificanceLevel && h < K + 1) { // small pvalues suggest "H0: iid" is false
        while (pval <= SignificanceLevel && h < hK - 1 + MaxAr) { // small pvalues suggest "H0: iid" is false

            acTest.setK(h);
            acTest.test(lossDiffCopy);  // calculates the m_val property (test-statistic), which is a protected property of the upper class StatisticalTest
            pval = acTest.getPValue();  // calculates the pvalue under the null*
            h++;// 
        }

        // after the while is finished:
        // pval>0.10 or h=K
        if (pval <= SignificanceLevel) { // (therefore h=K: H0 has been rejected also for K, so let's assume K is the largest significant autocorr                  

            return h; // which is K: this would suggest at least one of the models is misspecified
        } else {
            return h - 1;// which is K-1
        }
        //}         //* LB Test pvalues:
        // when pval>0.10, "H0:iid" is finally not rejected (at 90% confidence level), 
        // against the AR(h) alternative, we propose h-1, which is the last order that was
        // able to represent a strong alternative to the iid hypothesis
    }

    /**
     * Calculates the denominator of all our test statistics. For example, DM =
     * dBar/(sqrt(2pi f(0)/T)). Note that 2pi f(0) is an replaced by an
     * autocorrelation consistent estimate of the (modified) standard deviation
     * of the input time-series for the DM statistic. We calculate it by
     * truncating the autocovariance generating function (ACF). ALGORITHM: In
     * both STANDARD and HIBRID AsymptoticsType, we first test the order of
     * autocorrelation with the Ljung-Box tests at alpha=0.2 (the test starts
     * with an order equal to the forecasting horizon in months, and then it
     * iteratively continues until the absence of autocorrelation can be
     * rejected. - In the case of Standard asymptotics, we use a rectangular
     * window for orders of autocorrelation 0 and 1, and a triangular window
     * with M=T^(1/3) otherwise. If this does not return a positive variance, we
     * simply use the short run variance, as if the order of autocorrelation was
     * zero. The test-statistic will be assumed to follow a Normal distribution.
     * - In the case of Hybrid asymptotics, we use the Harvey, Leybourne and
     * Newbold (HLN) modification for orders of autocorrelation 0 and 1 (in
     * which case the test statistic will follow a student distribution), and
     * HAR proposed by Coroneo and Iacone (2016) based on fixed-b asymptotics
     * for larger orders of autocorrelation. In the last case, a triangular
     * window of width M=T^(1/3) is used to smooth the ACF (we use Diefer and
     * Vogelsang 2005 tabulation) . When the order of autocorrelation is equal
     * to one and the sample is large enough, we still use the HLN midification.
     *
     * @param lossDiff: The loss function differential is a time-series that
     * depends on what we want to test ( Equality of forecast accuracy:
     * difference of squared/abs errors, Bias: errors, etc...)
     * @param type AsymptoticsType: STANDARD, HYBRID, HLN, HAR-Fixed(b),
     * HCR-Fixed(M)
     * @param SmallSample true activates my algorithm: the use of the HLN
     * modification of the DM statistic, which follows a t-student distribution
     * under the null when order of autocorrelation is 0 (typically for h=1, not
     * not necessarily) True should be default for small samples (2-3 years of
     * monthly data; 5-6 years of quarterly data) When the order of
     * autocorrelation is equal to one and the sample is large enough, we still
     * use the HLN midification However, for larger order of autocorrelation, or
     * for very small samples, we use the Kiefer and Vogelsang tabulation (as
     * proposed by Coroneo and Iacone)
     * @return DM_denom (denominator of the Diebold-Mariano statistic): AR
     * consistent estimate of the (modified) standard deviation
     */
    public double calcHAC_SE(TsData lossDiff, AsymptoticsType type) { // introduce marker to know when Normal, T-student or calibrated distribtitions needs to be used

        lossDiff.removeMean();

        // Determine largest significant order of serial autocorrelation in "d", 
        //but make it a function of the sample size
        int MaxAR = 4;
        ntotal = lossDiff.getObsCount(); // counts non missing
        int M = (int) Math.pow(ntotal, (1.0) / 3);
        order = calcOrderAutoCorr(lossDiff, MaxAR, 0.20);
        // calculate 2πf(0)= ∑_{τ=-(T-1))}^{(T-1)}[w(τ) γ(τ)] where γ(τ) is a "modified" 
        // autocovariance of the loss differential 
        double denom = calcDenom(lossDiff, order, KernelType.RECTANGULAR); // USE THE ORDER IDENTIFIED IN A PREVIOUS STEP
        // DM = dBar/(sqrt(2pif/T)), where 2pif = gammaw/T

        int h = order + 1;

        switch (type) {
            case HYBRID:

                if (order == 0) {
                    dType = DistributionType.T_STUDENT;
                    double DM_SqrtDenom = Math.sqrt(denom);
                    double HLN = ntotal / Math.sqrt(ntotal + 1 - 2 * h + (1 / ntotal) * (h) * (h - 1));
                    return HLN * DM_SqrtDenom;
                }

                if (order == 1 & ntotal > 16 & denom > 0) { // short horizons & medium sized samples : better power with HLN than CI
                    dType = DistributionType.T_STUDENT;
                    double DM_SqrtDenom = Math.sqrt(denom);
                    double HLN = ntotal / Math.sqrt(ntotal + 1 - 2 * h + (1 / ntotal) * (h) * (h - 1));
                    return HLN * DM_SqrtDenom;

                } else {
                    // only case in which pvalues need to be adjusted
                    //
                    // Coroneo and Iacone: use fixed-b (=M/T) asymptotics to estimate the variance (denom); 
                    // set M=n^(1/3)
                    // Nice discussion in page 1139 in Kiefer and Vogelsang. 
                    // For highly correlated loss functions (rho>0.9), the standard asymptotics are oversized for small M because
                    // there is a downward bias when you don't add up Gamma(i) for i>0, and variance is high because
                    // Gamma(0)+ Gamma(i) will depend too much on the particular sample chosen. 
                    // Increasing M increases variance but it decreases bias by more, so overall size distortion improves
                    // However, when M is too large there is (downward) bias that 
                    // appears when the Gamma(h) for large h are downweighted (underestimation of the variance
                    // can lead us to refuse the null even when it is true) and on top of that the variance of that estimate
                    // decreases. Still, the CI proposal should 
                    // help to measure the estimation uncertainty in general cases and correct the size, but
                    // but for rho=0.9 or close to one, there is still some oversize (see KF 2005 or Müller 2014)

                    // recalculate denom using triangular window and M bandwidth
                    denom = calcDenom(lossDiff, M, KernelType.TRIANGULAR);
                    if (denom > 0) {
                        dType = DistributionType.KIEFERVOGELSANG;
                        double DM_SqrtDenom = Math.sqrt(denom);
                        return DM_SqrtDenom;
                    } else {
                        h = 1;
                        denom = calcDenom(lossDiff, 0, KernelType.RECTANGULAR);
                        dType = DistributionType.T_STUDENT;

                        double DM_SqrtDenom = Math.sqrt(denom);
                        double HLN = ntotal / Math.sqrt(ntotal + 1 - 2 * h + (1 / ntotal) * (h) * (h - 1));
                        return HLN * DM_SqrtDenom;
                    }
                }

            case STANDARD: // In the case we want to use standard asymptotics

                dType = DistributionType.NORMAL;

                if (order < 2 | denom > 0) {

                    double DM_SqrtDenom = Math.sqrt(denom);
                    return DM_SqrtDenom;
                } else { // necesarily denom<0

                    denom = calcDenom(lossDiff, M, KernelType.TRIANGULAR);
                    if (denom > 0) {
                        double DM_SqrtDenom = Math.sqrt(denom);
                        return DM_SqrtDenom;
                    } else {
                        denom = calcDenom(lossDiff, 0, KernelType.RECTANGULAR);
                        double DM_SqrtDenom = Math.sqrt(denom);
                        return DM_SqrtDenom;
                    }
                }
        }
        throw new IllegalArgumentException("Invalid type");
    }

    /**
     * Calculation of 2pif/T, which will be used in DM = dBar/(sqrt(2pif/T)),
     * where 2pif = gammaw/T
     *
     * @param lossDiff loss differential
     * @param r truncation lag
     * @param type
     * @param kernel KernelType (rectangular window) OR (triangular window)
     * @return
     */
    public double calcDenom(TsData lossDiff, int r, KernelType type) {
        int[] w = new int[(lossDiff.getLength() - 1) + 1];
        double[] gamma = new double[(lossDiff.getLength() - 1) + 1];
        //int[] w = null;
        //double[] gamma = null;
        double gammaw = 0;
        // double TwoPIf = 0;

        int count = 0;
        int T = 0;
        //  for (int tau = -(lossDiff.getLength() - 1); tau <= lossDiff.getLength() - 1; tau++) {
        for (int tau = 0; tau <= lossDiff.getLength() - 1; tau++) {

            // w
//                if (Math.abs(tau / (h - 1)) <= 1) {
//                    w[count] = 1;
//                }
            if (r == 0 && tau == 0) {
                w[count] = 1;
            } else if (r == 0) { // r==0 (it implies that tau>0)
                w[count] = 0;
            } else if ((Math.abs(tau / r) <= 1)) { // r is not zero for sure, tau>r imply w=0 
                // Rectangular window based on r (not r=h-1 in DM)
                switch (type) {
                    case RECTANGULAR:
                        w[count] = 2;
                        break;
                    case TRIANGULAR:
                        w[count] = 2 * (1 - Math.abs(tau / (r + 1))); //  tau=r has small weight, but not zero
                        break;
                }
            }

            gamma[count] = ForecastEvaluationResults.cov(lossDiff.getValues().internalStorage(), lossDiff.getValues().internalStorage(), tau);

            // T
            if (gamma[count] != 0 && !Double.isNaN(gamma[count])) {
                T++; // we only count covariances that can be computed
                gammaw += gamma[count] * w[count]; // sum w*gamma
            }

            count++;
        }

        // DM = dBar/(sqrt(2pif/T)), where 2pif = gammaw/T
        double DM_denom = ((gammaw / T) / T);
        return DM_denom;
    }

    /**
     * Calculates combined forecast resulting from a "forecast encompasing
     * test". MODEL ENCOMPASSING BENCHMARK : fB*alpha + f *(1-alpha) MODEL
     * ENCOMPASSED BY BENCHMARK: f*beta+ fB *(1-beta) One can check that alpha =
     * 1-beta, by construction (otherwise there is a mistake)
     *
     * @param type
     * @return
     */
    public TsData ForecastCombination(TestHypothesisType type) {

        switch (type) {
            case ENCOMPASSING_BENCHMARK:
                TsData X = e.minus(eB);
                TsData Y = e;
                TsDataBlock.all(X.times(X)).data.sum();
                TsDataBlock.all(X.times(Y)).data.sum();
                double alpha = Math.pow(TsDataBlock.all(X.times(X)).data.sum(), -1) * (TsDataBlock.all(X.times(Y)).data.sum());
                TsData c = TsData.add(f.times(alpha), fB.times(1 - alpha));
                return c;
            case ENCOMPASSED_BY_BENCHMARK:
                TsData X2 = eB.minus(e);
                TsData Y2 = eB;
                TsDataBlock.all(X2.times(X2)).data.sum();
                TsDataBlock.all(X2.times(Y2)).data.sum();
                double beta = Math.pow(TsDataBlock.all(X2.times(X2)).data.sum(), -1) * (TsDataBlock.all(X2.times(Y2)).data.sum());
                c = TsData.add(fB.times(beta), f.times(1 - beta));
                return c;
        }
        throw new IllegalArgumentException("Invalid type: this function is only for the case ENCOMPASSING_BENCHMARK and ENCOMPASSED_BY_BENCHMARK");

    }

    public int getH() {
        double temp = Math.ceil((-horizon) * (e.getFrequency().intValue() / 365.25));
        if (temp > 0) {
            hK = (int) temp;
        } else {
            hK = 0;
        }
        return hK;
    }

    /**
     * Computes the (modified) covariance between two arrays of doubles for
     * usage in the construction of the Diebold-Mariano test. The arrays are do
     * not necessarily have zero means and might contain missing values
     * (Double.NaN). Those values are omitted in the computation the covariance
     * . This method modifies the cov method in the DescriptiveStatistics class.
     *
     * @param x The first array
     * @param y The second array
     * @param t The delay between the two arrays
     * @return The covariance (suitably modified by David to calculate the
     * Diebold-Mariano statistic is: cov = sum((x(i)-xbar*y(i+t)-ybar) and NOT
     * cov = sum((x(i)*y(i+t)/(n-t))
     */
    public static double cov(double[] x, double[] y, int t) {
        // x and y must have the same Length...
        if (t < 0) {
            return cov(y, x, -t);
        }
        double v = 0;

        double xbar = 0;
        double ybar = 0;
        int n = x.length - t;
        int nm = 0;
        for (int i = 0; i < n; ++i) {
            double xcur = x[i];
            double ycur = y[i];
            if (Double.isFinite(xcur) && Double.isFinite(ycur)) {
                xbar += xcur;
                ybar += ycur;
            } else {
                ++nm;
            }
        }
        int m = x.length - nm;

        if (m == 0) {
            return 0; // better than NaN
        }
        xbar = xbar / (n - nm);
        ybar = ybar / (n - nm);

        //     DescriptiveStatistics MdAE = new DescriptiveStatistics(eAbs_double); 
        //     return MdAE.getMedian();  
        for (int i = 0; i < n; ++i) {
            double xcur = x[i] - xbar; // demean
            double ycur = y[i + t] - ybar; // demean
            if (Double.isFinite(xcur) && Double.isFinite(ycur)) {
                v += xcur * ycur;
            }
        }

        //return v / m;
        return v;
        //return v / x.length;
    }

    public enum DistributionType {
        UNDEFINED,
        NORMAL,
        T_STUDENT,
        KIEFERVOGELSANG
    }

    public enum AsymptoticsType {
        HLN,
        HAR_FIXED_B,
        HAR_FIXED_M,
        /**
         * Standard asymptotics based on the HAC estimation (normality).
         */
        STANDARD,
        /**
         * Small sample asymptotics based on the HAR modification for small
         * samples or HLN modification for the case of no autocorrelation or
         * autocorrelation of order one. The algorithm exploits a Ljung-Box test
         * to decide on the asymptotic distribution: either T_student or Kiefer
         * & Vogelsang calibration).
         *
         * @ see DistributionType
         *
         */
        HYBRID // 
    }

    public enum KernelType {
        TRIANGULAR, // Bartlett
        RECTANGULAR
    }

    public enum TestHypothesisType {
        /**
         * Based on: Squared Error of the model minus squared error of the
         * Benchmark model. Useful for testing differences in forecasting
         * accuracy
         *
         * d =(y-f)^2-(y-f_b)^2
         */
        EQUAL_FA_quadratic,
        /**
         * Based on: Abs Error of the model times abs error of the Benchmark
         * model. Useful for testing differences in forecasting accuracy
         *
         * dABS=abs(y-f) -abs(y-f_b)
         */
        EQUAL_FA_absolute,
        /**
         * H0: Model encompasses Benchmark H1: There is relevant information in
         * the Benchmark forecast The test statistic represents the weight
         * associated to the benchmark model so that values close to zero imply
         * that the reference model encompasses the benchmark (H0). Consider the
         * regression: (y-f) = alpha * (f_b-f)+ error or equivalently e= alpha *
         * (e-e_b)+ error. If alpha is significant, it means that the benchmark
         * should be exploited For example, the model forecast could be improved
         * by combining it with the benchmark. Thus, alpha can be considered as
         * a HAC estimate of the weight of the benchmark model in the following
         * regression: y = alpha f_b + (1-alpha) f + error.
         *
         * @ see ForecastCombination to calculate f_c = alpha f_b + (1-alpha) f
         * Based on: Error of the Benchmark model times its difference with
         * respect the error of our preferred (encompassing) model Useful for
         * the test HO: the benchmark model encompasses our reference model
         *
         * d_e2 =(y-f_b)*((y-f_b)*(y-f))
         */
        ENCOMPASSING_BENCHMARK,
        /**
         * H0: Benchmark encompasses Model H1: There is relevant information in
         * the Model that is not in Benchmark The test statistic represents the
         * weight associated to our Model so that values close to zero imply
         * that the Benchmark encompasses our model (H0). Consider the
         * regression: (y-f_b) = alpha * (f-f_b)+ error or equivalently e= alpha
         * * (e_b-e)+ error. If alpha is significant, it means that the Model
         * should be exploited to improve f_b. For example, the benchmark
         * forecast could be improved by combining it with those of our model.
         * Thus, alpha can be considered as a HAC estimate of the weight of the
         * benchmark model in the following regression: y = alpha f + (1-alpha)
         * f_b + error.
         *
         * @ see ForecastCombination to calculate f_c = alpha f_b + (1-alpha) f
         * Based on: Error of the Benchmark model times its difference with
         * respect the error of our preferred (encompassing) model Useful for
         * the test HO: the benchmark model encompasses our reference model
         *
         * d_e2 =(y-f_b)*((y-f_b)*(y-f))
         */
        ENCOMPASSED_BY_BENCHMARK,
        /**
         * H0: Bias is significant (using HAC estimates of variance) Based on:
         * Forecast Error (model or benchmark)
         *
         * e = (y-f) , eB= (y-f_b)
         */
        BIAS, BIAS_B,
        /**
         * H0: AR coefficient is significant (using HAC estimates of variance).
         * Based on: Forecast Error times its first lag
         *
         * eAR = e*e(-1), eAR_B = eB*eB(-1),
         */
        AR, AR_B
    }
}
