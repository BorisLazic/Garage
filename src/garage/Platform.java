package garage;

import garage.Vehicles.Vehicle;
import javafx.scene.control.Label;
import javafx.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import static garage.UserMode.GRID_HEIGHT;
import static garage.UserMode.GRID_WIDTH;

public class Platform implements Serializable {
    private static int platformIndexCounter;
    private int platformIndex;
    public int freeParkingSpots;
    private LinkedList<Vehicle> platformVehicles;
    public Vector<Traveler> traversalNodes;
    private ArrayList<ArrayList<PlatformNode>> vertices;
    public ArrayList<PlatformNode> freePlatformParkingRoute, fullPlatformParkingRoute, innerParkingExit, propagatedExitRoute;
    private boolean accidentHappened = false;
    public final Integer objectLocker = 69;

    Platform() {
        freeParkingSpots = 28;
        platformVehicles = new LinkedList<>();
        platformIndex = platformIndexCounter++;
        traversalNodes = new Vector<>();
        vertices = new ArrayList<>();
        freePlatformParkingRoute = new ArrayList<>();
        fullPlatformParkingRoute = new ArrayList<>();
        innerParkingExit = new ArrayList<>();
        propagatedExitRoute = new ArrayList<>();
        for (int i = 0; i < GRID_HEIGHT; i++) {
            vertices.add(new ArrayList<>());
            for (int j = 0; j < GRID_WIDTH; j++) {
                if ((i >= 2 && (j == 0 || j == 7)) || ((i >= 2 && i <= 7) && (j == 3 || j == 4)))
                    vertices.get(i).add(new PlatformNode(true));
                else
                    vertices.get(i).add(new PlatformNode(false));
            }
        }
        initializeRoutes();
    }

    private void initializeRoutes() {

        freePlatformParkingRoute.add(vertices.get(1).get(0));
        for (int i = 1; i < 10; i++)
            freePlatformParkingRoute.add(vertices.get(i).get(1));
        for (int j = 2; j < 7; j++)
            freePlatformParkingRoute.add(vertices.get(9).get(j));
        for (int i = 8; i >= 2; i--)
            freePlatformParkingRoute.add(vertices.get(i).get(6));

        for (int j = 0; j < 8; j++)
            fullPlatformParkingRoute.add(vertices.get(1).get(j));

        for (int i = 2; i < 9; i++)
            innerParkingExit.add(vertices.get(i).get(5));
        innerParkingExit.add(vertices.get(8).get(4));
        innerParkingExit.add(vertices.get(8).get(3));
        for (int i = 8; i >= 2; i--)
            innerParkingExit.add(vertices.get(i).get(2));

        for (int j = 7; j >= 0; j--)
            propagatedExitRoute.add(vertices.get(0).get(j));
    }

    public void setupPlatform(LinkedList<Vehicle> minimumFulfillment) {
        LinkedList<Vehicle> saverOfVehicles = new LinkedList<>(platformVehicles);

        for (int i = 0; i < GRID_HEIGHT; i++) {
            for (int j = 0; j < GRID_WIDTH; j++) {
                PlatformNode place = this.getPlatformPlace(i, j);
                place.setVehicleLabel();
                if (!place.isOccupied() && place.isParkingSpot) {
                    if (!saverOfVehicles.isEmpty()) {
                        place.vehicle = saverOfVehicles.remove();
                        place.setVehicleLabel();
                        place.setOccupied(true);
                        traversalNodes.add(new Traveler(place, this));
                        freeParkingSpots--;
                    } else if (!minimumFulfillment.isEmpty()) {
                        place.vehicle = minimumFulfillment.remove();
                        place.setVehicleLabel();
                        place.setOccupied(true);
                        platformVehicles.add(place.vehicle);
                        traversalNodes.add(new Traveler(place, this));
                        freeParkingSpots--;
                    }
                }
            }
        }
    }

    public LinkedList<Vehicle> getPlatformVehicles() {
        return platformVehicles;
    }

    public String toString() {
        return "Platform" + platformIndex;
    }

    public PlatformNode getPlatformPlace(int i, int j) throws ArrayIndexOutOfBoundsException {
        if (i >= GRID_HEIGHT || i < 0 || j >= GRID_WIDTH || j < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return vertices.get(i).get(j);
    }

    public Pair<ArrayList<PlatformNode>, Integer> getPlatformNodeRow(PlatformNode object) {
        for (int i = 0; i < GRID_HEIGHT; i++) {
            int secondIndex = vertices.get(i).indexOf(object);
            if (secondIndex != -1)
                return new Pair<>(vertices.get(i), secondIndex);
        }
        return null;
    }

    public ArrayList<PlatformNode> findRoute(PlatformNode currentPlace) {
        if (freePlatformParkingRoute.contains(currentPlace))
            return freePlatformParkingRoute;
        else if (propagatedExitRoute.contains(currentPlace))
            return propagatedExitRoute;
        else if (innerParkingExit.contains(currentPlace))
            return innerParkingExit;
        return null;
    }

    public void synchroniseView() {
        for (int i = 0; i < GRID_HEIGHT; i++) {
            for (int j = 0; j < GRID_WIDTH; j++) {
                Label help = (Label) UserMode.userModeGridPane.getChildren().get(i * 8 + j + 1);
                help.textProperty().bind(getPlatformPlace(i, j).nodeLabel.textProperty());
            }
        }
    }

    public void startSimulation() {
        Random random = new Random();
        for (Traveler traveler :
                traversalNodes) {
            if (random.nextInt(100) < 15 && !traveler.isMoving())
            {
                traveler.start();
        }}
    }


    public int getCardinalIndex(PlatformNode node) {
        for (int i = 0; i < GRID_HEIGHT; i++) {
            int secondIndex = vertices.get(i).indexOf(node);
            if (secondIndex != -1)
                return i * GRID_WIDTH + secondIndex;
        }
        throw new ArrayIndexOutOfBoundsException();
    }


    public boolean accidentHappened() {
        return accidentHappened;
    }

    public void setAccidentHappened(boolean accidentHappened) {
        this.accidentHappened = accidentHappened;
    }
}











