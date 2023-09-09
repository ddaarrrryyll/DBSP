package DBSP.project1.indexComponent;

import java.util.ArrayList;
import java.util.List;
import DBSP.project1.storageComponent.Address;
import DBSP.project1.storageComponent.Record;
import DBSP.project1.storageComponent.Database;

public class BPlusTree {

    static final int NODE_SIZE = 100; // TODO temporary value
    static Node rootNode;
    Node nodeToInsertTo;

    public BPlusTree() {
        rootNode = createNode();
    }

    public static Node getRoot() {
        return rootNode;
    }
}