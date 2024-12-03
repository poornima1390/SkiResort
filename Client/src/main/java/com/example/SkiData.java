package com.example;

public class SkiData {
    public Integer getSkierID() {
        return skierID;
    }

    public Integer getLiftID() {
        return liftID;
    }

    public Integer getResortID() {
        return resortID;
    }

    public Integer getSeasonID() {
        return seasonID;
    }

    public Integer getDayID() {
        return dayID;
    }

    public Integer getTime() {
        return time;
    }

    private Integer skierID, liftID, resortID, seasonID, dayID, time;

    public SkiData(Integer skierID1,Integer resortID1, Integer liftID1, Integer seasonID1, Integer dayID1, Integer time1){
        skierID=skierID1;
        resortID=resortID1;
        liftID=liftID1;
        seasonID= seasonID1;
        dayID=dayID1;
        time=time1;

    }
}
