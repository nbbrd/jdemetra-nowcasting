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

import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.realfunctions.ISsqFunctionMinimizer;
import ec.tstoolkit.maths.realfunctions.levmar.LevenbergMarquardtMethod;
import ec.tstoolkit.mssf2.IMSsfData;
import ec.tstoolkit.mssf2.MSsfAlgorithm;
import ec.tstoolkit.mssf2.MSsfFunction;
import ec.tstoolkit.mssf2.MSsfFunctionInstance;
import ec.tstoolkit.mssf2.MultivariateSsfData;
import ec.tstoolkit.timeseries.simplets.TsDomain;

/**
 *
 * @author Jean Palate
 */
public class DfmEstimator implements IDfmEstimator {

    public static interface IEstimationHook {

        boolean next(DynamicFactorModel current, Likelihood ll);
    }

    private static class Minimizer extends LevenbergMarquardtMethod {

        private final IEstimationHook hook_;

        private Minimizer(IEstimationHook hook) {
            hook_ = hook;
        }

        @Override
        protected boolean iterate() {
            boolean rslt = super.iterate();
            MSsfFunctionInstance pt = (MSsfFunctionInstance) getResult();
            DynamicFactorModel model = ((DynamicFactorModel.Ssf) pt.ssf).getModel();
            if (hook_ != null) {
                rslt = hook_.next(model, pt.getLikelihood()) && rslt;
            }
            return rslt;
        }
    }
    private int maxiter_ = 100;
    private boolean converged_;
    private final ISsqFunctionMinimizer min_;
    private int nstart_ = 10, nnext_ = 3;
   private TsDomain idom_;
    
 
   public DfmEstimator() {
        min_ = new LevenbergMarquardtMethod();
    }

   public DfmEstimator(IEstimationHook hook) {
        min_ = new Minimizer(hook);
    }

    public DfmEstimator(ISsqFunctionMinimizer min) {
        min_ = min.exemplar();
    }

   public TsDomain getEstimationDomain(){
        return idom_;
    }
    
    public void setEstimationDomain(TsDomain dom){
        idom_=dom;
    }
    
    public int getMaxIter() {
        return maxiter_;
    }

    public int getMaxInitialIter() {
        return nstart_;
    }

    public int getMaxNextIter() {
        return nnext_;
    }

    public void setMaxIter(int iter) {
        maxiter_ = iter;
    }

    public void setMaxInitialIter(int n) {
        nstart_ = n;
    }

    public void setMaxNextIter(int n) {
        nnext_ = n;
    }

    public boolean hasConverged() {
        return converged_;
    }

    @Override
    public boolean estimate(final DynamicFactorModel dfm, DfmInformationSet input) {
        try {
            converged_ = false;
            Matrix m = input.generateMatrix(idom_);
            MSsfAlgorithm algorithm = new MSsfAlgorithm();
            IMSsfData mdata = new MultivariateSsfData(m.subMatrix().transpose(), null);
            DfmMapping mapping;
            MSsfFunction fn;
            MSsfFunctionInstance pt;
            int niter = 0;
            DynamicFactorModel model = dfm.clone();
            model.normalize();
            while (true) {
                mapping = new DfmMapping(model, false, true);
                fn = new MSsfFunction(mdata, mapping, algorithm);
                min_.setMaxIter(niter == 0 ? nstart_ : nnext_);
                min_.minimize(fn, fn.evaluate(mapping.map(model)));
                niter += min_.getIterCount();
                if (niter >= maxiter_) {
                    break;
                }
                pt = (MSsfFunctionInstance) min_.getResult();
                model = ((DynamicFactorModel.Ssf) pt.ssf).getModel();
                mapping = new DfmMapping(model, true, false);
                fn = new MSsfFunction(mdata, mapping, algorithm);
                min_.setMaxIter(nnext_);
                min_.minimize(fn, fn.evaluate(mapping.map(model)));
                niter += min_.getIterCount();
                if (niter >= maxiter_) {
                    break;
                }
                pt = (MSsfFunctionInstance) min_.getResult();
                model = ((DynamicFactorModel.Ssf) pt.ssf).getModel();
                mapping = new DfmMapping(model, false, false);
                fn = new MSsfFunction(mdata, mapping, algorithm);
                converged_ = min_.minimize(fn, fn.evaluate(mapping.map(model)))
                        && min_.getIterCount() < nnext_;
                niter += min_.getIterCount();
                pt = (MSsfFunctionInstance) min_.getResult();
                model = ((DynamicFactorModel.Ssf) pt.ssf).getModel();
                if (converged_) {
                    break;
                }
                if (niter >= maxiter_) {
                    break;
                }
            }
            model.normalize();
            dfm.copy(model);
            return true;
        } catch (Exception err) {
            return false;
        }
    }
}
