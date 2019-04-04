package garage.Vehicles;

import garage.Administrator;

import java.io.Serializable;

public class Van extends Vehicle implements Serializable {
    private int carryingCapacity;//in KGs

    public Van(){
        super();
        setImageURI(Administrator.appFilesPath + "Van.jpg");

        carryingCapacity = 1000;
    }

    public Van(String name, String chassisNumber, String engineNumber, String registryNumber, String imageURI, int carryingCapacity){
        super(name,chassisNumber,engineNumber,registryNumber,imageURI);

        this.carryingCapacity = carryingCapacity;
    }

    public int getCarryingCapacity() {
        return carryingCapacity;
    }

    public void setCarryingCapacity(int carryingCapacity) {
        this.carryingCapacity = carryingCapacity;
    }
}
