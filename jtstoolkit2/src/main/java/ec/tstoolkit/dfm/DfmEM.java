/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockIterator;
import ec.tstoolkit.data.DataBlockStorage;
import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementDescriptor;
import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementLoads;
import ec.tstoolkit.dfm.DfmProcessor;
import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementType;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.MatrixStorage;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import static ec.tstoolkit.maths.matrices.SymmetricMatrix.lsolve;
import ec.tstoolkit.mssf2.MFilteringResults;
import ec.tstoolkit.mssf2.MSmoothingResults;
import ec.tstoolkit.ssf2.FilteringResults;
import ec.tstoolkit.timeseries.simplets.TsData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author deanton
 */
public class DfmEM implements IDfmInitializer, IDfmEstimator {

    private int maxiter; // = 10000;number of iterations
    private double eps;     // = 1e-6; we assume EM algorithm converges if 
    // the percentage increase in log 
    // likelihood is smaller than eps
    private boolean conv;  // TRUE if convergence has been achieved 

    private DynamicFactorModel dfm;    // 
    private DfmProcessor dfmproc;
    private DataBlockStorage m_a;

    private int m_used, m_dim;
    // private MSmoothingResults ms;    // ?
    // private MFilteringResults fs;    // ?
    private HashSet<MeasurementLoads> unique_logic;
    private Matrix unique_types;

    // private    Matrix             unique_typesQ, unique_typesY, unique_typesM;
    private DfmInformationSet data;
    private int iter;
    //private double loglik, oldloglik;
    private Likelihood L, oldL;
    // references to model parameterization
    private int nf_; // sum(r)? I will assume this is the total number of factors (ignore here the possible blocks structure)

    private int nb_; // assume 3 blocks
    private int[] r;
    private int c_;
    private int nlags;

    // matrices needed to implement restrictions in loadings
    private Matrix R_c, R_con_c, q_con_c;
    private Matrix R_cd, R_con_cd, q_con_cd;
    private Matrix idx_iY, idx_iM, idx_iQ;

    // unique types of loading structure  
    // selection of factors
    private int[] temp2;
    private int[] temp2_;
    private boolean[] logic_temp2;
    private boolean[] logic_temp2_;

    private MSmoothingResults srslts_;
    private MFilteringResults frslts_;

    private Matrix ezz;
    private Matrix eZLZL;
    private Matrix ezZL;

    // M-step
    private Matrix A_, A, A_new;
    private Matrix Q, Q_;
    // data
    int nobs; // number of observations
    int N;// number of time series
    Matrix data_m; // transformed data in matrix form nobs x N
    Matrix y; //      transformed data matrix form nobs x N (with zeros instead of nan)

    int[] idx_M, idx_Q, idx_Y;
    List<Integer> Lidx_M, Lidx_Q, Lidx_Y;

