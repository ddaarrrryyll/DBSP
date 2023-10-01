package utils;

import indexComponent.BPlusTree;

import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import storageComponent.Address;
import storageComponent.Record;
import storageComponent.Database;

public class Parser {
    public static final int BLOCK_SIZE = 400;
    public static final int POINTER_SIZE = 8; // pointer size for 64 bit systems = 8 bytes
    public static final int KEY_SIZE = 4; // type(key) = float 4 bytes
    private static int counter = 0;

    public static void readTXTFile(String filePath, int diskCapacity) {
        try {
            String line;
            Database db = new Database(diskCapacity, BLOCK_SIZE);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            // throw header away
            reader.readLine();
            int invalidDataCount = 0;

            BPlusTree bPlusTree = new BPlusTree();

            while ((line = reader.readLine()) != null) {
                counter++;
                if (counter % 10000 == 0)
                    System.out.println(counter + " data rows read");
                String[] tuple = line.split("\t");
                try {
                    Record row = parseTuple(tuple);                
                    // if (counter == 1) {
                    //     System.out.println(record.toString());
                    // }
                    Address addr = db.writeRecordToStorage(row);
                    float key = row.getFgPctHome();
                    // do multiple inserts, another option is bulk loading
                    bPlusTree.insertKeyAddrPair(key, addr);
                } catch (Exception e) { // handles empty cells + parse exception
                    invalidDataCount++;
                }
                
            }
            reader.close();
            System.out.println(invalidDataCount + " tuples skipped due to invalid data");
            
            System.out.println("Running experiments 1 - 5 sequentially... (please wait for 1-2s before each experiment executes)");
            try {
                db.ex1();
                TimeUnit.SECONDS.sleep(2);
                BPlusTree.ex2(bPlusTree);
                TimeUnit.SECONDS.sleep(2);
                BPlusTree.ex3(db, bPlusTree);
                TimeUnit.SECONDS.sleep(2);
                BPlusTree.ex4(db, bPlusTree);
                TimeUnit.SECONDS.sleep(2);
                bPlusTree.ex5(db, bPlusTree);
            } catch (InterruptedException e) {
                System.out.println("User interrupted program, exiting run time");
            }
            System.out.println("\n@@@@@ Execution complete @@@@@\n");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Record parseTuple(String[] tuple) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date gameDate = formatter.parse(tuple[0]);
        int gameDateEst = Integer.parseInt(Long.toString(gameDate.getTime() / 1000));
        // sanity check TO REMOVE
        // if (counter == 1) {
        //     Date date = new Date((long) gameDateEst * 1000);
        //     System.out.println(formatter.format(date));
        // }
        int teamIdHome = Integer.parseInt(tuple[1]);
        int ptsHome_int = Integer.parseInt(tuple[2]);
        byte ptsHome = (byte) (ptsHome_int & 0xFF);
        float fgPctHome = Float.parseFloat(tuple[3]);
        float fg3PctHome = Float.parseFloat(tuple[5]);
        byte astHome = Byte.parseByte(tuple[6]);
        byte rebHome = Byte.parseByte(tuple[7]);
        byte homeTeamWins = Byte.parseByte(tuple[8]);
        return createRecord(gameDateEst, teamIdHome, ptsHome, fgPctHome, fg3PctHome, astHome, rebHome, homeTeamWins);    
    }

    public static Record createRecord(int gameDateEst, int teamIdHome, byte ptsHome, 
                                        float fgPctHome, float fg3PctHome, 
                                        byte astHome, byte rebHome, byte homeTeamWins) {
        return new Record(gameDateEst, teamIdHome, ptsHome, fgPctHome, fg3PctHome, astHome, rebHome, homeTeamWins);
    }

}