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
package ec.tstoolkit.ssf2;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockIterator;
import ec.tstoolkit.maths.matrices.SubMatrix;

/**
 *
 * @author palatej
 */
public class Utilities {

    /**
     *
     * @param ssf
     * @param pos
     * @param m
     * @param zm
     */
    public static void ZM(ISsf ssf, int pos, DataBlockIterator m, DataBlock zm) {
        DataBlock cur = m.getData();
        do {
            zm.set(m.getPosition(), ssf.ZX(pos, cur));
        } while (m.next());
    }

    /**
     *
     * @param ssf
     * @param pos
     * @param M
     */
    public static void TM(ISsfBase ssf, int pos, DataBlockIterator M) {
        DataBlock cur = M.getData();
        do {
            ssf.TX(pos, cur);
        } while (M.next());
    }

    /**
     * Computes T * V * T', where V is assumed to be symmetric
     *
     * @param ssf
     * @param pos
     * @param V Symmetric (sub-)matrix
     */
    public static void TVTt(ISsfBase ssf, int pos, SubMatrix V) {
        TM(ssf, pos, V.columns());
        TM(ssf, pos, V.rows());
    }

    /**
     *
     * @param ssf
     * @param pos
     * @param M
     */
    public static void MT(ISsfBase ssf, int pos, DataBlockIterator M) {
        DataBlock cur = M.getData();
        do {
            ssf.XT(pos, cur);
        } while (M.next());
    }

    /**
     * Computes T' * V * T, where V is assumed to be symmetric
     *
     * @param ssf
     * @param pos
     * @param V Symmetric (sub-)matrix
     */
    public static void TtVT(ISsfBase ssf, int pos, SubMatrix V) {
        MT(ssf, pos, V.rows());
        MT(ssf, pos, V.columns());
    }
    /**
     * Computes XL = X(T-K*Z)
     *
     * @param ssf
     * @param pos
     * @param X
     * @param K
     */
    public static void XL(ISsf ssf, int pos, DataBlock X, DataBlock K) {
        // X*L = XT - XKZ
        // compute XT
        // XT*M/f
        double a = X.dot(K);
        ssf.XT(pos, X);
        ssf.XpZd(pos, X, -a);
    }

    /**
     *
     * @param ssf
     * @param pos
     * @param X
     * @param K
     */
    public static void XL(ISsf ssf, int pos, DataBlockIterator X, DataBlock K) {
        DataBlock x = X.getData();
        do {
            XL(ssf, pos, x, K);
        } while (X.next());
    }
    /**
     * Computes V = L' * V * L , where V is assumed to be symmetric
     * @param ssf
     * @param pos
     * @param V
     * @param K 
     */
    public static void LtVL(ISsf ssf, int pos, SubMatrix V, DataBlock K) {
        XL(ssf,pos,V.rows(), K);
        XL(ssf,pos,V.columns(), K);
    }
}
