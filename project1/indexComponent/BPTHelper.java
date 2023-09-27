package indexComponent;

public class BPTHelper {

    private static int nodeCount;
    private static int treeDegree;
    private static int nodeReads;
    private static int nodeReadsEx4;

    static void addNode() {
        nodeCount++;
    }

    public int getNodeCount() {
        return nodeCount;
    }


    public static void deleteOneTreeDegree() {
        treeDegree--;
    }

 
    public int getNodeReads() {
        return nodeReads;
    }


    static void addNodeReads() {
        nodeReads++;
    }

 
    public int getNodeReadsEx4() {
        return nodeReadsEx4;
    }


    static void addIndexNodeReads() {
        nodeReadsEx4++;
        addNodeReads();
    }

}
