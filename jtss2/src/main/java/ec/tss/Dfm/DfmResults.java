/*
 * Copyright 2014 National Bank of Belgium
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
package ec.tss.Dfm;

import ec.tstoolkit.algorithm.IProcResults;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockStorage;
import ec.tstoolkit.dfm.DfmInformationSet;
import ec.tstoolkit.dfm.DfmProcessor;
import ec.tstoolkit.dfm.DynamicFactorModel;
//import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementDescriptor;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.information.InformationMapper;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.maths.matrices.CroutDoolittle;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import ec.tstoolkit.mssf2.DefaultMultivariateSsf;
import ec.tstoolkit.mssf2.IMSsf;
import ec.tstoolkit.mssf2.MFilteringResults;
import ec.tstoolkit.mssf2.MSmoothingResults;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jean Palate
 */
public class DfmResults implements IProcResults {

    private final DynamicFactorModel model;
    private final DfmInformationSet input;
    // optimization (if any)
    private Likelihood likelihood;
    private Matrix hessian;
    private double[] gradient;
    // smoothing/filtering
    private MSmoothingResults smoothing;
    private MFilteringResults filtering;
    private TsData[] smoothedShocks; // one Ts for each shock
    private TsData[] smoothedNoise;  // one Ts for each observable
    private TsData[] theData; // one Ts for each observable
    private TsData[] smoothedSignal;  // one Ts for each observable
    private TsData[][] shockDecomposition;
    private Matrix[] varianceDecomposition;

    public DfmResults(DynamicFactorModel model, DfmInformationSet input) {
        this.model = model;
        this.input = input;
    }

    public DynamicFactorModel getModel() {
        return model;
    }

    public DfmInformationSet getInput() {
        return input;
    }

    public TsData getFactor(int idx) {
        if (smoothing == null) {
            calcSmoothedStates();
        }
        TsDomain currentDomain = input.getCurrentDomain();
        return new TsData(currentDomain.getStart(), smoothing.component(idx * model.getBlockLength()), true);
    }

    public TsData getFactorStdev(int idx) {
        if (smoothing == null) {
            calcSmoothedStates();
        }
        TsDomain currentDomain = input.getCurrentDomain();
        return new TsData(currentDomain.getStart(), smoothing.componentStdev(idx * model.getBlockLength()), true);
    }

    private void calcSmoothedStates() {
        DfmProcessor processor = new DfmProcessor();
        processor.setCalcVariance(true);
        processor.process(model, input);
        smoothing = processor.getSmoothingResults();
        filtering = processor.getFilteringResults();
    }

    public TsData[] getTheData() {
        if (input == null) {
            throw new Error("There is no data");
        }

        if (theData == null) {
            pleaseGetTheData();
        }

        return theData;
    }

    public void pleaseGetTheData() {

        theData = new TsData[input.getSeriesCount()];
        for (int i = 0; i < input.getSeriesCount(); i++) {
            theData[i] = input.series(i);
        }

    }

    public TsData[] getSignal() {
        if (smoothedSignal == null) {
            calcSmoothedSignal(); // It is not well calculated because the signal becomes the same for all variables
        }
        return smoothedSignal;
    }

    public TsData[] getNoise() {
        if (smoothedNoise == null) {
            calcSmoothedNoise();
        }
        return smoothedNoise;
    }

    public void calcSmoothedSignal() {

        if (smoothing == null) {
            calcSmoothedStates();
        }

        DataBlockStorage m_a = smoothing.getSmoothedStates();

        if (m_a == null) {
            throw new Error("smoothed states are null");
        }

        int m_used = m_a.getCurrentSize();
        int r = model.getFactorsCount();
        int c_ = model.getBlockLength();
        int nlags = model.getTransition().nlags;
          // arrange the factors in a notation compatible with the state-space 
        // representation
        Matrix Z = new Matrix(r * c_, m_used);
        for (int i = 0; i < r * c_; i++) {
            Z.row(i).copy(m_a.item(i));
        }

        IMSsf ssf = model.ssfRepresentation();
        int N = ssf.getVarsCount();

       // List<DynamicFactorModel.MeasurementDescriptor> measurements = model.getMeasurements();
        double[][] signal = new double[N][m_used];

        for (int t = 0; t < m_used; t++) {

            DataBlock temp = Z.column(t).deepClone();
            for (int v = 0; v < N; v++) {

                signal[v][t] = ssf.ZX(0, v, temp);

            }

        }

        TsDomain currentDomain = input.getCurrentDomain();
        smoothedSignal = new TsData[N];
        for (int v = 0; v < N; v++) {
            smoothedSignal[v] = new TsData(currentDomain.getStart(), signal[v], false);
        }

    }

