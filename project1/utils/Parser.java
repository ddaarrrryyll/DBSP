package utils;

import indexComponent.BPlusTree;
import java.util.Scanner;

import java.io.*;

import storageComponent.Address;
import storageComponent.Record;
import storageComponent.Database;

import indexComponent.*;

public class Parser {
    public static final int BLOCK_SIZE = 400;
    public static final int OVERHEAD = 8;
    public static final int POINTER_SIZE = 8; // for 64-bit systems
    public static final int KEY_SIZE = 4; // Integer datatype
    private static int counter = 0;

    public static void readTSVFile(String filePath, int diskCapacity) {
        try {
            String line;
            // initialise database
            Database db = new Database(diskCapacity, BLOCK_SIZE);
            // start loading data
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            reader.readLine(); // skip the first line (the column line)

            // initialise a new B+ tree
            BPlusTree tree = new BPlusTree();

            while ((line = reader.readLine()) != null) {
                counter++;
                if (counter % 100000 == 0)
                    System.out.println(counter + " data rows read");
                String[] fields = line.split("\t");
                // need to change for game date to remove the dashes
                int gameDateEst = Integer.parseInt(fields[0]);
                int teamIdHome = Integer.parseInt(fields[1]);
                byte ptsHome = Byte.parseByte(fields[2]);
                float fgPctHome = Float.parseFloat(fields[3]);
                float fg3PctHome = Float.parseFloat(fields[5]);
                byte astHome = Byte.parseByte(fields[6]);
                byte rebHome = Byte.parseByte(fields[7]);
                byte homeTeamWins = Byte.parseByte(fields[8]);

                /*
                Record rec = createRecord(gameDateEst, teamIdHome, ptsHome, fgPctHome, fg3PctHome, astHome, rebHome, homeTeamWins);
                Address add = db.writeRecordToStorage(rec);
                int key = rec.getNumVotes();
                tree.insertKey(key, add);
                
                Temperoraily commented out to test the B+ tree
                */ 

            }
            reader.close();

            // TODO: to run the experiments independently of one another
            // Choose Experiment number
            try {

                int index = 0;
                while (true) {
                    try {
                        System.out.println("\nChoose Experiment (1-5):");
                        Scanner sc = new Scanner(System.in);
                        index = sc.nextInt();

                        if (index > 0 && index < 6) {
                            break;
                        } else {
                            System.out.println("\nPlease only input 1-5!");
                        }
                    } catch (Exception e) {
                        System.out.println("\nPlease only input 1-5!");
                    }
                }
                
                /*
                switch (index) {
                    case 1:
                        db.experimentOne();
                        break;
                    case 2:
                        BplusTree.experimentTwo(tree);
                        break;
                    case 3:
                        BplusTree.experimentThree(db, tree);
                        break;
                    case 4:
                        BplusTree.experimentFour(db, tree);
                        break;
                    case 5:
                        BplusTree.experimentFive(db, tree);
                        break;
                }
                */

            } catch (Exception e) {

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * for each line of data read in create a record object and stores it into the
     * database
     *
     * @param teamID        numeric unique identifier of the team
     * @param FG_PCT_home   
     * @param FG3_PCT_home  
     */
    public static Record createRecord(int gameDateEst, int teamIdHome, byte ptsHome, 
                                        float fgPctHome, float fg3PctHome, 
                                        byte astHome, byte rebHome, byte homeTeamWins) {
        Record rec = new Record(gameDateEst, teamIdHome, ptsHome, fgPctHome, fg3PctHome, astHome, rebHome, homeTeamWins);
        return rec;
    }

}