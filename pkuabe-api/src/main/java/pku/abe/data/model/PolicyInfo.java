package pku.abe.data.model;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import pku.abe.PolicyParser.Parser;
import pku.abe.data.model.BinaryTreeInfo.TreeNode;
import pku.abe.data.model.BinaryTreeInfo.NodeType;
/**
 * Created by vontroy on 7/10/16.
 */
public class PolicyInfo {
    private String policy;
    private int[][] matrix;
    private Map<Integer, String> rho;

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public int[][] getMatrix() {
        return matrix;
    }

    public void setMatrix(int[][] matrix) {
        this.matrix = matrix;
    }

    public Map<Integer, String> getRho() {
        return rho;
    }

    public void setRho(Map<Integer, String> rho) {
        this.rho = rho;
    }

    private String format(String policy) {
        policy = policy.trim();
        policy = policy.replaceAll("\\(", "( ");
        policy = policy.replaceAll("\\)", " )");
        return policy;
    }

    private void printMatrix(int[][] matrix){
        for(int i=0; i<matrix.length; i++){
            for(int j=0; j<matrix[i].length; j++){
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("---------------------------");
    }
    
    private void compute() {
        Map<String, LinkedList<LinkedList<Integer>>> map = new LinkedHashMap<String, LinkedList<LinkedList<Integer>>>();
        int maxLen = 0;
        policy = format(policy);
        BinaryTreeInfo.TreeNode root = new Parser().parse(policy);
        BinaryTreeInfo.updateParentPointer(root);

        int rows = 0;
        int c = 1;
        LinkedList<Integer> vector = new LinkedList<Integer>();
        vector.add(1);
        root.setVector(vector);

        LinkedList<TreeNode> queue = new LinkedList<TreeNode>();
        queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode p = queue.removeFirst();
            if (p.getType() == NodeType.AND) {
                int size = p.getVector().size();
                LinkedList<Integer> pv = new LinkedList<Integer>();
                if (size < c) {
                    pv.addAll(p.getVector());
                    for (int i = 0; i < c - size; i++) {
                        pv.add(0);
                    }
                } else {
                    pv.addAll(p.getVector());
                }

                TreeNode right = p.getRight();
                LinkedList<Integer> lv = new LinkedList<Integer>();
                lv.addAll(pv);
                lv.addLast(1);
                right.setVector(lv);
                queue.add(right);

                TreeNode left = p.getLeft();
                LinkedList<Integer> rv = new LinkedList<Integer>();
                for (int i = 0; i < c; i++) {
                    rv.add(0);
                }
                rv.addLast(-1);
                left.setVector(rv);
                queue.add(left);

                c += 1;
            } else if (p.getType() == NodeType.OR) {
                TreeNode left = p.getLeft();
                LinkedList<Integer> lv = new LinkedList<Integer>();
                lv.addAll(p.getVector());
                left.setVector(lv);
                queue.add(left);

                TreeNode right = p.getRight();
                LinkedList<Integer> rv = new LinkedList<Integer>();
                rv.addAll(p.getVector());
                right.setVector(rv);
                queue.add(right);
            } else {
                // leaf node
                rows += 1;
                int size = p.getVector().size();
                maxLen = size > maxLen ? size : maxLen;
                if (map.containsKey(p.getValue())) {
                    map.get(p.getValue()).add(p.getVector());
                } else {
                    LinkedList<LinkedList<Integer>> list = new LinkedList<LinkedList<Integer>>();
                    list.add(p.getVector());
                    map.put(p.getValue(), list);
                }
            }
        }

        for (Map.Entry<String, LinkedList<LinkedList<Integer>>> entry : map
                .entrySet()) {
            LinkedList<LinkedList<Integer>> v = entry.getValue();
            for (int i = 0; i < v.size(); i++) {
                int size = v.get(i).size();
                if (size < maxLen) {
                    for (int j = 0; j < maxLen - size; j++) {
                        v.get(i).add(0);
                    }
                }
            }
        }
        this.matrix = new int[rows][];
        this.rho = new LinkedHashMap<Integer, String>();
        int i = 0;
        for (Map.Entry<String, LinkedList<LinkedList<Integer>>> entry : map
                .entrySet()) {
            LinkedList<LinkedList<Integer>> v = entry.getValue();
            for (int j = 0; j < v.size(); j++) {
                this.rho.put(i, entry.getKey());
                this.matrix[i] = new int[maxLen];
                for(int k = 0; k < maxLen; k++){
                    this.matrix[i][k] = v.get(j).get(k);
                }
                i += 1;
            }
        }
        printMatrix(this.matrix);
    }

    public PolicyInfo( String policy ) {
        this.policy = policy;
        compute();
    }
}
