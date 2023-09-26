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
    
    // finding the leaf node to find/insert the key to
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
                    if (nodeToInsertTo.getKey(i) <= key) {
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

    // GETTERS AND SETTERS
    public static void setRoot(Node root) {
        rootNode = root;
        rootNode.setRoot(true);
    }

    public static Node getRoot() {
        return rootNode;
    }

    /**
     * Finds the lower bound of the specified key in the B+ tree.
     *
     * @param key the key to find the lower bound of
     * @return the lower bound of the specified key
     */

    private Float checkForLowerbound(Float key) {

        InternalNode node = (InternalNode) rootNode;
        boolean found = false;
        Float lowerbound = 0.0f;

        // loop from back to front to find the first key that is smaller than the key
        for (int i = node.getKeySize() - 1; i >= 0; i--) {
            if (key >= node.getKeyAt(i)) {
                node = (InternalNode) node.getChild(i + 1);
                found = true;
                break;
            }
        }
        if (found == false && key < node.getKeyAt(0)) {
            node = (InternalNode) node.getChild(0);
        }

        // loop till get leftmost key
        while (!node.getChild(0).isLeaf()) {
            node = (InternalNode) node.getChild(0);
        }

        lowerbound = node.getChild(0).getKeyAt(0);
        return (lowerbound);

    }

    /**
     * Wrapper function for deleting node
     * Also finds the lower bound of the subtree containing the node with the
     * specified key value.
     *
     * @param key the key to be deleted
     * @return AraryList of address to be removed from database
     */
    public ArrayList<Address> deleteKey(Float key) {
        Float lowerbound = 0.0f;
        lowerbound = checkForLowerbound(key);
        return (deleteNode(rootNode, null, -1, -1, key, lowerbound));
    }

    /**
     * Recursive function that deletes a key from the b plus tree with the specified
     * key value, starting from the given node.
     * Calls a check for invalid tree after deleting key
     *
     * @param node               the node from which to begin the search for the
     *                           node to be deleted
     * @param parent             the parent node of the current node
     * @param parentPointerIndex an integer representing the index of the pointer to
     *                           the current node in the parent node
     * @param parentKeyIndex     an integer representing the index of the key in the
     *                           parent node that points to the current node
     * @param key                the key value of the node to be deleted
     * @param lowerbound         the lower bound of the subtree containing the node
     *                           to be deleted
     * @return an ArrayList of Address objects representing the deleted node(s), or
     *         an empty list if no nodes were deleted.
     */
    public ArrayList<Address> deleteNode(Node node, InternalNode parent, int parentPointerIndex, int parentKeyIndex,
            Float key, Float lowerbound) {
        ArrayList<Address> addOfRecToDelete = new ArrayList<>();

        if (node.isLeaf()) {
            // search for the key to delete
            LeafNode leaf = (LeafNode) node;
            int keyIdx = node.searchKey(key, false);
            if ((keyIdx == leaf.getKeySize()) || (key != leaf.getKeyAt(keyIdx))) {
                return null;
            }

            // found keys to delete: 1) remove key in map 2) remove idx in records
            addOfRecToDelete.addAll(leaf.getAddressesForKey(key));
            leaf.removeKeyAt(keyIdx);
            leaf.removeKeyInMap(key);

            // Update Key after deleting
            int ptrIdx = node.searchKey(key, true);
            keyIdx = ptrIdx - 1;

            LeafNode LeafNode = (LeafNode) node;
            Float newLowerBound = 0.0f;

            // Get newLowerBound (possible for current key taken to be the lowerbound) if
            // KeyIdx is not KeySize
            if (LeafNode.getKeySize() >= (keyIdx + 1)) {
                newLowerBound = lowerbound;
                List<Float> keys = LeafNode.getKeys();
                LeafNode.updateKey(ptrIdx - 1, keys.get(0), false, newLowerBound);

            } else {
                newLowerBound = checkForLowerbound(LeafNode.getKey(keyIdx + 1)); // Get new lowerbound
                List<Float> keys = LeafNode.getKeys();
                LeafNode.updateKey(ptrIdx - 1, keys.get(0), true, newLowerBound);
            }

        } else {
            // traverse to leaf node to find records to delete
            InternalNode nonLeafNode = (InternalNode) node;
            int ptrIdx = node.searchKey(key, true);
            int keyIdx = ptrIdx - 1;

            // read the next level node (read action will be recorded in the next level)
            Node next = nonLeafNode.getChild(ptrIdx);
            addOfRecToDelete = deleteNode(next, nonLeafNode, ptrIdx, keyIdx, key, lowerbound);

        }

        // carry out re-balancing tree magic if needed
        if (node.isUnderUtilized(NODE_SIZE)) {
            handleInvalidTree(node, parent, parentPointerIndex, parentKeyIndex);
        }

        return addOfRecToDelete;
    }

    /**
     * Handles the case when a node in the b plus tree is underutilized and needs to
     * be rebalanced.
     * If the underutilized node is the root node, the
     * {@link #handleInvalidRoot(Node)} method is called.
     * If the underutilized node is a leaf node, the
     * {@link #handleInvalidLeaf(Node, InternalNode, int, int)} method is called.
     * If the underutilized node is a non-leaf node, the
     * {@link #handleInvalidInternal(Node, InternalNode, int, int)} method is called.
     *
     * @param underUtilizedNode  the node that is underutilized and needs to be
     *                           rebalanced
     * @param parent             the parent node of the underutilized node
     * @param parentPointerIndex the index of the pointer to the underutilized node
     *                           in the parent node
     * @param parentKeyIndex     the index of the key in the parent node that points
     *                           to the underutilized node
     * @throws IllegalStateException if the state of the tree is incorrect
     */
    private void handleInvalidTree(Node underUtilizedNode, InternalNode parent, int parentPointerIndex,
            int parentKeyIndex) throws IllegalStateException {
        if (parent == null) {
            handleInvalidRoot(underUtilizedNode);
        } else if (underUtilizedNode.isLeaf()) {
            // Rebalancing of Leaf node
            handleInvalidLeaf(underUtilizedNode, parent,
                    parentPointerIndex, parentKeyIndex);
        } else if (underUtilizedNode.isNonLeaf()) {
            // Rebalancing of Non-leaf node
            handleInvalidInternal(underUtilizedNode, parent,
                    parentPointerIndex, parentKeyIndex);
        } else {
            throw new IllegalStateException("state is wrong!");
        }
    }

    /**
     * Handles the case when the root node of the b plus search tree is
     * underutilized and needs to be rebalanced.
     * If the root node is a leaf node, all keys are removed from the leaf node. ->
     * Empty Tree
     * If the root node is a non-leaf node, the first child of the root node becomes
     * the new root node.
     *
     * @param underUtilizedNode the root node that is underutilized and needs to be
     *                          rebalanced
     */
    public void handleInvalidRoot(Node underUtilizedNode) {
        if (underUtilizedNode.isLeaf()) { // Only node in B+ Tree - Root
            ((LeafNode) underUtilizedNode).clear();
        } else {
            InternalNode nonLeafRoot = (InternalNode) underUtilizedNode;
            Node newRoot = nonLeafRoot.getChild(0);
            newRoot.setParent(null);
            rootNode = newRoot;
        }
    }

    /**
     * Handles the case when a leaf node in the b plus tree is underutilized and
     * needs to be rebalanced.
     * Checks if it can redistribute with the next sibling node, then with the
     * previous sibling node, and if neither are possible, merges the two nodes.
     * If the merging results in the parent node being underutilized, the
     * {@link #handleInvalidTree(Node, InternalNode, int, int)} method is called
     * recursively.
     *
     * @param underUtilizedNode  the leaf node that is underutilized and needs to be
     *                           rebalanced
     * @param parent             the parent node of the underutilized node
     * @param parentPointerIndex the index of the pointer to the underutilized node
     *                           in the parent node
     * @param parentKeyIndex     the index of the key in the parent node that points
     *                           to the underutilized node
     * @throws IllegalStateException if both previous and next sibling nodes are
     *                               null, or if the state of the tree is incorrect
     */
    private void handleInvalidLeaf(Node underUtilizedNode,
            InternalNode parent,
            int parentPointerIndex,
            int parentKeyIndex) throws IllegalStateException {
        int numChildrenOfNextParent = 0;
        int numChildrenOfNodeParent = 0;
        LeafNode rightSibling;
        rightSibling = null;
        // load the neighbors
        LeafNode underUtilizedLeaf = (LeafNode) underUtilizedNode;
        if (underUtilizedLeaf.getRightSibling() != null) {
            rightSibling = (LeafNode) underUtilizedLeaf.getRightSibling();
            if (rightSibling.getParent() != null) {
                numChildrenOfNextParent = rightSibling.getParent().getChildren().size();
            }
        }

        LeafNode leftSibling = (LeafNode) underUtilizedLeaf.getLeftSibling();

        if (underUtilizedNode.getParent() != null) {
            numChildrenOfNodeParent = underUtilizedNode.getParent().getChildren().size();
        }

        if (rightSibling != null && rightSibling.isAbleToGiveOneKey(NODE_SIZE)) {
            // Move one key from right to left
            // handle invalid leaf: leaf to right
            moveOneKey(rightSibling, underUtilizedLeaf, false, parent, parentKeyIndex + 1);
        } else if (leftSibling != null && leftSibling.isAbleToGiveOneKey(NODE_SIZE)) {
            // Move one key from left to righ
            // handle invalid leaf: right to left
            moveOneKey(leftSibling, underUtilizedLeaf, true, parent, parentKeyIndex);
        }
        // we can't redistribute, try merging with next
        else if ((leftSibling != null && (leftSibling.getKeySize() + underUtilizedLeaf.getKeySize()) <= NODE_SIZE
                && (numChildrenOfNodeParent >= underUtilizedNode.getParent().getMinInternalNodeSize()))) {
            // it's the case where split node is in the left from parent
            // merge with left node
            mergeLeafNodes(leftSibling, underUtilizedLeaf, parent, parentPointerIndex, parentKeyIndex, false);
        } else if (rightSibling != null && (rightSibling.getKeySize() + underUtilizedLeaf.getKeySize()) <= NODE_SIZE
                && (numChildrenOfNextParent >= underUtilizedNode.getParent().getMinInternalNodeSize())) {
            // it's the case where under utilized node is the left node from parent
            // merge with right node
            mergeLeafNodes(underUtilizedLeaf, rightSibling, parent, parentPointerIndex + 1, parentKeyIndex + 1, true);
        } else {
            throw new IllegalStateException("Can't have both leaf " +
                    "pointers null and not be root or no " +
                    "common parent");
        }
    }

    /**
     * Handles an invalid internal node by either redistributing keys with adjacent
     * nodes
     * or merging it with adjacent nodes.
     *
     * @param underUtilizedNode  the internal node that is underutilized and needs
     *                           to be handled
     * @param parent             the parent node of the underutilized node
     * @param parentPointerIndex the index of the pointer to the underutilized node
     *                           in the parent node's child list
     * @param parentKeyIndex     the index of the key in the parent node that points
     *                           to the underutilized node
     * @throws IllegalStateException if both prevInternal and nextInternal are null
     *                               or if the underutilized node cannot be
     *                               redistributed or merged with adjacent nodes
     */
    private void handleInvalidInternal(Node underUtilizedNode,
            InternalNode parent,
            int parentPointerIndex,
            int parentKeyIndex) throws IllegalStateException {

        Node underUtilizedInternal = underUtilizedNode;

        // load the adjacent nodes
        InternalNode prevInternal = null;
        InternalNode nextInternal = null;
        try {
            nextInternal = (InternalNode) parent.getChild(parentPointerIndex + 1);
        } catch (Exception e) {
            System.out.print(e);
        }

        try {
            prevInternal = (InternalNode) parent.getChild(parentPointerIndex - 1);
        } catch (Exception e) {
            System.out.print(e);
        }

        if (nextInternal == null && prevInternal == null)
            throw new IllegalStateException("Both prevInternal and nextInternal is null for " + underUtilizedNode);

        if (prevInternal != null && prevInternal.isAbleToGiveOneKey(NODE_SIZE)) {
            // Move one key from left non leaf node
            moveOneKeyInternalNode(prevInternal, (InternalNode) underUtilizedInternal, true, parent, parentKeyIndex);

        } else if (nextInternal != null && nextInternal.isAbleToGiveOneKey(NODE_SIZE)) {
            // Move one key from right non leaf node
            moveOneKeyInternalNode(nextInternal, (InternalNode) underUtilizedInternal, false, parent, parentKeyIndex + 1);

        }
        // now, check if we can merge with the left node
        else if (prevInternal != null &&
                (underUtilizedInternal.getKeySize() + prevInternal.getKeySize()) <= NODE_SIZE) {
            // Merge with left Non-Leaf node
            mergeInternalNodes(prevInternal, (InternalNode) underUtilizedInternal, parent,
                    parentPointerIndex, parentKeyIndex, true);
        }
        // // check if we can merge with the right node
        else if (nextInternal != null &&
                (underUtilizedInternal.getKeySize() + nextInternal.getKeySize()) <= NODE_SIZE) {
            // Merge with Right Non-Leaf Node
            mergeInternalNodes((InternalNode) underUtilizedInternal, nextInternal, parent,
                    parentPointerIndex + 1, parentKeyIndex + 1, false);
        } else {
            throw new IllegalStateException("Can't merge or redistribute internal node " + underUtilizedInternal);
        }
    }

    /**
     * Handles the case when a non-leaf node in the b plus tree is underutilized and
     * needs to be rebalanced.
     * Checks if it can redistribute with the next sibling node, then with the
     * previous sibling node, and if neither are possible, merges the two nodes.
     * If the merging results in the parent node being underutilized, the
     * {@link #handleInvalidTree(Node, InternalNode, int, int)} method is called
     * recursively.
     *
     * @param underUtilizedNode  the non-leaf node that is underutilized and needs
     *                           to be rebalanced
     * @param parent             the parent node of the underutilized node
     * @param parentPointerIndex the index of the pointer to the underutilized node
     *                           in the parent node
     * @param parentKeyIndex     the index of the key in the parent node that points
     *                           to the underutilized node
     * @throws IllegalStateException if both previous and next sibling nodes are
     *                               null, or if the state of the tree is incorrect
     */
    private void moveOneKeyInternalNode(InternalNode giver, InternalNode receiver,
            boolean giverOnLeft, InternalNode parent,
            int inBetweenKeyIdx) {
        Float key;

        if (giverOnLeft) {
            // "Moving one key from Left non-leaf sibling"
            // Get last key from giver non leaf node to the receiver non leaf node
            // Remove last key from giver
            giver.removeKeyAt(giver.getKeySize() - 1);

            // Remove child from the giver node
            // Add child to the non leaf node
            Node nodeToMove = giver.getChild(giver.getKeySize()); // get last child of giver
            giver.removeChild(nodeToMove);
            receiver.addChild(nodeToMove);
            receiver.getKeys().add(receiver.getKeySize(), receiver.getChild(1).getFirstKey());

            key = receiver.getKeyAt(0);
        } else {
            // Moving one key from Right non-leaf sibling
            // Get first key from giver non leaf node to the receiver non leaf node
            // Remove first key from giver
            giver.removeKeyAt(0);

            // Remove child from the giver node
            // Add child to the non leaf node
            Node nodeToMove = giver.getChild(0); // get first child of giver
            giver.removeChild(nodeToMove);
            receiver.addChild(nodeToMove);

            receiver.getKeys().add(receiver.getKeySize(), receiver.getChild(1).getFirstKey());

            key = receiver.getKeyAt(0);
        }
        // in either case update the parent key

        // UpdateKey at higher levels with the correct lowerbound
        int ptrIdx = receiver.searchKey(key, true);
        int keyIdx = ptrIdx - 1;

        InternalNode LeafNode = (InternalNode) receiver;
        Float lowerbound = checkForLowerbound(key);

        Float newLowerBound = 0.0f;

        // Get newLowerBound (possible for current key taken to be the lowerbound) if
        // KeyIdx is not KeySize
        if (LeafNode.getKeySize() >= (keyIdx + 1)) {
            newLowerBound = lowerbound;
        } else {
            newLowerBound = checkForLowerbound(LeafNode.getKey(keyIdx + 1));
            parent.updateKey(inBetweenKeyIdx - 1, key, false, checkForLowerbound(key));

        }
        parent.replaceKeyAt(inBetweenKeyIdx, newLowerBound);

    }

    /**
     * Merge the node with the adjacent sibling (either left or right)
     * if neither of the adjacent sibling can be used to merge or redistribute,
     * the function will throw an exception. Otherwise, it will call the
     * appropriate method to redistribute the keys.
     *
     * @param nodeToMergeTo   the node to merge to (either left or right)
     * @param current         the underutilized node
     * @param parent          the parent node of the underutilized node
     * @param rightPointerIdx the index of the pointer pointing to the right sibling
     * @param inBetweenKeyIdx the index of the key between two siblings
     * @param mergeWithLeft   a boolean value indicating whether to merge with the
     *                        left sibling or not
     * @throws IllegalStateException if neither of the adjacent sibling can be used
     *                               to merge or redistribute
     */
    private void mergeInternalNodes(InternalNode nodeToMergeTo, InternalNode current, InternalNode parent,
            int rightPointerIdx,
            int inBetweenKeyIdx, boolean mergeWithLeft) {
        Float keyToRemove;

        // merge the right node to left
        if (mergeWithLeft) {

            int moveKeyCount = current.getKeySize();
            keyToRemove = nodeToMergeTo.getChild(nodeToMergeTo.getKeySize()).getLastKey();

            // move every key from current node into nodeToMergeTo
            for (int i = 0; i < moveKeyCount; i++) {
                nodeToMergeTo.getKeys().add(nodeToMergeTo.getKeySize(), current.getKeyAt(i));
            }

            // move every child from current node into nodeToMergeTo
            for (int i = 0; i < current.getChildren().size(); i++) {
                nodeToMergeTo.getChildren().add(current.getChild(i));
            }

            // Update parent after merging
            nodeToMergeTo.getKeys().add(nodeToMergeTo.getKeySize(),
                    nodeToMergeTo.getChild(nodeToMergeTo.getKeySize() + 1).getFirstKey());
            current.getParent().removeChild(current);

        }

        // merge the left node with right
        else {
            int moveKeyCount = current.getKeySize();

            keyToRemove = current.getFirstKey();

            // move every key from current node into nodeToMergeTo
            for (int i = 0; i < moveKeyCount; i++) {
                nodeToMergeTo.getKeys().add(0, current.getKeyAt(i));
            }
            for (int i = 0; i < current.getChildren().size(); i++) {
                nodeToMergeTo.getChildren().add(current.getChild(i));
            }
            // Update parent after merging
            nodeToMergeTo.getKeys().add(0, nodeToMergeTo.getChild(1).getFirstKey());
            current.getParent().removeChild(current);

        }

        // UpdateKey at higher levels with the correct lowerbound
        int ptrIdx = nodeToMergeTo.searchKey(keyToRemove, true);
        int keyIdx = ptrIdx - 1;

        InternalNode LeafNode = (InternalNode) nodeToMergeTo;
        Float lowerbound = checkForLowerbound(keyToRemove);
        Float newLowerBound = 0.0f;

        // Get newLowerBound (possible for current key taken to be the lowerbound) if
        // KeyIdx is not KeySize
        if (LeafNode.getKeySize() >= (keyIdx + 1)) {
            newLowerBound = lowerbound;
        } else {
            newLowerBound = checkForLowerbound(LeafNode.getKey(keyIdx + 1)); // Get new lowerbound
            parent.updateKey(inBetweenKeyIdx - 1, keyToRemove, false, checkForLowerbound(keyToRemove));

        }
    }

    /**
     * Merges the current leaf node with its adjacent leaf node, either to its left
     * or right, depending on the value of the 'mergetoright' boolean flag.
     *
     * @param nodeToMergeTo   the leaf node that will merge with the current node
     * @param current         the leaf node that will be merged with the adjacent
     *                        node
     * @param parent          the parent node of the two leaf nodes being merged
     * @param rightPointerIdx the index of the pointer to the right of the current
     *                        node in the parent node's child list
     * @param inBetweenKeyIdx the index of the key in the parent node that lies
     *                        between the two leaf nodes being merged
     * @param mergetoright    a boolean flag indicating whether to merge with the
     *                        right or left adjacent node
     * @throws IllegalArgumentException if the parent node is null
     */

    private void mergeLeafNodes(LeafNode nodeToMergeTo, LeafNode current, InternalNode parent,
            int rightPointerIdx, int inBetweenKeyIdx, boolean mergetoright) {
        Float removedKey = 0.0f;
        int moveKeyCount = current.getKeySize();
        int NoOfChildren = current.getParent().getChildren().size();
        for (int i = 0; i < moveKeyCount; i++) {
            removedKey = current.removeKeyAt(0);
            int leftLastIdx = nodeToMergeTo.getLastIdx();
            nodeToMergeTo.insertKeyAt(leftLastIdx + 1, removedKey);
            // 2. Move over the records
            nodeToMergeTo.insertByRedistribution(removedKey, current.getAddressesForKey(removedKey));
            current.removeKeyInMap(removedKey);

        }

        parent.removeChild(current); // To remove the empty leaf node
        if ((parent.getChildren().size()) == (parent.getKeySize())) {
            // No need to update parent
        } else {
            parent.removeKeyAt(inBetweenKeyIdx);
        }

        if (mergetoright == true) {
            // update the prev pointer of right next node (if any)
            if (current.getRightSibling() != null) {
                LeafNode currentNext = current.getRightSibling();
                currentNext.setLeftSibling(current.getLeftSibling());
            }

            nodeToMergeTo.setRightSibling(current.getRightSibling());
            if (current.getKeySize() == 0) {

                InternalNode currParent = current.getParent();
                currParent.removeChild(current);
                currParent.removeKeyAt(0);
            }
        } else {
            // update the prev pointer of left getLeftSibling node (if any)

            if (current.getLeftSibling() != null) {
                LeafNode currentPrev = current.getLeftSibling();
                if (currentPrev != null && (currentPrev.getLeftSibling() != null)) {
                    currentPrev.getLeftSibling().setLeftSibling(current.getLeftSibling());
                }

            }

            if (current.getRightSibling() != null) {
                nodeToMergeTo.setRightSibling(current.getRightSibling());
                current.getRightSibling().setLeftSibling(nodeToMergeTo);
            }
            if (current.getKeySize() == 0) {

                // currParent.removeChild(current);
                InternalNode currParent = current.getParent();
                currParent.removeChild(current);
                // if (currParent.getKeySize() > 0){
                if (inBetweenKeyIdx < 0) {
                    currParent.removeKeyAt(inBetweenKeyIdx + 1);

                } else if (currParent.getKeySize() > 0) {

                    currParent.removeKeyAt(inBetweenKeyIdx);
                } else {
                    currParent.removeKeyAt(0);
                }

            } else {

                InternalNode currParent = current.getRightSibling().getParent();
                currParent.removeChild(current);
                // Check if parent key satisfy min node size
                if ((currParent.getKeySize() > currParent.getMinInternalNodeSize())
                        && (currParent.getChildren().size() > current.getMinInternalNodeSize())) {
                    currParent.removeKeyAt(0);

                }
            }
        }

        Float lowerbound = checkForLowerbound(removedKey);
        Float newLowerBound = 0.0f;
        // Get newLowerBound (possible for current key taken to be the lowerbound) if
        // KeyIdx is not KeySize
        if (current.getParent().getKeySize() >= NoOfChildren) { // check if number of children == original # of children
            newLowerBound = lowerbound;
        } else {
            newLowerBound = current.getParent().getChild(0).getFirstKey();

            if (inBetweenKeyIdx == 0) {
                // inBetweenKeyIdx is 0
            } else {
                current.getParent().updateKey(inBetweenKeyIdx - 1, newLowerBound, true, newLowerBound);
            }
        }

    }

    /**
     * Merges two leaf nodes by moving keys and records from the current node to the
     * nodeToMergeTo.
     * Updates the parent node and the next and previous pointers of the nodes
     * involved in the merge.
     * Also updates the lower bound key for the merged nodes.
     *
     * @param nodeToMergeTo   The leaf node that will receive the keys and records
     *                        from the current node.
     * @param current         The leaf node that will be merged into nodeToMergeTo.
     * @param parent          The parent node of the leaf nodes.
     * @param rightPointerIdx The index of the right pointer in the parent node.
     * @param inBetweenKeyIdx The index of the key in the parent node that is
     *                        between the current and nodeToMergeTo nodes.
     * @param mergetoright    A boolean value indicating whether the current node
     *                        will be merged to the right (true) or to the left
     *                        (false) of the nodeToMergeTo.
     */
    private void moveOneKey(LeafNode giver, LeafNode receiver,
            boolean giverOnLeft, InternalNode parent,
            int inBetweenKeyIdx) {
        Float key;
        if (giverOnLeft) {
            // move the key from left node to right
            // 1. Move and edit map records
            Float giverKey = giver.getLastKey();
            receiver.insertByRedistribution(giverKey, giver.getAddressesForKey(giverKey));
            giver.removeKeyInMap(giverKey);

            // 2. Move and edit key in node
            receiver.insertKeyAt(0, giverKey);
            giver.removeKeyAtLast();
            key = receiver.getKeyAt(0);
        } else {
            // move key from right node to left node
            // 1. Move and edit map records
            Float giverKey = giver.getFirstKey();
            receiver.insertByRedistribution(giverKey, giver.getAddressesForKey(giverKey));
            giver.removeKeyInMap(giverKey);

            // 2. Move and edit key in node
            giver.removeKeyAt(0);
            receiver.insertKeyAt(receiver.getKeySize(), giverKey);
            key = giver.getKeyAt(0);

        }

        // Update receiver parent
        if (inBetweenKeyIdx == -1) {
            // Do not update parent
        } else if (inBetweenKeyIdx >= 0) {
            if (parent.getKeySize() == inBetweenKeyIdx) {
                parent.replaceKeyAt(inBetweenKeyIdx - 1, key);

                int lastParentChild = receiver.getParent().getKeys().size() - 1;// point to last child
                Float lastParentChildKey = receiver.getParent().getChild(receiver.getParent().getKeys().size())
                        .getFirstKey();
                if (giver.getParent().getChild(giver.getParent().getChildren().size() - 1).getFirstKey() != key) {
                    receiver.getParent().replaceKeyAt(lastParentChild, lastParentChildKey);
                }
            } else {
                parent.replaceKeyAt(inBetweenKeyIdx, key);

                // if giver is from the same parent, update parent @ index+1 with firstkey
                if (giver.getParent().getChild(inBetweenKeyIdx + 1).getFirstKey() != key) {
                    giver.getParent().replaceKeyAt(inBetweenKeyIdx,
                            giver.getParent().getChild(inBetweenKeyIdx + 1).getFirstKey());
                }
            }

        } else {
            parent.replaceKeyAt(inBetweenKeyIdx - 1, key);
        }

        int ptrIdx = receiver.searchKey(key, true);
        int keyIdx = ptrIdx - 1;

        LeafNode LeafNode = (LeafNode) receiver;
        Float lowerbound = checkForLowerbound(key);
        Float newLowerBound = 0.0f;

        // Get newLowerBound (possible for current key taken to be the lowerbound) if
        // KeyIdx is not KeySize
        if (LeafNode.getKeySize() >= (keyIdx + 1)) {
            newLowerBound = lowerbound;
        } else {
            newLowerBound = checkForLowerbound(LeafNode.getKey(keyIdx + 1)); // Get new lowerbound
            parent.updateKey(inBetweenKeyIdx - 1, parent.getChild(inBetweenKeyIdx).getFirstKey(), false,
                    checkForLowerbound(key));

        }

    }

    /**
     * Wrapper function on top of searchNodeContaining
     *
     * @param key
     * @return ArrayList of Address in the database
     */
    public ArrayList<Address> searchKey(Float key) {
        return (searchValue(this.rootNode, key));
    }

    /**
     * Searches for a value associated with a given key in a B+ tree.
     *
     * @param node The root node of the B+ tree.
     * @param key  The key to search for.
     * @return An ArrayList of Addresses that correspond to the given key,
     *         or null if the key is not found.
     */
    public ArrayList<Address> searchValue(Node node, Float key) {
        BPTHelper.addNodeReads();

        // Find if key is within the rootNode
        if (node.isLeaf()) {
            int ptrIdx = node.searchKey(key, false);
            if (ptrIdx >= 0 && ptrIdx < node.getKeySize() && key == node.getKeyAt(ptrIdx)) {
                return ((LeafNode) node).getAddressesForKey(key); // returns an ArrayList of addresses
            }
            return null;
        }
        // If it's an internal node, descend until we reach a leaf node to find the
        // results
        else {
            int ptrIdx = node.searchKey(key, false); // looks for the upper bound of the key in the node
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
    //     ArrayList<Address> resultAdd = tree.searchKey(500);
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
            ptr = node.searchKey(minKey, false);
            LeafNode leaf = (LeafNode) node;
            while (true) {
                if (ptr == leaf.getKeySize()) {
                    if (leaf.getRightSibling() == null) break;
                    leaf = (LeafNode) (leaf.getRightSibling());
                    BPTHelper.addIndexNodeReads();
                    ptr = 0;
                }
                if (leaf.getKey(ptr) > maxKey) break;
                Float key = leaf.getKey(ptr);
                resultList.addAll(leaf.getAddressesForKey(key));
                ptr++;
            }
            return (resultList.size() > 0 ? resultList : null);
        } else {
            ptr = node.searchKey(minKey, true);
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