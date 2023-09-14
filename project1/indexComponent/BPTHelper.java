package indexComponent;

public class BPTHelper {
    private static int totalNode;
    private static int treeDegree;

    public int getTotalNodes() {
        return totalNode;
    }

    static void addNode() {
        totalNode++;
    }

    public int getTreeDegree() {
        return treeDegree;
    }

    public static void addTreeDegree() {
        treeDegree++;
    }

    public static void deleteTreeDegree() {
        treeDegree--;
    }
}
