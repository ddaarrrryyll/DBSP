package indexComponent;

import java.util.ArrayList;
import java.util.List;
import storageComponent.Address;
import storageComponent.Record;
import storageComponent.Database;
import utils.Parser;


public class BPlusTree {

    static final int NODE_SIZE = (Parser.BLOCK_SIZE-Parser.POINTER_SIZE)/(Parser.POINTER_SIZE+Parser.KEY_SIZE);
    static Node rootNode;
    Node nodeToInsertTo;

    public BPlusTree() {
        rootNode = createFirstNode();
    }

    public LeafNode createFirstNode() {
        LeafNode newNode = new LeafNode();
        newNode.setRoot(true);
        newNode.setLeaf(true);
        setRoot(newNode);
        return newNode;
    }

    public static Node createNode() {
        Node newNode = new Node();
        return newNode;
    }

    // insert key: addr to a suitable node
    public void insertKeyAddrPair(Float key, Address add) {
        nodeToInsertTo = searchNodeContaining(key);
        ((LeafNode) nodeToInsertTo).addRecord(key, add);
    }
    
    // find the leaf node to find/insert the key to
    public LeafNode searchNodeContaining(Float key) {
        ArrayList<Float> keys;

        // basse case is rootNode as Leaf
        if (BPlusTree.rootNode.isLeaf()) {
            setRoot(rootNode);
            return (LeafNode) rootNode;
        } else {
            Node nodeToInsertTo = (InternalNode) getRoot();

            // keep traversing until own child is a leaf node
            while (!((InternalNode) nodeToInsertTo).getChild(0).isLeaf()) {
                keys = nodeToInsertTo.getKeys();
                for (int i = keys.size() - 1; i >= 0; i--) {

                    // node is suitable if there is a key <= key to insert
                    if (nodeToInsertTo.getKeyAtIdx(i) <= key) {
                        nodeToInsertTo = ((InternalNode) nodeToInsertTo).getChild(i + 1);
                        break;
                    } else if (i == 0) { // key to insert is smaller than smallest key in curr node
                        nodeToInsertTo = ((InternalNode) nodeToInsertTo).getChild(0);
                    }
                }
                if (nodeToInsertTo.isLeaf()) {
                    break;
                }
            }

            keys = nodeToInsertTo.getKeys();
            // find the child node to insert to
            for (int i = keys.size() - 1; i >= 0; i--) {
                if (keys.get(i) <= key) {
                    return (LeafNode) ((InternalNode) nodeToInsertTo).getChild(i + 1);
                }
            }
            // if key to insert is smaller than smallest key in node
            return (LeafNode) ((InternalNode) nodeToInsertTo).getChild(0);
        }
    }

    // delete node and return addresses to be removed
    public ArrayList<Address> deleteKey(Float key) {
        Float lowerbound = checkForLowerbound(key);
        return (deleteNode(rootNode, null, -1, -1, key, lowerbound));
    }

    // handles an invalid node type (wrapper function)
    private void handleInvalidTree(Node underUtilizedNode, InternalNode parent, int parentPointerIndex,
            int parentKeyIndex) throws IllegalStateException {
        if (parent == null) {
            handleInvalidRootNode(underUtilizedNode);
        } else if (underUtilizedNode.isLeaf()) {
            // Rebalancing of Leaf node
            // System.out.println("LEAF UNU");
            handleInvalidLeafNode(underUtilizedNode, parent,
                    parentPointerIndex, parentKeyIndex);
        } else if (!underUtilizedNode.isLeaf()) {
            // Rebalancing of Non-leaf node
            // System.out.println("INODE UNU");
            handleInvalidInternalNode(underUtilizedNode, parent,
                    parentPointerIndex, parentKeyIndex);
        } else {
            throw new IllegalStateException("state is wrong!");
        }
    }

    // GETTERS AND SETTERS + misc / helpers
    public static void setRoot(Node root) {
        rootNode = root;
        rootNode.setRoot(true);
    }

    public static Node getRoot() {
        return rootNode;
    }