    void initCalc(DynamicFactorModel dfm, DfmInformationSet data) {
        this.dfm = dfm;
        this.data = data;
        nobs = data.getCurrentDomain().getLength();
        L = new Likelihood();
        oldL = null;
        conv = false;
      
        maxiter = 100;
        
        eps = 1e-6;
        nf_ = dfm.getFactorsCount(); // nf_=sum(r);
        nb_ = 3; // assume three blocks
        r = new int[nb_];
        r[0] = 1;
        r[1] = 1;
        r[2] = 1;

        c_ = dfm.getBlockLength();
        nlags = dfm.getTransition().nlags;

        // reads better if I use a method
        // Measurements of type C 
        R_con_c = new Matrix(11 * (nf_), c_ * nf_);
        q_con_c = new Matrix(11 * (nf_), 1);
        R_c = new Matrix(11, c_);
        R_c.subMatrix(0, 11, 0, 11).diagonal().set(1);
        R_c.subMatrix(0, 11, 1, 12).diagonal().set(-1);
        // Selection matrices temp2 and temp2_
        temp2 = new int[nb_ * nlags];
        temp2_ = new int[nb_];

        int contador = 0;
        // Taking block structure into account
        for (int count = 0, count2 = 0, count3 = 0;
                count < nb_;
                count2 += 11 * r[count], count3 += c_ * r[count], count++) {

            temp2_[count] = count3;

            for (int nl = 0;
                    nl < nlags;
                    nl += 1) {
                temp2[contador] = count3 + nl;
                contador++;
            }

            R_con_c.subMatrix(count2, count2 + 11, count3, count3 + c_).kronecker(R_c.subMatrix(), Matrix.identity(r[count]).subMatrix());
        }
        // Measurements of type CD
        R_con_cd = new Matrix(4 * (nf_), c_ * nf_);
        q_con_cd = new Matrix(4 * (nf_), 1);
        R_cd = new Matrix(4, c_);
        R_cd.set(0, 0, 2);
        R_cd.set(1, 0, 3);
        R_cd.set(2, 0, 2);
        R_cd.set(3, 0, 1);
        R_cd.subMatrix(0, 4, 1, 5).diagonal().set(-1);
        // Taking block structure into account
        for (int count = 0, count2 = 0, count3 = 0;
                count < nb_;
                count2 += 4 * r[count], count3 += c_ * r[count], count++) {

            R_con_cd.subMatrix(count2, count2 + 4, count3, count3 + c_).kronecker(R_cd.subMatrix(), Matrix.identity(r[count]).subMatrix());

        }

        logic_temp2 = logic(temp2);
        logic_temp2_ = logic(temp2_);

// I NEED THE UNIQUE TYPES! and also I need to how many variable we have of each type
        unique_logic = new HashSet<>();

        for (MeasurementDescriptor mdesc : dfm.getMeasurements()) {
            unique_logic.add(mdesc.getLoads());
        }

      // all that is probably useless     
        // sum the elements of array "r";
        int sum = 0;
        for (int i : r) {
            sum += i;
        }
        unique_types = new Matrix(unique_logic.size(), sum);

        Iterator<MeasurementLoads> itr = unique_logic.iterator();
        int i = 0;
        while (itr.hasNext()) {
            boolean[] current = itr.next().used;
            for (int j = 0; j < current.length; j++) {
                if (current[j] == true) {
                    unique_types.set(i, j, 1);
                } else {
                    unique_types.set(i, j, 0);
                }
            }
            i++;
        }

        // unique_logic is the set of  unique types of loading structure     
        int ntypes = unique_logic.size();
        idx_iQ = new Matrix(ntypes, nf_ * c_);
        idx_iY = new Matrix(ntypes, nf_ * c_);
        idx_iM = new Matrix(ntypes, nf_ * c_);

        for (int block_i = 0, counter = 0;//,  counter=0, countQ=0, countY=0, countM=0;  
                block_i < nb_;
                counter += r[block_i] * c_, block_i++) {
            // countQ+=4*r[i],
            // countY+=c_*r[i],
            // countM+=c_*r[i],
            for (int iQ = 0; iQ < 5 * r[block_i]; iQ++) {
                idx_iQ.subMatrix(0, ntypes, counter + iQ, counter + iQ + 1).copy(unique_types.subMatrix(0, ntypes, block_i, block_i + 1));
            }
            for (int iY = 0; iY < 12 * r[block_i]; iY++) {
                idx_iY.subMatrix(0, ntypes, counter + iY, counter + iY + 1).copy(unique_types.subMatrix(0, ntypes, block_i, block_i + 1));
            }

            for (int iM = 0; iM < 1 * r[block_i]; iM++) {
                idx_iM.subMatrix(0, ntypes, counter + iM, counter + iM + 1).copy(unique_types.subMatrix(0, ntypes, block_i, block_i + 1));

            }
        }

        dfmproc = new DfmProcessor();
        dfmproc.setCalcVariance(true);


        // put the data in matrix form and index by type
        //->   ArrayList<TsData> ts = new ArrayList<TsData>();
        // A FOR LOOP
        double[] buffer = new double[nobs];
        N = data.getSeriesCount();
       // data_m = new Matrix(nobs, N);
        // for (int idx = 0; idx < N; idx++) {
        //      TsData ts = data.series(idx);
        //     ts.copyTo(buffer, 0);
        //    for (int t = 0; t < nobs; t++) {
        //       data_m.subMatrix(t, t + 1, idx, idx + 1).set(buffer[t]);
        //   }
        //}

        // generateMatrix FUNCTION
        y = data.generateMatrix(data.getCurrentDomain());
        data_m = y.clone();

        // delicate!!! I am changing data_ inside a matrix
        double[] data_ = y.internalStorage();
        for (int ii = 0; ii < data_.length; ii++) {
            if (Double.isNaN(data_[ii])) {
                data_[ii] = 0.0;
            }
        }

    }

    private List<boolean[]> logic(Matrix m) {
    // It has to be a matrix of ones and zeros. 
        //Incorporate an error expection if this is not the case
        ArrayList<boolean[]> list = new ArrayList<>();
        DataBlockIterator rows = m.rows();
        do {
            DataBlock row = rows.getData();
            boolean[] b = new boolean[row.getLength()];
            for (int i = 0; i < b.length; ++i) {
                b[i] = (row.get(i) == 1);
            }
            list.add(b);
        } while (rows.next());
        return list;
    }

    private boolean[] logic(int[] index) {

            // index contains positions of an array
        int maxi = index[0];// selects the largest element of the array

        for (int i = 1;
                i < index.length;
                i++) {
            if (maxi <= index[i]) {
                maxi = index[i];
            }
        }

        boolean[] logicSelect = new boolean[maxi + 1]; //?

        for (int i = 0; i < index.length; i++) {
            logicSelect[index[i]] = true;
        }

        return logicSelect;
    }

    private boolean[] logic(int[] index, int fixedsize) {

            // select the largest element of the array
        boolean[] logicSelect = new boolean[fixedsize]; //?

        for (int i = 0; i < index.length; i++) {
            logicSelect[index[i]] = true;
        }

        return logicSelect;
    }

