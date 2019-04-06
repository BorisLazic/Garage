package garage.Vehicles.Firefigher;

import garage.Administrator;
import garage.Vehicles.Civil.Van;
import garage.Vehicles.ServiceVehicle;

import java.io.Serializable;

public class Firetruck extends Van implements Serializable, ServiceVehicle {
    public Firetruck() {
        super();
        setImageURI(Administrator.appFilesPath + "Firetruck.jpg");
    }

    public Firetruck(String name, String chassisNumber, String engineNumber, String registryNumber, String imageURI, int carryingCapacity) {
        super(name, chassisNumber, engineNumber, registryNumber, imageURI, carryingCapacity);
    }
}
