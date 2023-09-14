package indexComponent;

import java.util.ArrayList;
import java.util.List;
import storageComponent.Address;
import storageComponent.Record;
import utils.Parser;
import storageComponent.Database;

public class BPlusTree {

    static final int NODE_SIZE = (Parser.BLOCK_SIZE - Parser.OVERHEAD)/(Parser.POINTER_SIZE + Parser.KEY_SIZE);
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

    public void insertKey(int key, Address add) {
        nodeToInsertTo = findNodeContaining(key);
        ((LeafNode) nodeToInsertTo).addRecord(key, add);
    }

    public LeafNode findNodeContaining(int key) {
        ArrayList<Integer> keys;
        if (BPlusTree.rootNode.isLeaf()) {
            setRoot(rootNode);
            return (LeafNode) rootNode;
        } else {
            Node targetNode = (InternalNode) getRoot();
            while (!((InternalNode) targetNode).getChild(0).isLeaf()) {
                keys = targetNode.getKeys();
                for (int i = keys.size()-1; i>=0; i--) {
                    if (targetNode.getKeyAt(i) <= key) {
                        targetNode = ((InternalNode) targetNode).getChild(i + 1);
                        break;
                    } else if (i == 0) {
                        targetNode = ((InternalNode) targetNode).getChild(0);
                    }
                }
                if (targetNode.isLeaf()) {
                    break;
                }
            }
            keys = targetNode.getKeys();
            for (int i = keys.size()-1; i >= 0; i--) {
                if (keys.get(i) <= key) {
                    return (LeafNode) ((InternalNode) targetNode).getChild(i+1);
                }
            }
            return (LeafNode) ((InternalNode) targetNode).getChild(0);
        }
    }

    private int lowerbound(int key) {
        InternalNode node = (InternalNode) rootNode;
        boolean found = false;
        int lowerbound = 0;
        for (int i = node.keys.size()-1; i >= 0; i--) {
            if (key >= node.getKeyAt(i)) {
                node = (InternalNode) node.getChild(i+1);
                found = true;
                break;
            }
        }
        if (!found && key < node.getKeyAt(0)) {
            node = (InternalNode) node.getChild(0);
        }
        while (!node.getChild(0).isLeaf()) {
            node = (InternalNode) node.getChild(0);
        }
        lowerbound = node.getChild(0).getKeyAt(0);
        return lowerbound;
    }

    public ArrayList<Address> deleteKey(int key) {
        int lowerbound = lowerbound(key);
        return deleteNode(rootNode, null, -1, -1, key, lowerbound);
    }

    public ArrayList<Address> deleteNode(Node node, InternalNode parent, int parentPointerIdx, int parentKeyIdx, int key, int lowerbound) {
        ArrayList<Address> addrToDelete = new ArrayList<>();
        if (node.isLeaf()) {
            LeafNode leafNode = (LeafNode) node;
            int keyIdx = node.searchKeyIndex(key, false);
            if ((keyIdx == leafNode.keys.size()) || (key != leafNode.getKeyAt(keyIdx))) {
                return null;
            }
            addrToDelete.addAll(leafNode.getAddressesForKey(key));
            leafNode.removeKeyAt(keyIdx);
            leafNode.removeKeyFromMap(key);

            int pointerIdx = node.searchKeyIndex(key, true);
            keyIdx = pointerIdx-1;

            LeafNode leafNode2 = (LeafNode) node;
            int newLB = 0;
            if (leafNode2.keys.size() >= (keyIdx+1)) {
                newLB = lowerbound;
                List<Integer> keys = leafNode2.getKeys();
                leafNode2.updateKey(pointerIdx-1, keys.get(0), false, newLB);
            } else {
                newLB = lowerbound(leafNode2.getKeyAt(keyIdx + 1));
                List<Integer> keys = leafNode2.getKeys();
                leafNode2.updateKey(pointerIdx-1, keys.get(0), true, newLB);
            }
        } else {
            InternalNode internalNode = (InternalNode) node;
            int pointerIdx = node.searchKeyIndex(key, true);
            int keyIdx = pointerIdx-1;

            Node child = internalNode.getChild(pointerIdx);
            addrToDelete = deleteNode(child, internalNode, pointerIdx, keyIdx, key, lowerbound);
        }
        if (node.isDeficient(NODE_SIZE)) {
            balanceTree(node, parent, parentPointerIdx, parentKeyIdx);
        }
        return addrToDelete;
    }

