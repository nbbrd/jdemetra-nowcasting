/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementDescriptor;

/**
 *
 * @author Jean
 */
public class DefaultInitializer implements IDfmInitializer {

    @Override
    public boolean initialize(DynamicFactorModel dfm, DfmInformationSet data) {
        for (MeasurementDescriptor m : dfm.getMeasurements()) {
            m.var = 1;
            for (int i=0; i<m.coeff.length; ++i)
                if (! Double.isNaN(m.coeff[i]))
                    m.coeff[i]=1;
        }
        int nf = dfm.getFactorsCount();
        DynamicFactorModel.TransitionDescriptor t = dfm.getTransition();
        int nl = t.nlags;
        t.covar.set(0);
        t.covar.diagonal().set(1);
        for (int i = 0; i < nf; ++i) {
            t.varParams.set(-.1);
            t.varParams.set(i, i * nl, .9);
        }
        return true;
    }

}
