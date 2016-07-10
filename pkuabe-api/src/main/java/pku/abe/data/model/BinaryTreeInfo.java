package pku.abe.data.model;

import java.util.LinkedList;
/**
 * Created by vontroy on 7/10/16.
 */
public class BinaryTreeInfo {
    public static class TreeNode {
        private NodeType type;
        private String value;
        private TreeNode parent;
        private TreeNode left;
        private TreeNode right;

        private LinkedList<Integer> vector;

        public NodeType getType() {
            return type;
        }

        public void setType(NodeType type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public TreeNode getParent() {
            return parent;
        }

        public void setParent(TreeNode parent) {
            this.parent = parent;
        }

        public TreeNode getLeft() {
            return left;
        }

        public void setLeft(TreeNode left) {
            this.left = left;
        }

        public TreeNode getRight() {
            return right;
        }

        public void setRight(TreeNode right) {
            this.right = right;
        }

        public LinkedList<Integer> getVector() {
            return vector;
        }

        public void setVector(LinkedList<Integer> vector) {
            this.vector = vector;
        }
    }

    public enum NodeType{ AND, OR, LEAF }

    public static void updateParentPointer( TreeNode root ) {
        TreeNode p = root;
        TreeNode left = root.getLeft();
        TreeNode right = root.getRight();
        if( left != null ) {
            left.setParent(p);
            updateParentPointer(left);
        }
        if( right != null ) {
            right.setParent(p);
            updateParentPointer(right);
        }
    }


}
