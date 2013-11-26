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
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.Matrix;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class DefaultTimeInvariantMultivariateSsf extends DefaultMultivariateSsf {

    /**
     *
     * @param ssf
     * @return
     */
    public static DefaultTimeInvariantMultivariateSsf of(IMSsf ssf) {
        if (!ssf.isTimeInvariant()) {
            return null;
        }
        DefaultTimeInvariantMultivariateSsf nssf = new DefaultTimeInvariantMultivariateSsf();
        int dim = ssf.getStateDim(), nvar = ssf.getVarsCount(), nd = ssf.getNonStationaryDim(),
                rdim = ssf.getTransitionResDim();
        nssf.initialize(dim, nvar, rdim, ssf.hasH());
        // measurement
        ssf.Z(0, nssf.m_Z.subMatrix());
        if (ssf.hasH()){
            Matrix tmpH=new Matrix(nvar,nvar);
            ssf.H(0, tmpH.subMatrix());
            if (! tmpH.subMatrix().isDiagonal())
                    return null;
            tmpH.diagonal().copyTo(nssf.m_H, 0);
        }
        // transition
        ssf.T(0, nssf.m_T.subMatrix());
        // innovations
        ssf.Q(0, nssf.m_Q.subMatrix());
        if (ssf.hasS()) {
            ssf.S(0, nssf.m_S.subMatrix());
        }
        nssf.updateTransition();
        // initialization
        if (nd > 0) {
            nssf.m_B0 = new Matrix(dim, nd);
            ssf.diffuseConstraints(nssf.m_B0.subMatrix());
        }
        nssf.m_Pf0 = new Matrix(dim, dim);
        ssf.Pf0(nssf.m_Pf0.subMatrix());
        return nssf;
    }

    /**
     *
     */
    public DefaultTimeInvariantMultivariateSsf() {
    }

    /**
     *
     * @param pos
     */
    @Override
    public void load(int pos) {
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isMeasurementEquationTimeInvariant() {
        return true;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isTimeInvariant() {
        return true;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isTransitionEquationTimeInvariant() {
        return true;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isTransitionResidualTimeInvariant() {
        return true;
    }

    /**
     *
     * @param pos
     * @param q
     * @return
     */
    @Override
    protected boolean loadQ(final int pos, final Matrix q) {
        return true;
    }

    /**
     *
     * @param pos
     * @return
     */
    @Override
    protected boolean loadH(final int pos, final double[] h) {
        return true;
    }

    /**
     *
     * @param pos
     * @param t
     * @return
     */
    @Override
    protected boolean loadT(final int pos, final Matrix t) {
        return true;
    }

    /**
     *
     * @param pos
     * @param s
     * @return
     */
    @Override
    protected boolean loadS(final int pos, final Matrix s) {
        return true;
    }

    /**
     *
     * @param pos
     * @param z
     * @return
     */
    @Override
    protected boolean loadZ(final int pos, final Matrix z) {
        return true;
    }

    /**
     *
     * @param q
     * @param s
     */
    public void setTransitionInnovations(final Matrix q, final Matrix s) {
        m_Q = q;
        m_S = s;
        updateTransition();
    }

    /**
     *
     * @param e
     */
    public void setTransitionInnovations(final IReadDataBlock e) {
        m_Q = Matrix.diagonal(e);
        m_S = null;
        m_W = Matrix.diagonal(e);
        m_W.diagonal().sqrt();
    }

    /**
     * }
     * /**
     *
     * @param h
     */
    public void setH(final double[] h) {
        m_H = h;
    }

    /**
     *
     * @param t
     */
    public void setT(final Matrix t) {
        m_T = t;
    }

    // Initialisation
    /**
     *
     * @param z
     */
    public void setZ(final Matrix z) {
        m_Z = z;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Transition equation\r\n\r\n");
        builder.append(("T:\r\n"));
        builder.append(m_T);
        builder.append(("V:\r\n"));
        builder.append(m_V);
        builder.append("Measurement equation\r\n\r\n");
        builder.append(("Z:\r\n"));
        builder.append(m_Z);
        if (m_H != null) {
            builder.append(("H:\r\n"));
            builder.append(new DataBlock(m_H));
        }
        return builder.toString();
    }
}
