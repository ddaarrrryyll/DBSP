package DBSP.project1;

import DBSP.project1.storageComponent.Record;

public class RecordTest {
    
    public static void main(String[] args) {
        // Create a new Record instance
        Record record1 = new Record(
            Record.dateToInt("15/03/2022"), 
            1610612737, 
            (byte) 100, 
            0.5f, 
            0.4f, 
            (byte) 20, 
            (byte) 50, 
            (byte) 1
        );
       
        
        // Print out the record
        System.out.println(record1);
        
        // Test date conversion
        String dateStr = "15/03/2022";
        int dateInt = Record.dateToInt(dateStr);
        System.out.println("Converted date string to int: " + dateInt);
        System.out.println("Converted int back to date string: " + Record.intToDate(dateInt));
    }

}
