package garage.Vehicles.Ambulance;

import garage.Administrator;
import garage.Vehicles.Civil.Car;
import garage.Vehicles.ServiceVehicle;

import java.io.Serializable;

public class AmbulanceCar extends Car implements Serializable, ServiceVehicle {

    public AmbulanceCar(){
        super();
        this.setImageURI(Administrator.appFilesPath + "AmbulanceCar.jpg");
    }

    public AmbulanceCar(String name, String chassisNumber, String engineNumber, String registryNumber, String imageURI, int doorQuantity){
        super(name,chassisNumber,engineNumber,registryNumber,imageURI,doorQuantity);
    }
}