    private boolean[] logic(int index, int fixedsize) {

            // select the largest element of the array
        boolean[] logicSelect = new boolean[fixedsize]; //?

        logicSelect[index] = true;

        return logicSelect;
    }

    private void calc(DynamicFactorModel dfm,  DfmInformationSet data) { // private  modifier has been eliminated

        
        iter = 0;
        


        dfmproc.process(dfm, data);
      // dfmproc.getFilteringResults().evaluate(L);
      //   double Ln    = L.getLogLikelihood();
      //   double oldLn = Ln;
      //  double Ln   = 0;
      //  double oldLn= 0;
       
        dfmproc.getFilteringResults().evaluate(L);
        double Ln  = L.getLogLikelihood();
       
        double oldLn=0;
        while ( iter < maxiter && !convergence(Ln, oldLn)) {
//        while ( iter < maxiter && !convergence(Ln, oldLn)) {

            iter++;
            oldLn = Ln;
            Ln = emstep(dfm,data); // Given the parameters of dfm, get moments
            
// Given moments of dfm, recompute parameters and plugged them
            //                                               into dfmclone
//         System.out.println(iter);
            System.out.println("Iter=" + iter + ", Ln=" + Ln);
        }

    }

    private boolean convergence(double Ln, double oldLn) {
        double epsi = (Ln - oldLn) / Math.abs(oldLn);
        if (epsi > eps) {  // instead of getEps?
            return false;
        } else if (epsi <= eps && epsi > 0) {
            System.out.print("The likelihood is increasing! ");
            System.out.print(oldLn);
            System.out.println(Ln);
            return true;
        } else {
            System.out.print("The likelihood is decreasing! ");
            System.out.print(oldLn);
            System.out.println(Ln);

            return false;
        }
    }

