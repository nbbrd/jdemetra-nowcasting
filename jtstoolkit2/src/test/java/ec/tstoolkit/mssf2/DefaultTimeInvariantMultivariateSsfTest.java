/*
 * Copyright 2013 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 will be approved by the European Commission - subsequent
 versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the
 Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 writing, software distributed under the Licence is
 distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 express or implied.
 * See the Licence for the specific language governing
 permissions and limitations under the Licence.
 */
package ec.tstoolkit.mssf2;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.ssf2.Smoother;
import ec.tstoolkit.ssf2.SmoothingResults;
import org.junit.Test;

/**
 *
 * @author palatej
 */
public class DefaultTimeInvariantMultivariateSsfTest {

    final DefaultTimeInvariantMultivariateSsf ssf = new DefaultTimeInvariantMultivariateSsf();
    final Matrix d;
    final int V = 30, K = 35, N = 500;

    public DefaultTimeInvariantMultivariateSsfTest() {
        ssf.initialize(V, K, V, true);
        Matrix T = new Matrix(V, V);
        T.diagonal().set(.9);
        T.subDiagonal(-1).set(-.4);
        ssf.setT(T);

        Matrix Z = new Matrix(K, V);
        Z.diagonal().set(3);
        Z.subDiagonal(1).set(2);
        Z.subDiagonal(2).set(1);
        Z.subDiagonal(-1).set(-1);
        ssf.setZ(Z);
        Matrix Q = new Matrix(V, V);
        Q.diagonal().set(1);
        ssf.setTransitionInnovations(Q, null);
        DataBlock H = new DataBlock(K);
        H.set(10);
        ssf.setH(H.getData());
        Matrix P0 = Q.clone();
        ssf.setPf0(P0);
        d = new Matrix(K, N);
        d.randomize();
        d.sub(.5);
        for (int i = 0; i < N; i += 4) {
            for (int j = 0; j < V / 2; ++j) {
                if (i % 4 != 0) {
                    d.set(j, i, Double.NaN);
                }
            }
        }
    }

    //@Test
    public void testVar() {
        long q0 = System.currentTimeMillis();
        SmoothingResults rslts = new SmoothingResults();
        Smoother smoother = new Smoother();
        smoother.setSsf(new M2UAdapter(ssf, new FullM2UMap(K)));
        smoother.setCalcVar(true);
        smoother.process(new M2UData(d, null), rslts);
        Matrix C = new Matrix(d.getColumnsCount(), V);
        Matrix EC = new Matrix(d.getColumnsCount(), V);
        for (int i = 0; i < V; ++i) {
            double[] component = rslts.component(i);
            C.column(i).copy(new DataBlock(component, 0, component.length, K));
            EC.column(i).copy(new DataBlock(rslts.componentStdev(i), 0, component.length, K));
        }
        long q1 = System.currentTimeMillis();
        System.out.println(q1 - q0);
        System.out.println(C);
        System.out.println(EC);
    }

    @Test
    public void testVar2() {
        long q0 = System.currentTimeMillis();
        for (int k = 0; k < 10; ++k) {
            ArrayFilter filter = new ArrayFilter();
            MPredictionErrorDecomposition results = new MPredictionErrorDecomposition(true);
            filter.process(ssf, new MultivariateSsfData(d.subMatrix(), null), results);

        }
        long q1 = System.currentTimeMillis();
        System.out.println("test2");
        System.out.println(q1 - q0);
    }

    @Test
    public void testVar3() {
        long q0 = System.currentTimeMillis();
        for (int k = 0; k < 10; ++k) {
            MFilter filter = new MFilter();
            MPredictionErrorDecomposition results = new MPredictionErrorDecomposition(true);
            filter.process(ssf, new MultivariateSsfData(d.subMatrix(), null), results);
        }
        long q1 = System.currentTimeMillis();
        System.out.println("test3");
        System.out.println(q1 - q0);
    }
}
