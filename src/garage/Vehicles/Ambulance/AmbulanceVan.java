package garage.Vehicles.Ambulance;

import garage.Administrator;
import garage.Vehicles.Civil.Van;
import garage.Vehicles.ServiceVehicle;

import java.io.Serializable;

public class AmbulanceVan extends Van implements Serializable, ServiceVehicle {

    public AmbulanceVan() {
        super();
        setImageURI(Administrator.appFilesPath + "AmbulanceVan.jpg");
    }

    public AmbulanceVan(String name, String chassisNumber, String engineNumber, String registryNumber, String imageURI, int carryingCapacity) {
        super(name, chassisNumber, engineNumber, registryNumber, imageURI, carryingCapacity);
    }
}
