/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tss.Dfm;

import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.MatrixException;

/**
 * Builds a Rotation object that contains:
 * - The rotation matrix R itself, which is the product of q(q-1)/2 rotation 
 *   matrices representing all possible pairwise angular rotations.
 * - An array of angles (as many angles as needed to rotate all possible pairs 
 *   of dimensions of an hypothetical vector of size q). 
 * - The dimension q of an hypothetical vector to which the rotation is applied
 *   q = (int) (0.5 * (1 + Math.sqrt(1 + 8 * angles.length)));
 *   
 *  Notes:
 * - Because the array of angles univocally determines the rotation matrix,
 *   we define it as a final variable. 
 * - If R was declared as final, we would have to define it in the Rotation 
 *   constructor (as we have done for "angles"). However, at the current 
 *   development stage, we are not sure that the calculation of R as a function 
 *   of angles and multiplying them by a given vector is more efficient that 
 *   applying a rotation directly to a multidimensional axis.
 *   
 * @author deanton
 */
public class Rotation {

    private final double[] angles;
    private int q;
    private Matrix R;

    public Rotation(double[] angles) {
        if (angles ==null || angles.length == 0)
            throw new MatrixException("Invalid rotation");

           
        this.angles = angles.clone();
        
        q = getq(); 
        
        if (angles.length != q*(q-1)/2)
            throw new MatrixException("Invalid number of angles");
      
    }

    public Matrix getRotation() {
        if (R == null) {
            calcR();
        }
        return R;
    }

       public int getq() {
        if (q == 0) {
          q = (int) (0.5 * (1 + Math.sqrt(1 + 8 * angles.length)));
        }
        return q;
    }
       
       public double[]  getangles() {
       
        return angles;
    }
       
    private void calcR() {
        q = (int) (0.5 * (1 + Math.sqrt(1 + 8 * angles.length)));
        R = Matrix.identity(q);
        Matrix Ra = new Matrix(2, 2);
        int[] irows = new int[2];
        int[] icols = new int[2];

    
        int pairwise = 0;

        double alpha = 0;

        for (int j = 0; j < q - 1; j++) {
            for (int kk = j + 1; kk < q; kk++) {

                alpha = angles[pairwise];
               

                Ra.subMatrix(0, 1, 0, 1).set(Math.cos(alpha));
                Ra.subMatrix(1, 2, 0, 1).set(-Math.sin(alpha));
                Ra.subMatrix(0, 1, 1, 2).set(Math.sin(alpha));
                Ra.subMatrix(1, 2, 1, 2).set(Math.cos(alpha));

                irows[0] = j;
                irows[1] = kk;
                icols[0] = j;
                icols[1] = kk;

                Matrix temp = Matrix.identity(q);
                temp.subMatrix().copy(Ra.subMatrix(), irows, icols);

                R = R.times(temp);

                pairwise = +1;

            }
        }
    }

    // rendre plus eficace (peut-etre)
    public Matrix apply(Matrix Q) {

        Matrix CR = Q.times(getRotation());
        return CR;
    }

}
