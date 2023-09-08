package DBSP.project1;

public class Database {
    private Block[] database = new Block[262144];
    

    // convert each row into a Record obj using record constructor for insertion
    // for block in database:
        // insert 16 records into block using block constructor
}

class Block {
    private Record[] records = new Record[16];
    // make constructor that takes in record array and set to this.records
}

class Record {
    // declare byte array for each column according to specs (date should be 4 bytes or less, rmb to stay within 25bytes)
    // make constructor to take in the values in each column so Database obj can call
}