    double emstep(DynamicFactorModel dfm, DfmInformationSet data) {
    //    dfmproc = new DfmProcessor();
        //    dfmproc.process(dfm, data);
        //    MSmoothingResults srslts_;
        //     srslts_ = dfmproc.getSmoothingResults();
        //     MFilteringResults frslts_;
        //    frslts_ = dfmproc.getFilteringResults();

      
        double logLike;
 
        dfmproc.process(dfm, data);
        //  MSmoothingResults srslts_;
        srslts_ = dfmproc.getSmoothingResults();
        // MFilteringResults frslts_;
        frslts_ = dfmproc.getFilteringResults();

        m_a = srslts_.getSmoothedStates();

        m_used = m_a.getCurrentSize();
        m_dim = m_a.getDim();
        
        ezz = Ezz(dfm,data);
        eZLZL = EZLZL(dfm,data);
        ezZL = EzZL(dfm,data);

       System.out.println(dfm.getTransition().varParams.subMatrix(0,1,0,1));
        
        // XS=B, where S is a symmetric positive definite matrix.
        A_ = ezZL.clone();     //  true stays,  always changes      
        SymmetricMatrix.lsolve(eZLZL, A_.subMatrix(), true);

        System.out.println(A_.subMatrix(0, 1, 0, 1));
        System.out.println(iter);
        System.out.println("comparing.......................");
 
             
         // VAR form in my state space representation
        // here i have to define inde3 and indicator2 and convert them to logical
        A = new Matrix(c_ * nf_, c_ * nf_);
        int[] ind3 = new int[r.length];
        int[] indicador2 = new int[r.length];
        int[] indicador = new int[r.length];

        // constructing vectors to select matrices
        for (int i = 1; i < temp2_.length; i++) {
            int ri = r[i - 1];
            ind3[i] = ind3[i - 1] + r[i - 1];
            indicador2[i] = indicador2[i - 1] + r[i - 1] * nlags;
            indicador[i] = indicador[i - 1] + c_ * ri;

        }

        
        
          // converting to logic, maybe not needed
        //      boolean[] logic_ind3, logic_indicador2,logic_indicador  ;
        //       logic_ind3= logic(ind3);
        //      logic_indicador2 = logic(indicador2);
        //      logic_indicador = logic(indicador);
        for (int i = 0; i < nb_; i++) {
            int ri = r[i];

            for (int j = 0; j < nb_; j++) {
                int rj = r[j];

                A.subMatrix(indicador[i], indicador[i] + ri, indicador[j], indicador[j] + rj * nlags).copy(A_.subMatrix(ind3[i], ind3[i] + ri, indicador2[j], indicador2[j] + rj * nlags));

            }

            int temp = (c_ - 1) * ri;

            for (int ii = 0; ii < temp; ii++) {
                A.subMatrix(indicador[i] + ri + ii, indicador[i] + ri + ii + 1, indicador[i] + ii, indicador[i] + ii + 1).set(1);
            }

        }

        
        
          // SHOCKS
        Q_= new Matrix(nf_, nf_);
        Q = new Matrix(c_ * nf_, c_ * nf_);

        // WHY IT IS NOT WORKING, Q_ = (ezz.minus(A_.times(ezZL.transpose() ))).mul(1.0/nobs);
        Q_ = (ezz.minus(A_.times(ezZL.transpose())));

        for (int i = 0; i < temp2_.length; i++) {
            for (int j = 0; j < temp2_.length; j++) {
                Q.subMatrix(temp2_[i], temp2_[i] + 1, temp2_[j], temp2_[j] + 1).copy(Q_.subMatrix(i, i + 1, j, j + 1));
            }
        }

// THE MOST TRICKY PART OF THE EM        
        //-> for (int i = 0; i < unique_logic.size() ; i++) {
 //->    int[] bli = new int[nf_];
        // doesnt work because copyTo is for Matrix and not subMatrix
        //->    unique_types.subMatrix(i, i+1, 0, nf_).copyTo(bli, 0);
// LOADINGS MONTHLY VARIABLES
//------------------------------------*****************
        int type = 0;
        Matrix C_new = new Matrix(N, c_ * nf_); // the Matrix of factor loadings
        Matrix C_index = new Matrix(N, c_ * nf_); // position of non zero loadings

        List<boolean[]> logic_idx_iM = logic(idx_iM);  // matrix of positions where each row corresponds to a type
        List<boolean[]> logic_idx_iQ = logic(idx_iQ);  // matrix of positions where each row corresponds to a type
        List<boolean[]> logic_idx_iY = logic(idx_iY);  // matrix of positions where each row corresponds to a type

        for (MeasurementLoads loads : unique_logic) {

            // ideally, we should use the fact that we have 
            // ennumerated the MeasurementType
            Lidx_M = new ArrayList<>();
            Lidx_Q = new ArrayList<>();
            Lidx_Y = new ArrayList<>();

            int counting = 0;

            for (MeasurementDescriptor mdesc : dfm.getMeasurements()) {
                mdesc.getStructure();

                if (DynamicFactorModel.getMeasurementType(mdesc.type) == MeasurementType.C && mdesc.getLoads().equals(loads)) {
                    Lidx_Y.add(counting);
                }
                if (DynamicFactorModel.getMeasurementType(mdesc.type) == MeasurementType.CD && mdesc.getLoads().equals(loads)) {
                    Lidx_Q.add(counting);
                }
                if (DynamicFactorModel.getMeasurementType(mdesc.type) == MeasurementType.L && mdesc.getLoads().equals(loads)) {
                    Lidx_M.add(counting);
                }

            //if (mdesc.type=="CD" ){Listx_Q.add(counter);}
                //if (mdesc.type=="L" ){Listx_M.add(counter);}
                counting = counting + 1;

            }

            idx_M = new int[Lidx_M.size()];
            idx_Q = new int[Lidx_Q.size()];
            idx_Y = new int[Lidx_Y.size()];

            for (int iM = 0; iM < Lidx_M.size(); iM++) {
                idx_M[iM] = Lidx_M.get(iM);
            }
            for (int iQ = 0; iQ < Lidx_Q.size(); iQ++) {
                idx_Q[iQ] = Lidx_Q.get(iQ);
            }
            for (int iY = 0; iY < Lidx_Y.size(); iY++) {
                idx_Y[iY] = Lidx_Y.get(iY);
            }

/////////////////////////////////////////////////////////////////
//////////// MONTHLY////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
           // a couple of parameters needed to define numerator and denominator 
            int rsi = 0;
            for (int i = 0; i < r.length; i++) {
                if (loads.used[i]) { // because loads is public
                    rsi += r[i];
                }
            }

            int nMi = idx_M.length;

            boolean[] logic_idx_M = logic(idx_M, N);          // integer[] with positions for variables of the given type

        //     int nQi = idx_Q.length;
            //     int nYi = idx_Y.length;
            Matrix denom = new Matrix(nMi * rsi, nMi * rsi);
            Matrix denom_interm = new Matrix(nMi * rsi, nMi * rsi);
            Matrix nom = new Matrix(nMi, rsi);
            Matrix nom_interm = new Matrix(nMi, rsi);

            for (int i = 0; i < nobs; i++) {

                boolean[] logic_i = new boolean[nobs];
                logic_i[i] = true;

                // this is messy
                double[] buffer = new double[N];
                data_m.row(i).copyTo(buffer, 0); //subMatrix(i, i+1, 0, N)

                double[] temp = new double[idx_M.length];
                for (int j = 0; j < idx_M.length; j++) {
                    if (!Double.isNaN(buffer[idx_M[j]])) {
                        temp[j] = 1;
                    }
                }

                Matrix nanYt = Matrix.diagonal(temp);

                DataBlock bloque = DataBlock.select(srslts_.A(i).deepClone(), logic_idx_iM.get(type));
                Matrix ztemp = new Matrix(bloque.getData(), sumAll(logic_idx_iM.get(type)), 1); //idx_iM.row(type).sum()   sumAll(logic_idx_iM.get(type))                   

                Matrix ezztemp = ztemp.times(ztemp.transpose()).plus(Matrix.select(srslts_.P(i), logic_idx_iM.get(type), logic_idx_iM.get(type)));
                denom_interm.subMatrix().kronecker(ezztemp.subMatrix(), nanYt.subMatrix());
                denom.add(denom_interm);

                nom_interm.subMatrix().product(Matrix.select(y.subMatrix(), logic_i, logic_idx_M).subMatrix().transpose(), ztemp.subMatrix().transpose());
                nom.add(nom_interm);

            }

            Matrix vec_C = new Matrix(nom.internalStorage(), nom.getColumnsCount() * nom.getRowsCount(), 1);
            SymmetricMatrix.rsolve(denom, vec_C.subMatrix(), true);

            Matrix Ctype = new Matrix(vec_C.internalStorage(), nMi, rsi);
            // upload Jean's new function 
            C_new.subMatrix().copy(Ctype.subMatrix(), logic_idx_M, logic_idx_iM.get(type));
            //   System.out.println(C_new);
            
            // UPDATING LOADINGS!!!!!
            for (int j = 0; j < idx_M.length; j++) {
                MeasurementDescriptor desc = dfm.getMeasurements().get(idx_M[j]);
                for (int k = 0; k < dfm.getFactorsCount(); ++k) {
                    if (!Double.isNaN(desc.coeff[k])) {
                        desc.coeff[k] = C_new.get(idx_M[j], c_ * k);
                    }
                }
  
            }

/////////////////////////////////////////////////////////////////
//////////// QUARTERLY////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
           // a couple of parameters  and matrices needed to define numerator and denominator 
            int rpsi = rsi * Math.max(nlags, 5); // 5 is the numer of lags needed in Mariano Murasawa transformation

            int nQi = idx_Q.length;

            boolean[] logic_idx_Q = logic(idx_Q, N);          // integer[] with positions for variables of the given type

            Matrix R_con_cd_i = Matrix.selectColumns(R_con_cd.subMatrix(), logic_idx_iQ.get(type));
            Matrix q_con_cd_i = q_con_cd.clone();

            // let's remove the rows that are full of zeros because they do not imply any rstrictions (this couldp happen if r[i]>1
            boolean[] select = new boolean[R_con_cd_i.getRowsCount()];
            for (int i = 0; i < R_con_cd_i.getRowsCount(); i++) {

                if (R_con_cd_i.row(i).isZero() == false) {
                    select[i] = true;
                }

            }

            R_con_cd_i = Matrix.selectRows(R_con_cd_i.subMatrix(), select);
            q_con_cd_i = Matrix.selectRows(q_con_cd_i.subMatrix(), select);

            // for quarterly variables let's do it one by one
            for (int i = 0; i < idx_Q.length; i++) {

                boolean[] logic_idx_Q_i = logic(idx_Q[i], N);          // integer[] with positions for variables of the given type

                denom = new Matrix(rpsi, rpsi);
                denom_interm = new Matrix(rpsi, rpsi);
                nom = new Matrix(1, rpsi);
                nom_interm = new Matrix(1, rpsi);

                // here the for loop
                for (int t = 0; t < nobs; t++) {

                    boolean[] logic_t = new boolean[nobs];
                    logic_t[t] = true;

                    // this is messy
                    double[] buffer = new double[N];
                    data_m.row(t).copyTo(buffer, 0); //subMatrix(i, i+1, 0, N)

                    double temp = 0;

                    if (!Double.isNaN(buffer[idx_Q[i]])) {
                        temp = 1;
                    }

                    Matrix nanYt = new Matrix(1, 1);
                    nanYt.set(temp);

                    DataBlock bloque = DataBlock.select(srslts_.A(t).deepClone(), logic_idx_iQ.get(type));
                    Matrix ztemp = new Matrix(bloque.getData(), sumAll(logic_idx_iQ.get(type)), 1); //idx_iM.row(type).sum()   sumAll(logic_idx_iM.get(type))                   

                    Matrix ezztemp = ztemp.times(ztemp.transpose()).plus(Matrix.select(srslts_.P(t), logic_idx_iQ.get(type), logic_idx_iQ.get(type)));
                    denom_interm.subMatrix().kronecker(ezztemp.subMatrix(), nanYt.subMatrix());
                    denom.add(denom_interm);

                    nom_interm.subMatrix().product(Matrix.select(y.subMatrix(), logic_t, logic_idx_Q_i).subMatrix().transpose(), ztemp.subMatrix().transpose());
                    nom.add(nom_interm);

                }  // closing loop for each time (nobs)

                vec_C = new Matrix(nom.internalStorage(), nom.getColumnsCount() * nom.getRowsCount(), 1);
                   //vec_C= nom.clone().transpose(); //

                // NOW INCORPORATE RESTRICTIONS
                SymmetricMatrix.rsolve(denom, vec_C.subMatrix(), true);

                Matrix temp = R_con_cd_i.clone().transpose();
                SymmetricMatrix.rsolve(denom, temp.subMatrix(), true);
                Matrix temp2 = R_con_cd_i.times(temp);
                Matrix temp3 = temp.clone();

                Matrix Ctype_i = new Matrix(nom.getColumnsCount() * nom.getRowsCount(), 1);
                Ctype_i = vec_C.minus(Matrix.lsolve(temp2.subMatrix(), temp3.subMatrix()).times((R_con_cd_i.times(vec_C)).minus(q_con_cd_i)));

                // upload Jean's new function 
                C_new.subMatrix().copy(Ctype_i.subMatrix().transpose(), logic_idx_Q_i, logic_idx_iQ.get(type));

            }  // closeing loop for each Q variable
            
            
                        // UPDATING LOADINGS!!!!!
            for (int j = 0; j < idx_Q.length; j++) {
                MeasurementDescriptor desc = dfm.getMeasurements().get(idx_Q[j]);
                for (int k = 0; k < dfm.getFactorsCount(); ++k) {
                    if (!Double.isNaN(desc.coeff[k])) {
                        desc.coeff[k] = C_new.get(idx_Q[j], c_ * k);
                    }
                }
  
            }

/////////////////////////////////////////////////////////////////
//////////// SURVEYS    ////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
           // a couple of parameters  and matrices needed to define numerator and denominator 
            rpsi = rsi * Math.max(nlags, 12); // 12 is the numer of lags needed in cumsum transformation

            int nYi = idx_Y.length;

            boolean[] logic_idx_Y = logic(idx_Y, N);          // integer[] with positions for variables of the given type

            Matrix R_con_c_i = Matrix.selectColumns(R_con_c.subMatrix(), logic_idx_iY.get(type));
            Matrix q_con_c_i = q_con_c.clone();

            // let's remove the rows that are full of zeros because they do not imply any rstrictions (this couldp happen if r[i]>1
            select = new boolean[R_con_c_i.getRowsCount()];
            for (int i = 0; i < R_con_c_i.getRowsCount(); i++) {

                if (R_con_c_i.row(i).isZero() == false) {
                    select[i] = true;
                }

            }

            R_con_c_i = Matrix.selectRows(R_con_c_i.subMatrix(), select);
            q_con_c_i = Matrix.selectRows(q_con_c_i.subMatrix(), select);

            // for quarterly variables let's do it one by one
            for (int i = 0; i < idx_Y.length; i++) {

                boolean[] logic_idx_Y_i = logic(idx_Y[i], N);          // integer[] with positions for variables of the given type

                denom = new Matrix(rpsi, rpsi);
                denom_interm = new Matrix(rpsi, rpsi);
                nom = new Matrix(1, rpsi);
                nom_interm = new Matrix(1, rpsi);

                // here the for loop
                for (int t = 0; t < nobs; t++) {

                    boolean[] logic_t = new boolean[nobs];
                    logic_t[t] = true;

                    // this is messy
                    double[] buffer = new double[N];
                    data_m.row(t).copyTo(buffer, 0); //subMatrix(i, i+1, 0, N)

                    double temp = 0;

                    if (!Double.isNaN(buffer[idx_Y[i]])) {
                        temp = 1;
                    }

                    Matrix nanYt = new Matrix(1, 1);
                    nanYt.set(temp);

                    DataBlock bloque = DataBlock.select(srslts_.A(t).deepClone(), logic_idx_iY.get(type));
                    Matrix ztemp = new Matrix(bloque.getData(), sumAll(logic_idx_iY.get(type)), 1); //idx_iM.row(type).sum()   sumAll(logic_idx_iM.get(type))                   

                    Matrix ezztemp = ztemp.times(ztemp.transpose()).plus(Matrix.select(srslts_.P(t), logic_idx_iY.get(type), logic_idx_iY.get(type)));
                    denom_interm.subMatrix().kronecker(ezztemp.subMatrix(), nanYt.subMatrix());
                    denom.add(denom_interm);

                    nom_interm.subMatrix().product(Matrix.select(y.subMatrix(), logic_t, logic_idx_Y_i).subMatrix().transpose(), ztemp.subMatrix().transpose());
                    nom.add(nom_interm);

                }  // closing loop for each time (nobs)

                vec_C = new Matrix(nom.internalStorage(), nom.getColumnsCount() * nom.getRowsCount(), 1);
                   //vec_C= nom.clone().transpose(); //

                // NOW INCORPORATE RESTRICTIONS
                SymmetricMatrix.rsolve(denom, vec_C.subMatrix(), true);

                Matrix temp = R_con_c_i.clone().transpose();
                SymmetricMatrix.rsolve(denom, temp.subMatrix(), true);
                Matrix temp2 = R_con_c_i.times(temp);
                Matrix temp3 = temp.clone();

                Matrix Ctype_i = new Matrix(nom.getColumnsCount() * nom.getRowsCount(), 1);
                Ctype_i = vec_C.minus(Matrix.lsolve(temp2.subMatrix(), temp3.subMatrix()).times((R_con_c_i.times(vec_C)).minus(q_con_c_i)));

                // upload Jean's new function 
                C_new.subMatrix().copy(Ctype_i.subMatrix().transpose(), logic_idx_Y_i, logic_idx_iY.get(type));

                
              
            }  // closeing loop for each Y variable

                        // UPDATING LOADINGS!!!!!
            for (int j = 0; j < idx_Y.length; j++) {
                MeasurementDescriptor desc = dfm.getMeasurements().get(idx_Y[j]);
                for (int k = 0; k < dfm.getFactorsCount(); ++k) {
                    if (!Double.isNaN(desc.coeff[k])) {
                        desc.coeff[k] = C_new.get(idx_Y[j], c_ * k);
                    }
                }
  
            }
            
            
            type++;

        }
//------------------------------------*****************

      // NOW CALCULATE m step for IDIOSYNCRATIC COMPONENT
        Matrix R_new = new Matrix(N, N);
     //   Matrix RR_new = new Matrix(N, N);
        for (int i = 0; i < nobs; i++) {

            boolean[] logic_i = new boolean[nobs];
            logic_i[i] = true;

            // this is messy
            double[] buffer = new double[N];
            data_m.row(i).copyTo(buffer, 0); //subMatrix(i, i+1, 0, N)

            double[] temp = new double[N];
            for (int j = 0; j < N; j++) {
                if (!Double.isNaN(buffer[j])) {
                    temp[j] = 1;
                }
            }

            Matrix nanYt = Matrix.diagonal(temp);
            DataBlock bloque = srslts_.A(i).deepClone();
            Matrix ztemp = new Matrix(bloque.getData(), bloque.getLength(), 1); //idx_iM.row(type).sum()   sumAll(logic_idx_iM.get(type))                   
            Matrix V = new Matrix(srslts_.P(i));

            Matrix R = Matrix.identity(N);  // replace by the R obtained in previous iteration

            Matrix whatsgoingon = Matrix.selectRows(y.subMatrix(), logic_i).transpose();
            Matrix whatsgoingon2 = nanYt.times(C_new.times(ztemp));
            Matrix tempres = whatsgoingon.minus(whatsgoingon2);
            Matrix tempres2 = (Matrix.selectRows(y.subMatrix(), logic_i).transpose().minus(nanYt.times(C_new.times(ztemp)))).transpose();
            Matrix tempres3 = (nanYt.times(C_new.times(V))).times(C_new.transpose()).times(nanYt);

            Matrix tempres4 = ((Matrix.identity(N).minus(nanYt)).times(R)).times(Matrix.identity(N).minus(nanYt));

            Matrix R_temp = tempres.times(tempres2).plus(tempres3).plus(tempres4);
 
            R_new = R_new.plus(R_temp);
//System.out.println(tempres);
//System.out.println("______________________________________________");

//System.out.println(tempres2.transpose());
//System.out.println("check it");

        }

        //System.out.println("check it");
        //System.out.println(R_new);
         
        double scale = 1.0/nobs;
        R_new.mul(scale);
        
       // System.out.println("check it again .....");
       // System.out.println(R_new);
//            R_new= Matrix.diagonal(R_temp.internalStorage());
//        System.out.println(C_new);

        // EM UPDATE

           // will it work here???
      //   dfmproc.process(dfm, data);
        
        
        List<MeasurementDescriptor> measurements = dfm.getMeasurements();
        int counting=0;
        for (MeasurementDescriptor desc : measurements) {
            desc.var = R_new.get(counting, counting);
            counting++;
        }
        
        dfm.getTransition().covar.copy(Q_);
        dfm.getTransition().varParams.copy(A_);

        
    //    System.out.println(C_new);
//         dfmproc = new DfmProcessor();
//        dfmproc.setCalcVariance(true);
//        dfmproc.process(dfm, data);
//         DfmProcessor dfmproc_update = new DfmProcessor();
         dfmproc.process(dfm, data);
         dfmproc.getFilteringResults().evaluate(L);
         logLike = L.getLogLikelihood();
       // Ezz = null;
        //  EZLZL = null;
        //  EzZL = null;
        return logLike;
    }
    /*
     DataBlockStorage smoothedStates = sr.getSmoothedStates();
     SubMatrix subMatrix = smoothedStates.subMatrix();
     boolean[] csel=null;
     Matrix m=Matrix.selectColumns(subMatrix, csel);

     new DynamicFactorModel. dfmx;  
 
     // E-STEP, uses functions that compute moments from the model
     // M-STEP, uses functions that generate the matrices  needed
     throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
     }
     */