    private Float checkForLowerbound(Float key) {

        InternalNode node = (InternalNode) rootNode;
        Node targetNode = node;
        boolean found = false;
        // find the largest key in node smaller than key
        for (int i = node.getKeyCount() - 1; i >= 0; i--) {
            if (key >= node.getKeyAt(i)) {
                targetNode = node.getChild(i + 1);
                found = true;
                break;
            }
        };
        if (!found && key < node.getKeyAt(0)) {
            targetNode = ((InternalNode) node).getChild(0);
        }

        // loop till get leftmost key
        while (!node.getChild(0).isLeaf()) {
            targetNode = ((InternalNode) node).getChild(0);
        }

        if (targetNode.isLeaf()) {
            return targetNode.getFirstKey();
        } else {
            return ((InternalNode) targetNode).getChild(0).getKeyAt(0);
        }
    }

    // ex5 boolean ensures we delete everything less than key also
    public ArrayList<Address> deleteNode(Node node, InternalNode parent, int parentPointerIndex, int parentKeyIndex,
            Float key, Float lowerbound) {
        // System.out.printf("GET FIRST KEY %.3f\n", node.getFirstKey());
        ArrayList<Address> addressesToDel = new ArrayList<>();
        // hits the target node location
        if (node.isLeaf()) {
            LeafNode leafNode = (LeafNode) node;
                int keyIdx = node.getIdxOfKey(key, false);
                if ((keyIdx == leafNode.getKeyCount()) || (!key.equals(leafNode.getKeyAt(keyIdx)))) {
                    return null;
                }

                addressesToDel.addAll(leafNode.getAddressesForKey(key));
                leafNode.removeKeyAt(keyIdx);
                leafNode.removeKeyInMap(key);

                int ptrIdx = node.getIdxOfKey(key, true);
                keyIdx = ptrIdx - 1;
                // update tree recursively
                if (leafNode.getKeyCount() >= (keyIdx + 1)) {
                    Float newLowerBound = lowerbound;
                    List<Float> keys = leafNode.getKeys();
                    // System.out.printf("ptrIdx: %d, nlb: %.3f\n", ptrIdx, newLowerBound);
                    // System.out.println(keys);
                    leafNode.updateKeyAt(ptrIdx - 1, keys.get(0), false, newLowerBound);
                } else {
                    Float newLowerBound = checkForLowerbound(leafNode.getKeyAtIdx(keyIdx + 1));
                    List<Float> keys = leafNode.getKeys();
                    leafNode.updateKeyAt(ptrIdx - 1, keys.get(0), true, newLowerBound);
                }
        } else {
            // traverse to leaf node to find records to delete
            InternalNode nonLeafNode = (InternalNode) node;
            int ptrIdx = node.getIdxOfKey(key, true);
            int keyIdx = ptrIdx - 1;
            // read the next level node
            Node next = nonLeafNode.getChild(ptrIdx);
            addressesToDel = deleteNode(next, nonLeafNode, ptrIdx, keyIdx, key, lowerbound);
        }

        if (node.isUnderUtilized(NODE_SIZE)) {
            // System.out.println("NODE UNU");
            handleInvalidTree(node, parent, parentPointerIndex, parentKeyIndex);
        }
        

        return addressesToDel;
    }

    
    public void handleInvalidRootNode(Node underUtilizedNode) {
        if (underUtilizedNode.isLeaf()) {
            ((LeafNode) underUtilizedNode).clear();
        } else {
            InternalNode nonLeafRoot = (InternalNode) underUtilizedNode;
            Node newRoot = nonLeafRoot.getChild(0);
            newRoot.setParent(null);
            this.rootNode = newRoot;
        }
    }

