package garage;

import garage.Vehicles.ServiceVehicle;
import garage.Vehicles.Vehicle;
import javafx.util.Pair;

import java.io.Serializable;
import java.util.*;

import static garage.Administrator.Garage;
import static garage.UserMode.GRID_WIDTH;
import static garage.UserMode.parkingRegulation;


public class Traveler extends Thread implements Serializable {

    private static long waitAmount = 250;
    private PlatformNode currentNode;
    private Vehicle vehicle;
    private Platform platform;
    private String labelText;
    private Iterator<PlatformNode> iterator;
    private boolean isLookingForParking = false;
    private boolean isLookingForExit = false;
    private boolean isMoving;
    private static final Integer freeParkingSpotsLocker = 420;

    public Traveler(PlatformNode currentNode, Platform platform) {
        this.currentNode = currentNode;
        this.vehicle = currentNode.vehicle;
        if (currentNode.isParkingSpot) {
            isLookingForExit = true;
        } else {
            isLookingForParking = true;
        }
        if (!(vehicle instanceof ServiceVehicle))
            parkingRegulation.startParkingMeter(vehicle.getRegistrationNumber());
        labelText = currentNode.nodeLabel.getText();
        this.platform = platform;
        if (isLookingForParking) {
            if (platform.freeParkingSpots > 0) {
                iterator = platform.freePlatformParkingRoute.listIterator(1);
            } else {
                iterator = platform.fullPlatformParkingRoute.listIterator(1);
            }
        } else if (isLookingForExit) {
            try {
                getExitRoute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        isMoving = true;
        if (currentNode.isParkingSpot && currentNode.isOccupied()) {
            synchronized (freeParkingSpotsLocker) {
                platform.freeParkingSpots++;
                currentNode.setOccupied(false);
            }
        }
        while (true) {
            boolean isParked = false;
            if (iterator.hasNext()) {
                while (iterator.hasNext()) {
                    PlatformNode nextNode = iterator.next();
                    waitForNextNode(nextNode);
                    moveTo(nextNode);
                    if (isLookingForParking)
                        isParked = park();
                    try {
                        sleep(waitAmount * 2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (isParked) {
                try {
                    sleep(new Random().nextInt(10) * 10 * waitAmount);
                    PlatformNode parkingNode = currentNode;
                    getExitRoute();
                    PlatformNode nextNode =iterator.next();
                    waitForNextNode(nextNode);
                    moveTo(nextNode);
                    synchronized (freeParkingSpotsLocker) {
                        platform.freeParkingSpots++;
                        parkingNode.setOccupied(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                iterator = extendRoute();
                if (!iterator.hasNext()) {
                    if (!(vehicle instanceof ServiceVehicle)) {
                        UserMode.parkingRegulation.endParkingMeterAndCharge(vehicle.getRegistrationNumber());
                    }
                    currentNode.leave();
                    break;
                }
            }
        }
    }

    private void getExitRoute() throws Exception {
        Pair<ArrayList<PlatformNode>, Integer> row = this.platform.getPlatformNodeRow(currentNode);
        PlatformNode nextNode;
        ArrayList<PlatformNode> currentRoute;
        int picker;
        if ((row.getValue() % 4) == 0) {
            picker = 1;
        } else if ((row.getValue() % 4) == 3) {
            picker = -1;
        } else {
            throw new Exception("Vehicle not parked.");
        }
        nextNode = row.getKey().get(row.getValue() + picker);
        currentRoute = new ArrayList<>(platform.findRoute(nextNode));
        iterator = currentRoute.listIterator(currentRoute.indexOf(nextNode));
    }

    private void moveTo(PlatformNode nextNode) {
        synchronized (platform.objectLocker) {
            if(platform.accidentHappened()) {
                try {
                    platform.objectLocker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            currentNode.leave();
            currentNode = nextNode;
            currentNode.emplace(vehicle, labelText);
        }
    }

    private void waitForNextNode(PlatformNode nextNode) {
        Random random = new Random();
        while (!nextNode.isEmpty()) {
            if(random.nextInt(100)<10 && !platform.accidentHappened())
                causeAccident(nextNode.vehicle);
            try {
                sleep(waitAmount);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
    }

    private boolean park() {
        Pair<ArrayList<PlatformNode>, Integer> row = platform.getPlatformNodeRow(currentNode);
        int sideChooser = 0;
        if ((row.getValue() % 4) == 1) {
            sideChooser = 1;
        } else if ((row.getValue() % 4) == 2) {
            sideChooser = -1;
        }
        if (sideChooser == 0)
            return false;
        PlatformNode parkingNode;

        if (!(parkingNode = row.getKey().get(row.getValue() - sideChooser)).isOccupied() && parkingNode.isParkingSpot) {
            iterator = (Collections.singletonList(parkingNode).listIterator(0));
            isLookingForParking = false;
            isLookingForExit = true;
            synchronized (freeParkingSpotsLocker) {
                platform.freeParkingSpots--;
                parkingNode.setOccupied(true);
            }
            if (!(vehicle instanceof ServiceVehicle)) {
                UserMode.parkingRegulation.startParkingMeter(vehicle.getRegistrationNumber());
            }
            return true;
        }
        if (!(parkingNode = row.getKey().get(row.getValue() + 2 * sideChooser)).isOccupied() && parkingNode.isParkingSpot) {
            iterator = Arrays.asList(row.getKey().get(row.getValue() + sideChooser), row.getKey().get(row.getValue() + 2 * sideChooser))
                    .listIterator(0);
            isLookingForParking = false;
            isLookingForExit = true;
            synchronized (freeParkingSpotsLocker) {
                platform.freeParkingSpots--;
                parkingNode.setOccupied(true);
            }
            if (!(vehicle instanceof ServiceVehicle)) {
                UserMode.parkingRegulation.startParkingMeter(vehicle.getRegistrationNumber());
            }
            return true;
        }
        return false;
    }

    private void causeAccident(Vehicle vehicle) {
        platform.setAccidentHappened(true);
        labelText = "X";
        currentNode.emplace(vehicle,labelText);
        for (Traveler traveler :
                platform.traversalNodes) {
            if(traveler.vehicle.equals(vehicle)) {
                platform.setAccidentHappened(true);
                traveler.labelText = "X";
                traveler.currentNode.emplace(vehicle,"X");
                break;
            }

        }
    }

    private Iterator<PlatformNode> extendRoute() {
        int indexOfCurrentNode = platform.getCardinalIndex(currentNode);
        if (isLookingForExit) {
            if (((indexOfCurrentNode % 4) == 2) && ((indexOfCurrentNode / GRID_WIDTH) == 2)) {
                PlatformNode nextNode = platform.getPlatformPlace(1, indexOfCurrentNode % GRID_WIDTH);
                waitForNextNode(nextNode);
                moveTo(nextNode);
                try {
                    sleep(waitAmount * 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                southWaitForRightSide(indexOfCurrentNode % GRID_WIDTH);
                return platform.propagatedExitRoute.listIterator(platform.propagatedExitRoute.indexOf(platform.getPlatformPlace(0, indexOfCurrentNode % GRID_WIDTH)));
            } else if ((indexOfCurrentNode == 0)) {
                if (Garage.indexOf(platform) == 0) {
                    return new ArrayList<PlatformNode>().iterator();
                } else {
                    platform.getPlatformVehicles().remove(this.vehicle);
                    platform.traversalNodes.remove(this);
                    platform = Garage.get(Garage.indexOf(platform) - 1);
                    return platform.propagatedExitRoute.listIterator();
                }
            }
        }
        if (isLookingForParking) {
            if ((Garage.indexOf(platform) == (Garage.size() - 1)) && (indexOfCurrentNode == 15)) {
                isLookingForParking = false;
                isLookingForExit = true;
                return platform.propagatedExitRoute.listIterator(0);
            } else if (indexOfCurrentNode == 22) {
                PlatformNode nextNode = platform.getPlatformPlace(1,indexOfCurrentNode%8);
                return platform.fullPlatformParkingRoute.listIterator(platform.fullPlatformParkingRoute.indexOf(nextNode));
            }
            synchronized (platform.objectLocker) {
                platform.getPlatformVehicles().remove(this.vehicle);
                platform.traversalNodes.remove(this);
            }
            platform = Garage.get(Garage.indexOf(platform) + 1);
            synchronized (platform.objectLocker) {
                platform.getPlatformVehicles().add(this.vehicle);
                platform.traversalNodes.add(this);
            }
            if (platform.freeParkingSpots > 0)
                return platform.freePlatformParkingRoute.listIterator();
            else
                return platform.fullPlatformParkingRoute.listIterator();
        }
        return new ArrayList<PlatformNode>().iterator();
    }

    private void southWaitForRightSide(int horizontalIndex) {
        ArrayList<PlatformNode> prioritizedNodes = new ArrayList<>(Arrays.asList(platform.getPlatformPlace(0, horizontalIndex), platform.getPlatformPlace(0, horizontalIndex + 1)));
        while (!prioritizedNodes.get(0).isEmpty() || !prioritizedNodes.get(1).isEmpty()) {
            try {
                sleep(waitAmount);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isMoving() {
        return isMoving;
    }

}



