    Matrix allcomponents() {


        if (m_a == null) {
            return null;
        }
        if (m_dim != c_ * nf_) {
            System.out.print("Problem in the factors dimension: m_dim is different from c_*nf_");
            return null;
        }

        Matrix c = new Matrix(m_dim, m_used);

        for (int i = 0; i < m_dim; i++) {
         //   ci = new Matrix(srslts_.component(i),1,m_used);
            //    c.row(i).copyFrom(srslts_.component(i), 0);
            c.row(i).copy(m_a.item(i));
            //                 c.subMatrix(i, i+1, 0, m_used).copy(ci.subMatrix());
//                c.subMatrix(i, i+1, 0, m_used).copy(Matrix(m_a.item(i),1,m_used).subMatrix());
        }

        return c;
    }

    MatrixStorage ms;
    DataBlockStorage ds;

    private Matrix Ezz(DynamicFactorModel dfm, DfmInformationSet data) {
   
        dfmproc.process(dfm, data);
        Matrix z0 = allcomponents();
        SubMatrix z1 = z0.subMatrix(0, z0.getRowsCount(), 1, z0.getColumnsCount());
        Matrix z = Matrix.selectRows(z1, logic_temp2_);
        Matrix zz = z.times(z.transpose());

        Matrix eP = new Matrix(temp2_.length, temp2_.length);
        double covij_sum;
        double[] petittest;
        for (int i = 0; i < temp2_.length; i++) {
            for (int j = 0; j < temp2_.length; j++) {
                petittest = srslts_.componentCovar(temp2_[i], temp2_[j]);   // null, because m_P in srslts_ is null  
                covij_sum = sum(srslts_.componentCovar(temp2_[i], temp2_[j]));
                eP.set(i, j, covij_sum);
            }
        }

        Matrix ezz = zz.plus(eP);
        return ezz;
    }