    // helper function to handle invalid leaf node
    private void handleInvalidLeafNode(Node underUtilizedNode,
            InternalNode parent,
            int parentPointerIndex,
            int parentKeyIndex) {
        LeafNode underUtilizedLeaf = (LeafNode) underUtilizedNode;
        // get left sibling
        LeafNode leftSibling = (LeafNode) underUtilizedLeaf.getLeftSibling();
        // get right sibling
        LeafNode rightSibling = (LeafNode) underUtilizedLeaf.getRightSibling(); 
        // move one key from sibling to target
        if (leftSibling != null && leftSibling.canDonate(NODE_SIZE)) {
            moveOneKeyLeafNode(leftSibling, underUtilizedLeaf, true, parent, parentKeyIndex);
        } else if (rightSibling != null && rightSibling.canDonate(NODE_SIZE)) {
            moveOneKeyLeafNode(rightSibling, underUtilizedLeaf, false, parent, parentKeyIndex + 1);

        // cant' donate, need to merge, check if left/right + self size <= NODE_SIZE, then see if we parent can handle a decrement of children
        } else if (leftSibling != null && (leftSibling.getKeyCount() + underUtilizedLeaf.getKeyCount()) <= NODE_SIZE) {
            mergeLeafNodes(leftSibling, underUtilizedLeaf, parent, parentPointerIndex, parentKeyIndex, false);
        } else if (rightSibling != null && (rightSibling.getKeyCount() + underUtilizedLeaf.getKeyCount()) <= NODE_SIZE) {
            mergeLeafNodes(underUtilizedLeaf, rightSibling, parent, parentPointerIndex + 1, parentKeyIndex + 1, true);
        }
    }

    private void handleInvalidInternalNode(Node underUtilizedNode,
            InternalNode parent,
            int parentPointerIndex,
            int parentKeyIndex) {

        Node underUtilizedInternalNode = underUtilizedNode;

        InternalNode leftInNodeSibling = null;
        InternalNode rightInNodeSibling = null;
        try {
            rightInNodeSibling = (InternalNode) parent.getChild(parentPointerIndex + 1);
        } catch (Exception e) {
            System.out.print(e);
        }

        try {
            leftInNodeSibling = (InternalNode) parent.getChild(parentPointerIndex - 1);
        } catch (Exception e) {
            System.out.print(e);
        }

        // if node has no siblings and is underutilized, something went wrong
        if (rightInNodeSibling == null && leftInNodeSibling == null)
            throw new IllegalStateException("Both leftInNodeSibling and rightInNodeSibling is null for " + underUtilizedNode);

        // move one key from sibling to target node, prioritise left        
        if (leftInNodeSibling != null && leftInNodeSibling.canDonate(NODE_SIZE)) {
            moveOneKeyInternalNode(leftInNodeSibling, (InternalNode) underUtilizedInternalNode, true, parent, parentKeyIndex);
        } else if (rightInNodeSibling != null && rightInNodeSibling.canDonate(NODE_SIZE)) {
            moveOneKeyInternalNode(rightInNodeSibling, (InternalNode) underUtilizedInternalNode, false, parent, parentKeyIndex + 1);
        
            // if cant donate we check if can merge
        } else if (leftInNodeSibling != null && (underUtilizedInternalNode.getKeyCount() + leftInNodeSibling.getKeyCount()) <= NODE_SIZE) {
            mergeInternalNodes(leftInNodeSibling, (InternalNode) underUtilizedInternalNode, parent, parentPointerIndex, parentKeyIndex, true);
        } else if (rightInNodeSibling != null && (underUtilizedInternalNode.getKeyCount() + rightInNodeSibling.getKeyCount()) <= NODE_SIZE) {
            mergeInternalNodes((InternalNode) underUtilizedInternalNode, rightInNodeSibling, parent, parentPointerIndex + 1, parentKeyIndex + 1, false);
        }
    }

