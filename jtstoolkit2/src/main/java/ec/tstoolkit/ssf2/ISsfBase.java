/*
 * Copyright 2013 National Bank of Belgium
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
package ec.tstoolkit.ssf2;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.maths.matrices.SubMatrix;

/**
 *
 * @author Jean Palate
 */
public interface ISsfBase {
    
    /**
     * Dimensions of the models
     */
    
    /**
     * Dimension of the state vector
     * @return
     */
    int getStateDim(); 
    
    /**
     * Dimension of the non stationary part of the state vector
     * @return
     */
    int getNonStationaryDim();

    /**
     * Dimension of the random effects. See V for further information
     * @return
     */
    int getTransitionResDim(); // E
    
     /**
     * 
     * @return
     */
    boolean isDiffuse();


    // information
    /**
     * Z, H, V, T time invariant
     * @return
     */
    boolean isTimeInvariant();

    /**
     * Z, H time invariant
     * @return
     */
    boolean isMeasurementEquationTimeInvariant();
    
    /**
     * V, T time invariant
     * @return
     */
    boolean isTransitionEquationTimeInvariant();

    /**
     * V is time invariant
     * @return
     */
    boolean isTransitionResidualTimeInvariant();

    /**
     *
     * @return
     */
    boolean isValid();
    
    
    /**
     * Initialisation 
     */
   
    /**
     * B = d x nd, where d = getStateDim(), nd = getNonStationaryDim()
     * P(-1, inf) = B * B'
     * @param b
     */
    void diffuseConstraints(SubMatrix b);
    
    /**
     * Modelling of the stationary variance of the initial state
     * P(-1, *) 
     * @param pf0
     */
    void Pf0(SubMatrix pf0);

    /**
     * Modelling of the non stationary part of the initial state
     * P(-1, inf)
     * @param pi0 
     */
    void Pi0(SubMatrix pi0);
    

    /**
     * Variance matrix of the innovations in the transition equation.
     * V is also modelled as
     * 
     * V = S*Q*S'
     * 
     * V = d x d where d = getStateDim()
     * Q = q x q where q = getTransitionResDim()
     * S = d x q
     * 
     * When r = d, R should be considered as missing (R = 0...d-1)
     * When W = I, it should be considered as missing.
     * 
     * @param pos
     * @param qm
     */
    void V(int pos, SubMatrix qm);



    /**
     * @return
     */
    boolean hasS();

    /**
     *
     * @param pos
     * @return
     */
    boolean hasTransitionRes(int pos);

    /**
     *
     * @param pos
     * @param qm
     */
    void Q(int pos, SubMatrix qm);

    /**
     *
     * @param pos
     * @param sm  
     */
    void S(int pos, SubMatrix sm);

    /**
     * Gets the transition matrix.
     * @param pos The position of the model
     * @param tr The sub-matrix that will receive the transition matrix.
     * It must have the dimensions (getStateDim() x getStateDim()). 
     * The caller has the responsibility to provide a clean sub-matrix, 
     * so that the callee can safely set only the non zero values.  
     */
    void T(int pos, SubMatrix tr);

    /**
     * Computes T V T'
     * @param pos The position of the model
     * @param vm 
     */
    void TVT(int pos, SubMatrix vm);
    
    /**
     * Adds the variance of the transition equation
     * p = p + V(pos)
     * @param pos
     * @param p 
     */
    void addV(int pos, SubMatrix p);


    /**
     * Computes x * T(pos) 
     * @param pos
     * @param x
     */
    void XT(int pos, DataBlock x);

    /**
     * Computes T(pos) * x
     * @param pos
     * @param x  
     */
    void TX(int pos, DataBlock x);

 }
