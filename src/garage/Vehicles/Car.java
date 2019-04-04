package garage.Vehicles;

import garage.Administrator;

import java.io.Serializable;
import java.util.Random;

public class Car extends Vehicle implements Serializable {
    private int doorQuantity;

    public Car(){
        super();
        Random randomNumberGenerator = new Random();
        this.setImageURI(Administrator.appFilesPath + "Car" + randomNumberGenerator.nextInt(3) + ".jpg");

        doorQuantity = 4;
    }

    public Car(String name, String chassisNumber, String engineNumber, String registryNumber, String imageURI, int doorQuantity){
        super(name,chassisNumber,engineNumber,registryNumber,imageURI);

        this.doorQuantity = doorQuantity;
    }

    public int getDoorQuantity() {
        return doorQuantity;
    }

    public void setDoorQuantity(int doorQuantity) {
        this.doorQuantity = doorQuantity;
    }
}