    private void moveOneKeyInternalNode(InternalNode donor, InternalNode receiver,
            boolean donorOnLeft, InternalNode parent,
            int inBetweenKeyIdx) {
        Float key;

        if (donorOnLeft) {
            // move last key & child from donor to target node (receiver)
            donor.removeKeyAt(donor.getKeyCount() - 1);
            Node nodeToMove = donor.getChild(donor.getKeyCount());
            donor.removeChild(nodeToMove);
            receiver.addChild(nodeToMove);

            receiver.getKeys().add(receiver.getKeyCount(), receiver.getChild(1).getFirstKey());
            key = receiver.getKeyAt(0);
        } else {
            // move first key from right donor to target node
            donor.removeKeyAt(0);
            Node nodeToMove = donor.getChild(0);
            donor.removeChild(nodeToMove);
            receiver.addChild(nodeToMove);

            receiver.getKeys().add(receiver.getKeyCount(), receiver.getChild(1).getFirstKey());
            key = receiver.getKeyAt(0);
        }

        int ptrIdx = receiver.getIdxOfKey(key, true);
        int keyIdx = ptrIdx - 1;

        // InternalNode LeafNode = (InternalNode) receiver;
        Float lowerbound = checkForLowerbound(key);
        Float newLowerBound;
        // if receiver previously already have keys
        if (receiver.getKeyCount() >= (keyIdx + 1)) {
            newLowerBound = lowerbound;
        } else {
            newLowerBound = checkForLowerbound(receiver.getKeyAtIdx(keyIdx + 1));
            parent.updateKeyAt(inBetweenKeyIdx - 1, key, false, checkForLowerbound(key));
        }
        parent.replaceKeyAt(inBetweenKeyIdx, newLowerBound);

    }

    // TODO MAYBE WRONG NEED TO CHECK AGAIN
    private void mergeInternalNodes(InternalNode targetNode, InternalNode sacrificialNode, InternalNode parent,
            int rightPointerIdx,
            int inBetweenKeyIdx, boolean targetNodeInsufficient) {
        Float keyToRemove;

        if (targetNodeInsufficient) {
            int moveKeyCount = sacrificialNode.getKeyCount();
            keyToRemove = targetNode.getChild(targetNode.getKeyCount()).getLastKey();

            // move keys and children from siblingNode into back of targetnode
            for (int i = 0; i < moveKeyCount; i++) {
                targetNode.getKeys().add(targetNode.getKeyCount(), sacrificialNode.getKeyAt(i));
            }
            for (int i = 0; i < sacrificialNode.getChildren().size(); i++) {
                targetNode.getChildren().add(sacrificialNode.getChild(i));
            }
            targetNode.getKeys().add(targetNode.getKeyCount(), targetNode.getChild(targetNode.getKeyCount() + 1).getFirstKey());

            // update parent
            sacrificialNode.getParent().removeChild(sacrificialNode);
        } else {
            int moveKeyCount = sacrificialNode.getKeyCount();
            keyToRemove = sacrificialNode.getFirstKey();

            // move keys and children from siblingNode into front of targetnode
            for (int i = 0; i < moveKeyCount; i++) {
                targetNode.getKeys().add(0, sacrificialNode.getKeyAt(i));
            }
            for (int i = 0; i < sacrificialNode.getChildren().size(); i++) {
                targetNode.getChildren().add(sacrificialNode.getChild(i));
            }
            targetNode.getKeys().add(0, targetNode.getChild(1).getFirstKey());
        
            // update parent
            sacrificialNode.getParent().removeChild(sacrificialNode);

        }

        int ptrIdx = targetNode.getIdxOfKey(keyToRemove, true);
        int keyIdx = ptrIdx - 1;

        // InternalNode LeafNode = (InternalNode) targetNode;
        Float lowerbound = checkForLowerbound(keyToRemove);
        Float newLowerBound;

        if (targetNode.getKeyCount() >= (keyIdx + 1)) {
            newLowerBound = lowerbound;
        } else {
            newLowerBound = checkForLowerbound(targetNode.getKeyAtIdx(keyIdx + 1)); // Get new lowerbound
            parent.updateKeyAt(inBetweenKeyIdx - 1, keyToRemove, false, checkForLowerbound(keyToRemove));
        }
        // parent.replaceKeyAt(inBetweenKeyIdx, newLowerBound);
    }


