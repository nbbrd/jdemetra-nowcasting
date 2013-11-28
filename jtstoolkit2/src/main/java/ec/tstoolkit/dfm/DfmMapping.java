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
public class DfmMapping implements IParametricMapping<IMSsf> {

    private final DynamicFactorModel template;
    // [0, nml[ loadings
    // [nml, nml+nm[ meas. variance (square roots)
    // [nml+nm, nml+nm+nb*nb*nl[ var parameters 
    // [nml+nb*nb*nl+nm, nml+nb*nb*nl+nm+nb*(nb-1)/2[ trans. covariance (cholesky factor), by row 
    private final int np;
    private final int nml, nm, nb, nl;
    private final int l0, mv0, v0, tv0;
    private double lmax = 5;
    private final int[] mmax;
    private final double[] fmax;

    private IReadDataBlock loadings(IReadDataBlock p) {
        return l0 < 0 ? null : p.rextract(l0, nml);
    }

    private IReadDataBlock vparams(IReadDataBlock p) {
        return v0 < 0 ? null : p.rextract(v0, nb * nb * nl);
    }

    private IReadDataBlock mvars(IReadDataBlock p) {
        return mv0 < 0 ? null : p.rextract(mv0, nm);
    }

    private IReadDataBlock tvars(IReadDataBlock p) {
        return tv0 < 0 ? null : p.rextract(tv0, nb * (nb + 1) / 2);
        // return tv0 < 0 ? null : p.rextract(tv0, nb);
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

    private DataBlock tvars(DataBlock p) {
//        return tv0 < 0 ? null : p.extract(tv0, nb );
        return tv0 < 0 ? null : p.extract(tv0, nb * (nb + 1) / 2);
    }

    private void mtvar(Matrix v, IReadDataBlock tv) {
        int i0 = 0;
        Matrix tmp = new Matrix(nb, nb);
        for (int i = 0; i < nb; ++i) {
            DataBlock x = tmp.row(i).range(0, i + 1);
            x.copy(tv.rextract(i0, i + 1));
            i0 += i + 1;
        }
        SymmetricMatrix.XXt(tmp.subMatrix(), v.subMatrix());
//        v.set(0);
//        DataBlock d=v.diagonal();
//        d.copy(tv);
//        d.square();
    }

    public DfmMapping(DynamicFactorModel model) {
        this(model, false, false);
    }

    public DfmMapping(DynamicFactorModel model, final boolean mfixed, final boolean tfixed) {
        template = model.clone();
        nb = template.getFactorsCount();
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
            mmax = null;
            fmax = null;
        } else {
            int n = 0;
            for (MeasurementDescriptor desc : template.getMeasurements()) {
                for (int i=0; i<nb; ++i)
                    if (! Double.isNaN(desc.coeff[i]))
                        ++n;
            }
            nml = n - nb;
            mmax = new int[nb];
            for (int i = 0; i < nb; ++i) {
                mmax[i] = -1;
            }
            n = 0;
            fmax = new double[nb];
            for (MeasurementDescriptor desc : template.getMeasurements()) {
                for (int j = 0; j < nb; ++j) {
                    double f = desc.coeff[j];
                    if (!Double.isNaN(f) && (mmax[j] < 0 || Math.abs(f) > fmax[j])) {
                        mmax[j] = n;
                        fmax[j] = f;
                    }
                }
                ++n;
            }
            for (int i = 0; i < nb; ++i) {
                if (fmax[i] == 0) {
                    fmax[i] = 1;
                }
            }
            l0 = 0;
            mv0 = nml;
            p += nml + nm;
        }
        if (tfixed) {
            v0 = -1;
            tv0 = -1;
        } else {
            v0 = p;
            tv0 = p + nb * nb * nl;
            p = tv0 + nb * (nb + 1) / 2;
            //         p = tv0 + nb;
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

    public IReadDataBlock parameters() {
        return map(template);
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
                for (int k = 0; k < nb; ++k) {
                    if (!Double.isNaN(desc.coeff[k])) {
                        if (n != mmax[k]) {
                            desc.coeff[k] = l.get(i0++);
                        } else {
                            desc.coeff[k] = fmax[k];
                        }
                    }
                }
                double x = mv.get(j0++);
                desc.var = x * x;
                ++n;
            }
        }
        IReadDataBlock tv = tvars(p), vp = vparams(p);
        if (tv != null) {
            Matrix v = m.getTransition().covar;
            Matrix t = m.getTransition().varParams;
            mtvar(v, tv);
            vp.copyTo(t.internalStorage(), 0);
        }
        return m.ssfRepresentation();
    }

