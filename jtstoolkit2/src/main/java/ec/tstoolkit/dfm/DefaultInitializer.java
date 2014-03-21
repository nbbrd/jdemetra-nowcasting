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
        dfm.setDefault();
        return true;
    }

}
