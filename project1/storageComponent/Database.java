package storageComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class Database {
    
    private Block[] blocks;
    private Set<Integer> availableBlocks;
    private Set<Integer> filledBlocks;
    int diskSize;
    int blkSize;
    private int numRecords = 0;

    private static int blockAccesses = 0;

    // TODO MAYBE REMOVE
    // an integer representing the number of block accesses that were reduced due to the presence
    // of the LRU cache.
    private int blockAccessReduced = 0;
  

    public Database(int diskSize, int blkSize) {
        this.diskSize = diskSize;
        this.blkSize = blkSize;
        this.blocks = new Block[diskSize / blkSize];
        this.availableBlocks = new HashSet<>();
        this.filledBlocks = new HashSet<>();
        // all block are avail at the start
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = new Block(blkSize);
            availableBlocks.add(i);
        }
    }

    
    public Address writeRecordToStorage(Record rec) {
        numRecords++;
        int blockPtr = getFirstAvailableBlockId();
        return this.insertRecordIntoBlock(blockPtr, rec);
    }

    public int getNumberOfRecords() {
        return numRecords;
    }

    private int getFirstAvailableBlockId() {
        if (availableBlocks.isEmpty()) {
            return -1;
        }
        return availableBlocks.iterator().next();
    }

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
    public int getFilledBlocksCount() {
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
        System.out.println(String.format("Number of Blocks for storage: %d\n", this.getFilledBlocksCount()));
    }
}