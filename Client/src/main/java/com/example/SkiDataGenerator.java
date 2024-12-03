package com.example;

import com.google.gson.JsonObject;
import java.util.Random;

public class SkiDataGenerator {
    private static final Random random = new Random();

    public static String generateSkiEvent() {
        SkiData skii=new SkiData(random.nextInt(100000) + 1,random.nextInt(10) + 1,random.nextInt(40) + 1,2024,1,random.nextInt(360) + 1);
        JsonObject skiEvent = new JsonObject();
        skiEvent.addProperty("skierID", skii.getSkierID());
        skiEvent.addProperty("resortID", skii.getResortID());
        skiEvent.addProperty("liftID", skii.getLiftID());
        skiEvent.addProperty("seasonID", skii.getSeasonID());
        skiEvent.addProperty("dayID", skii.getDayID());
        skiEvent.addProperty("time", skii.getTime());

        return skiEvent.toString();
    }
}
