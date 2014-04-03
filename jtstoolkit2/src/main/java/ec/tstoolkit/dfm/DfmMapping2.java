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
import ec.tstoolkit.maths.matrices.EigenSystem;
import ec.tstoolkit.maths.matrices.IEigenSystem;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.MatrixException;
import static ec.tstoolkit.maths.realfunctions.IParametersDomain.PARAM;
import ec.tstoolkit.maths.realfunctions.ParamValidation;
import ec.tstoolkit.mssf2.IMSsf;

/**
 *
 * @author Jean Palate
 */
public class DfmMapping2 implements IDfmMapping {

    static final double EPS = 1e-5;

    private final DynamicFactorModel template;
    // [0, nml[ loadings
    // [nml, nml+nm[ meas. variance (square roots)
    // [nml+nm, nml+nm+nb*nb*nl[ var parameters 
    // trans. covariance = I
    private final int np;
    private final int nml, nm, nb, nl;
    private final int l0, mv0, v0;
    private final int ivmax;
    private final double vmax;

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
        nb = template.getFactorsCount();
        nl = template.getTransition().nlags;
        // measurement: all loadings, all var
        // vparams
        // covar
        int p;
        if (mfixed) {
            nml = 0;
            nm = 0;
            l0 = -1;
            mv0 = -1;
            v0 = 0;
            ivmax = -1;
            vmax = 0;
            p = nb * nb * nl;
        } else {
            int n = 0, m = 0;
            int iv = -1;
            double v = 0;
            for (MeasurementDescriptor desc : template.getMeasurements()) {
                if (desc.var > v) {
                    v = desc.var;
                    iv = m;
                }
                for (int i = 0; i < nb; ++i) {
                    if (!Double.isNaN(desc.coeff[i])) {
                        ++n;
                    }
                }
                ++m;
            }
            l0 = 0;
            ivmax = iv;
            vmax = v;
            nm = template.getMeasurementsCount() - 1;
            nml = n;
            mv0 = nml;
            p = nm + nml;
            if (tfixed) {
                v0 = -1;
            } else {
                //         p = tv0 + nb;
                v0 = p;
                p += nb * nb * nl;
            }
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
                        desc.coeff[k] = l.get(i0++);
                    }
                }
                if (n == ivmax) {
                    desc.var = vmax;
                } else {
                    double x = mv.get(j0++);
                    desc.var = x * x;
                }
                ++n;
            }
        }
        IReadDataBlock vp = vparams(p);
        if (vp != null) {
            Matrix t = m.getTransition().varParams;
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

    @Override
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
                    if (!Double.isNaN(desc.coeff[k])) {
                        l.set(i0++, desc.coeff[k]);
                    }
                }
                if (n != ivmax) {
                    mv.set(j0++, Math.sqrt(desc.var));
                }
                ++n;
            }
        }
        DataBlock vp = vparams(p);
        if (vp != null) {
            Matrix t = m.getTransition().varParams;
            vp.copyFrom(t.internalStorage(), 0);
        }
        return p;
    }

    @Override
    public boolean checkBoundaries(IReadDataBlock inparams) {
        // check the stability of VAR
        try {
            IReadDataBlock vp = vparams(inparams);
            if (vp == null) {
                return true;
            }
            // s=(f0,t f1,t f2,t f0,t-1 f1,t-1 f2,t-1 ...f0,t-l+1 f1,t-l+1 f2,t-l+1)
            //    |x00 x10 x20   
            // T =|...
            // T =|1   0   0
            //    |0   1   0
            //    |...
            Matrix Q = new Matrix(nb * nl, nb * nl);
            for (int i = 0, i0 = 0; i < nb; ++i) {
                for (int l = 0; l < nl; ++l, i0 += nb) {
                    DataBlock c = Q.column(l * nb + i).range(0, nb);
                    c.copy(vp.rextract(i0, nb));
                }
            }
            Q.subDiagonal(-nb).set(1);
            IEigenSystem es = EigenSystem.create(Q, false);
            return es.getEigenValues(1)[0].abs() < .99;
        } catch (MatrixException err) {
            return false;
        }
//        return true;
    }

    @Override
    public double epsilon(IReadDataBlock inparams, int idx) {
        return inparams.get(idx) > 0 ? -EPS : EPS;
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
        return checkBoundaries(ioparams) ? ParamValidation.Valid : ParamValidation.Invalid;
    }

    @Override
    public String getDescription(int idx) {
        return PARAM + idx;
    }
}
