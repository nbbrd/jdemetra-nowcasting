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

import ec.tstoolkit.ssf2.*;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.Matrix;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class BaseMState {
    

    /**
     *
     */
    public static final AtomicLong fnCalls = new AtomicLong(0);

    /**
     * A is the state vector (=a(t|t-1)
     * E is the standardised prediction error (=F^-1)*(y(t)-Z(t)A(t)))
     */
    public final DataBlock A, E;

    /**
     * =(ZPZ'+H)^1/2 Cholesky factor of the variance/covariance matrix of the
     * prediction errors (lower triangular)
     */
    public final Matrix F;
   /**
     * 
     * @param mdim 
     * @param nvars 
     */
    protected BaseMState(final int mdim, final int nvars)
    {
	A = new DataBlock(mdim);
        E=new DataBlock(nvars);
        F = new Matrix(nvars, nvars);
    }
    
    /**
     *
     * @return
     */
    public int getMeasurementDim(){
        return E.getLength();
    }
    
    /**
     *
     * @return
     */
    public int getStateDim(){
        return A.getLength();
    }

    /**
     * 
     * @param state
     */
    public void copy(final BaseMState state)
    {
	E.copy(state.E);
	A.copy(state.A);
        F.copy(state.F);
    }

    /**
     * 
     * @param pos 
     * @return
     */
    public boolean isMissing(int pos)
    {
	return Double.isNaN(E.get(pos));
    }
}