    private void balanceTree(Node deficientNode, InternalNode parent, int parentPointerIdx, int parentKeyIdx) throws IllegalStateException {
        if (parent == null) {
            balanceRoot(deficientNode);
        } else if (deficientNode.isLeaf()) {
            balanceLeaf(deficientNode, parent, parentPointerIdx, parentKeyIdx);
        } else if (deficientNode.isInternalNode()) {
            balanceInternal(deficientNode, parent, parentPointerIdx, parentKeyIdx);
        } else {
            throw new IllegalStateException("BALANCE TREE CALLED WRONGLY!");
        }
    }

    public void balanceRoot(Node deficientNode) {
        if (deficientNode.isLeaf()) {
            ((LeafNode) deficientNode).clear();
        } else {
            InternalNode internalNode = (InternalNode) deficientNode;
            Node newRoot = internalNode.getChild(0);
            newRoot.setParent(null);
            rootNode = newRoot;
        }
    }

    public void balanceLeaf(Node deficientNode, InternalNode parent, int parentPointerIdx, int parentKeyIdx) throws IllegalStateException {
        int numChildrenOfNextParent = 0;
        int numChildrenOfNodeParent = 0;
        LeafNode rightSibling = null;
        LeafNode deficientLeaf = (LeafNode) deficientNode;
        if (deficientLeaf.getRightSibling() != null) {
            rightSibling = (LeafNode) deficientLeaf.getRightSibling();
            if (rightSibling.getParent() != null) {
                numChildrenOfNextParent = rightSibling.getParent().getChildren().size();
            }
        }
        LeafNode leftSibling = (LeafNode) deficientLeaf.getLeftSibling();
        if (deficientLeaf.getParent() != null) {
            numChildrenOfNodeParent = deficientNode.getParent().getChildren().size();
        }
        if (rightSibling != null && rightSibling.isLendable(NODE_SIZE)) {
            moveOneKeyLeaf(rightSibling, deficientLeaf, false, parent, parentKeyIdx+1);
        } else if (leftSibling != null && leftSibling.isLendable(NODE_SIZE)) {
            moveOneKeyLeaf(leftSibling, deficientLeaf, true, parent, parentKeyIdx);
        } else if ((leftSibling != null && (leftSibling.getKeys().size() + deficientLeaf.getKeys().size()) <= NODE_SIZE
            && (numChildrenOfNodeParent >= deficientNode.getParent().getMinInternalNodeSize()))) {
                mergeLeafNodes(leftSibling, deficientLeaf, parent, parentKeyIdx, false);
        } else if ((rightSibling != null && (rightSibling.getKeys().size() + deficientLeaf.getKeys().size()) <= NODE_SIZE
            && (numChildrenOfNextParent >= deficientNode.getParent().getMinInternalNodeSize()))) {
                mergeLeafNodes(deficientLeaf, rightSibling, parent, parentKeyIdx + 1, true);
        } else {
            throw new IllegalStateException("Both leaf pointers null while not being root or no common parent not allowed");
        }
    }

    public void balanceInternal(Node deficientNode, InternalNode parent, int parentPointerIdx, int parentKeyIdx) throws IllegalStateException {
        Node deficientINode = deficientNode;

        InternalNode leftSibling = (InternalNode) parent.getChild(parentPointerIdx - 1);
        InternalNode rightSibling = (InternalNode) parent.getChild(parentPointerIdx + 1);


        if (rightSibling == null && leftSibling == null)
            throw new IllegalStateException("Both leftSibling and rightSibling is null for " + deficientINode);

        if (leftSibling != null && leftSibling.isLendable(NODE_SIZE)) {
            moveOneKeyInternal(leftSibling, (InternalNode) deficientINode, true, parent, parentKeyIdx);

        } else if (rightSibling != null && rightSibling.isLendable(NODE_SIZE)) {
            moveOneKeyInternal(rightSibling, (InternalNode) deficientINode, false, parent, parentKeyIdx + 1);

        } else if (leftSibling != null && (deficientINode.getKeySize() + leftSibling.getKeySize()) <= NODE_SIZE) {
            mergeInternalNodes(leftSibling, (InternalNode) deficientINode, parent, parentKeyIdx, true);
        } else if (rightSibling != null && (deficientINode.getKeySize() + rightSibling.getKeySize()) <= NODE_SIZE) {
            mergeInternalNodes((InternalNode) deficientINode, rightSibling, parent, parentKeyIdx + 1, false);
        } else {
            throw new IllegalStateException("Can't merge or redistribute internal node " + deficientINode);
        }
    }