    // TODO CHECK AGAIN
    private void mergeLeafNodes(LeafNode targetNode, LeafNode sacrificialNode, InternalNode parent,
            int rightPointerIdx, int inBetweenKeyIdx, boolean targetNodeInsufficient) {
        Float removedKey = 0.0f;
        int moveKeyCount = sacrificialNode.getKeyCount();
        int childrenCount = sacrificialNode.getParent().getChildren().size();
        
        // move all keys from sacrificialNode to target
        for (int i = 0; i < moveKeyCount; i++) {
            removedKey = sacrificialNode.removeKeyAt(0);
            int targetNodeLastKeyPos = targetNode.getLastIdx();
            targetNode.insertKeyAt(targetNodeLastKeyPos + 1, removedKey);
            targetNode.insertKeyAddrArrPair(removedKey, sacrificialNode.getAddressesForKey(removedKey));
            sacrificialNode.removeKeyInMap(removedKey);
        }

        // remove sacrificialNode from parent
        parent.removeChild(sacrificialNode);
        // need to update parent if there is a mismatch in child count and key count
        if ((parent.getChildren().size()) != (parent.getKeyCount())) parent.removeKeyAt(inBetweenKeyIdx);

        if (targetNodeInsufficient) {
            // update siblings
            if (sacrificialNode.getRightSibling() != null) {
                LeafNode sacrificialNodeRightSibling = sacrificialNode.getRightSibling();
                sacrificialNodeRightSibling.setLeftSibling(sacrificialNode.getLeftSibling());
            }
            targetNode.setRightSibling(sacrificialNode.getRightSibling());

            // update the parent of sacrificialNode
            if (sacrificialNode.getKeyCount() == 0) {
                InternalNode sacNodeParent = sacrificialNode.getParent();
                sacNodeParent.removeChild(sacrificialNode);
                sacNodeParent.removeKeyAt(0);
            }
        } else {
            // update the pointers of both sacNode's siblings
            if (sacrificialNode.getLeftSibling() != null) {
                LeafNode sacNodeLeftSibling = sacrificialNode.getLeftSibling();
                if (sacrificialNode.getRightSibling() != null) {
                    LeafNode sacNodeRightSibling = sacrificialNode.getRightSibling();
                    sacNodeLeftSibling.setRightSibling(sacNodeRightSibling);
                } else sacNodeLeftSibling.setRightSibling(null);
            }

            if (sacrificialNode.getRightSibling() != null) {
                LeafNode sacNodeRightSibling = sacrificialNode.getRightSibling();
                if (sacrificialNode.getLeftSibling() != null) {
                    LeafNode sacNodeLeftSibling = sacrificialNode.getLeftSibling();
                    sacNodeRightSibling.setLeftSibling(sacNodeLeftSibling);
                } else sacNodeRightSibling.setLeftSibling(null);
            }

            if (sacrificialNode.getKeyCount() == 0) {
                InternalNode sacNodeParent = sacrificialNode.getParent();
                sacNodeParent.removeChild(sacrificialNode);
                if (inBetweenKeyIdx < 0) {
                    sacNodeParent.removeKeyAt(inBetweenKeyIdx + 1);
                } else if (sacNodeParent.getKeyCount() > 0) {
                    sacNodeParent.removeKeyAt(inBetweenKeyIdx);
                } else {
                    sacNodeParent.removeKeyAt(0);
                }
            } else {
                InternalNode sacNodeParent = sacrificialNode.getRightSibling().getParent();
                sacNodeParent.removeChild(sacrificialNode);
                // Check if parent key satisfy min node size
                if ((sacNodeParent.getKeyCount() > sacNodeParent.getMinInternalNodeSize())
                        && (sacNodeParent.getChildren().size() > sacrificialNode.getMinInternalNodeSize())) {
                    sacNodeParent.removeKeyAt(0);

                }
            }
        }

        Float lowerbound = checkForLowerbound(removedKey);
        Float newLowerBound;

        if (sacrificialNode.getParent().getKeyCount() >= childrenCount) {
            newLowerBound = lowerbound;
        } else {
            newLowerBound = sacrificialNode.getParent().getChild(0).getFirstKey();

            if (inBetweenKeyIdx != 0) {
                sacrificialNode.getParent().updateKeyAt(inBetweenKeyIdx - 1, newLowerBound, true, newLowerBound);
            }
        }
    }

