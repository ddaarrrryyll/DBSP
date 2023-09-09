package DBSP.project1.indexComponent;

import java.util.ArrayList;
import java.util.TreeMap;
import DBSP.project1.storageComponent.Address;


public class LeafNode extends Node {

    protected TreeMap<Integer, ArrayList<Address>> map;
    protected ArrayList<Address> records;
    private LeafNode rightSibling;
    private LeafNode leftSibling;

    public LeafNode() {
        super();
        this.rightSibling = null;
        this.leftSibling = null;
        setLeaf(true);
    }

    // TODO CHECK THIS METHOD AND THE ONE BELOW
    public ArrayList<Address> findRecord(int key) {
        if (this.map.containsKey(key) || this.keys.contains(key)) {
            return map.get(key);
        }
        return null;
    }
    public ArrayList<Address> getAddressesForKey(int key) {
        return map.get(key);
    }

    public void addRecord(int key, Address add) {
        if (this.keys == null) {
            this.records = new ArrayList<Address>();
            this.records.add(add);
            this.map = new TreeMap<Integer, ArrayList<Address>>();
            this.map.put(key, records);
            this.keys = new ArrayList<Integer>();
            insertKey(this.keys, key);   
        } else if (this.map.containsKey(key) || this.keys.contains(key)) {
            ArrayList<Address> existingRecords = map.get(key);
            existingRecords.add(add);
            map.put(key, existingRecords);
        } else if (this.keys.size() < NODE_SIZE) {
            this.records = new ArrayList<Address>();
            this.records.add(add);

            this.map.put(key, records);
            insertKey(this.keys, key);
        } else {
            this.splitLeafNode(key, add);
        }

    }

    public Node findNodeByKey(int key, Node rootNode) {
        if (rootNode == null) {
            return null;
        }
        for (Node child : ((InternalNode) rootNode).getChildren()) {
            Node foundNode = findNodeByKey(key, child);
            if (foundNode != null) {
                return foundNode;
            }
        }
        return null;
    }

    public static void insertKey(ArrayList<Integer> keys, int key) {
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i) >= key) {
                keys.add(i, key);
                return;
            }
        }
    }

    public void clear() {
        keys.clear();
        records.clear();
    }

    public void insertByRedistribution(int key, ArrayList<Address> add) {
        map.put(key, add);
    }

    public void removeKeyFromMap(int key) {
        map.remove(key);
    }

    // GETTERS + SETTERS
     public LeafNode getRightSibling() {
        return this.rightSibling;
    }
    public void setRightSibling(LeafNode sibling) {
        this.rightSibling = sibling;
    }
    public LeafNode getLeftSibling() {
        return this.leftSibling;
    }
    public void setLeftSibling(LeafNode sibling) {
        this.leftSibling = sibling;
    }

    @Override
    public String toString() {
        return String.format("\n--------LEAF NODE CONTAINS: map %s records %s, rightSibling ------------\n", map.toString(),
                records, this.rightSibling);
    }

}