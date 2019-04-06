package garage.Vehicles.Civil;

import garage.Administrator;
import garage.Vehicles.Vehicle;

import java.io.Serializable;

public class Motorcycle extends Vehicle implements Serializable {
    public Motorcycle() {
        super();
        setImageURI(Administrator.appFilesPath + "Motorcycle.jpg");
    }

    public Motorcycle(String name, String chassisNumber, String engineNumber, String registrationNumber, String imageURI) {
        super(name, chassisNumber, engineNumber, registrationNumber, imageURI);
    }
}
