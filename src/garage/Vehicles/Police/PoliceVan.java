package garage.Vehicles.Police;

import garage.Administrator;
import garage.Vehicles.Civil.Van;
import garage.Vehicles.EmergencyService;

import java.io.Serializable;

public class PoliceVan extends Van implements Serializable, EmergencyService {
    public PoliceVan() {
        super();
        setImageURI(Administrator.appFilesPath + "PoliceVan.jpg");
    }

    public PoliceVan(String name, String chassisNumber, String engineNumber, String registryNumber, String imageURI, int carryingCapacity) {
        super(name, chassisNumber, engineNumber, registryNumber, imageURI, carryingCapacity);
    }
}
