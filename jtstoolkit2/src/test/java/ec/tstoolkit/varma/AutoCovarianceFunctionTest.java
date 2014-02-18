/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.varma;

import ec.tstoolkit.maths.matrices.Matrix;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author palatej
 */
public class AutoCovarianceFunctionTest {

    public AutoCovarianceFunctionTest() {
    }

    @Test
    public void testVAR() {
        int n = 3, p = 3;

        Matrix[] P = new Matrix[p];
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < P.length; ++i) {
            Matrix m = new Matrix(n, n);
            m.diagonal().set(.15*(p-i));
            for (int r = 0; r < n; ++r) {
                for (int c = 0; c < n; ++c) {
                    if (r != c) {
                        m.set(r, c, rnd.nextDouble() * .2 - .1);
                    }
                }

            }
            P[i] = m;
        }
        
        VarmaModel varma=new VarmaModel(P, null, null);
        AutoCovarianceFunction acf=new AutoCovarianceFunction(varma);
        acf.cov(120);

    }

}
