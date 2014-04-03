/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.maths.realfunctions.IParametricMapping;
import ec.tstoolkit.mssf2.IMSsf;

/**
 *
 * @author Jean
 */
public interface IDfmMapping extends IParametricMapping<IMSsf> {

    IReadDataBlock parameters();

    IReadDataBlock map(DynamicFactorModel m);
}
