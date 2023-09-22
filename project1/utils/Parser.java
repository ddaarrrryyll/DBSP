package utils;

import indexComponent.BPlusTree;

import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

import storageComponent.Address;
import storageComponent.Record;
import storageComponent.Database;

import indexComponent.*;

public class Parser {
    public static final int BLOCK_SIZE = 400;
    public static final int OVERHEAD = 8; // TODO may not need
    public static final int POINTER_SIZE = 8; // for 64-bit systems
    public static final int KEY_SIZE = 4; // Integer datatype
    private static int counter = 0;

    public static void readTXTFile(String filePath, int diskCapacity) {
        try {
            String line;
            Database db = new Database(diskCapacity, BLOCK_SIZE);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            reader.readLine(); // skip the first line (the column line)
            int invalidDataCount = 0;

            BPlusTree tree = new BPlusTree();

            while ((line = reader.readLine()) != null) {
                counter++;
                if (counter % 100000 == 0)
                    System.out.println(counter + " data rows read");
                String[] fields = line.split("\t");
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                try {
                    Date gameDate = formatter.parse(fields[0]);
                    int gameDateEst = Integer.parseInt(Long.toString(gameDate.getTime() / 1000));
                    int teamIdHome = Integer.parseInt(fields[1]);
                    int ptsHome_int = Integer.parseInt(fields[2]);
                    byte ptsHome = (byte) (ptsHome_int & 0xFF);
                    float fgPctHome = Float.parseFloat(fields[3]);
                    float fg3PctHome = Float.parseFloat(fields[5]);
                    byte astHome = Byte.parseByte(fields[6]);
                    byte rebHome = Byte.parseByte(fields[7]);
                    byte homeTeamWins = Byte.parseByte(fields[8]);
                    Record record = createRecord(gameDateEst, teamIdHome, ptsHome, fgPctHome, fg3PctHome, astHome, rebHome, homeTeamWins);
                    Address addr = db.writeRecordToStorage(record);
                    // int key = rec.getNumVotes();
                    // tree.insertKey(key, addr);
                } catch (Exception e) { // handles empty cells + parse exception
                    invalidDataCount++;
                }
                
            }
            reader.close();
            System.out.println(invalidDataCount + " tuples skipped due to invalid data");
            
            try {
                int experimentNum = 0;
                Scanner sc = new Scanner(System.in);
                while (true) {
                    try {
                        System.out.println("\nChoose Experiment (1-5):");
                        experimentNum = sc.nextInt();
                        if (experimentNum > 0 && experimentNum < 6) {
                            break;
                        } else {
                            System.out.println("\nPlease only input 1-5!");
                        }
                    } catch (Exception e) {
                        // e.printStackTrace();
                        System.out.println("\nPlease only input 1-5!");
                        break;
                    }
                }
                
                switch (experimentNum) {
                    case 1:
                        db.experimentOne();
                        break;
                    // case 2:
                    //     BPlusTree.experimentTwo(tree);
                    //     break;
                    // DIFFERENT
                    // case 3:
                    //     BplusTree.experimentThree(db, tree);
                    //     break;
                    // DIFFERENT
                    // case 4:
                    //     BplusTree.experimentFour(db, tree);
                    //     break;
                    // DIFFERENT
                    // case 5:
                    //     BplusTree.experimentFive(db, tree);
                    //     break;
                }

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
        return new Record(gameDateEst, teamIdHome, ptsHome, fgPctHome, fg3PctHome, astHome, rebHome, homeTeamWins);
    }

}