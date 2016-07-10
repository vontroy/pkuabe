package pku.abe.commons.util;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;

import java.util.HashMap;
import java.util.Map;

/*
 * author: licong
 */


public class Utils {
    public static char SPACE = ' ';
    private static Pairing pairing = PairingManager.getDefaultPairing();
    /**
     * format the string. i.e. remove redundant space
     * @param s
     * @return
     */
    public static String format(String s){
        return s.trim().replaceAll("\\s+", SPACE+"");
    }

    public static boolean isEmptyString(String s){
        return s == null ? true : s.equals("") ? true : false;
    }

    public static Element[] multiple(int[][] matrix, Element[] y){
        if(matrix == null || y == null)
            return null;
        Element[] res = new Element[matrix.length];
        for(int i=0; i<matrix.length; i++){
            res[i] = multiple(matrix[i], y);
        }
        return res;
    }

    private static Element multiple(int[] array, Element[] y){
        if(array == null || y == null || array.length != y.length)
            return null;
        Element res = pairing.getZr().newZeroElement();
        for(int i=0; i<array.length; i++){
            res.add(y[i].duplicate().mul(array[i]));
        }
        return res;
    }

    public static <T> void printArray(T[] array){
        System.out.println("-------------array begin-------------");
        for(int i=0; i<array.length; i++){
            System.out.println(array[i]);
        }
        System.out.println("-------------array end-------------");
    }

    public static Element innerProduct(Element[] a, Element[] b){
        if(a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length){
            return null;
        }

        Element res = pairing.getZr().newZeroElement();
        for(int i=0; i<a.length; i++){
            res.add(a[i].duplicate().mul(b[i]));
        }
        return res;
    }


    public static void printMatrix(int[][] m){

        if(m == null)
            return;
        for(int i=0; i<m.length; i++){
            for(int j=0; j<m[i].length; j++){
                System.out.print(m[i][j] + "\t");
            }
            System.out.println();
        }
    }


    public static Element[] computeOmega(int[][] matrix,Map<Integer, Integer> setI){

        int cols = matrix[0].length;
        int[][] Mi = new int[setI.size()][cols];

        int k=0;
        for(Map.Entry<Integer, Integer> entry : setI.entrySet()){
            System.arraycopy(matrix[entry.getKey()], 0, Mi[k], 0, cols);
            k++;
        }
        if (Mi.length==0||Mi[0].length==0){
//			System.out.println("Secret key can not satisfy the policy in the ciphertext!");
//			System.out.println("Decryption unsuccessfully!");
            return null;
        }
        Element[][] Mi_ele = new Element[Mi.length][Mi[0].length];
//		BigInteger order=pairing.getZr().getOrder();
        for (int i = 0; i < Mi_ele.length; i++) {
            for(int j = 0; j < Mi_ele[0].length; j++){
                Mi_ele[i][j] = pairing.getZr().newElement((int) Mi[i][j]).getImmutable();
            }
        }
        Element[][] Minv=Utils.inverse(Mi_ele);
        GaussianElimination solve =new GaussianElimination(Minv,Minv.length);
        Element[] solution = solve.equationSolve();
        if (solution == null) {
//			System.out.println("Secret key can not satisfy the policy in the ciphertext!");
//			System.out.println("Decryption unsuccessfully!");
            return null;
        }
        return solution;
    }


    public static Element[][] inverse(Element[][] M){

        if(M.length==0||M[0].length==0){
            System.out.println("The matrix is illegal!");
            return null;
        }
        Element[][] Minv= new Element[M[0].length][M.length];

        for (int i = 0; i < M.length; i++) {
            for(int j = 0; j < M[0].length; j++){
                Minv[j][i]=M[i][j];
            }
        }

        return Minv;
    }

}