    private void mergeLeafNodes(LeafNode targetNode, LeafNode currNode, InternalNode parent, int inBetweenKeyIdx, boolean mergeToRight) {
        int removedKey = 0;
        int moveKeyCount = currNode.getKeys().size();
        int noChildren = currNode.getParent().getChildren().size();
        for (int i = 0; i < moveKeyCount; i++) {
            removedKey = currNode.removeKeyAt(0);
            int leftLastIdx = targetNode.getKeyAt(targetNode.getKeySize()-1);
            targetNode.insertKeyAt(leftLastIdx + 1, removedKey);
            targetNode.insertByRedistribution(removedKey, currNode.getAddressesForKey(removedKey));
            currNode.removeKeyFromMap(removedKey);
        }
        parent.removeChild(currNode);
        if (parent.getChildren().size() != parent.getKeySize()) {
            parent.removeKeyAt(inBetweenKeyIdx);
        }
        if (mergeToRight) {
            if (currNode.getRightSibling() != null) {
                LeafNode currRightSibling = currNode.getRightSibling();
                currRightSibling.setLeftSibling(currNode.getLeftSibling());
            }
            targetNode.setRightSibling(currNode.getRightSibling());
            if (currNode.getKeySize() == 0) {
                InternalNode currParent = currNode.getParent();
                currParent.removeChild(currNode);
                currParent.removeKeyAt(0);
            }
        } else {
            if (currNode.getLeftSibling() != null) {
                LeafNode currLeftSibling = currNode.getLeftSibling();
                if (currLeftSibling != null && (currLeftSibling.getLeftSibling() != null)) {
                    currLeftSibling.getLeftSibling().setLeftSibling(currNode.getLeftSibling());
                }
            }
            if (currNode.getRightSibling() != null) {
                targetNode.setRightSibling(currNode.getRightSibling());
                currNode.getRightSibling().setLeftSibling(targetNode);
            }
            if (currNode.keys.isEmpty()) {
                InternalNode currParent = currNode.getParent();
                currParent.removeChild(currNode);
                if (inBetweenKeyIdx < 0) {
                    currParent.removeKeyAt(inBetweenKeyIdx+1);
                } else if (currParent.getKeySize() > 0) {
                    currParent.removeKeyAt(inBetweenKeyIdx);
                } else {
                    currParent.removeKeyAt(0);
                }
            } else {
                InternalNode currParent = currNode.getRightSibling().getParent();
                currParent.removeChild(currNode);
                if ((currParent.getKeySize() > currParent.getMinInternalNodeSize()) && (currParent.getChildren().size() > currNode.getMinInternalNodeSize())) {
                    currParent.removeKeyAt(0);
                }
            }
        }

        int lowerbound = lowerbound(removedKey);
        int newLB = 0;
        if (currNode.getParent().getKeySize() >= noChildren) {
            newLB = lowerbound;
        } else {
            newLB = currNode.getParent().getChild(0).getKeyAt(0);
            if (inBetweenKeyIdx != 0) {
                currNode.getParent().updateKey(inBetweenKeyIdx-1, newLB, true, newLB);
            }
        }

    }

    private void mergeInternalNodes (InternalNode targetNode, InternalNode currNode, InternalNode parent, int inBetweenKeyIdx, boolean mergeWithLeft) {
        int targetKey;
        int moveKeyCount = currNode.getKeySize();
        if (mergeWithLeft) {
            targetKey = targetNode.getChild(targetNode.getKeySize()).getLastKey();
            for (int i = 0; i < moveKeyCount; i++) {
                targetNode.getKeys().add(targetNode.getKeySize(), currNode.getKeyAt(i));
            }
            for (int i = 0; i < currNode.getChildren().size(); i++) {
                targetNode.getChildren().add(currNode.getChild(i));
            }
            targetNode.getKeys().add(targetNode.getKeySize(), targetNode.getChild(targetNode.getKeySize()+1).getKeyAt(0));
            currNode.getParent().removeChild(currNode);
        } else {
            targetKey = currNode.getKeyAt(0);
            for (int i = 0; i < moveKeyCount; i++) {
                targetNode.getKeys().add(0, currNode.getKeyAt(i));
            }
            for (int i = 0; i < currNode.getChildren().size(); i++) {
                targetNode.getChildren().add(currNode.getChild(i));
            }
            targetNode.getKeys().add(0, targetNode.getChild(1).getKeyAt(0));
            currNode.getParent().removeChild(currNode);
        }

        int pointerIdx = targetNode.searchKeyIndex(targetKey, true);
        int keyIdx = pointerIdx - 1;

        InternalNode iNode = (InternalNode) targetNode;
        int lowerbound = lowerbound(targetKey);
        int newLB = 0;
        if (iNode.getKeySize() >= (keyIdx + 1)) {
            newLB = lowerbound;
        } else {
            newLB = lowerbound(iNode.getKeyAt(keyIdx+1));
            parent.updateKey(inBetweenKeyIdx-1, targetKey, false, lowerbound(targetKey));
        }
    }