    public void calcSmoothedNoise() {

        if (smoothing == null) {
            calcSmoothedStates();
        }

        DataBlockStorage m_a = smoothing.getSmoothedStates();

        if (m_a == null) {
            throw new Error("smoothed states are null");
        }

        int m_used = m_a.getCurrentSize();
        int r = model.getFactorsCount();
        int c_ = model.getBlockLength();
        int nlags = model.getTransition().nlags;
          // arrange the factors in a notation compatible with the state-space 
        // representation
        Matrix Z = new Matrix(r * c_, m_used);
        for (int i = 0; i < r * c_; i++) {
            Z.row(i).copy(m_a.item(i));
        }

        IMSsf ssf = model.ssfRepresentation();
        int N = ssf.getVarsCount();

       // List<DynamicFactorModel.MeasurementDescriptor> measurements = model.getMeasurements();
        double[][] signal = new double[N][m_used];
        double[][] noise = new double[N][m_used];
        Matrix m = input.generateMatrix(null);

        for (int v = 0; v < N; v++) {

            for (int t = 0; t < m_used; t++) {
                DataBlock temp = Z.column(t).deepClone();

                signal[v][t] = ssf.ZX(0, v, temp);
                noise[v][t] = m.get(t, v) - signal[v][t];
            }
        }

        TsDomain currentDomain = input.getCurrentDomain();
        smoothedNoise = new TsData[N];
        for (int v = 0; v < N; v++) {
            smoothedNoise[v] = new TsData(currentDomain.getStart(), noise[v], false);
        }

    }

    public TsData[] getShocks() {
        if (smoothedShocks == null) {
            calcSmoothedShocks();
        }
        return smoothedShocks;
    }

