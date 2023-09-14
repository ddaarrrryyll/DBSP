package storageComponent;

import java.time.DateTimeException;
import java.time.LocalDate;

// Total size of record: 20 bytes
public class Record {
    private int gameDateEst; // 4 bytes
    private int teamIdHome; // 4 bytes
    private byte ptsHome; // 1 byte
    private float fgPctHome; // 4 bytes
    private float fg3PctHome; // 4 bytes
    private byte astHome; // 1 byte 
    private byte rebHome; // 1 byte
    private byte homeTeamWins; // 1 byte

    public Record(int gameDateEst, int teamIdHome, byte ptsHome, 
                  float fgPctHome, float fg3PctHome, 
                  byte astHome, byte rebHome, byte homeTeamWins) {
        this.gameDateEst = gameDateEst; 
        this.teamIdHome = teamIdHome;
        this.ptsHome = ptsHome;
        this.fgPctHome = fgPctHome;
        this.fg3PctHome = fg3PctHome;
        this.astHome = astHome;
        this.rebHome = rebHome;
        this.homeTeamWins = homeTeamWins;
    }

    // This is assuming all records must be NOT NULL
    public Record() {
        this.gameDateEst = 0;
        this.teamIdHome = 0;
        this.ptsHome = 0;
        this.fgPctHome = 0.0f;
        this.fg3PctHome = 0.0f;
        this.astHome = 0;
        this.rebHome = 0;
        this.homeTeamWins = 0;
    }

    public static int getRecordSize() {
        return 20;
    }

    // Getters
    public int getGameDateEst() {
        return gameDateEst;
    }

    public int getTeamIdHome() {
        return teamIdHome;
    }

    public byte getPtsHome() {
        return ptsHome;
    }

    public float getFgPctHome() {
        return fgPctHome;
    }

    public float getFg3PctHome() {
        return fg3PctHome;
    }

    public byte getAstHome() {
        return astHome;
    }

    public byte getRebHome() {
        return rebHome;
    }

    public byte getHomeTeamWins() {
        return homeTeamWins;
    }

    // Setter 
    public void setGameDateEst(int gameDateEst) {
        this.gameDateEst = gameDateEst;
    }

    public void setTeamIdHome(int teamIdHome) {
        this.teamIdHome = teamIdHome;
    }

    public void setPtsHome(byte ptsHome) {
        this.ptsHome = ptsHome;
    }

    public void setFgPctHome(float fgPctHome) {
        this.fgPctHome = fgPctHome;
    }

    public void setFg3PctHome(float fg3PctHome) {
        this.fg3PctHome = fg3PctHome;
    }

    public void setAstHome(byte astHome) {
        this.astHome = astHome;
    }

    public void setRebHome(byte rebHome) {
        this.rebHome = rebHome;
    }

    public void setHomeTeamWins(byte homeTeamWins) {
        this.homeTeamWins = homeTeamWins;
    }

    @Override
    public String toString() {
        return "Record {" +
               "\n\tGAME_DATE_EST: " + intToDate(gameDateEst) + 
               "\n\tTEAM_ID_home: " + teamIdHome +
               "\n\tPTS_home: " + ptsHome +
               "\n\tFG_PCT_home: " + fgPctHome +
               "\n\tFG3_PCT_home: " + fg3PctHome +
               "\n\tAST_home: " + astHome +
               "\n\tREB_home: " + rebHome +
               "\n\tHOME_TEAM_WINS: " + homeTeamWins +
               "\n}";
    }

    public static int dateToInt(String date) {
        if (date == null || date.length() != 10 || date.charAt(2) != '/' || date.charAt(5) != '/') {
            throw new IllegalArgumentException("Invalid date format. Expected DD/MM/YYYY format.");
        }

        int day = Integer.parseInt(date.substring(0, 2));
        int month = Integer.parseInt(date.substring(3, 5));
        int year = Integer.parseInt(date.substring(6, 10));

        // Validating the date using LocalDate
        try {
            LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid date value provided: " + date);
        }

        return day * 1000000 + month * 10000 + year;
    }
    
    public static String intToDate(int dateInt) {
        int day = dateInt / 1000000;                    // Extracting day
        int month = (dateInt / 10000) % 100;            // Extracting month
        int year = dateInt % 10000;                     // Extracting year
    
        // Validating the date using LocalDate
        try {
            LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid date integer provided: " + dateInt);
        }
    
        return String.format("%02d/%02d/%d", day, month, year);
    }
    
}

