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

import ec.tss.dfm.ForecastEvaluationResults;
import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.dstats.Normal;
import ec.tstoolkit.dstats.ProbabilityType;
import ec.tstoolkit.stats.LjungBoxTest;
import ec.tstoolkit.timeseries.simplets.TsData;

/**
 *
 * @author Mats Maggi
 */
public abstract class AccuracyTests {

    protected final TsData e;

    protected double val = Double.NaN;
    protected double testStat = Double.NaN;
    private TsData loss = null;

    private int order;
    private Integer delay = null;
    private Integer fctHorizon = null;

    private DistributionType dType;
    private AsymptoticsType asympType;
    private int nTotal = -1;
    private int bandwith = -1;
    private boolean denomPositive = false;

    private double denomIdeal;

    void setDenomIdeal(double value) {
        this.denomIdeal = value;
    }

    protected AccuracyTests(TsData fcts, TsData y, AsymptoticsType asympType) {
        this(TsData.subtract(y, fcts), asympType);
    }

    protected AccuracyTests(TsData fcts, TsData y, AsymptoticsType asympType, Integer delay, Integer horizon) {
        this(fcts, y, asympType);
        this.delay = delay;
        this.fctHorizon = horizon;
    }

    protected AccuracyTests(TsData error, AsymptoticsType asympType) {
        this.e = error;
        this.asympType = asympType;
    }

    public void setForecastHorizon(Integer fctHorizon) {
        this.fctHorizon = fctHorizon;
    }

