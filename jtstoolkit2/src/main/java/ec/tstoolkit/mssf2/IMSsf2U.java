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
package ec.tstoolkit.mssf2;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.ssf2.ISsfBase;

/**
 *
 * @author Jean Palate
 */
public interface IMSsf2U extends ISsfBase {

    /**
     * Gets the dimension of the measurement equation (number of variables)
     *
     * @return
     */
    int getVarsCount();
    /**
     *
     * @return
     */
    boolean hasH();
    
    /**
     * Variance of the v-th measurement equation (at position v)
     * @param pos
     * @param v
     * @return 
     */
    double H(int pos, int v);

    /**
     *
     * @param pos
     * @param v
     * @return
     */
    boolean hasZ(int pos, int v);
    
     /**
     * Computes L = T(pos) - k * Z(pos)[v,]
     *
     * @param pos
     * @param v 
     * @param k 
     * @param lm
     */
    void L(int pos, int v, DataBlock k, SubMatrix lm);

    /**
     * Z(pos)[v,]
     *
     * @param pos
     * @param v
     * @param z
     */
    void Z(int pos, int v, DataBlock z);

    /**
     * Computes Z(pos)[v,] * V * Z(pos)[w,]'
     *
     * @param pos
     * @param v
     * @param w
     * @param V
     * @return  
     */
    double ZVZ(int pos, int v, int w, SubMatrix V);

    // forward operations
    /**
     * Computes Z(pos)[v,] * x
     *
     * @param pos
     * @param v
     * @param x
     * @return
     */
    double ZX(int pos, int v, DataBlock x);
    
       // backward operations
    /**
     * Computes V = V + Z(pos) * d * Z'(pos)
     * 
     * @param pos
     * @param v
     * @param w
     * @param V
     * @param d
     */
    void VpZdZ(int pos, int v, int w, SubMatrix V, double d);

    /**
     * Computes x = x + Z(pos)[v,]' * d
     *
     * @param pos Current position
     * @param v 
     * @param x (dim x 1) matrix (in array form)
     * @param d (
     */
    void XpZd(int pos, int v, DataBlock x, double d);

}
