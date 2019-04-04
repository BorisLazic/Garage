package garage.Vehicles;

import garage.Administrator;

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
