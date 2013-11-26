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
public interface ISsf extends ISsfBase{
 
    /**
     * Computes L = T(pos) - K * Z(pos)
     * @param pos
     * @param K
     * @param lm
     */
    void L(int pos, DataBlock K, SubMatrix lm);

    // backward operations
    /**
     * Computes V = V + Z(pos) * D * Z'(pos)
     * 
     * @param pos
     * @param vm
     * @param d
     */
    void VpZdZ(int pos, SubMatrix vm, double d);

    /**
     * Computes x = x + Z * D
     * @param posv 
     * @param x
     * @param d
     */
    void XpZd(int posv, DataBlock x, double d);

    // Matrix version
    /**
     *
     * @param pos
     * @param z
     */
    void Z(int pos, DataBlock z);

    /**
     *
     * @param pos
     * @param m
     * @return  
     */
    double ZX(int pos, DataBlock m);

     /**
     * Computes Z(pos) * V * Z'(pos)
     * @param pos
     * @param v
     * @return  
     */
    double ZVZ(int pos, SubMatrix v);

    
    /**
     * Gets the variance of the measurement  error at a given position 
     * @param pos
     * @return  
     */
    double H(int pos);
    
    /**
     *
     * @return
     */
    boolean hasH();
}
