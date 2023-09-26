package indexComponent;

import java.util.ArrayList;
import java.util.List;
import storageComponent.Address;
import storageComponent.Record;
import storageComponent.Database;
import utils.Parser;


public class BPlusTree {

    static final int NODE_SIZE = (Parser.BLOCK_SIZE - Parser.OVERHEAD)/(Parser.POINTER_SIZE+Parser.KEY_SIZE);
    static Node rootNode;
    Node nodeToInsertTo;

    public BPlusTree() {
        rootNode = createFirstNode();
    }

    public LeafNode createFirstNode() {
        LeafNode newNode = new LeafNode();
        BPTHelper.addNode();
        newNode.setRoot(true);
        newNode.setLeaf(true);
        setRoot(newNode);
        return newNode;
    }

    public static Node createNode() {
        Node newNode = new Node();
        BPTHelper.addNode();
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
        Float lowerbound = 0.0f;
        lowerbound = checkForLowerbound(key);
        return (deleteNode(rootNode, null, -1, -1, key, lowerbound));
    }

    // handles an invalid node type (wrapper function)
    private void handleInvalidTree(Node underUtilizedNode, InternalNode parent, int parentPointerIndex,
            int parentKeyIndex) throws IllegalStateException {
        if (parent == null) {
            handleInvalidRootNode(underUtilizedNode);
        } else if (underUtilizedNode.isLeaf()) {
            // Rebalancing of Leaf node
            handleInvalidLeafNode(underUtilizedNode, parent,
                    parentPointerIndex, parentKeyIndex);
        } else if (!underUtilizedNode.isLeaf()) {
            // Rebalancing of Non-leaf node
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
        boolean found = false;
        Float lowerbound = 0.0f;

        // find the largest key in node smaller than key
        for (int i = node.getKeyCount() - 1; i >= 0; i--) {
            if (key >= node.getKeyAt(i)) {
                node = (InternalNode) node.getChild(i + 1);
                found = true;
                break;
            }
        }
        if (!found && key < node.getKeyAt(0)) {
            node = (InternalNode) node.getChild(0);
        }

        // loop till get leftmost key
        while (!node.getChild(0).isLeaf()) {
            node = (InternalNode) node.getChild(0);
        }

        lowerbound = node.getChild(0).getKeyAt(0);
        return (lowerbound);

    }

    public ArrayList<Address> deleteNode(Node node, InternalNode parent, int parentPointerIndex, int parentKeyIndex,
            Float key, Float lowerbound) {

        ArrayList<Address> addressesToDel = new ArrayList<>();
        if (node.isLeaf()) {
            LeafNode leaf = (LeafNode) node;
            int keyIdx = node.getIdxOfKey(key, false);
            if ((keyIdx == leaf.getKeyCount()) || (key != leaf.getKeyAt(keyIdx))) {
                return null;
            }

            // found keys to delete: 1) remove key in map 2) remove idx in records
            addressesToDel.addAll(leaf.getAddressesForKey(key));
            leaf.removeKeyAt(keyIdx);
            leaf.removeKeyInMap(key);

            int ptrIdx = node.getIdxOfKey(key, true);
            keyIdx = ptrIdx - 1;

            LeafNode LeafNode = (LeafNode) node;
            Float newLowerBound = 0.0f;

            if (LeafNode.getKeyCount() >= (keyIdx + 1)) {
                newLowerBound = lowerbound;
                List<Float> keys = LeafNode.getKeys();
                LeafNode.updateKeyAt(ptrIdx - 1, keys.get(0), false, newLowerBound);
            } else {
                newLowerBound = checkForLowerbound(LeafNode.getKeyAtIdx(keyIdx + 1));
                List<Float> keys = LeafNode.getKeys();
                LeafNode.updateKeyAt(ptrIdx - 1, keys.get(0), true, newLowerBound);
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
        // conduct necessary rebalancing
        if (node.isUnderUtilized(NODE_SIZE)) {
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
            int parentKeyIndex) throws IllegalStateException {
        int numChildrenOfRightSiblingParent = 0;
        int numChildrenOfNodeParent = 0;
        LeafNode rightSibling = null;
        // get right sibling
        LeafNode underUtilizedLeaf = (LeafNode) underUtilizedNode;
        if (underUtilizedLeaf.getRightSibling() != null) {
            rightSibling = (LeafNode) underUtilizedLeaf.getRightSibling();
            if (rightSibling.getParent() != null) {
                numChildrenOfRightSiblingParent = rightSibling.getParent().getChildren().size();
            }
        }
        // get left sibling
        LeafNode leftSibling = (LeafNode) underUtilizedLeaf.getLeftSibling();
        if (underUtilizedNode.getParent() != null) {
            numChildrenOfNodeParent = underUtilizedNode.getParent().getChildren().size();
        }

        // move one key from sibling to target, prioritise left
        if (leftSibling != null && leftSibling.canDonate(NODE_SIZE)) {
            moveOneKeyLeafNode(leftSibling, underUtilizedLeaf, true, parent, parentKeyIndex);
        } else if (rightSibling != null && rightSibling.canDonate(NODE_SIZE)) {
            moveOneKeyLeafNode(rightSibling, underUtilizedLeaf, false, parent, parentKeyIndex + 1);

        // cant' donate, need to merge, check if left/right + self size <= NODE_SIZE, then see if we parent can handle a decrement of children
        } else if ((leftSibling != null && (leftSibling.getKeyCount() + underUtilizedLeaf.getKeyCount()) <= NODE_SIZE
                && (numChildrenOfNodeParent >= underUtilizedNode.getParent().getMinInternalNodeSize()))) {
            mergeLeafNodes(leftSibling, underUtilizedLeaf, parent, parentPointerIndex, parentKeyIndex, false);
        } else if (rightSibling != null && (rightSibling.getKeyCount() + underUtilizedLeaf.getKeyCount()) <= NODE_SIZE
                && (numChildrenOfRightSiblingParent >= underUtilizedNode.getParent().getMinInternalNodeSize())) {
            mergeLeafNodes(underUtilizedLeaf, rightSibling, parent, parentPointerIndex + 1, parentKeyIndex + 1, true);
        } else {
            throw new IllegalStateException("Can't have both leaf " +
                    "pointers null and not be root or no " +
                    "common parent");
        }
    }

    private void handleInvalidInternalNode(Node underUtilizedNode,
            InternalNode parent,
            int parentPointerIndex,
            int parentKeyIndex) throws IllegalStateException {

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
        } else {
            throw new IllegalStateException("Can't merge or redistribute internal node " + underUtilizedInternalNode);
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
            if (ptrIdx >= 0 && ptrIdx < node.getKeyCount() && key == node.getKeyAt(ptrIdx)) {
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

    // -------------------------EXPERIMENT 2-------------------------

    public static void ex2(BPlusTree tree) {
        System.out.println("\nEXPERIMENT 2: Build a B+Tree on FG_PCT_home by inserting the records sequentially:");
        BPTHelper treeHelper = new BPTHelper();
        System.out.println("Parameter n: " + NODE_SIZE);
        System.out.printf("No. of Nodes in B+ tree: %d\n", treeHelper.getNodeCount());
        System.out.printf("No. of Levels in B+ tree: %d\n", tree.getDepth(tree.getRoot()));
        System.out.println("Content of the root node: " + BPlusTree.getRoot().keys);
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

    // /**
    //  * Function for Experiment 3 of Project 1 (Search Query for numVotes equal to '500')
    //  * 
    //  * @param db the database used for the experiment
    //  * @param tree the B+ Tree used for the experiment
    //  */
    // public static void experimentThree(Database db, BPlusTree tree) {
    //     System.out.println("\n----------------------EXPERIMENT 3-----------------------");
    //     BPTHelper performance = new BPTHelper();
    //     System.out.println("Movies with the 'numVotes' equal to 500: ");

    //     long startTime = System.nanoTime();
    //     ArrayList<Address> resultAdd = tree.getIdxOfKey(500);
    //     long endTime = System.nanoTime();
    //     double totalAverageRating = 0;
    //     int totalCount = 0;
    //     ArrayList<Record> results = new ArrayList<>();
    //     if (resultAdd != null) {
    //         for (Address add : resultAdd) {
    //             Record record = db.getRecord(add);
    //             System.out.print("\n" + record);
    //             results.add(record);
    //             totalAverageRating += record.getAverageRating();
    //             totalCount++;
    //         }
    //     }
    //     System.out.printf("\n\nNo. of Index Nodes the process accesses: %d\n", performance.getNodeReads());
    //     System.out.printf("No. of Data Blocks the process accesses: %d\n", db.getBlockAccesses());
    //     System.out.printf("Average of 'averageRating's' of the records accessed: %.2f\n",
    //             (double) totalAverageRating / totalCount);
    //     long duration = (endTime - startTime); // divide by 1000000 to get milliseconds.
    //     System.out.printf("Running time of retrieval process: %d nanoseconds\n", duration);
    //     startTime = System.nanoTime();
    //     int bruteForceAccessCount = db.getBlocksAccessedByForce(500, 500);
    //     endTime = System.nanoTime();
    //     System.out.printf("Number of Data Blocks Accessed by Brute Force (numVotes = 500): %d", bruteForceAccessCount);
    //     System.out.printf("\nLinear Time Accessed by Brute Force (numVotes = 500): %d", endTime - startTime);
    //     System.out.printf("\nNo. of Data Blocks accessed reduced in total: %d\n ", db.getBlockAccessReduced());
    // }

    // -------------------------EXPERIMENT 4-------------------------
    public ArrayList<Address> ex4SearchFunction(Float minKey, Float maxKey) {
        return ex4Helper(this.getRoot(), minKey, maxKey);
    }

    public static ArrayList<Address> ex4Helper(Node node, Float minKey, Float maxKey) {
        int ptr;
        ArrayList<Address> resultList = new ArrayList<>();
        BPTHelper.addIndexNodeReads();
        // only leaf node is useful to get addresses
        if (node.isLeaf()) {
            ptr = node.getIdxOfKey(minKey, false);
            LeafNode leaf = (LeafNode) node;
            while (true) {
                if (ptr == leaf.getKeyCount()) {
                    if (leaf.getRightSibling() == null) break;
                    leaf = (LeafNode) (leaf.getRightSibling());
                    BPTHelper.addIndexNodeReads();
                    ptr = 0;
                }
                if (leaf.getKeyAtIdx(ptr) > maxKey) break;
                Float key = leaf.getKeyAtIdx(ptr);
                resultList.addAll(leaf.getAddressesForKey(key));
                ptr++;
            }
            return (resultList.size() > 0 ? resultList : null);
        } else {
            ptr = node.getIdxOfKey(minKey, true);
            Node childNode = ((InternalNode) node).getChild(ptr);
            return (ex4Helper(childNode, minKey, maxKey));
        }
    }
    

    // /**
    //  * Function for Experiment 4 of Project 1 (Range Query Search for numVotes of '30,000' to '40,000')
    //  *
    //  * @param db the database used for the experiment
    //  * @param tree the B+ Tree used for the experiment
    //  */
    // public static void experimentFour(Database db, BPlusTree tree) {
    //     System.out.println("\n\n----------------------EXPERIMENT 4-----------------------");
    //     BPTHelper performance = new BPTHelper();
    //     System.out.println("Movies with the 'numVotes' from 30,000 to 40,000, both inclusively: ");
    //     long startTime = System.nanoTime();
    //     ArrayList<Address> resultAdd = tree.rangeSearch(30000, 40000);
    //     long endTime = System.nanoTime();
    //     double totalAverageRating = 0;
    //     int totalCount = 0;
    //     ArrayList<Record> results = new ArrayList<>();
    //     if (resultAdd != null) {
    //         for (Address add : resultAdd) {
    //             Record record = db.getRecord(add);
    //             System.out.print("\n From Indexing" + record);
    //             results.add(record);
    //             totalAverageRating += record.getAverageRating();
    //             totalCount++;
    //         }
    //     }
    //     System.out.printf("\n\nNo. of Index Nodes the process accesses: %d\n", performance.getIndexNodeReads());
    //     System.out.printf("No. of Data Blocks the process accesses: %d\n", db.getBlockAccesses());
    //     System.out.printf("Average of 'averageRating's' of the records accessed: %.2f",
    //             (double) totalAverageRating / totalCount);
    //     long duration = (endTime - startTime); // divide by 1000000 to get milliseconds.
    //     System.out.printf("\nRunning time of retrieval process: %d nanoseconds\n", duration);
    //     startTime = System.nanoTime();
    //     int bruteForceAccessCount = db.getBlocksAccessedByForce(30000, 40000);
    //     endTime = System.nanoTime();
    //     System.out.printf("Number of Data Blocks Accessed by Brute Force (30000<=numVotes<=40000): %d",
    //             bruteForceAccessCount);
    //     System.out.printf("\nLinear Time Accessed by Brute Force (30000<=numVotes<=40000): %d", endTime - startTime);
    //     System.out.printf("\nNo. of Data Blocks accessed reduced in total: %d\n ", db.getBlockAccessReduced());
    // }


    // -------------------------EXPERIMENT 5-------------------------
    // /**
    //  * Function for Experiment 5 of Project 1 (Deletion for numVotes equal to value '1000')
    //  * 
    //  * @param db the database used for the experiment
    //  * @param tree the B+ Tree used for the experiment
    //  */
    // public static void experimentFive(Database db, BPlusTree tree) {
    //     System.out.println("\n\n----------------------EXPERIMENT 5-----------------------");
    //     BPTHelper performance = new BPTHelper();
    //     System.out.println("-- Deleting all records with 'numVotes' of 1000 -- ");
    //     long startTime = System.nanoTime();
    //     ArrayList<Address> deletedAdd = tree.deleteKey(1000);

    //     db.deleteRecord(deletedAdd);
    //     long endTime = System.nanoTime();
    //     System.out.printf("No. of Nodes in updated B+ tree: %d\n", performance.getNodeCount());
    //     tree.countLevel(tree.getRoot());
    //     System.out.printf("No. of Levels in updated B+ tree: %d\n", performance.getDepth());
    //     System.out.printf("\nContent of the root node in updated B+ tree: %s\n", BPlusTree.getRoot().keys);
    //     long duration = (endTime - startTime); // divide by 1000000 to get milliseconds.
    //     System.out.printf("Running time of retrieval process: %d nanoseconds\n", duration);
    //     System.out.println("Number of Data Blocks Accessed by Brute Force (numVotes=1000):");
    //     startTime = System.nanoTime();
    //     int bruteForceAccessCount = db.getBlocksAccessedByForce(1000, 1000);
    //     endTime = System.nanoTime();
    //     System.out.printf("Number of Data Blocks Accessed by Brute Force (numVotes = 1000): %d", bruteForceAccessCount);
    //     System.out.printf("\nLinear Time Accessed by Brute Force (numVotes = 1000): %d", endTime - startTime);
    //     System.out.printf("\nNo. of Data Blocks accessed reduced in total: %d\n ", db.getBlockAccessReduced());
    // }

}