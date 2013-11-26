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

import ec.tstoolkit.maths.matrices.SubMatrix;

/**
 *
 * @author Jean Palate
 */
public interface IArraySsf extends IMSsf{
    /**
     * Gets the Cholesky factor of the variance of the measurement error 
     * at a given position
     *
     * @param pos The position
     * @param r 
     */
    void R(int pos, SubMatrix r);
    
    /**
     * Gets a matrix W such that WW' is the variance of the state equation  
     * at a given position.
     * The dimension of W should be getStateDim() x getResidualsDim()
     *
     * @param pos The position
     * @param w  
     */
    void W(int pos, SubMatrix w);
    
    
}
