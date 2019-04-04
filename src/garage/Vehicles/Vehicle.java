package garage.Vehicles;

import java.io.Serializable;
import java.util.Random;

abstract public class Vehicle implements Serializable {

    private String name, chassisNumber, engineNumber, registrationNumber, imageURI;

    public Vehicle() {
        Random randomNumberGenerator = new Random();
        name = ("Random_Vehicle" + randomNumberGenerator.nextInt(100000));
        chassisNumber = Integer.toString(randomNumberGenerator.nextInt(1000000000));
        engineNumber = Integer.toString(randomNumberGenerator.nextInt(10000000));
        registrationNumber = "Registration" + Integer.hashCode(randomNumberGenerator.nextInt(2000000000));
    }

    public Vehicle(String name, String chassisNumber, String engineNumber, String registrationNumber, String imageURI) {
        this.name = name;
        this.chassisNumber = chassisNumber;
        this.engineNumber = engineNumber;
        this.registrationNumber = registrationNumber;
        this.imageURI = imageURI;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChassisNumber() {
        return chassisNumber;
    }

    public void setChassisNumber(String chassisNumber) {
        this.chassisNumber = chassisNumber;
    }

    public String getEngineNumber() {
        return engineNumber;
    }

    public void setEngineNumber(String engineNumber) {
        this.engineNumber = engineNumber;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getImageURI() {
        return imageURI;
    }

    public void setImageURI(String imageURI) {
        this.imageURI = imageURI;
    }


    public String toString() {
        return name + " " + chassisNumber + " " + engineNumber + " " + registrationNumber;
    }
}