    public void calcSmoothedShocks() {

        if (smoothing == null) {
            calcSmoothedStates();
        }

        DataBlockStorage m_a = smoothing.getSmoothedStates();

        if (m_a == null) {
            throw new Error("smoothed states are null");
        }

        int m_used = m_a.getCurrentSize();

        int r = model.getFactorsCount();

        int c_ = model.getBlockLength();

        int nlags = model.getTransition().nlags;

        Matrix z = new Matrix(r, m_used);
        Matrix zL = new Matrix(r * nlags, m_used);

        for (int i = 0; i < r; i++) {
            z.row(i).copy(m_a.item(i * c_));
        }

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < nlags; j++) {
                //             zL.row(i+j).copy(m_a.item(i*c_+j + 1));      wrong!!!!!
                zL.row(i * nlags + j).copy(m_a.item(i * c_ + j + 1));
            }
        }

        Matrix A_ = model.getTransition().varParams;
        Matrix U = z.minus(A_.times(zL));

     //   Matrix TESTit= U.times(U.transpose()).times(1.0/m_used);
        //    Matrix QtestIt=model.getTransition().covar ;
        TsDomain currentDomain = input.getCurrentDomain();
        smoothedShocks = new TsData[r];
        for (int i = 0; i < r; i++) {
            smoothedShocks[i] = new TsData(currentDomain.getStart(), U.row(i));
        }

    }

    public Matrix[] getVarianceDecomposition(int[] horizon) {
        if (varianceDecomposition == null) {
            calcVarianceDecomposition(horizon);
        }
        return varianceDecomposition;
    }

    public void calcVarianceDecomposition(int[] horizon) {

        IMSsf ssf = model.ssfRepresentation();
        int r = model.getFactorsCount();
        int c_ = model.getBlockLength();
        Matrix Q = model.getTransition().covar.clone();
        Matrix TQT;
            //Matrix[] ZVZt = null;

        varianceDecomposition = new Matrix[horizon.length];
        for (int h = 0; h < horizon.length; h++) {

            if (horizon[h] == 0) {
                System.err.println("The smallest forecast horizon is one period ahead, not zero");
            }

            if (horizon[h] == 1) {
                TQT = new Matrix(ssf.getStateDim(), ssf.getStateDim());
                for (int i = 0; i < r; i++) {
                    TQT.set(i * c_, i * c_, Q.subMatrix().get(i, i));
                }

            } else {
                TQT = new Matrix(ssf.getStateDim(), ssf.getStateDim());
                for (int i = 0; i < r; i++) {
                    TQT.set(i * c_, i * c_, Q.subMatrix().get(i, i));
                }

                for (int i = 0; i < horizon[h]; i++) {           // ????    
                    ssf.TVT(0, TQT.subMatrix());
                }

            }

            Matrix zvz = new Matrix(ssf.getVarsCount(), ssf.getVarsCount());

            ssf.ZVZ(0, TQT.subMatrix(), zvz.subMatrix());

        //       ZVZt[h]=ZVZ.clone();
            varianceDecomposition[h] = zvz.clone();
        }

    }

    public TsData[][] getShocksDecomposition() {
        if (shockDecomposition == null) {
            calcShocksDecomposition();
        }
        return shockDecomposition;
    }

    public void calcShocksDecomposition() {

        int r = model.getFactorsCount();
        int N = model.getMeasurementsCount();

        getShocks();
        int T = smoothedShocks[0].getLength();
        int c_ = model.getBlockLength();
        int nlags = model.getTransition().nlags;

        double[] angles = new double[r * (r - 1) / 2]; // initialized at zero, si the rotation
        // matrix will be the identify and it will
        // not have any effect for the moment

        // convert the TsData[] array Smoothedshocks into a matrix
        Matrix U = new Matrix(r, T);
        Matrix Ustar = new Matrix(r, T);

        for (int i = 0; i < r; i++) {
            U.row(i).copy(smoothedShocks[i]);
        }

        // Use Cholesky to orthogonalize and a rotation to pick up one of the
        // infinite shock decompositions available:
        // Normally, this should be the way  
        // Matrix C = model.getTransition().covar.clone();
        // But then the orthogonal smoothed shocks should be computed with the Kalman Smoother
        // FOR THE MOMENT, I WILL JUST USE 
        Matrix C = U.times(U.transpose());

        SymmetricMatrix.lcholesky(C);
        Rotation rot = new Rotation(angles);
        Matrix R = rot.getRotation();
        Matrix B = C.times(R);
        Matrix Bss = new Matrix(r * c_, r * c_); // compatible with SS
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                Bss.set(i * c_, j * c_, B.get(i, j));
            }
        }

        System.out.println(R);

        // Orthogonalize and rotate errors
        CroutDoolittle er = new CroutDoolittle();
        er.decompose(B);
        Ustar = er.solve(U);

        ////////////////////////////////////////////////
        // EXTRACTING THE INITIAL STATE
        if (smoothing == null) {
            calcSmoothedStates();
        }

        DataBlockStorage m_a = smoothing.getSmoothedStates();

        if (m_a == null) {
            throw new Error("smoothed states are null");
        }

        Matrix f0 = new Matrix(r * c_, 1);

        for (int i = 0; i < r * c_; i++) {

            f0.row(i).copy(m_a.item(i).range(0, 1));
            // + 1 because I want the first lag of the first t, 
            // which correponds to the initial state
        }

        // compute the role of initial states here, it only depends on 
        // the time
        IMSsf ssf = model.ssfRepresentation();

        double[][] initial = new double[N][T];
        DataBlock tempf = f0.column(0);
        for (int pos = 0; pos < T; pos++) {
            // converts the r*nlags x 1 matrix into a datablock
            ssf.TX(0, tempf);

            DataBlock tempy = tempf.clone(); // A^(pos+1) 
            for (int v = 0; v < N; v++) {
                initial[v][pos] = ssf.ZX(0, v, tempy);  // Lambda(v)* A^(pos+1)
            }

        }

        //    Matrix TEST       = Ustar.times(Ustar.transpose()); // why this is not identity
        //    Matrix TESTUstar  = (B.times(Ustar)).times((B.times(Ustar)).transpose()).times(1.0/T);
        //    Matrix TESTU      = U.times(U.transpose()).times(1.0/T);
        //   Matrix CCtransp   = C.times(C.transpose());
        //   Matrix Qtest      = model.getTransition().covar;
        //  Historical shock decomposition for each variable v here
        //  do the same trick I used for the initial state, but sequentiall adding to get the cumsum right  
        DataBlock tempy;
        DataBlock tempx;
        DataBlock tempCurrent;
        double[][][] shockDec = new double[r][N][T];    // N = number of variables
        // r shocks 

        for (int i = 0; i < r; i++) {

            Matrix Ustar_i = new Matrix(c_ * r, T);
            Ustar_i.row(i * c_).copy(Ustar.row(i)); // isolate shock i 
            Matrix BUstar_i = Bss.times(Ustar_i);

            for (int pos = 0; pos < T; pos++) {

                if (pos == 0) {

                    tempCurrent = BUstar_i.column(pos).deepClone();
                    for (int v = 0; v < N; v++) {
                        shockDec[i][v][pos] = ssf.ZX(0, v, tempCurrent);
                    }

                } else {

                    tempCurrent = BUstar_i.column(pos).deepClone();

                    // this look computes A e(pos-1) + ... + A^{pos-1}e(1)
                    //  and ads it to tempCurrent = e(pos)
                    //  for (int h = pos - 1; h >-1; h--) {  // h =0 is also computed
                    // ANOTHER OPTION IS TO SEPARATE THE EFFECT OF THE FIRST SHOCK
                    // THIS IS IMPORTANT BECAUSE THE EFFECT DEPENDS ON THE INITIALIZATION STRATEGY
                    // AND FOR MODELS WITH LONG MEMORY THIS EFFECT DISAPPEARS VERY SLOWLY  
                    for (int h = pos - 1; h > 0; h--) {
                        tempx = BUstar_i.column(h).deepClone();

                        // this loop computes A^{k+1}
                        for (int k = 0; k < pos - h; k++) {
                            ssf.TX(0, tempx);
                        }

                        tempCurrent.add(tempx);

                    }

                    tempy = tempCurrent.deepClone();

                    for (int v = 0; v < N; v++) {

                        shockDec[i][v][pos] = ssf.ZX(0, v, tempy);
                    }

                }

            }
        }

          //////////////////////////////////////////////////
        getNoise();
        TsDomain currentDomain = input.getCurrentDomain();

         // TsData ts = new TsData(currentDomain.getStart(), shockDec[1][1], false);
        shockDecomposition = new TsData[r + 2][N];// +2 because I want to incorporate initial factor and measurement errors

      //  DataBlock db = new DataBlock(shockDecomposition[0][0]);
        //  db.cumul();
        for (int i = 0; i < r + 2; i++) {

            for (int v = 0; v < N; v++) {

                if (i < r) {
                    DataBlock sd = new DataBlock(shockDec[i][v]);
                    //  sd.cumul();
                    shockDecomposition[i][v] = new TsData(currentDomain.getStart(), sd);
                } else if (i == r) {  // i==r+1 refers to the initial factors' inertia   
                    shockDecomposition[i][v] = new TsData(currentDomain.getStart(), initial[v], false);
                } else { // the last i==r+2 refers to the noise for variable v
                    shockDecomposition[i][v] = smoothedNoise[v];

                }

            }
        }
    }

    @Override
    public Map<String, Class> getDictionary() {
        return dictionary();
    }

    @Override
    public <T> T getData(String id, Class<T> tclass) {
        if (InformationSet.isPrefix(id, MODEL)) {
            return model.getData(InformationSet.removePrefix(id), tclass);
        } else {
            return mapper.getData(this, id, tclass);
        }
    }

    @Override
    public boolean contains(String id) {
        if (InformationSet.isPrefix(id, MODEL)) {
            return model.contains(InformationSet.removePrefix(id));
        } else {
            return mapper.contains(id);
        }
    }

    public static void fillDictionary(String prefix, Map<String, Class> map) {
        mapper.fillDictionary(prefix, map);
        DynamicFactorModel.fillDictionary(MODEL, map);
    }

    public static Map<String, Class> dictionary() {
        LinkedHashMap<String, Class> map = new LinkedHashMap<>();
        fillDictionary(null, map);
        return map;
    }

    public static <T> void addMapping(String name, InformationMapper.Mapper<DfmResults, T> mapping) {
        synchronized (mapper) {
            mapper.add(name, mapping);
        }
    }

    private static final InformationMapper<DfmResults> mapper = new InformationMapper<>();

    public static final String MODEL = "model", NLAGS = "nlags", NFACTORS = "nfactors", VPARAMS = "vparams", VCOVAR = "vcovar",
            MVARS = "mvars", MCOEFFS = "mcoeffs";

    static {
        mapper.add(InformationSet.item(MODEL, NLAGS), new InformationMapper.Mapper<DfmResults, Integer>(Integer.class) {
            @Override
            public Integer retrieve(DfmResults source) {
                return source.model.getTransition().nlags;
            }
        });
    }

}
