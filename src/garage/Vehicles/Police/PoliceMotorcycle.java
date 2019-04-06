package garage.Vehicles.Police;

import garage.Administrator;
import garage.Vehicles.Civil.Motorcycle;
import garage.Vehicles.ServiceVehicle;

import java.io.Serializable;

public class PoliceMotorcycle extends Motorcycle implements Serializable, ServiceVehicle {

    public PoliceMotorcycle() {
        super();
        setImageURI(Administrator.appFilesPath + "PoliceMotorcycle.jpg");
    }

    public PoliceMotorcycle(String name, String chassisNumber, String engineNumber, String registryNumber, String imageURI) {
        super(name, chassisNumber, engineNumber, registryNumber, imageURI);
    }
}
