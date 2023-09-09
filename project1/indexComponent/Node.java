package DBSP.project1.indexComponent;

import java.util.*;
import DBSP.project1.storageComponent.Address;
import java.lang.Math;

public class Node {

    static final int NODE_SIZE = BPlusTree.NODE_SIZE;

    private int nodeSize = NODE_SIZE;
    private int minInternalNodeSize;
    private int minLeafNodeSize;
    private boolean isRoot;
    private boolean isLeaf;
    private InternalNode parent;
    protected ArrayList<Integer> keys;

    Node rootNode;

    public Node() {
        this.rootNode = BPlusTree.getRoot();
        this.isRoot = false;
        this.isLeaf = false;
        this.minInternalNodeSize = (int) Math.floor((nodeSize / 2.0));
        this.minLeafNodeSize = (int) Math.floor((nodeSize+1) / 2.0);
    }

    public void createNode(Node node) {
        InternalNode newParent = new InternalNode();
        BPTHelper.addNode();
        newParent.keys = new ArrayList<Integer>();
        newParent.addChild(this);
        newParent.addChild(node);
        newParent.keys.add(node.getKeyFromIndex(0));
        this.setParent(newParent);
        node.setParent(newParent);
    }

    // public void createRootNode(InternalNode node) {
    //     InternalNode newParent = new InternalNode();
    //     BPTHelper.addNode();
    //     newParent.keys = new ArrayList<Integer>();
    //     newParent.addChild(this);
    //     newParent.addChild(node);
    //     newParent.keys.add(node.getKeyFromIndex(0));
    //     this.setParent(newParent);
    //     node.setParent(newParent);
    // }

    public void insertNewNodeToParent(LeafNode newNode) {
        int index = 0;
        boolean insertedNode = false;
        InternalNode parent = this.getParent();
        try { 
            for (Node currNode : parent.getChildren()) {
                if (newNode.getKeyAt((newNode.keys.size() - 1)) < currNode.getKeyAt(0)) {
                    parent.getChildren().add(index, newNode);
                    parent.keys.add(index-1, newNode.getKeyAt(0));
                    insertedNode = true;
                    break;
                }
                index++;
            }
            if (insertedNode == false) {
                parent.getChildren().add(newNode);
                parent.keys.add(newNode.getKeyAt(0));
            }
        } catch (Exception e) {
            parent.getChildren().add(newNode);
            parent.keys.add(newNode.getKeyAt(0))
        }
        newNode.setParent(parent);
        if (parent.keys.size() > NODE_SIZE) {
            parent.splitInternalNode();
        }
    }

    public void splitLeafNode(int key, Address addr) {
        // TODO
    }

    public void splitInternalNode() {
        // TODO
    }
    public LeafNode splitAndDistributeLeaf() {
        // TODO
    }
    public InternalNode splitAndDistributeInternal() {
        // TODO
    }

    public int searchKeyIndex(int key, boolean upperBound) {
        int keyCount = keys.size();
        return searchKey(0, keyCount-1, key, upperBound);
    }

    private int searchKey(int left, int right, int key, boolean upperBound) {
        if (left > right) return left;
        int mid = (left + right) / 2;
        int midKey = getKeyAt(mid);
        if (midKey < key) {
            return searchKey(mid+1, right, key, upperBound);
        } else if (midKey > key) {
            return searchKey(left, mid-1, key, upperBound);
        } else {
            while (mid < this.keys.size() && this.keys.get(mid) == key) {
                mid++;
            } if (!upperBound) return mid-1;
            return mid;
        }
    }

    public boolean isDeficient(int maxKeyCount) {
        if (this.isRoot) {
            return this.keys.isEmpty();
        } else if (this.isLeaf) {
            return this.keys.size() < (maxKeyCount + 1) / 2;
        } else {
            return this.keys.size() < maxKeyCount / 2;
        }
    }

    public static void insertKey(ArrayList<Integer> keys, int key) {
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i) >= key) {
                keys.add(i, key);
                return;
            }
        }
    }

    public void insertChild(InternalNode parent, InternalNode child) {
        int i = 0;
        int childToSort = child.getKeyAt(0);
        while (i < parent.keys.size() & parent.getKeyAt(i) < childToSort) {
            i++;
        }
        parent.children.add(i+1, child);
    }

    public void updateKey(int keyIndex, int newKey, boolean leafNotUpdated, int lowerbound) {
        if (keyIndex >= 0 && keyIndex < keys.size() && !leafNotUpdated) {
            keys.set(keyIndex, newKey);
        }
        if (this.parent != null && this.parent.isInternalNode()) {
            int childIndex = this.parent.getChildren().indexOf(this);
            if (childIndex >= 0) {
                if (childIndex > 0) {
                    this.parent.replaceKeyAt(childIndex-1, keys.get(0));
                }
                this.parent.updateKey(childIndex-1, newKey, false, lowerbound);
            }
        } else if (this.parent != null && this.parent.isLeaf()) {
            this.parent.updateKey(keyIndex, newKey, false, lowerbound);
        }
    }

    public boolean isLendable(int maxKeyCount) {
        if (this.isInternalNode()) {
            return this.keys.size() - 1 >= maxKeyCount / 2;
        }
        return this.keys.size() - 1 >= (maxKeyCount + 1) / 2;
    }


    // GETTERS + SETTERS + MISC
    public ArrayList<Integer> getKeys() {
        return this.keys;
    }
    public int getKeySize() {
        return this.keys.size();
    }
    public int getKeyAt(int index) {
        return this.keys.get(index);
    }
    public void insertKeyAt(int index, int key) {
        keys.add(index, key);
    }
    public void replaceKeyAt(int index, int key) {
        keys.set(index, key);
    }
    public int removeKeyAt(int index) {
        return this.keys.remove(index);
    }
    public int getMinInternalNodeSize() {
        return this.minInternalNodeSize;
    }
    public int getminLeafNodeSize() {
        return this.minLeafNodeSize;
    }
    public InternalNode getParent() {
        return this.parent;
    }
    public boolean isRoot() {
        return this.isRoot;
    }
    public boolean isLeaf() {
        return this.isLeaf;
    }
    public boolean isInternalNode() {
        return !this.isLeaf;
    }
    public void setRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }
    public void setLeaf(boolean isLeaf) {
        this.isLeaf = isLeaf;
    }
    public void setParent(InternalNode node) {
        if (this.isRoot) {
            this.setRoot(false);
            node.setRoot(true);
            node.setLeaf(false);
            BPlusTree.setRoot = node;
        } else {
            node.setLeaf(false);
        }
        this.parent = node;
    }
    public void printNode() {
        Set<Integer> keys = ((LeafNode) this).map.keySet();
        System.out.println(keys);
    }
}
