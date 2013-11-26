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

import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.Matrix;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class BaseOrdinaryMState extends BaseMState {

    /**
     *
     */
    public final Matrix K;
  
    /**
     *
     * @param mdim 
     * @param nvars 
     */
    protected BaseOrdinaryMState(final int mdim, final int nvars) {
        super(mdim, nvars);
        K = new Matrix(mdim, nvars);
    }

    /**
     *
     * @param state
     */
    public void copy(final BaseOrdinaryMState state) {
        super.copy(state);
        K.copy(state.K);
     }
}
