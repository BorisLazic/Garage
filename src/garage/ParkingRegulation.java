package garage;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

public class ParkingRegulation {

    private ConcurrentHashMap<String, Instant> carsThatEntered;
    private static int hourInSeconds = 3600;
    private static final Integer writeSync = 6969;

    public ParkingRegulation() {
        carsThatEntered = new ConcurrentHashMap<>();
    }

    public void startParkingMeter(String registrationNumber) {
        carsThatEntered.put(registrationNumber, Instant.now());
    }

    public void endParkingMeterAndCharge(String registrationsNumber) {
        Instant end = Instant.now();
        Duration parkingTime = Duration.between(carsThatEntered.get(registrationsNumber),end);
        String amountCharged;
        if(parkingTime.toMillis()/1000 < hourInSeconds)
            amountCharged = "1KM";
        else if(parkingTime.toMillis()/1000 < hourInSeconds*3)
            amountCharged = "3KM";
        else
            amountCharged = "8KM";

        synchronized (writeSync) {
            try(PrintWriter writeBill = new PrintWriter(new FileOutputStream(new File(Administrator.appFilesPath + "BilledCars.txt"),true))) {
                writeBill.println(LocalDateTime.now().toString()+": " + registrationsNumber + " was charged " + amountCharged);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