    private void moveOneKeyLeafNode(LeafNode donor, LeafNode receiver,
            boolean donorOnLeft, InternalNode parent,
            int inBetweenKeyIdx) {
        Float key;
        if (donorOnLeft) {
            // move the key from left node to right
            Float donorKey = donor.getLastKey();
            receiver.insertKeyAddrArrPair(donorKey, donor.getAddressesForKey(donorKey));
            donor.removeKeyInMap(donorKey);

            receiver.insertKeyAt(0, donorKey);
            donor.removeLastKey();
            key = receiver.getKeyAt(0);
        } else {
            // move key from right node to left node
            Float donorKey = donor.getFirstKey();
            receiver.insertKeyAddrArrPair(donorKey, donor.getAddressesForKey(donorKey));
            donor.removeKeyInMap(donorKey);

            receiver.insertKeyAt(receiver.getKeyCount(), donorKey);
            donor.removeKeyAt(0);
            key = donor.getKeyAt(0);
        }

        // update receiver's parent
        if (inBetweenKeyIdx == -1) {
            // pass
        } else if (inBetweenKeyIdx >= 0) {
            if (parent.getKeyCount() == inBetweenKeyIdx) {
                parent.replaceKeyAt(inBetweenKeyIdx - 1, key);

                int lastParentChild = receiver.getParent().getKeys().size() - 1;
                Float lastParentChildKey = receiver.getParent().getChild(receiver.getParent().getKeys().size()).getFirstKey();
                if (!(donor.getParent().getChild(donor.getParent().getChildren().size() - 1).getFirstKey()).equals(key)) {
                    receiver.getParent().replaceKeyAt(lastParentChild, lastParentChildKey);
                }
            } else {
                parent.replaceKeyAt(inBetweenKeyIdx, key);

                // if donor is from the same parent
                if (!(donor.getParent().getChild(inBetweenKeyIdx + 1).getFirstKey()).equals(key)) {
                    donor.getParent().replaceKeyAt(inBetweenKeyIdx,
                            donor.getParent().getChild(inBetweenKeyIdx + 1).getFirstKey());
                }
            }
        } else {
            parent.replaceKeyAt(inBetweenKeyIdx - 1, key);
        }

        int ptrIdx = receiver.getIdxOfKey(key, true);
        int keyIdx = ptrIdx - 1;

        LeafNode LeafNode = (LeafNode) receiver;
        Float lowerbound = checkForLowerbound(key);
        Float newLowerBound = 0.0f;

        if (LeafNode.getKeyCount() >= (keyIdx + 1)) {
            newLowerBound = lowerbound;
        } else {
            newLowerBound = checkForLowerbound(LeafNode.getKeyAtIdx(keyIdx + 1));
            parent.updateKeyAt(inBetweenKeyIdx - 1, parent.getChild(inBetweenKeyIdx).getFirstKey(), false,
                    checkForLowerbound(key));
        }

    }

    public ArrayList<Address> getAddresses(Float key) {
        return (searchValue(this.rootNode, key));
    }

    public ArrayList<Address> searchValue(Node node, Float key) {
        BPTHelper.addNodeReads();
        if (node.isLeaf()) {
            int ptrIdx = node.getIdxOfKey(key, false);
            if (ptrIdx >= 0 && ptrIdx < node.getKeyCount() && key.equals(node.getKeyAt(ptrIdx))) {
                return ((LeafNode) node).getAddressesForKey(key);
            }
            return null;
        }
        else {
            int ptrIdx = node.getIdxOfKey(key, false);
            Node childNode = ((InternalNode) node).getChild(ptrIdx);
            return (searchValue(childNode, key));
        }
    }

    public int countNodes(Node node) {
        // start with root
        int count = 1;
        if (node.isLeaf()) {
            return count;
        }
        for (Node child : ((InternalNode) node).getChildren()) {
            count += countNodes(child);
        }
        return count;
    }

    // -------------------------EXPERIMENT 2-------------------------

    public static void ex2(BPlusTree bPlusTree) {
        System.out.println("\nEXPERIMENT 2: Build a B+Tree on FG_PCT_home by inserting the records sequentially:");
        System.out.println("Parameter n: " + NODE_SIZE);
        System.out.printf("No. of Nodes in B+ tree: %d\n", bPlusTree.countNodes(bPlusTree.getRoot()));
        System.out.printf("No. of Levels in B+ tree: %d\n", bPlusTree.getDepth(bPlusTree.getRoot()));
        System.out.println("Content of the root node: " + bPlusTree.getRoot().keys);

        // sanity check
        // Node temp = bPlusTree.getRoot();
        // while (!temp.keys.isEmpty()) {
        //     System.out.println(temp.keys);
        //     temp = ((InternalNode) temp).getChild(0);
        // }
    }