    private Matrix EZLZL(DynamicFactorModel dfm, DfmInformationSet data) {
        dfmproc.process(dfm, data);
        Matrix z0 = allcomponents();
        SubMatrix z1 = z0.subMatrix(0, z0.getRowsCount(), 0, z0.getColumnsCount() - 1);
        Matrix z = Matrix.selectRows(z1, logic_temp2);
        Matrix zz = z.times(z.transpose());

        Matrix eP = new Matrix(temp2.length, temp2.length);
        double covij_sum;
        for (int i = 0; i < temp2.length; i++) {
            for (int j = 0; j < temp2.length; j++) {
                covij_sum = sumL(srslts_.componentCovar(temp2[i], temp2[j]));
                eP.set(i, j, covij_sum);
            }
        }
        Matrix eZLZL = zz.plus(eP);
        return eZLZL;
    }

    private Matrix EzZL(DynamicFactorModel dfm, DfmInformationSet data) {
        
        dfmproc.process(dfm, data);
        Matrix z0 = allcomponents();
        SubMatrix z1 = z0.subMatrix(0, z0.getRowsCount(), 1, z0.getColumnsCount());
        SubMatrix z1L = z0.subMatrix(0, z0.getRowsCount(), 0, z0.getColumnsCount() - 1);

        Matrix z = Matrix.selectRows(z1, logic_temp2_);
        Matrix zL = Matrix.selectRows(z1L, logic_temp2);

        Matrix zz = z.times(zL.transpose());

        Matrix eP = new Matrix(temp2_.length, temp2.length);
        double covij_sum;
        for (int i = 0; i < temp2_.length; i++) {
            for (int j = 0; j < temp2.length; j++) {
                covij_sum = sumAll(srslts_.componentCovar(temp2_[i], temp2[j]));
                eP.set(i, j, covij_sum);
            }
        }

        Matrix ezZL = zz.plus(eP);
        return ezZL;
    }

