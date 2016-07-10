package pku.abe.commons.util;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;

/**
 * Created by vontroy on 7/10/16.
 */
public class GaussianElimination {
    // get the default pairing
    private Pairing pairing = PairingManager.getDefaultPairing();

    // coefficient matrix
    private Element M[][];

    // vector
    private Element b[];

    // solution
    private Element omega[];

    // exchange times
    private int exchangeTimes;

//	// quare or not
//	private boolean isSquare;

    // error flag
    private boolean misMatchFlag=false;
    public GaussianElimination() {
        this.M = this.genRandomMatrix(2, 4);
        this.b = this.genb(2);
    }

    public GaussianElimination(Element[][] M, Element[] b) {
        this.M = M;
        this.b = b;
    }

    public GaussianElimination(Element[][] M, int n) {
        this.M = M;
        this.b = this.genb(n);
    }

    public void swap(int rowIndex) {

        Element maxEle;
        int tempRowIndex = rowIndex;
        int colPos;

        // set the colPos and maxEle
        if (rowIndex < M[0].length && rowIndex < M.length) {
            maxEle = M[rowIndex][rowIndex];
            colPos = rowIndex;
        } else {
            maxEle = M[rowIndex][M[0].length - 1];
            colPos = M[0].length - 1;
        }

        // Searching for the row need to be exchanged
        for (int i = rowIndex + 1; i < M.length; i++) {
            if (M[i][colPos].toBigInteger().compareTo(maxEle.toBigInteger()) > 0) {
                tempRowIndex = i;
                maxEle = M[i][colPos].duplicate();
            }
        }

        if (rowIndex != tempRowIndex) {
            exchangeTimes++;
            System.out.println("\nwhen rowIndex=" + rowIndex + ", swap:"
                    + rowIndex + "and " + tempRowIndex);
            // exchane two rows in M
            Element[] tempRow = M[rowIndex];
            M[rowIndex] = M[tempRowIndex];
            M[tempRowIndex] = tempRow;

            // exchange the corresponding coordinates in b
            Element tempbEle;
            tempbEle = b[rowIndex];
            b[rowIndex] = b[tempRowIndex];
            b[tempRowIndex] = tempbEle;

            // print matrix M
            System.out.println("After swap锟�?");
            PrintM();
        }
    }

    public void elimination() {

        Element zero = pairing.getZr().newZeroElement().getImmutable();
        for (int k = 0; k < M.length; k++) {
            swap(k);
            for (int i = k + 1; i < M.length ; i++) {
                // times
                if(k<M[0].length){
                    Element tempEle = M[i][k].div(M[k][k]).getImmutable();
                    M[i][k] = pairing.getZr().newZeroElement().getImmutable();
                    for (int j = k + 1; j < M[0].length; j++)
                        M[i][j] = M[i][j].add((tempEle.mul(M[k][j])).negate())
                                .getImmutable();
                    b[i] = b[i].add((tempEle.mul(b[k])).negate()).getImmutable();
                }
                else if(!b[k].equals(zero)){
                    misMatchFlag=true;
                }
            }
            System.out.println("\nAfter " + k + "th elimination:");
            PrintM();
        }
    }

    public int isEmpty(Element[] row) {

        Element zero = pairing.getZr().newZeroElement().getImmutable();
        int count = 0;
        for (int j = 0; j < M[0].length; j++) {
            if (!(row[j].isEqual(zero))) {
                count++;
            }
        }

        return count;
    }

