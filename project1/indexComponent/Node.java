package indexComponent;

import java.util.*;
import storageComponent.Address;
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
        newParent.keys.add(node.getKeyAt(0));
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
        LeafNode newNode = this.splitAndDistributeLeaf(key, addr);
        if (this.getParent() != null) {
            this.insertNewNodeToParent(newNode);
            if (this.getParent().keys.size() > NODE_SIZE) {
                this.getParent().splitInternalNode();
            }
        } else {
            this.createNode(newNode);
        }
    }

    public void splitInternalNode() {
        InternalNode newParent = this.splitAndDistributeInternal();
        if (this.getParent() != null) {
            insertChild(this.getParent(), newParent);
            newParent.setParent(this.getParent());
            insertKey(this.getParent().keys, newParent.getKeyAt(0));
            newParent.keys.remove(0);
            if (this.getParent().keys.size() > NODE_SIZE) {
                this.getParent().splitInternalNode();
            }
        } else {
            InternalNode newRoot = new InternalNode();
            BPTHelper.addNode();
            newRoot.keys = new ArrayList<Integer>();
            newRoot.keys.add(newParent.getKeyAt(0));
            newParent.keys.remove(0);

            newRoot.addChild(this);
            newRoot.addChild(newParent);
            this.setParent(newRoot);
            newParent.setParent(newRoot);
            BPlusTree.setRoot(newRoot);
        }
    }
    public LeafNode splitAndDistributeLeaf(int key, Address addr) {
        LeafNode newNode = new LeafNode();
        BPTHelper.addNode();
        ((LeafNode) this).records = new ArrayList<Address>();
        ((LeafNode) this).records.add(addr);
        ((LeafNode) this).map.put(key, ((LeafNode) this).records);

        int n = NODE_SIZE - minLeafNodeSize+1;
        int i = 0;
        int fromKey = 0;

        for (Map.Entry<Integer, ArrayList<Address>> entry : ((LeafNode) this).map.entrySet()) {
            if (i == n) {
                fromKey = entry.getKey();
                break;
            }
            i++;
        }

        SortedMap<Integer, ArrayList<Address>> lastnKeys = ((LeafNode) this).map.subMap(fromKey, true, ((LeafNode) this).map.lastKey(), true);
        
        newNode.map = new TreeMap<Integer, ArrayList<Address>>(lastnKeys);
        lastnKeys.clear();
        insertKey(this.keys, key);

        newNode.keys = new ArrayList<Integer>(this.keys.subList(n, this.keys.size()));
        this.keys.subList(n, this.keys.size()).clear()

        if (((LeafNode) this).getRightSibling() != null) {
            newNode.setRightSibling(((LeafNode) this).getRightSibling());
            ((LeafNode) this).getRightSibling().setLeftSibling(newNode);
        }
        ((LeafNode) this).setRightSibling(newNode);
        newNode.setLeftSibling(((LeafNode) this));
        return newNode;
    }

    public InternalNode splitAndDistributeInternal() {
        InternalNode currParent = (InternalNode) this;
        InternalNode newParent = new InternalNode();
        BPTHelper.addNode();
        newParent.keys = new ArrayList<Integer>();

        int keyToSplitAt = currParent.getKeyAt(minInternalNodeSize);
        for (int k = currParent.getKeySize(); k > 0; k--) {
            if (currParent.getKeyAt(k-1) < keyToSplitAt) {
                break;
            }
            int currKey = currParent.getKeyAt(k-1);
            Node currChild = currParent.getChild(k);

            newParent.children.add(0, currChild);
            newParent.keys.add(0, currKey);
            currChild.setParent(newParent);

            currParent.removeChild(currParent.getChild(k));
            currParent.keys.remove(k-1);
        }
        return newParent;
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
    public int getLastKey() {
        return this.keys.get(keys.size() - 1);
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
            BPlusTree.setRoot(node);
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