    private void moveOneKeyLeaf(LeafNode donor, LeafNode receiver, boolean leftDonor, InternalNode parent, int inBetweenKeyIdx) {
        int key;
        if (leftDonor) {
            int donorKey = donor.getKeyAt(donor.getKeySize()-1);
            receiver.insertByRedistribution(donorKey, donor.getAddressesForKey(donorKey));
            donor.removeKeyFromMap(donorKey);

            receiver.insertKeyAt(0, donorKey);
            donor.removeKeyAt(donor.getKeySize()-1);
            key = donor.getKeyAt(0);
        } else {
            int donorKey = donor.getKeyAt(0);
            receiver.insertByRedistribution(donorKey, donor.getAddressesForKey(donorKey));
            donor.removeKeyFromMap(donorKey);

            receiver.insertKeyAt(receiver.getKeySize(), donorKey);
            donor.removeKeyAt(0);
            key = donor.getKeyAt(0);
        }

        // TODO VERIFY
        if (inBetweenKeyIdx == -1) {
            // pass
        } else if (inBetweenKeyIdx >= 0) {
            if (parent.getKeySize() == inBetweenKeyIdx) {
                parent.replaceKeyAt(inBetweenKeyIdx-1, key);

                int lastParentChild = donor.getParent().getKeySize() - 1;
                int lastParentChildKey = donor.getParent().getChild(donor.getParent().getKeySize()).getKeyAt(0);
                if (donor.getParent().getChild(donor.getParent().getChildren().size() - 1).getKeyAt(0) != key) {
                    receiver.getParent().replaceKeyAt(lastParentChild, lastParentChildKey);
                }
            } else {
                parent.replaceKeyAt(inBetweenKeyIdx, key);
                if (donor.getParent().getChild(inBetweenKeyIdx+1).getKeyAt(0) != key) {
                    donor.getParent().replaceKeyAt(inBetweenKeyIdx, donor.getParent().getChild(inBetweenKeyIdx+1).getKeyAt(0));
                }
            }
        } else {
            parent.replaceKeyAt(inBetweenKeyIdx-1, key);
        }

        int pointerIdx = receiver.searchKeyIndex(key, true);
        int keyIdx = pointerIdx-1;

        LeafNode leafNode = (LeafNode) receiver;
        int lowerbound = lowerbound(key);
        int newLB = 0;

        if (leafNode.getKeySize() >= (keyIdx + 1)) {
            newLB = lowerbound;
        } else {
            newLB = lowerbound(leafNode.getKeyAt(keyIdx+1));
            parent.updateKey(inBetweenKeyIdx-1, parent.getChild(inBetweenKeyIdx).getKeyAt(0), false, lowerbound(key));
        }
    }

    private void moveOneKeyInternal(InternalNode donor, InternalNode receiver, boolean leftDonor, InternalNode parent, int inBetweenKeyIdx) {
        int key;

        if (leftDonor) {
            donor.removeKeyAt(donor.getKeySize() - 1);
            Node targetNode = donor.getChild(donor.getKeySize());
            donor.removeChild(targetNode);

            receiver.addChild(targetNode);
            receiver.getKeys().add(receiver.getKeySize(), receiver.getChild(1).getKeyAt(0));
            key = receiver.getKeyAt(0);
        } else {
            donor.removeKeyAt(0);
            Node targetNode = donor.getChild(0);
            donor.removeChild(targetNode);

            receiver.addChild(targetNode);
            receiver.getKeys().add(receiver.getKeySize(), receiver.getChild(1).getKeyAt(0));
            key = receiver.getKeyAt(0);
        }

        int ptrIdx = receiver.searchKeyIndex(key, true);
        int keyIdx = ptrIdx - 1;

        InternalNode iNode = receiver;
        int lowerbound = lowerbound(key);
        int newLB = 0;

        if (iNode.getKeySize() >= (keyIdx + 1)) {
            newLB = lowerbound;
        } else {
            newLB = lowerbound(iNode.getKeyAt(keyIdx + 1));
            parent.updateKey(inBetweenKeyIdx - 1, key, false, lowerbound(key));
        }
        parent.replaceKeyAt(inBetweenKeyIdx, newLB);

    }

    public static Node getRoot() {
        return rootNode;
    }
    public static void setRoot(Node node) {
        rootNode = node;
        rootNode.setRoot(true);
    }
}