import utils.Parser;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;



/**
 * Main entry into the program
 */
public class Main {
    private static final int DEFAULT_MAX_DISK_CAPACITY = 500 * (int) (Math.pow(10, 6));

    public static void main(String[] args) throws Exception {

        String separator = System.getProperty("file.separator");
        String filePath = new File("").getAbsolutePath();
        filePath = filePath.concat(separator + "DBSP" + separator + "project1" + separator + "games.txt");
        // filePath = filePath.concat(separator + "CZ4031 Project 1 Code" + separator + "src" + separator + "data.tsv");
        System.out.print(filePath + "\n");
        File file = new File(String.valueOf(filePath));
        if (file.exists()) {
            System.out.print("Reading data...\n");
            int diskSize = getDiskInput();
            Parser.readTXTFile(String.valueOf(filePath), diskSize);
        } else if (!file.exists()) {
            Scanner sc = new Scanner(System.in);
            System.out.print("Default file path failed! Please input the absolute file path of games.txt: ");
            filePath = sc.nextLine();
            File newFileCheck = new File(String.valueOf(filePath));
            if (newFileCheck.exists()) {
                System.out.print("Reading data...\n");
                int diskSize = getDiskInput();
                Parser.readTXTFile(String.valueOf(filePath), diskSize);
            }
        } else {
            throw new FileNotFoundException("File does not exist!");
        }
    }

    /**
     * The getDiskInput method prompts the user to input a disk size between 200 and 500 MB, and returns the disk size in bytes.
     * If the user does not enter a valid disk size within three attempts, the method returns the default disk size.
     *
     * @return the disk size
     */
    private static int getDiskInput() {
        int n = 0;
        Scanner sc = new Scanner(System.in);
        while (n < 3) {
            try {
                System.out.print("Enter Disk Size (size must be between 200-500MB): ");
                int diskSize = sc.nextInt();
                if (diskSize < 200 || diskSize > 500) {
                    n++;
                } else {
                    sc.close();
                    return diskSize * (int) (Math.pow(10, 6));
                }
            } catch (IndexOutOfBoundsException e) {
                System.out.printf("No argument detected, falling back to default disk size: %d\n", DEFAULT_MAX_DISK_CAPACITY);
                break;
            } catch (NumberFormatException e) {
                System.out.printf("Invalid disk size input detected, falling back to default disk size: %d\n", DEFAULT_MAX_DISK_CAPACITY);
                break;
            } catch (Exception e) {
                System.out.printf("Something went wrong, falling back to default disk size: %d\n", DEFAULT_MAX_DISK_CAPACITY);
                break;
            }
        }
        sc.close();
        return DEFAULT_MAX_DISK_CAPACITY;
    }
}
