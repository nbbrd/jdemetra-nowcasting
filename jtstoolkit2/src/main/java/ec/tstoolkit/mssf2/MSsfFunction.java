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

import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.realfunctions.IFunction;
import ec.tstoolkit.maths.realfunctions.IFunctionDerivatives;
import ec.tstoolkit.maths.realfunctions.IFunctionInstance;
import ec.tstoolkit.maths.realfunctions.IParametersDomain;
import ec.tstoolkit.maths.realfunctions.IParametricMapping;
import ec.tstoolkit.maths.realfunctions.ISsqFunction;
import ec.tstoolkit.maths.realfunctions.ISsqFunctionDerivatives;
import ec.tstoolkit.maths.realfunctions.ISsqFunctionInstance;
import ec.tstoolkit.maths.realfunctions.NumericalDerivatives;
import ec.tstoolkit.maths.realfunctions.SsqNumericalDerivatives;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class MSsfFunction<S extends IMSsf> implements IFunction, ISsqFunction {

    /**
     *
     */
    public final IMSsfAlgorithm algorithm;
    /**
     *
     */
    public final IParametricMapping<S> mapper;
    /**
     *
     */
    public final IMSsfData data;

    /**
     *
     * @param model
     * @param mapper
     * @param algorithm
     */
    public MSsfFunction(IMSsfData data, IParametricMapping<S> mapper,
	    IMSsfAlgorithm algorithm) {
	this.data = data;
	this.mapper = mapper;
	this.algorithm = algorithm;
    }

    @Override
    public MSsfFunctionInstance<S> evaluate(IReadDataBlock parameters) {
	return new MSsfFunctionInstance<>(this, parameters);
    }

    @Override
    public NumericalDerivatives getDerivatives(IFunctionInstance point) {
	return new NumericalDerivatives(this, point, false,true);
    }

    @Override
    public SsqNumericalDerivatives getDerivatives(ISsqFunctionInstance point) {
	return new SsqNumericalDerivatives(this, point, false, true);
    }

    /**
     *
     * @return
     */
    @Override
    public IParametersDomain getDomain() {
	return mapper;
    }

    @Override
    public MSsfFunctionInstance<S> ssqEvaluate(IReadDataBlock parameters) {
	return new MSsfFunctionInstance<>(this, parameters);
    }
}
