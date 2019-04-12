package garage;

import garage.Vehicles.Ambulance.AmbulanceCar;
import garage.Vehicles.Ambulance.AmbulanceVan;
import garage.Vehicles.EmergencyService;
import garage.Vehicles.Firefigher.Firetruck;
import garage.Vehicles.Police.PoliceCar;
import garage.Vehicles.Police.PoliceMotorcycle;
import garage.Vehicles.Police.PoliceVan;
import garage.Vehicles.Vehicle;
import javafx.util.Pair;

import java.io.Serializable;
import java.util.*;

import static garage.Administrator.Garage;
import static garage.UserMode.GRID_WIDTH;
import static garage.UserMode.parkingRegulation;


public class Traveler extends Thread implements Serializable {

    private static long waitAmount = 1000;
    private PlatformNode currentNode;
    private Vehicle vehicle;
    private Platform platform;
    private String labelText;
    private Iterator<PlatformNode> iterator;
    private boolean isLookingForParking = false;
    private boolean isLookingForExit = false;
    private boolean isMoving;
    private boolean beingOvertaken = false;
    private static final Integer freeParkingSpotsLocker = 420;

    private PlatformNode accidentSpot;
    private Platform accidentPlatform;

    public Traveler(PlatformNode currentNode, Platform platform) {
        this.currentNode = currentNode;
        this.vehicle = currentNode.vehicle;
        if (currentNode.isParkingSpot) {
            isLookingForExit = true;
        } else {
            isLookingForParking = true;
        }
        if (!(vehicle instanceof EmergencyService))
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
                getParkingExitRoute();
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
                    if (!rotationIsOn()) {
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
                    } else {
                        moveToCrash();
                    }
                }
            }
            if (isParked) {
                try {
                    sleep(new Random().nextInt(10) * 10 * waitAmount);
                    PlatformNode parkingNode = currentNode;
                    getParkingExitRoute();
                    PlatformNode nextNode = iterator.next();
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
                    if (!(vehicle instanceof EmergencyService)) {
                        UserMode.parkingRegulation.endParkingMeterAndCharge(vehicle.getRegistrationNumber());
                    }
                    currentNode.leave();
                    break;
                }
            }
        }
    }

    private boolean rotationIsOn() {
        return labelText.contains("R");
    }

    private void getParkingExitRoute() throws Exception {
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
            currentNode.leave();
            currentNode = nextNode;
            currentNode.emplace(vehicle, labelText);
        }
    }

    private void waitForNextNode(PlatformNode nextNode) {
        Random random = new Random();
        try {
            if (!platform.accidentHappened() && !rotationIsOn()) {
                while (!nextNode.isEmpty()) {
                    if (random.nextInt(100) < 100 && !platform.accidentHappened() && !nextNode.nodeLabel.getText().contains("R"))//TODO NE ZABORAVI PROMIJENITI
                        causeAccident(nextNode.vehicle);
                    sleep(waitAmount);
                }
            } else if (!(vehicle instanceof EmergencyService)) {
                synchronized (platform.objectLocker) {
                    System.out.println(Thread.currentThread().toString() + " I go for long schleep bcuz im civie and accident happ");
                    platform.objectLocker.wait();
                }
            } else if (!rotationIsOn()) {
                System.out.println(Thread.currentThread().toString() + " I'm retarded so I loop bcuz I got no R");
                while (rotationIsOn()) {
                    sleep(waitAmount * 2);
                }
            } else {
                while (!nextNode.isEmpty()) {
                    sleep(waitAmount);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
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
            if (!(vehicle instanceof EmergencyService)) {
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
            if (!(vehicle instanceof EmergencyService)) {
                UserMode.parkingRegulation.startParkingMeter(vehicle.getRegistrationNumber());
            }
            return true;
        }
        return false;
    }

    private void causeAccident(Vehicle secondVehicle) {
        platform.setAccidentHappened(true);
        labelText = "X";
        currentNode.emplace(secondVehicle, labelText);
        for (Traveler traveler :
                platform.traversalNodes) {
            if (traveler.vehicle.equals(secondVehicle)) {
                platform.setAccidentHappened(true);
                traveler.labelText = "X";
                traveler.currentNode.emplace(secondVehicle, "X");
                break;
            }
        }
        Traveler police = findPolice();
        Traveler ambulance = findAmbulance();
        Traveler firefighter = findFirefighter();
        try {
            if (police != null && ambulance != null && firefighter != null) {
                System.out.println("All've been found \n\n");
                police.accidentSpot = ambulance.accidentSpot = firefighter.accidentSpot = currentNode;
                police.accidentPlatform = ambulance.accidentPlatform = firefighter.accidentPlatform = platform;
                police.labelText = police.labelText.concat("R");
                ambulance.labelText = ambulance.labelText.concat("R");
                firefighter.labelText = firefighter.labelText.concat("R");
                System.out.println("Starting them up.");
                if (!police.isMoving)
                    police.start();
                if (!ambulance.isMoving)
                    ambulance.start();
                if (!firefighter.isMoving)
                    firefighter.start();
            }
            sleep(35000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Traveler findPolice() {
        int indexOfPlatform = Garage.indexOf(platform);
        for (int i = 0; i < Garage.size(); i++) {
            int sidePick = 1;
            int sidesPicked = 1;
            while (sidesPicked <= 2) {
                try {
                    Platform currentlySearched = Garage.get(indexOfPlatform + i * sidePick);
                    for (Traveler traveller : currentlySearched.traversalNodes) {
                        if ((traveller.vehicle instanceof PoliceVan) || (traveller.vehicle instanceof PoliceCar)
                                || (traveller.vehicle instanceof PoliceMotorcycle)) {
                            return traveller;
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                } finally {
                    sidesPicked++;
                    sidePick *= -1;
                    if (i == 0)
                        sidesPicked = 69;
                }
            }
        }
        return null;
    }

    private Traveler findAmbulance() {
        int indexOfPlatform = Garage.indexOf(platform);
        for (int i = 0; i < Garage.size(); i++) {
            int sidePick = 1;
            int sidesPicked = 1;
            while (sidesPicked <= 2) {
                try {
                    Platform currentlySearched = Garage.get(indexOfPlatform + i * sidePick);
                    for (Traveler traveller : currentlySearched.traversalNodes) {
                        if ((traveller.vehicle instanceof AmbulanceVan) || (traveller.vehicle instanceof AmbulanceCar)) {
                            return traveller;
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                } finally {
                    sidesPicked++;
                    sidePick *= -1;
                    if (i == 0)
                        sidesPicked = 69;
                }
            }
        }
        return null;
    }

    private Traveler findFirefighter() {
        int indexOfPlatform = Garage.indexOf(platform);
        for (int i = 0; i < Garage.size(); i++) {
            int sidePick = 1;
            int sidesPicked = 1;
            while (sidesPicked <= 2) {
                try {
                    Platform currentlySearched = Garage.get(indexOfPlatform + i * sidePick);
                    for (Traveler traveller : currentlySearched.traversalNodes) {
                        if (traveller.vehicle instanceof Firetruck) {
                            return traveller;
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                } finally {
                    sidesPicked++;
                    sidePick *= -1;
                    if (i == 0)
                        sidesPicked = 69;
                }
            }
        }
        return null;
    }

    private void moveToCrash() {
        int indexOfCrashPlatform = Garage.indexOf(accidentPlatform);
        boolean notThere = true;
        try {
            while (notThere) {
                if (indexOfCrashPlatform == Garage.indexOf(platform)) {
                    int indexOfCurrentNode = platform.getCardinalIndex(currentNode);
                    System.out.println("IN THE ADEQUATE PLATFORM");
                    sleep(100000);

                    notThere = false;
                } else if (indexOfCrashPlatform < Garage.indexOf(platform)) {
                    int indexOfCurrentNode = platform.getCardinalIndex(currentNode);
                    if ((indexOfCrashPlatform / GRID_WIDTH) == 1) {
                        PlatformNode upOne = platform.getPlatformPlace(0, indexOfCurrentNode % 8);
                        waitForNextNode(upOne);
                        moveTo(upOne);
                    }
                    isLookingForExit = true;
                    isLookingForParking = false;
                    PlatformNode nextNode;
                    while (iterator.hasNext()) {
                        nextNode = iterator.next();
                        if (nextNode.isEmpty()) {
                            System.out.println(labelText + "MOVING TO NEXT NODE");
                            moveTo(nextNode);
                            sleep(waitAmount / 2);
                        } else if (nextNode.nodeLabel.getText().contains("R")) {
                            System.out.println(labelText + "WAITING FOR THE NEXT NODE");
                            while (nextNode.nodeLabel.getText().contains("R"))
                                sleep(waitAmount / 2);
                            moveTo(nextNode);
                            sleep(waitAmount / 2);
                        } else {
                            System.out.println(labelText + "OVERTAKE NEXT NODE AND OTHERS");
                            overtake(nextNode);
                        }
                    }
                    System.out.println("Extending route");
                    iterator = extendRoute();
                    nextNode = iterator.next();
                    if (nextNode.isEmpty()) {
                        System.out.println(labelText + "MOVING TO NEXT NODE");
                        moveTo(nextNode);
                        sleep(waitAmount / 2);
                    } else if (nextNode.nodeLabel.getText().contains("R")) {
                        System.out.println(labelText + "WAITING FOR THE NEXT NODE");
                        while (nextNode.nodeLabel.getText().contains("R"))
                            sleep(waitAmount / 2);
                        moveTo(nextNode);
                        sleep(waitAmount / 2);
                    } else {
                        System.out.println(labelText + "OVERTAKE NEXT NODE AND OTHERS");
                        overtake(nextNode);
                    }
                } else if (indexOfCrashPlatform > Garage.indexOf(platform)) {
                    int indexOfCurrentNode = 0;
                    try {
                        indexOfCurrentNode = platform.getCardinalIndex(currentNode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if ((indexOfCrashPlatform / GRID_WIDTH) == 0) {
                        PlatformNode downOne = platform.getPlatformPlace(1, indexOfCurrentNode % 8);
                        waitForNextNode(downOne);
                        moveTo(downOne);
                    }
                    isLookingForExit = false;
                    isLookingForParking = true;
                    PlatformNode nextNode;
                    while (iterator.hasNext()) {
                        nextNode = iterator.next();
                        if (nextNode.isEmpty()) {
                            System.out.println(labelText + "MOVING TO NEXT NODE");
                            moveTo(nextNode);
                            sleep(waitAmount / 2);
                        } else if (nextNode.nodeLabel.getText().contains("R")) {
                            System.out.println(labelText + "WAITING FOR THE NEXT NODE");
                            while (nextNode.nodeLabel.getText().contains("R"))
                                sleep(waitAmount / 2);
                            moveTo(nextNode);
                            sleep(waitAmount / 2);
                        } else {
                            System.out.println(labelText + "OVERTAKE NEXT NODE AND OTHERS");
                            overtake(nextNode);
                        }
                    }
                    System.out.println("Extending route");
                    iterator = extendRoute();
                    nextNode = iterator.next();
                    if (nextNode.isEmpty()) {
                        System.out.println(labelText + "MOVING TO NEXT NODE");
                        moveTo(nextNode);
                        sleep(waitAmount / 2);
                    } else if (nextNode.nodeLabel.getText().contains("R")) {
                        System.out.println(labelText + "WAITING FOR THE NEXT NODE");
                        while (nextNode.nodeLabel.getText().contains("R"))
                            sleep(waitAmount / 2);
                        moveTo(nextNode);
                        sleep(waitAmount / 2);
                    } else {
                        System.out.println(labelText + "OVERTAKE NEXT NODE AND OTHERS");
                        overtake(nextNode);
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void overtake(PlatformNode nextNode) {
        System.out.println(labelText + "Bout to overtake");
        String previousLabel = nextNode.nodeLabel.getText();
        Vehicle previousVehicle = nextNode.vehicle;
        System.out.println(previousLabel + " Label of previous car?");
        boolean overtakeOngoing = true;
        synchronized (platform.objectLocker) {
            if (currentNode.vehicle == vehicle)
                currentNode.leave();
            else
                currentNode.overtakeLeave();
            System.out.println(labelText + "Overtake leaving");
            currentNode = nextNode;
            if (!nextNode.isEmpty()) {
                currentNode.emplace(currentNode.vehicle, labelText);
                if (iterator.hasNext())
                    nextNode = iterator.next();
                else {
                    overtakeOngoing = false;
                    currentNode.emplace(currentNode.vehicle, previousLabel);
                }
            } else {
                currentNode.emplace(vehicle, labelText);
                overtakeOngoing = false;
            }
        }
        try {
            sleep(waitAmount / 2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (iterator.hasNext() && overtakeOngoing) {
            synchronized (platform.objectLocker) {
                currentNode.emplace(currentNode.vehicle, previousLabel);
                previousLabel = nextNode.nodeLabel.getText();
                System.out.println(labelText + "Overtake leaving");
                currentNode = nextNode;
                if (!nextNode.isEmpty()) {
                    currentNode.emplace(currentNode.vehicle, labelText);
                    nextNode = iterator.next();
                } else {
                    currentNode.emplace(vehicle, labelText);
                    overtakeOngoing = false;
                }
            }
            try {
                sleep(waitAmount / 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (overtakeOngoing) {
            if (nextNode.nodeLabel.getText().contains("R"))
                while (nextNode.nodeLabel.getText().contains("R")) {
                    try {
                        sleep(waitAmount / 2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            synchronized (platform.objectLocker) {
                currentNode.emplace(currentNode.vehicle, previousLabel);
                currentNode = nextNode;
                currentNode.emplace(currentNode.vehicle, labelText);
            }
            try {
                sleep(waitAmount / 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Iterator<PlatformNode> extendRoute() {
        int indexOfCurrentNode = platform.getCardinalIndex(currentNode);
        if (isLookingForExit) {
            if (((indexOfCurrentNode % 4) == 2) && ((indexOfCurrentNode / GRID_WIDTH) == 2)) {
                PlatformNode nextNode = platform.getPlatformPlace(1, indexOfCurrentNode % GRID_WIDTH);
                if (!rotationIsOn()) {
                    waitForNextNode(nextNode);
                    moveTo(nextNode);
                    southWaitForRightSide(indexOfCurrentNode % GRID_WIDTH);
                    try {
                        sleep(waitAmount * 2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (nextNode.isEmpty()) {
                        moveTo(nextNode);
                        try {
                            sleep(waitAmount);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return platform.propagatedExitRoute.listIterator(platform.propagatedExitRoute.indexOf(platform.getPlatformPlace(0, indexOfCurrentNode % GRID_WIDTH)));
            } else if ((indexOfCurrentNode == 0)) {
                System.out.println("Extending at end" + toString() + "\n");
                if (Garage.indexOf(platform) == 0) {
                    return new ArrayList<PlatformNode>().iterator();
                } else {
                    System.out.println("Extending, to the next one" + toString() + "\n");
                    platform.getPlatformVehicles().remove(this.vehicle);
                    platform.traversalNodes.remove(this);
                    platform = Garage.get(Garage.indexOf(platform) - 1);
                    platform.getPlatformVehicles().add(this.vehicle);
                    platform.traversalNodes.add(this);
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
                PlatformNode nextNode = platform.getPlatformPlace(1, indexOfCurrentNode % 8);
                return platform.fullPlatformParkingRoute.listIterator(platform.fullPlatformParkingRoute.indexOf(nextNode));
            } else if (indexOfCurrentNode == 18) {
                PlatformNode nextNode = platform.getPlatformPlace(1, indexOfCurrentNode % 8);
                return platform.fullPlatformParkingRoute.listIterator(platform.fullPlatformParkingRoute.indexOf(nextNode));
            }
            platform.getPlatformVehicles().remove(this.vehicle);
            platform.traversalNodes.remove(this);
            platform = Garage.get(Garage.indexOf(platform) + 1);
            platform.getPlatformVehicles().add(this.vehicle);
            platform.traversalNodes.add(this);
            if (platform.freeParkingSpots > 0 && !rotationIsOn())
                return platform.freePlatformParkingRoute.listIterator();
            else
                return platform.fullPlatformParkingRoute.listIterator();
        }
        return new ArrayList<PlatformNode>().iterator();
    }

    private void southWaitForRightSide(int horizontalIndex) {//TODO RIGHT SIDE RULE FROM THE LEFT NEEDS TO BE ADDED
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



