    private int getDepth(Node node) {
        int level = 0;
        while (!node.isLeaf()) {
            node = ((InternalNode) node).getChild(0);
            level++;
        }
        level++;
        return level;
    }

    // -------------------------EXPERIMENT 3-------------------------

    public static void ex3(Database db, BPlusTree bPlusTree) {
        System.out.println("\nEXPERIMENT 3: Retrieve those records with the \"FG_PCT_home\" equal to 0.5:");
        BPTHelper performance = new BPTHelper();
        long startTime = System.nanoTime();
        ArrayList<Address> addresses = bPlusTree.getAddresses(0.500f);
        long endTime = System.nanoTime();
        float totalFG3_PCT_home = 0;
        int count = 0;
        ArrayList<Record> res = new ArrayList<>();
        if (addresses != null) {
            for (Address address : addresses) {
                Record rec = db.getRecord(address);
                res.add(rec);
                totalFG3_PCT_home += rec.getFg3PctHome();
                count++;
            }
        }

        System.out.printf("No. of index nodes accessed by process: %d", performance.getNodeReads());
        System.out.printf("\nNo. of data blocks accessed by process: %d", db.getBlockAccesses());
        System.out.printf("\n(Index Search) No. of records found: %d", count);
        System.out.printf("\nAverage of FG3_PCT_home of returned records: %.2f", count > 0 ? totalFG3_PCT_home/count : 0);
        // running time = endTime - startTime (in nanoseconds)
        System.out.printf("\n\tRunning time: %.3f ms", (endTime - startTime) / 1_000_000.0);
        // point 5: brute-force searching
        startTime = System.nanoTime();
        int blkAccesses = db.bruteForceSearch(0.5f, 0.5f);
        endTime = System.nanoTime();
        System.out.printf("\nNo. of data blocks accessed by bruteforce: %d", blkAccesses);
        System.out.printf("\n\tRunning Time: %.3f ms", (endTime - startTime) / 1_000_000.0);
    }


    // -------------------------EXPERIMENT 4-------------------------

    public static void ex4(Database db, BPlusTree bPlusTree) {
        System.out.println("\n\nEXPERIMENT 4: Retrieve those records with 0.6 <= \"FG_PCT_home\" <= 1:");
        BPTHelper performance = new BPTHelper();
        long startTime = System.nanoTime();
        ArrayList<Address> addresses = bPlusTree.getAddressesForKeysBetween(bPlusTree.getRoot(), 0.6f, 1f);
        long endTime = System.nanoTime();
        double totalFG3_PCT_home = 0;
        int count = 0;
        ArrayList<Record> res = new ArrayList<>();
        if (addresses != null) {
            for (Address address : addresses) {
                Record rec = db.getRecord(address);
                res.add(rec);
                totalFG3_PCT_home += rec.getFg3PctHome();
                count++;
            }
        }
        System.out.printf("No. of index nodes accessed by process: %d", performance.getNodeReadsEx4());
        System.out.printf("\nNo. of data blocks accessed by process: %d", db.getBlockAccesses());
        System.out.printf("\n(Index Search) No. of records found: %d", count);
        System.out.printf("\nAverage of FG3_PCT_home of returned records: %.2f", count > 0 ? totalFG3_PCT_home/count : 0);
        // running time = endTime - startTime (in nanoseconds)
        System.out.printf("\n\tRunning time: %.3f ms", (endTime - startTime) / 1_000_000.0);
        // point 5 brute-force searching
        startTime = System.nanoTime();
        int blkAccesses = db.bruteForceSearch(0.6f, 1.0f);
        endTime = System.nanoTime();
        endTime = System.nanoTime();
        System.out.printf("\nNo. of data blocks accessed by bruteforce: %d", blkAccesses);
        System.out.printf("\n\tRunning Time: %.3f ms", (endTime - startTime) / 1_000_000.0);
    }

