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

/**
 *
 * @author Jean Palate
 */
public class FullMSsf extends AbstractMultivariateSsf {

    public static IMSsf create(IMSsf ssf) {
        if (ssf.hasH()) {
            return new FullMSsf(ssf);
        } else {
            return ssf;
        }
    }

    private final IMSsf ssf;
    private final int dim, nvars;

    private FullMSsf(IMSsf ssf) {
        this.ssf = ssf;
        dim = ssf.getStateDim();
        nvars = ssf.hasH() ? ssf.getVarsCount() : 0;
    }

    @Override
    public void R(int pos, SubMatrix r) {
    }

    @Override
    public int getTransitionResDim() {
        return nvars + ssf.getTransitionResDim();
    }

    @Override
    public void W(int pos, SubMatrix w) {
        if (ssf instanceof IArraySsf) {
            int n = ssf.getTransitionResDim();
            IArraySsf assf = (IArraySsf) ssf;
            assf.W(pos, w.extract(0, dim, 0, n));
            assf.R(pos, w.extract(dim, dim + nvars, n, n + nvars));
        } else {
            throw new UnsupportedOperationException("No Array ssf support"); //To change body of generated methods, choose Tools | Templates.
        }
    }

    @Override
    public int getVarsCount() {
        return nvars;
    }

    @Override
    public void XpZd(int pos, int v, DataBlock x, double d) {
        ssf.XpZd(pos, v, x.range(0, dim), d);
        x.add(dim + v, d);
    }

    @Override
    public double ZX(int pos, int v, DataBlock x) {
        return ssf.ZX(pos, v, x.range(0, dim)) + x.get(dim + v); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasH() {
        return false;
    }

    @Override
    public void H(int pos, SubMatrix h) {
    }

    @Override
    public void addH(int pos, SubMatrix v) {
    }

    @Override
    public int getStateDim() {
        return dim + nvars;
    }

    @Override
    public int getNonStationaryDim() {
        return ssf.getNonStationaryDim();
    }

    @Override
    public boolean isDiffuse() {
        return ssf.isDiffuse();
    }

    @Override
    public boolean isTimeInvariant() {
        return ssf.isTimeInvariant();
    }

    @Override
    public boolean isMeasurementEquationTimeInvariant() {
        return ssf.isMeasurementEquationTimeInvariant();
    }

    @Override
    public boolean isTransitionEquationTimeInvariant() {
        return ssf.isTransitionEquationTimeInvariant();
    }

    @Override
    public boolean isTransitionResidualTimeInvariant() {
        return ssf.isTransitionResidualTimeInvariant() && ssf.isMeasurementEquationTimeInvariant();
    }

    @Override
    public boolean isValid() {
        return ssf.isValid();
    }

    @Override
    public void diffuseConstraints(SubMatrix b) {
        ssf.diffuseConstraints(b.extract(0, dim, 0, b.getColumnsCount()));
    }

    @Override
    public void Pf0(SubMatrix pf0) {
        ssf.Pf0(pf0.extract(0, dim, 0, dim));
    }

    @Override
    public void Pi0(SubMatrix pi0) {
        ssf.Pi0(pi0.extract(0, dim, 0, dim));
    }

    @Override
    public void V(int pos, SubMatrix qm) {
        ssf.V(pos, qm.extract(0, dim, 0, dim));
        ssf.H(pos, qm.extract(dim, dim + nvars, dim, dim + nvars));
    }

    @Override
    public void Q(int pos, SubMatrix qm) {
        int n = ssf.getTransitionResDim();
        ssf.Q(pos, qm.extract(0, n, 0, n));
        ssf.H(pos, qm.extract(n, n + nvars, n, n + nvars));
    }

    @Override
    public void S(int pos, SubMatrix qm) {
        int n = ssf.getTransitionResDim();
        ssf.S(pos, qm.extract(0, dim, 0, n));
        qm.extract(dim, dim + nvars, n, n + nvars).diagonal().set(1);
    }

    @Override
    public void T(int pos, SubMatrix tr) {
        ssf.T(pos, tr.extract(0, dim, 0, dim));
    }

    @Override
    public void addV(int pos, SubMatrix p) {
        ssf.addV(pos, p.extract(0, dim, 0, dim));
        ssf.addH(pos, p.extract(dim, dim + nvars, dim, dim + nvars));
    }

    /**
     * |x0 x1||T 0| = |x0 T 0| |0 0|
     *
     * @param pos
     * @param x
     */
    @Override
    public void XT(int pos, DataBlock x) {
        ssf.XT(pos, x.range(0, dim));
        x.range(dim, dim + nvars).set(0);
    }

    /**
     * |T 0||x0| = |T x0| |0 0||x1| |0 |
     *
     * @param pos
     * @param x
     */
    @Override
    public void TX(int pos, DataBlock x) {
        ssf.TX(pos, x.range(0, dim));
        x.range(dim, dim + nvars).set(0);
    }

    @Override
    public double H(int pos, int v) {
        return 0;
    }

    @Override
    public void Z(int pos, int v, DataBlock z) {
        ssf.Z(pos, v, z.range(0, dim));
        z.set(dim + v, 1);
    }

    @Override
    public double ZVZ(int pos, int v, int w, SubMatrix V) {
        double d = ssf.ZVZ(pos, v, w, V.extract(0, dim, 0, dim));
        d += ssf.ZX(pos, w, V.column(dim + w).range(0, dim));
        d += ssf.ZX(pos, w, V.row(dim + v).range(0, dim));
        d += V.get(v + dim, w + dim);
        return d;
    }

    @Override
    public void VpZdZ(int pos, int v, int w, SubMatrix V, double d) {
        ssf.VpZdZ(pos, v, w, V.extract(0, dim, 0, dim), d);
        ssf.XpZd(pos, w, V.column(dim + w).range(0, dim), d);
        ssf.XpZd(pos, w, V.row(dim + v).range(0, dim), d);
        V.add(v + dim, w + dim, d);
    }

}
