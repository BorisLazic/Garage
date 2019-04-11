package garage.Vehicles.Police;

import garage.Administrator;
import garage.Vehicles.Civil.Car;
import garage.Vehicles.EmergencyService;

import java.io.Serializable;

public class PoliceCar extends Car implements Serializable, EmergencyService {

    public PoliceCar(){
        super();
        setImageURI(Administrator.appFilesPath + "PoliceCar.jpg");
    }

    public PoliceCar(String name, String chassisNumber, String engineNumber, String registryNumber, String imageURI, int doorQuantity){
        super(name,chassisNumber,engineNumber,registryNumber,imageURI,doorQuantity);
    }
}