    public ArrayList<Address> getAddressesForKeysBetween(Node node, float minKey, float maxKey) {
        BPTHelper.addIndexNodeReads();
        // traverse until leaf
        if (!node.isLeaf()) {
            int ptr = node.getIdxOfKey(minKey, true);
            Node childNode = ((InternalNode) node).getChild(ptr);
            return getAddressesForKeysBetween(childNode, minKey, maxKey);
        } else {
            ArrayList<Address> addresses = new ArrayList<>();
            int ptr = node.getIdxOfKey(minKey, false);
            LeafNode leafNode = (LeafNode) node;
            while (true) {
                if (ptr == leafNode.getKeyCount()) {
                    if (leafNode.getRightSibling() == null) break;
                    leafNode = (LeafNode) leafNode.getRightSibling();
                    BPTHelper.addIndexNodeReads();
                    ptr = 0;
                }
                if (leafNode.getKeyAt(ptr) > maxKey) break;
                Float key = leafNode.getKeyAt(ptr);
                addresses.addAll(leafNode.getAddressesForKey(key));
                ptr++;
            }
            return (addresses.isEmpty() ? null : addresses);
        }
    }

    // -------------------------EXPERIMENT 5-------------------------
    
    public static void ex5(Database db, BPlusTree bPlusTree) {
        System.out.println("\n\nEXPERIMENT 5: Delete those records with \"FG_PCT_home\" <= 0.35:");
        long startTime = System.nanoTime();
        ArrayList<Float> keysToRemove = bPlusTree.ex5Helper(bPlusTree.getRoot(), Float.NEGATIVE_INFINITY, 0.35f);
        ArrayList<Address> addressesToRemove = new ArrayList<Address>();
        // There is a more efficient way to be mentioned in report
        for (Float key : keysToRemove) {
            addressesToRemove.addAll(bPlusTree.deleteKey(key));
        }
        System.out.printf("No. of records to delete: %d\n", addressesToRemove.size());
        // db.deleteRecord(addressesToRemove); Commented out to show a fair trade off with second part where deletion is not performed
        long endTime = System.nanoTime();
        System.out.printf("No. of Nodes in updated B+ tree: %d\n", bPlusTree.countNodes(bPlusTree.getRoot()));
        System.out.printf("No. of Levels in updated B+ tree: %d\n", bPlusTree.getDepth(bPlusTree.getRoot()));
        System.out.printf("\nContent of the root node of the updated B+ tree(only the keys): %s\n", BPlusTree.getRoot().keys);
        System.out.printf("\tRunning time: %.3f ms", (endTime - startTime) / 1_000_000.0);

        System.out.print("\nBrute-force range deletion:");
        startTime = System.nanoTime();
        int bruteForceAccessCount = db.bruteForceSearch(0.0f, 0.35f);
        endTime = System.nanoTime();
        System.out.printf("\nNumber of data blocks that would be accessed by a brute-force: %d\n", bruteForceAccessCount);
        System.out.printf("\tRunning time: %.3f ms", (endTime - startTime) / 1_000_000.0);
    }

    public static ArrayList<Float> ex5Helper(Node node, float lowerBound, float upperBound) {
        ArrayList<Float> keysToRemove = new ArrayList<Float>();
        // go all the way to the left
        while (!node.isLeaf()) {
            node = ((InternalNode) node).getChild(0);
        }
        LeafNode leafNode = (LeafNode) (Node) node;
        boolean done = false;
        int pointer = 0;
        while (!done && leafNode != null) {
            // System.out.println(leafNode.keys);
            while (pointer < leafNode.getKeyCount()) {
                Float key = leafNode.getKeyAt(pointer);
                pointer += 1;
                if (key >= lowerBound && key <= upperBound) {
                    keysToRemove.add(key);
                } else if (key < lowerBound) {
                    pointer += 1;
                } else if (key > upperBound) {
                    done = true;
                }
            }
            pointer = 0;
            leafNode = leafNode.getRightSibling();
        }
        System.out.printf("No. of keys to delete (not records): %d\n", keysToRemove.size());
        return keysToRemove;
    }
}