    public boolean backSubstitution() {
        omega = new Element[M[0].length];
        Element zero = pairing.getZr().newZeroElement().getImmutable();
        int[] count = new int[M.length];
        for (int i = M.length - 1; i >= 0; i--) {
            count[i] = isEmpty(M[i]);
            if (!b[i].isEqual(zero) && count[i] == 0||misMatchFlag) {
//				System.out.print("You can't decrypt this file!");
                return false;
            }
        }
        // example
        // 1 1 1 7
        // 0 2 3 1
        if (count[M.length - 1] > 1) {
            int difference = M[0].length - M.length;
            // set free variable zero
            for (int i = 0; i < difference; i++) {
                omega[M[0].length - 1 - i] = zero.duplicate();
            }
            for (int i = M.length - 1, j = M[0].length - 1 - difference; i >= 0
                    && j >= 0; i--, j--)
                omega[i] = (b[i].add(computePartialSum(i).negate())).div(
                        M[i][j]).getImmutable();
            return true;
        }
        // example
        // 1 1 1
        // 0 2 3
        // 0 0 0
        // 0 0 0
        else {
            int rowIndex = -1;
            for (int i = (M.length-1); i >= 0; i--) {
                if (count[i] != 0) {
                    rowIndex = i;
                    break;
                }
            }
            if (rowIndex!=(M.length-1)) {

                if(!(M[rowIndex][M[0].length - 1].equals(zero))){
                    omega[rowIndex] = b[rowIndex].div(
                            M[rowIndex][M[0].length - 1]).getImmutable();
                }
                else {
                    omega[rowIndex]=zero.duplicate();
                }
                for (int i = rowIndex-1, j = M[0].length - 2; i >= 0
                        && j >= 0; i--, j--)
                    if (!M[i][j].equals(zero))
                        omega[i] = (b[i].add(computePartialSum(i).negate()))
                                .div(M[i][j]).getImmutable();
                    else {
                        omega[i] = zero.duplicate();
                    }
            } else {
                if(!(M[M.length - 1][M[0].length - 1].equals(zero))){
                    omega[M[0].length - 1] = b[M.length - 1].div(
                            M[M.length - 1][M[0].length - 1]).getImmutable();
                }
                else {
                    omega[M[0].length - 1] = zero.duplicate();
                }
                for (int i = M.length - 2, j = M[0].length - 2; i >= 0
                        && j >= 0; i--, j--)
                    if (!M[i][j].equals(zero))
                        omega[i] = (b[i].add(computePartialSum(i).negate()))
                                .div(M[i][j]).getImmutable();
                    else {
                        omega[i] = zero.duplicate();
                    }
            }

            return true;
        }

    }

    public Element computePartialSum(int i) {
        Element ele = pairing.getZr().newZeroElement().getImmutable();
        for (int j = M[0].length - 1; j > i; j--)
            ele = ele.add(M[i][j].mul(omega[j])).getImmutable();
        return ele;
    }

    // judge the solution to be illegal or legal
    public boolean verification() {

        for (int i = 0; i < M.length; i++) {
            Element sum = pairing.getZr().newZeroElement().getImmutable();
            for (int j = 0; j < M[0].length; j++) {
                sum = sum.add(M[i][j].mul(omega[j])).getImmutable();
            }
            if (!sum.isEqual(b[i])) {
                return false;
            }
        }
        return true;
    }

    // print the augmented matrix
    public void PrintM() {
        System.out.println("Augmented Matrix: ");
        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < M[0].length; j++) {
                System.out.print(M[i][j].toString() + " ");
            }
            System.out.print(b[i].toString() + " ");
            System.out.print("\n");
        }
    }

    // print the solution
    public void Print() {
        System.out.println("solution:");
        for (int i = 0; i < M[0].length; i++)
            System.out.println("omega" + i + " = " + omega[i]);
    }

    // generate the random coefficient matrix according to the m and n
    public Element[][] genRandomMatrix(int m, int n) {

        Element[][] M = new Element[m][n];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++) {

                M[i][j] = pairing.getZr().newRandomElement().getImmutable();
            }
        return M;
    }

    // generate the random vector b according to m
    public Element[] genb(int n) {

        Element[] b = new Element[n];
        b[0] = pairing.getZr().newOneElement().getImmutable();
        for (int i = 1; i < n; i++) {
            b[i] = pairing.getZr().newZeroElement().getImmutable();
        }
        return b;
    }

    // perform the solve action
    public Element[] equationSolve() {
        //this.PrintM();
        this.elimination();
        boolean flag = this.backSubstitution();
        if (flag) {
            //this.Print();
//			System.out.println(this.verification());
            return omega;
        } else {
            return null;
        }
    }
}