    public Integer getForecastHorizon() {
        if (fctHorizon == null) {
            fctHorizon = calcForecastHorizon();
        }
        return fctHorizon;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public boolean getDenomPositive() {
        return denomPositive;
    }

    private boolean twoSided = false;

    private int calcForecastHorizon() {
        if (delay == null) {
            return 0;
        }

        double temp = Math.ceil((-delay) * (getLoss().getFrequency().intValue() / 365.25));
        if (temp > 0) {
            return (int) temp;
        } else {
            return 0;
        }
    }

    protected void clear() {
        val = Double.NaN;
        testStat = Double.NaN;
        loss = null;
        nTotal = -1;
        delay = 0;
        delay = null;
        fctHorizon = null;
    }

    public boolean isTwoSided() {
        return twoSided;
    }

    public void setTwoSided(boolean twoSided) {
        this.twoSided = twoSided;
    }

    public double getPValue(boolean twoSided) {

        if (twoSided != this.twoSided) {
            val = Double.NaN;
        }
        if (Double.isNaN(val)) {
            val = calcPValue(twoSided);
        }
        return val;
    }

    public TsData getLoss() {
        if (loss == null) {
            loss = calcLoss();
        }
        return loss;
    }

    public void setLoss(TsData loss) {
        this.loss = loss;
    }

    public void setBandwith(int bandwith) {
        if (bandwith < 0) {
            throw new IllegalArgumentException("Bandwith must be positive !");
        }
        this.bandwith = bandwith;
    }

    public DistributionType getDistributionType() {
        return dType;
    }

    public int getOrder() {

        return order;
    }

    public double getTestStat() {
        if (Double.isNaN(testStat)) {
            testStat = calcTestStat(asympType);
        }
        return testStat;
    }

    public abstract TsData calcLoss();

    public double getAverageLoss() {
        try {
            DescriptiveStatistics lossDesc = new DescriptiveStatistics(getLoss());
            return lossDesc.getAverage();
        } catch (ArithmeticException ex) {
            return Double.NaN;
        }
    }

    private double calcHAC_SE(AsymptoticsType asympType) {
        TsData lossCentered = getLoss().clone();
        lossCentered.removeMean();

        nTotal = lossCentered.getObsCount();
        int maxAR = 4;
        order = calcOrderAutoCorr(lossCentered, maxAR, 0.20);

        double denom;
        if (bandwith < 0) {
            bandwith = (int) Math.pow(nTotal, (1.0) / 2);
            denom = calcDenom(lossCentered, order, KernelType.RECTANGULAR);
            if (denom > 0) {
                denomPositive = true;
            }
        } else {
            denom = calcDenom(lossCentered, bandwith, KernelType.RECTANGULAR);
            if (denom > 0) {
                denomPositive = true;
            }

        }

        int h = order + 1;

        switch (asympType) {
            case HYBRID:
                if (order == 0) {
                    dType = DistributionType.T_STUDENT;
                    double hln = nTotal / Math.sqrt(nTotal + 1 - 2 * h + (1 / nTotal) * (h) * (h - 1));
                    return hln * Math.sqrt(denom);
                }

                if (order == 1 && nTotal > 16 && denom > 0) {
                    dType = DistributionType.T_STUDENT;
                    double hln = nTotal / Math.sqrt(nTotal + 1 - 2 * h + (1 / nTotal) * (h) * (h - 1));
                    return hln * Math.sqrt(denom);

                } else {
                    denom = calcDenom(lossCentered, bandwith, KernelType.TRIANGULAR);
                    if (denom > 0) {
                        denomPositive = true;
                    }

                    if (denom > 0) {
                        dType = DistributionType.KIEFERVOGELSANG;
                        return Math.sqrt(denom);
                    } else {
                        h = 1;
                        denom = calcDenom(lossCentered, 0, KernelType.RECTANGULAR);
                        dType = DistributionType.T_STUDENT;

                        double hln = nTotal / Math.sqrt(nTotal + 1 - 2 * h + (1 / nTotal) * (h) * (h - 1));
                        return hln * Math.sqrt(denom);
                    }
                }
            case STANDARD: // In the case we want to use standard asymptotics
                dType = DistributionType.NORMAL;
                if (order < 2 || denom > 0) {
                    return Math.sqrt(denom);
                } else {
                    denom = calcDenom(lossCentered, bandwith, KernelType.TRIANGULAR);
                    if (denom > 0) {
                        return Math.sqrt(denom);
                    } else {

                        denom = calcDenom(lossCentered, 0, KernelType.RECTANGULAR);
                        return Math.sqrt(denom);
                    }
                }

            case HLN: // Correct buggs using test method 4 (probably mixing doubles and integers creates the problem)
                dType = DistributionType.T_STUDENT;

                // denom has been calculated with "order" unless bandwidth is set
                if (denom > 0) {
                    denomPositive = true;
                    double hln = nTotal / Math.sqrt((double) nTotal + 1 - 2 * h + (1 / nTotal) * (h) * (h - 1));
                    return hln * Math.sqrt(denom);

                } else {

                    denom = calcDenom(lossCentered, 0, KernelType.RECTANGULAR);
                    // fixed  = Math.sqrt(nTotal)/
                    double hln = Math.sqrt(nTotal) / Math.sqrt((double) nTotal + 1 - 2 * 1 + (1 / nTotal) * (1) * (1 - 1));
                    return hln * Math.sqrt(denom);
                }

            case STANDARD_FIXED_B: // In the case we want to use standard asymptotics
                dType = DistributionType.NORMAL;

                denom = calcDenom(lossCentered, bandwith, KernelType.TRIANGULAR);
                if (denom > 0) {
                    denomPositive = true;
                    return Math.sqrt(denom);
                } else {

                    //denom = calcDenom(lossCentered, 0, KernelType.RECTANGULAR);
                    denom = 0.000000000001;
                    return Math.sqrt(denom);
                }

            case HAR_FIXED_B:
                if (bandwith < 0) {
                    bandwith = (int) Math.pow(nTotal, 0.5);
                }
                denom = calcDenom(lossCentered, bandwith, KernelType.TRIANGULAR);
                dType = DistributionType.KIEFERVOGELSANG;
                if (denom > 0) {
                    denomPositive = true;
                    return Math.sqrt(denom);
                } else {
                    // h = 1;
                    // denom = calcDenom(lossCentered, 0, KernelType.RECTANGULAR);
                    //  dType = DistributionType.T_STUDENT;

                    //    double hln = nTotal / Math.sqrt(nTotal + 1 - 2 * h + (1 / nTotal) * (h) * (h - 1));
                    //    return hln * Math.sqrt(denom);
                    denom = 0.000000000001;
                    return Math.sqrt(denom);
                }

            case UNFEASIBLE:
                // bandwith not needed!
                //if (bandwith<0){
                //bandwith = (int)Math.pow(nTotal, 0.5);
                // }

                dType = DistributionType.NORMAL;
                return Math.sqrt(denomIdeal);

            default:
                throw new UnsupportedOperationException("Type not yet supported : "
                        + asympType.toString());
        }
    }

    private double calcDenom(TsData lossCentered, int r, KernelType type) {
        double w = 0;
        double gammaw = 0;

        for (int tau = 0; tau <= r; tau++) {
            if (tau == 0) {
                w = 1.0;
            } else if ((Math.abs((double) tau / (double) r) <= 1)) {
                switch (type) {
                    case RECTANGULAR:
                        w = 2.0;
                        break;
                    case TRIANGULAR:
                        w = 2.0 * (1.0 - Math.abs((double) tau / ((double) r + 1.0)));
                        break;
                    default:
                        throw new IllegalArgumentException("Wrong kernel type : "
                                + type.toString());
                }
            }

            double cov = ForecastEvaluationResults.cov(
                    lossCentered.internalStorage(),
                    lossCentered.internalStorage(),
                    tau);

            if (Double.isNaN(cov)) {
                throw new IllegalArgumentException("Covariance equals NaN !");
            } else if (cov != 0) {
                gammaw += cov * w;
            }
        }

        return (((double) gammaw / (double) nTotal) / (double) nTotal);
    }

    private int calcOrderAutoCorr(TsData lossCentered, int maxAR, double significanceLevel) {

        LjungBoxTest acTest = new LjungBoxTest();

        IReadDataBlock lossCenteredCopy = lossCentered.rextract(0, lossCentered.getLength());
        acTest.usePositiveAc(true);
        acTest.setLag(1);

        int h = getForecastHorizon() - 1;
        double pval = 0;

        while (pval <= significanceLevel && h < getForecastHorizon() - 1 + maxAR) {
            acTest.setK(h);
            acTest.test(lossCenteredCopy);
            pval = acTest.getPValue();
            h++;
        }

        if (pval <= significanceLevel) {
            return h;
        } else {
            return h - 1;
        }
    }

    private double calcTestStat(AsymptoticsType asympType) {
        return getAverageLoss() / calcHAC_SE(asympType);
    }

    private double calcPValue(boolean twoSided) {
        getTestStat(); // Computes TestStat if not done yet

        switch (asympType) {

            case UNFEASIBLE:
                try {
                    Normal x = new Normal();
                    if (twoSided) {
                        return 2 * x.getProbability(Math.abs(testStat), ProbabilityType.Upper);
                    } else {
                        return x.getProbability((testStat), ProbabilityType.Upper);
                    }
                } catch (ArithmeticException ex) {
                    return Double.NaN;
                }
            case STANDARD:
                try {
                    Normal x = new Normal();

                    if (twoSided) {
                        return 2 * x.getProbability(Math.abs(testStat), ProbabilityType.Upper);
                    } else {
                        return x.getProbability((testStat), ProbabilityType.Upper);
                    }

                } catch (ArithmeticException ex) {
                    return Double.NaN;
                }
            case STANDARD_FIXED_B:
                try {
                    Normal x = new Normal();
                    //                return 2.0 * x.getProbability(Math.abs(testStat), ProbabilityType.Upper);
                    if (twoSided) {
                        return 2 * x.getProbability(Math.abs(testStat), ProbabilityType.Upper);
                    } else {
                        return x.getProbability((testStat), ProbabilityType.Upper);
                    }

                    //    return  x.getProbability(Math.abs(testStat), ProbabilityType.Upper);
                } catch (ArithmeticException ex) {
                    return Double.NaN;
                }

            case HAR_FIXED_B:
                // David: kiefer and vogelsang tabulation for Bartlett window

                double b = (double) bandwith / (double) nTotal; // bandwith is either fixed or made dependent on nTotal
                double cval_90 = 1.2816 + 1.3040 * b + 0.5135 * Math.pow(b, 2.0) - 0.3386 * Math.pow(b, 3.0);
                double cval_95 = 1.6449 + 2.1859 * b + 0.3142 * Math.pow(b, 2.0) - 0.3427 * Math.pow(b, 3.0);
                double cval_97_5 = 1.9600 + 2.9694 * b + 0.4160 * Math.pow(b, 2.0) - 0.5324 * Math.pow(b, 3.0);
                double cval_99 = 2.3263 + 4.1618 * b + 0.5368 * Math.pow(b, 2.0) - 0.9060 * Math.pow(b, 3.0);

                double threshold = 0;
                if (twoSided) {
                    if ((Math.abs(testStat)) < cval_90) {
                        threshold = Double.NaN; // rejection the null at significance level within interval [1, 0.1])
                    } else if ((Math.abs(testStat)) < cval_95) {
                        threshold = 0.10; // rejection the null at significance level within interval [0.1, 0.05))
                    } else if ((Math.abs(testStat)) < cval_97_5) {
                        threshold = 0.05; // [0.05, 0.025))
                    } else if ((Math.abs(testStat)) < cval_99) {
                        threshold = 0.025; // [0.025, 0.01))
                    } else {
                        threshold = 0.01; // rejection the null at significance level p=0.01 (also p<0.01)
                    }
                    return 2 * threshold;

                } else {

                    if ((testStat) < cval_90) {
                        threshold = Double.NaN; // rejection the null at significance level within interval [1, 0.1])
                    } else if ((testStat) < cval_95) {
                        threshold = 0.10; // rejection the null at significance level within interval [0.1, 0.05))
                    } else if ((testStat) < cval_97_5) {
                        threshold = 0.05; // [0.05, 0.025))
                    } else if ((testStat) < cval_99) {
                        threshold = 0.025; // [0.025, 0.01))
                    } else {
                        threshold = 0.01; // rejection the null at significance level p=0.01 (also p<0.01)
                    }
                    return threshold;
                }

            default:
                throw new IllegalArgumentException("Invalid AsymptoticsType: choose "
                        + "asymptotic theory for inference");
        }
    }

    public enum TestHypothesisType {
        EQUAL_FA_QUAD,
        EQUAL_FA_ABS,
        ENCOMPASSING_BENCHMARK,
        ENCOMPASSED_BY_BENCHMARK,
        BIAS, BIAS_B,
        AR, AR_B
    }

    public enum DistributionType {
        UNDEFINED,
        NORMAL,
        T_STUDENT,
        KIEFERVOGELSANG
    }

    public enum KernelType {
        TRIANGULAR,
        RECTANGULAR
    }

    public enum AsymptoticsType {
        UNFEASIBLE,
        HLN,
        HAR_FIXED_B,
        HAR_FIXED_M,
        STANDARD,
        STANDARD_FIXED_B,
        HYBRID
    }
}
