package indexComponent;

public class BPTHelper {

    private static int nodeCount;
    private static int treeDegree;
    private static int totalNodeReads;
    private static int totalRangeNodeReads;

    static void addNode() {
        nodeCount++;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    // TODO DELETE NEED TO SEE IF USEFUL


    public static void deleteOneTreeDegree() {
        treeDegree--;
    }

 
    public int getNodeReads() {
        return totalNodeReads;
    }


    static void addNodeReads() {
        totalNodeReads++;
    }

 
    public int getIndexNodeReads() {
        return totalRangeNodeReads;
    }


    static void addIndexNodeReads() {
        totalRangeNodeReads++;
        addNodeReads();
    }

}
