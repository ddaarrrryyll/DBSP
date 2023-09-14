package storageComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of an unspanned and unclustered disk class -
 * each record must be stored as a whole in single block
 */
public class Database {
    //an array of Block objects representing the memory pool of the disk.
    private Block[] blocks;
    //a set of integers representing the indices of the available blocks in the memory pool.
    private Set<Integer> availableBlocks;
    //an integer representing the number of block accesses made by the disk.
    private static int blockAccesses = 0;
    //a set of integers representing the indices of the filled blocks in the memory pool.
    private Set<Integer> filledBlocks;
    int memdiskSize; //an integer representing the size of the disk in bytes.
    int blkSize; // an integer representing the size of each block in bytes.
    private int numOfRecords = 0; // an integer representing the number of records stored in the disk.

    // an integer representing the number of block accesses that were reduced due to the presence
    // of the LRU cache.
    private int blockAccessReduced = 0;
  

    public Database(int diskSize, int blkSize) {
        this.memdiskSize = diskSize;
        this.blkSize = blkSize;
        this.blocks = new Block[diskSize / blkSize];
        this.availableBlocks = new HashSet<>();
        this.filledBlocks = new HashSet<>();
        // initialise all available blocks in hashMap
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = new Block(blkSize);
            availableBlocks.add(i);
        }
    }

    /**
     * Writes the given record to storage by inserting it into the first available block
     * and returns the address of the newly stored record.
     *
     * @param rec the record to be stored
     * @return the {@link Address} of the newly stored record
     */
    public Address writeRecordToStorage(Record rec) {
        numOfRecords++;
        int blockPtr = getFirstAvailableBlockId();
        Address addressofRecordStored = this.insertRecordIntoBlock(blockPtr, rec);
        return addressofRecordStored;
    }

    /**
     * Returns the total number of records stored in the storage system.
     *
     * @return the number of records stored in the system
     */
    public int getNumberOfRecords() {
        return numOfRecords;
    }

    /**
     * Searches through all the blocks on the disk to locate the first block that
     * is currently available for storing additional records.
     * The method then returns the block number of this available block.
     *
     * @return blockNumber of the first available block on Disk.
     */
    private int getFirstAvailableBlockId() {
        if (availableBlocks.isEmpty()) {
            return -1;
        }
        return availableBlocks.iterator().next();
    }

    /***
     * Writes a record to the current block, pointed to by blockPrt
     * @param blockPtr position to write to within the block
     * @param rec record to write to
     */
    private Address insertRecordIntoBlock(int blockPtr, Record rec) {
        if (blockPtr == -1) {
            return null;
        }
        int offset = blocks[blockPtr].insertRecordIntoBlock(rec);
        filledBlocks.add(blockPtr);
        if (!blocks[blockPtr].isBlockAvailable()) {
            availableBlocks.remove(blockPtr);
        }
        return new Address(blockPtr, offset);
    }

    
    /** 
     * @return int
     */
    public int getNumberBlockUsed() {
        return filledBlocks.size();
    }

    
    /** 
     * @return int
     */
    public int getBlockAccesses() {
        return blockAccesses;
    }


    /**
     * Retrieves a block from Disk, either from the LRU cache or directly from Disk, given its block number.
     * If the block is on cache, return that block.
     * Else, load the block onto the cache from the Disk.
     *
     * @param blockNumber The block number to be retrieved.
     * @return The requested Block object.
     */
    private Block getBlock(int blockNumber) {
        Block block = blocks[blockNumber];
        blockAccesses++;
        return block;
    }

    /**
     * Retrieves the number of block accesses reducted due to cache hits
     *
     * @return number of block accessed reduced
     */
    public int getBlockAccessReduced() {
        return blockAccessReduced;
    }


    /**
     * Retrieves the record stored at the specified address.
     *
     * @param add The address of the record to retrieve.
     * @return The record stored at the specified address.
     */
    public Record getRecord(Address add) {
        Block block = getBlock(add.getBlockId());
        return block.getRecord(add.getOffset());
    }

    /**
     * Deletes the record at the specified address  on Disk.
     *
     * @param ArrayList of records to be deleted
     */
    public void deleteRecord(ArrayList<Address> addList) {
        for (Address add : addList) {
            int blockId = add.getBlockId();
            int offset = add.getOffset();
            Block block = getBlock(blockId);
            block.deleteRecord(offset);
            if (filledBlocks.contains(blockId)) {
                filledBlocks.remove(blockId);
            }
            availableBlocks.add(blockId);
        }
    }


    public void experimentOne() {
        System.out.println("\n----------------------EXPERIMENT 1-----------------------");
        System.out.printf("Number of Records: %d\n", this.getNumberOfRecords());
        System.out.println(String.format("Record Size: %d Bytes", Record.getRecordSize()));
        System.out.printf("Number of Records in each Block: %d\n", Block.getTotalRecords());
        System.out.println(String.format("Number of Blocks for storage: %d\n", this.getNumberBlockUsed()));
    }
}