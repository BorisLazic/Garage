package garage;

import garage.Vehicles.Ambulance.AmbulanceCar;
import garage.Vehicles.Ambulance.AmbulanceVan;
import garage.Vehicles.Firefigher.Firetruck;
import garage.Vehicles.Police.PoliceCar;
import garage.Vehicles.Police.PoliceMotorcycle;
import garage.Vehicles.Police.PoliceVan;
import garage.Vehicles.Vehicle;
import javafx.scene.control.Label;

import java.io.Serializable;

public class PlatformNode implements Serializable {

    public Vehicle vehicle;
    public transient Label nodeLabel;
    public final boolean isParkingSpot;
    private String defaultText;
    private static int counter = 0;
    private int index;
    private boolean occupied;

    public PlatformNode(boolean isParkingSpot) {
        vehicle = null;
        nodeLabel = new Label();
        this.isParkingSpot = isParkingSpot;
        if(isParkingSpot)
            defaultText = "*";
        else
            defaultText = "";
        index= counter++;
    }

    public boolean isEmpty() {
        return vehicle == null;
    }


    public void setVehicleLabel() {
        if (nodeLabel == null)
            nodeLabel = new Label();
        if (!isEmpty()) {
            quickSetVehicleLabel();
        } else if (isParkingSpot) {
            nodeLabel.setText("*");
        } else {
            nodeLabel.setText("");
        }
    }

    public void quickSetVehicleLabel(){
        if (vehicle instanceof PoliceMotorcycle || vehicle instanceof PoliceCar || vehicle instanceof PoliceVan) {
            nodeLabel.setText("P");
        } else if (vehicle instanceof AmbulanceCar || vehicle instanceof AmbulanceVan) {
            nodeLabel.setText("H");
        } else if (vehicle instanceof Firetruck) {
            nodeLabel.setText("F");
        } else {
            nodeLabel.setText("V");
        }
    }

    public String toString(){
        return "PlatformNode" + index;
    }

    public void leave() {
        vehicle = null;
        javafx.application.Platform.runLater(() -> nodeLabel.setText(defaultText));
    }

    public void overtakeLeave(){
        javafx.application.Platform.runLater(() -> nodeLabel.setText(defaultText));
    }

    public void emplace(Vehicle vehicle, String label){
        this.vehicle = vehicle;
        javafx.application.Platform.runLater(() -> nodeLabel.setText(label));
    }

    public boolean isOccupied() {
        return occupied;
    }
    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }
}











