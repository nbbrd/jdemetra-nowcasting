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
package ec.tstoolkit.dfm;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.IDataBlock;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementDescriptor;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.MatrixException;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import ec.tstoolkit.maths.realfunctions.IParametricMapping;
import ec.tstoolkit.maths.realfunctions.ParamValidation;
import ec.tstoolkit.mssf2.IMSsf;

/**
 *
 * @author Jean Palate
 */
public class DfmMapping2 implements IParametricMapping<IMSsf> {

    private final DynamicFactorModel template;
    // [0, nml[ loadings
    // [nml, nml+nm[ meas. variance (square roots)
    // [nml+nm, nml+nm+nb*nb*nl[ var parameters 
    // [nml+nb*nb*nl+nm, nml+nb*nb*nl+nm+nb*(nb-1)/2[ trans. covariance (cholesky factor), by row 
    private final int np;
    private final int nml, nm, nb, nl;
    private final int l0, mv0, v0;
    private double lmax = 5;

    private IReadDataBlock loadings(IReadDataBlock p) {
        return l0 < 0 ? null : p.rextract(l0, nml);
    }

    private IReadDataBlock vparams(IReadDataBlock p) {
        return v0 < 0 ? null : p.rextract(v0, nb * nb * nl);
    }

    private IReadDataBlock mvars(IReadDataBlock p) {
        return mv0 < 0 ? null : p.rextract(mv0, nm);
    }

    private DataBlock loadings(DataBlock p) {
        return l0 < 0 ? null : p.extract(l0, nml);
    }

    private DataBlock vparams(DataBlock p) {
        return v0 < 0 ? null : p.extract(v0, nb * nb * nl);
    }

    private DataBlock mvars(DataBlock p) {
        return mv0 < 0 ? null : p.extract(mv0, nm);
    }

    public DfmMapping2(DynamicFactorModel model) {
        this(model, false, false);
    }

    public DfmMapping2(DynamicFactorModel model, final boolean mfixed, final boolean tfixed) {
        template = model.clone();
        template.normalize();
        template.getTransition().covar.set(0);
        template.getTransition().covar.diagonal().set(1);
        nb = template.getTransition().nbloks;
        nl = template.getTransition().nlags;
        nm = template.getMeasurementsCount();
        // measurement: all loadings, all var
        // vparams
        // covar
        int p = 0;
        if (mfixed) {
            nml = 0;
            l0 = -1;
            mv0 = -1;
        } else {
            int n = 0;
            for (MeasurementDescriptor desc : template.getMeasurements()) {
                n += desc.coeff.length;
            }
            nml = n;
            l0 = 0;
            mv0 = nml;
            p += nml + nm;
        }
        if (tfixed) {
            v0 = -1;
        } else {
            v0 = p;
            p += nl * nb * nb;
        }
        np = p;

    }

    public DataBlock getDefault() {
        DataBlock p = new DataBlock(np);
        if (mv0 >= 0) {
            mvars(p).set(1);
        }
        //loadings(p).set(.1);
        return p;
    }

    @Override
    public IMSsf map(IReadDataBlock p) {
        DynamicFactorModel m = template.clone();
        IReadDataBlock l = loadings(p);
        IReadDataBlock mv = mvars(p);
        int i0 = 0, j0 = 0;
        if (l != null) {
            int n = 0;
            for (MeasurementDescriptor desc : m.getMeasurements()) {
                l.rextract(i0, desc.coeff.length).copyTo(desc.coeff, 0);
                i0 += desc.coeff.length;
                double x = mv.get(j0++);
                desc.var = x * x;
                ++n;
            }
        }
        IReadDataBlock vp = vparams(p);
        if (vp != null) {
            Matrix v = m.getTransition().covar;
            Matrix t = m.getTransition().varParams;
            vp.copyTo(t.internalStorage(), 0);
        }
        return m.ssfRepresentation();
    }

    @Override
    public IReadDataBlock map(IMSsf mssf) {
        DynamicFactorModel.Ssf ssf = (DynamicFactorModel.Ssf) mssf;
        DynamicFactorModel m = ssf.getModel();
        // copy to p
        DataBlock p = new DataBlock(np);
        DataBlock l = loadings(p);
        DataBlock mv = mvars(p);
        int i0 = 0, j0 = 0;
        if (l != null) {
            for (MeasurementDescriptor desc : m.getMeasurements()) {
                l.extract(i0, desc.coeff.length).copyFrom(desc.coeff, 0);
                i0 += desc.coeff.length;
                mv.set(j0++, Math.sqrt(desc.var));
            }
        }
        DataBlock vp = vparams(p);
        if (vp != null) {
//            Matrix v = m.getTransition().covar.clone();
//            SymmetricMatrix.lcholesky(v);
//            i0 = 0;
//            for (int i = 0; i < nb; ++i) {
//                tv.extract(i0, i+1).copy(v.row(i).range(0, i + 1));
//                i0 += i + 1;
//            }
            Matrix t = m.getTransition().varParams;
            vp.copyFrom(t.internalStorage(), 0);
        }
        return p;
    }

    @Override
    public boolean checkBoundaries(IReadDataBlock inparams) {
        return true;
    }

    @Override
    public double epsilon(IReadDataBlock inparams, int idx) {
        return 1e-6;
    }

    @Override
    public int getDim() {
        return np;
    }

    @Override
    public double lbound(int idx) {
        return -Double.MAX_VALUE;
    }

    @Override
    public double ubound(int idx) {
        return Double.MAX_VALUE;
    }

    @Override
    public ParamValidation validate(IDataBlock ioparams) {
        return ParamValidation.Valid;
    }
}
