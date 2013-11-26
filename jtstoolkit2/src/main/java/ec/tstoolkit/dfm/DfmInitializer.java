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
import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.data.ReadDataBlock;
import ec.tstoolkit.eco.Ols;
import ec.tstoolkit.eco.RegModel;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.MatrixException;
import ec.tstoolkit.maths.matrices.SingularValueDecomposition;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import ec.tstoolkit.pca.PrincipalComponents;
import ec.tstoolkit.timeseries.simplets.AverageInterpolator;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDataTable;
import ec.tstoolkit.timeseries.simplets.TsDataTableInfo;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import ec.tstoolkit.utilities.IntList;

/**
 *
 * @author Jean Palate
 */
public class DfmInitializer {

    private TsData[] input_;
    private TsDomain domain_;
    private DynamicFactorModel model_, modelc_;
    private Matrix data_, datac_;
    private double[] w_;
    private ec.tstoolkit.pca.PrincipalComponents[] pc_;

    public boolean initialize(DynamicFactorModel model, TsData[] input, TsDomain domain) {
        clear();
        model_ = model.normalize();
        input_ = input;
        domain_ = domain;
        if (!computeMatrix()) {
            return false;
        }
        if (!computePrincipalComponents()) {
            return false;
        }
        if (!computeVar()) {
            return false;
        }
        if (!computeLoadings()) {
            return false;
        }
        modelc_ = model_.normalize();
        return true;
    }

    public Matrix getData() {
        return data_;
    }
    
    public DynamicFactorModel getInitialModel(){
        return modelc_;
    }

    public Matrix getInterpolatedData() {
        return datac_;
    }

    public PrincipalComponents getPrincipalComponents(int block) {
        return pc_[block];
    }

    private void clear() {
        modelc_=null;
        datac_=null;
        pc_=null;
    }

    private boolean computeMatrix() {
        DfmInformationSet info = new DfmInformationSet(input_);
        data_ = info.generateMatrix(domain_);
        if (domain_ == null) {
            domain_ = info.getCurrentDomain();
        }
        datac_ = new Matrix(data_.getRowsCount(), data_.getColumnsCount());
        AverageInterpolator interpolator = new AverageInterpolator();
        for (int i = 0; i < input_.length; ++i) {
            TsData c = new TsData(domain_.getStart(), data_.column(i));
            if (!interpolator.interpolate(c, null)) {
                return false;
            }
            datac_.column(i).copy(c);
        }
        return true;
    }

    private boolean computePrincipalComponents() {
        int nb = model_.getTransition().nbloks;
        pc_ = new PrincipalComponents[nb];
        for (int i = 0; i < nb; ++i) {
            Matrix x = prepareDataForCompenent(i);
            pc_[i] = new PrincipalComponents();
            pc_[i].process(x);
        }
        return true;
    }

    private Matrix prepareDataForCompenent(int cmp) {
        int np = 0;
        for (DynamicFactorModel.MeasurementDescriptor desc : model_.getMeasurements()) {
            for (int i = 0; i < desc.factors.length; ++i) {
                if (desc.factors[i] == cmp) {
                    ++np;
                    break;
                }
            }
        }
        Matrix m = new Matrix(datac_.getRowsCount(), np);
        np = 0;
        int s = 0;
        for (DynamicFactorModel.MeasurementDescriptor desc : model_.getMeasurements()) {
            for (int i = 0; i < desc.factors.length; ++i) {
                if (desc.factors[i] == cmp) {
                    m.column(np).copy(datac_.column(s));
                    for (int j = 0; j < desc.factors.length; ++j) {
                        int jcmp = desc.factors[j];
                        if (jcmp < cmp) {
                            SingularValueDecomposition svd = pc_[jcmp].getSvd();
                            double l = -svd.S()[0] * svd.V().get(cmp, s);
                            m.column(cmp).addAY(-1, svd.U().column(0));
                        }
                    }
                    ++np;
                    break;
                }
            }
            ++s;
        }
        return m;
    }

    private boolean computeVar() {
        DynamicFactorModel.TransitionDescriptor tr = model_.getTransition();
        int nl = tr.nlags, nb = tr.nbloks;
        DataBlock[] f = new DataBlock[nb];
        DataBlock[] e = new DataBlock[nb];
        Matrix M = new Matrix(domain_.getLength() - nl, nl * nb);
        int c = 0;
        for (int i = 0; i < nb; ++i) {
            DataBlock cur = pc_[i].getFactor(0);
            f[i] = cur.drop(nl, 0);
            for (int j = 1; j <= nl; ++j) {
                M.column(c++).copy(cur.drop(nl - j, j));
            }
        }
        RegModel model = new RegModel();
        for (int j = 0; j < M.getColumnsCount(); ++j) {
            model.addX(M.column(j));
        }
        for (int i = 0; i < nb; ++i) {
            model.setY(f[i]);
            Ols ols = new Ols();
            if (!ols.process(model)) {
                return false;
            }
            tr.varParams.row(i).copyFrom(ols.getLikelihood().getB(), 0);
            e[i] = ols.getResiduals();
        }

        for (int i = 0; i < nb; ++i) {
            for (int j = 0; j <= i; ++j) {
                tr.covar.set(i, j, DescriptiveStatistics.cov(e[i].getData(), e[j].getData(), 0));
            }
        }
        SymmetricMatrix.fromLower(tr.covar);
        return true;
    }

    private boolean computeLoadings() {
        // creates the matrix of factors
        DynamicFactorModel.TransitionDescriptor tr = model_.getTransition();
        int nb = tr.nbloks, blen = model_.getBlockLength();
        Matrix M = new Matrix(domain_.getLength() - (blen - 1), nb * blen);
        int c = 0;
        for (int i = 0; i < nb; ++i) {
            DataBlock cur = pc_[i].getFactor(0);
            for (int j = 0; j < blen; ++j) {
                M.column(c++).copy(cur.drop(blen - 1 - j, j));
            }
        }
        int v = 0;
        for (DynamicFactorModel.MeasurementDescriptor desc : model_.getMeasurements()) {
            DataBlock y = datac_.column(v++).drop(blen - 1, 0);
            RegModel model = new RegModel();
            model.setY(y);
            for (int j = 0; j < desc.factors.length; ++j) {
                double[] x = new double[y.getLength()];
                int fac = desc.factors[j];
                int s = fac * blen;
                int l = desc.type.getLength();
                for (int r = 0; r < x.length; ++r) {
                    x[r] = desc.type.dot(M.row(r).extract(s, l));
                }
                model.addX(new DataBlock(x));
            }
            Ols ols = new Ols();
            if (!ols.process(model)) {
                return false;
            }
            System.arraycopy(ols.getLikelihood().getB(), 0, desc.coeff, 0, desc.coeff.length);
            desc.var = ols.getLikelihood().getSigma();
        }
        return true;
    }
}