    @Override
    public boolean initialize(DynamicFactorModel dfm, DfmInformationSet data) {
        initCalc(dfm, data);
        calc(dfm,data);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean estimate(DynamicFactorModel dfm, DfmInformationSet input) {
        initCalc(dfm, data);
        calc(dfm,data);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Matrix getHessian() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DataBlock getGradient() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * @return the maxiter
     */
    public int getMaxiter() {
        return maxiter;
    }

    /**
     * @param maxiter the maxiter to set
     */
    public void setMaxiter(int maxiter) {
        this.maxiter = maxiter;
    }

    /**
     * @return the eps
     */
    public double getEps() {
        return eps;
    }

    /**
     * @param eps the eps to set
     */
    public void setEps(double eps) {
        this.eps = eps;
    }

    /**
     * @return the conv
     */
    public boolean isConv() {
        return conv;
    }

    /**
     * @return the iter
     */
    public int getIter() {
        return iter;
    }

    // sum the elements of array "r";
    double sum(double[] elements) {
        double suma = 0.0;
        for (int i = 0; i < elements.length; i++) {
            suma += elements[i];
        }
        suma = suma - elements[0];
        return suma;
    }

    double sumL(double[] elements) {
        double suma = 0.0;
        for (int i = 0; i < elements.length; i++) {
            suma += elements[i];
        }
        suma = suma - elements[elements.length - 1];
        return suma;
    }

    double sumAll(double[] elements) {
        double suma = 0.0;
        for (int i = 0; i < elements.length; i++) {
            suma += elements[i];
        }
        return suma;
    }

    int sumAll(boolean[] elements) {
        int suma = 0;
        for (int i = 0; i < elements.length; i++) {
            if (elements[i]) {
                suma++;
            }

        }
        return suma;
    }

}
