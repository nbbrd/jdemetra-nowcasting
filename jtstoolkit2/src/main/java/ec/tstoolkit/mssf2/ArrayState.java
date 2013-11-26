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
import ec.tstoolkit.maths.matrices.SubMatrix;

/**
 * The array state at position t will contain: E(t), the forecast error = y(t) -
 * Z(t)a(t|t-1) Fl(t), the Cholesky factor of the covariance of E(t) a(t|t-1),
 * the forecasted state Pl(t|t-1), the Cholesky factor of the covariance of
 * a(t|t-1) K(t) = T(t)P(t|t-1)Z(t)'*R(t)^-1/2;(the gain matrix is G(t) =
 * K(t)*R(t)^-1/2)
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class ArrayState extends BaseOrdinaryMState {

    /**
     * Cholesky factor of P, the covariance of the state prediction a(t+1|t)
     */
    public final Matrix Pl;

    /**
     *
     * @param mdim 
     * @param nvars 
     */
    public ArrayState(final int mdim, final int nvars) {
        super(mdim, nvars);
        Pl = new Matrix(mdim, mdim);
    }

    /**
     *
     * @param state
     */
    public void copy(final ArrayState state) {
        super.copy(state);
        Pl.copy(state.Pl);

    }
}