    @Override
    public IReadDataBlock map(IMSsf mssf) {
        DynamicFactorModel.Ssf ssf = (DynamicFactorModel.Ssf) mssf;
        DynamicFactorModel m = ssf.getModel();
        return map(m);
    }

    public IReadDataBlock map(DynamicFactorModel m) {
        // copy to p
        DataBlock p = new DataBlock(np);
        DataBlock l = loadings(p);
        DataBlock mv = mvars(p);
        int i0 = 0, j0 = 0;
        if (l != null) {
            int n = 0;
            for (MeasurementDescriptor desc : m.getMeasurements()) {
                for (int k = 0; k < nb; ++k) {
                    if (!Double.isNaN(desc.coeff[k]) && n != mmax[k]) {
                        l.set(i0++, desc.coeff[k]);
                    }
                }
                mv.set(j0++, Math.sqrt(desc.var));
                ++n;
            }
        }
        DataBlock tv = tvars(p), vp = vparams(p);
        if (tv != null) {
            Matrix v = m.getTransition().covar.clone();
            SymmetricMatrix.lcholesky(v);
            i0 = 0;
            for (int i = 0; i < nb; ++i) {
                tv.extract(i0, i + 1).copy(v.row(i).range(0, i + 1));
                i0 += i + 1;
            }
//            tv.copy(m.getTransition().covar.diagonal());
//            tv.sqrt();
            Matrix t = m.getTransition().varParams;
            vp.copyFrom(t.internalStorage(), 0);
        }
        return p;
    }

    @Override
    public boolean checkBoundaries(IReadDataBlock inparams) {
//        IReadDataBlock t=tvars(inparams);
//        if (t != null ){
//            for (int i=0; i<t.getLength(); ++i)
//                if (t.get(i)<0)
//                    return false;
//        }
//            
//        IReadDataBlock t = tvars(inparams);
//        IReadDataBlock l = loadings(inparams);
//        if (l != null) {
//            for (int i = 0; i < nml; ++i) {
//                if (Math.abs(l.get(i)) > lmax) {
//                    return false;
//                }
//            }
//        }
//        if (t != null) {
//            for (int i = 1, k = 0; i < nb; ++i) {
//                double x = 0;
//                for (int j = 0; j < i; ++j, ++k) {
//                    double y = t.get(k);
//                    x += y * y;
//                }
//                if (x >= 1) {
//                    return false;
//                }
//            }
//        }
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
//        boolean changed = false;
//        if (l0 >= 0) {
//            for (int i = 0; i < nml; ++i) {
//                double cur = ioparams.get(l0 + i);
//                if (Math.abs(cur) > lmax) {
//                    changed = true;
//                    ioparams.set(l0 + i, cur < 0 ? -lmax : lmax);
//                }
//            }
//        }
//        IReadDataBlock t = tvars(ioparams);
//        if (t != null) {
//            for (int i = 1, k = 0; i < nb; ++i) {
//                int l = k;
//                double x = 0;
//                for (int j = 0; j < i; ++j, ++k) {
//                    double y = t.get(k);
//                    x += y * y;
//                }
//                if (x >= 1) {
//                    changed = true;
//                    double w = .99 / Math.sqrt(x);
//                    int s = tv0;
//                    for (int j = 0; j < i; ++j, ++l) {
//                        ioparams.set(s + l, t.get(l) * w);
//                    }
//                }
//            }
//        }
//        return changed ? ParamValidation.Changed : ParamValidation.Valid;
        return ParamValidation.Valid;
    }
}
