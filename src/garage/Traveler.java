package garage;

import garage.Platform.Platform;
import garage.Platform.PlatformNode;
import garage.Vehicles.Ambulance.AmbulanceCar;
import garage.Vehicles.Ambulance.AmbulanceVan;
import garage.Vehicles.EmergencyService;
import garage.Vehicles.Firefigher.Firetruck;
import garage.Vehicles.Police.PoliceCar;
import garage.Vehicles.Police.PoliceMotorcycle;
import garage.Vehicles.Police.PoliceVan;
import garage.Vehicles.Vehicle;
import javafx.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static garage.Administrator.Garage;
import static garage.Administrator.appFilesPath;
import static garage.UserMode.GRID_WIDTH;
import static garage.UserMode.parkingRegulation;


public class Traveler extends Thread implements Serializable {

    private static long waitAmount = 1000;
    private PlatformNode currentNode;
    private Vehicle vehicle;
    private Platform platform;
    private String labelText;
    private transient Iterator<PlatformNode> iterator;//this
    private boolean isLookingForParking = false;
    private boolean isLookingForExit = false;
    private boolean isMoving;
    private static final Integer freeParkingSpotsLocker = 420;

    private Platform accidentPlatform;
    private Vehicle crashedVehicleOne;
    private Vehicle crashedVehicleTwo;
    private long accidentResolveTime = 3000;

    public Traveler(PlatformNode currentNode, Platform platform) {
        this.currentNode = currentNode;
        this.vehicle = currentNode.vehicle;
        if (currentNode.isParkingSpot) {
            isLookingForExit = true;
        } else {
            isLookingForParking = true;
        }
        if (!(vehicle instanceof EmergencyService)) {
            parkingRegulation.startParkingMeter(vehicle.getRegistrationNumber());
            labelText = "V";
        } else if (vehicle instanceof Firetruck) {
            labelText = "F";
        } else if (vehicle instanceof AmbulanceCar || vehicle instanceof AmbulanceVan) {
            labelText = "H";
        } else {
            labelText = "P";
        }
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
            } catch (Exception ignored) {
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
            parkingRegulation.startParkingMeter(vehicle.getRegistrationNumber());
            if (iterator == null) {
                try {
                    getParkingExitRoute();
                } catch (Exception ignored) {

                }
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
                        } catch (InterruptedException ignored) {
                        }
                    } else {
                        moveToCrash();
                        if (platform.accidentHappened()) {
                            synchronized (platform.crashLocker) {
                                try {
                                    platform.crashLocker.wait();
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                        try {
                            sleep(accidentResolveTime);
                        } catch (InterruptedException ignored) {
                        }
                        if (vehicle instanceof Firetruck) {
                            labelText = "F";
                        } else if (vehicle instanceof AmbulanceCar || vehicle instanceof AmbulanceVan) {
                            labelText = "H";
                        } else {
                            labelText = "P";
                        }
                        currentNode.leave();
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
                } catch (Exception ignored) {
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
        UserMode.amountOfCarsMoving--;
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
        Vehicle crashVehicle = nextNode.vehicle;
        try {
            if (!platform.accidentHappened() && !rotationIsOn()) {
                while (!nextNode.isEmpty()) {
                    if (random.nextInt(100) < platform.crashProbability && !platform.accidentHappened() && !(nextNode.vehicle instanceof EmergencyService)) {
                        platform.setAccidentHappened(true);
                        causeAccident(crashVehicle);
                    }
                    sleep(waitAmount);
                }
            } else if (!(vehicle instanceof EmergencyService)) {
                synchronized (platform.objectLocker) {
                    
                    platform.objectLocker.wait();
                }
            } else if (!rotationIsOn()) {
                
                while (!rotationIsOn()) {
                    sleep(waitAmount * 2);
                }
            } else {
                while (!nextNode.isEmpty()) {
                    sleep(waitAmount);
                }
            }
        } catch (InterruptedException ignored) {
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

    public void causeAccident(Vehicle secondVehicle) {
        labelText = "X";
        currentNode.emplace(currentNode.vehicle, labelText);
        platform.crashProbability = 0;
        for (Traveler traveler : platform.traversalNodes) {
            if (traveler.vehicle.equals(secondVehicle)) {
                traveler.labelText = "X";
                traveler.currentNode.emplace(secondVehicle, "X");
                break;
            }
        }
        accidentResolveTime = 3000 + (new Random().nextInt(7000));
        Traveler police = findPolice();
        Traveler ambulance = findAmbulance();
        Traveler firefighter = findFirefighter();
        if (police != null) {
            police.accidentPlatform = platform;
            police.crashedVehicleOne = vehicle;
            police.crashedVehicleTwo = secondVehicle;
            police.labelText = police.labelText.concat("R");
            if (!police.isMoving)
                police.start();
        } else {
            PoliceCar policeCar = new PoliceCar();
            Platform currentlyChosen = Garage.get(0);
            currentlyChosen.getPlatformPlace(1, 0).vehicle = policeCar;
            police = new Traveler(currentlyChosen.getPlatformPlace(1, 0), currentlyChosen);
            police.accidentPlatform = platform;
            police.crashedVehicleOne = vehicle;
            police.crashedVehicleTwo = secondVehicle;
            police.iterator = currentlyChosen.fullPlatformParkingRoute.iterator();
            police.labelText = police.labelText.concat("R");
            Garage.get(0).traversalNodes.add(police);
            police.start();
        }
        if (ambulance != null) {
            ambulance.accidentPlatform = platform;
            ambulance.labelText = ambulance.labelText.concat("R");
            if (!ambulance.isMoving)
                ambulance.start();
        } else {
            AmbulanceCar ambulanceCar = new AmbulanceCar();
            Platform currentlyChosen = Garage.get(0);
            currentlyChosen.getPlatformPlace(1, 0).vehicle = ambulanceCar;
            ambulance = new Traveler(currentlyChosen.getPlatformPlace(1, 0), currentlyChosen);
            Garage.get(0).traversalNodes.add(ambulance);
            ambulance.iterator = currentlyChosen.fullPlatformParkingRoute.iterator();
            ambulance.accidentPlatform = platform;
            ambulance.labelText = ambulance.labelText.concat("R");
            try {
                sleep(waitAmount);
            } catch (InterruptedException ignored) {
            }
            ambulance.start();
        }
        if (firefighter != null) {
            firefighter.accidentPlatform = platform;
            firefighter.labelText = firefighter.labelText.concat("R");
            if (!firefighter.isMoving)
                firefighter.start();
        } else {
            Firetruck firetruck = new Firetruck();
            Platform currentlyChosen = Garage.get(0);
            currentlyChosen.getPlatformPlace(1, 0).vehicle = firetruck;
            firefighter = new Traveler(currentlyChosen.getPlatformPlace(1, 0), currentlyChosen);
            firefighter.iterator = currentlyChosen.fullPlatformParkingRoute.iterator();
            Garage.get(0).traversalNodes.add(firefighter);
            firefighter.accidentPlatform = platform;
            firefighter.labelText = firefighter.labelText.concat("R");
            try {
                sleep(waitAmount * 2);
            } catch (InterruptedException ignored) {
            }
            firefighter.start();
        }
        Instant waitStart = Instant.now();
        Duration waitDuration = Duration.between(waitStart, Instant.now());

        while ((police.notThere || firefighter.notThere || ambulance.notThere) && (waitDuration.getSeconds() < (long) 30)) {
            try {
                sleep(waitAmount);
            } catch (InterruptedException ignored) {
            }
            waitDuration = Duration.between(waitStart, Instant.now());
        }
        police.notThere = false;
        firefighter.notThere = false;
        ambulance.notThere = false;
        synchronized (platform.crashLocker) {
            platform.crashLocker.notifyAll();
        }

        try (PrintWriter accidentReportWriter = new PrintWriter(
                new File(appFilesPath, vehicle.getRegistrationNumber() + ".bin"))) {
            accidentReportWriter.println(vehicle.getRegistrationNumber() + vehicle.getImageURI());
            accidentReportWriter.println(secondVehicle.getRegistrationNumber() + secondVehicle.getImageURI());
        } catch (FileNotFoundException ignored) {
        }
        platform.setAccidentHappened(false);
        synchronized (platform.objectLocker) {
            platform.objectLocker.notifyAll();
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
                        if (((traveller.vehicle instanceof PoliceVan) || (traveller.vehicle instanceof PoliceCar)
                                || (traveller.vehicle instanceof PoliceMotorcycle)) && !traveller.rotationIsOn()) {
                            return traveller;
                        }
                    }
                } catch (IndexOutOfBoundsException ignored) {
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
                        if (((traveller.vehicle instanceof AmbulanceVan) || (traveller.vehicle instanceof AmbulanceCar)) && !traveller.rotationIsOn()) {
                            return traveller;
                        }
                    }
                } catch (IndexOutOfBoundsException ignored) {
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
                        if (traveller.vehicle instanceof Firetruck && !traveller.rotationIsOn()) {
                            return traveller;
                        }
                    }
                } catch (IndexOutOfBoundsException ignored) {
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

    private boolean notThere = true;

    private void moveToCrash() {
        int indexOfCrashPlatform = Garage.indexOf(accidentPlatform);
        notThere = true;
        PlatformNode nextNode;
        try {
            while (notThere) {
                if (indexOfCrashPlatform == Garage.indexOf(platform)) {
                    int indexOfCurrentNode = platform.getCardinalIndex(currentNode);
                    if (indexOfCurrentNode / 8 == 1) {
                        String previousLabel;
                        while (notThere && iterator.hasNext()) {
                            nextNode = iterator.next();
                            previousLabel = nextNode.nodeLabel.getText();
                            indexOfCurrentNode = platform.getCardinalIndex(currentNode);
                            if (((indexOfCurrentNode % 8) == 3) || (indexOfCurrentNode == 15) || (indexOfCurrentNode == 0)) {
                                notThere = false;
                            } else if (nextNode.nodeLabel.getText().contains("R")) {
                                if (((indexOfCurrentNode % 8) == 4) || ((indexOfCurrentNode % 8) == 2))
                                    notThere = false;
                                else {
                                    int waitingQualifier = 0;
                                    while (nextNode.nodeLabel.getText().contains("R") && waitingQualifier++ < 5) {
                                        sleep(waitAmount / 2);
                                    }
                                    if (waitingQualifier >= 4)
                                        notThere = false;
                                    else
                                        moveTo(nextNode);
                                    sleep(waitAmount / 2);
                                }
                            } else if (nextNode.isEmpty()) {
                                moveTo(nextNode);
                                sleep(waitAmount / 2);
                            } else {
                                synchronized (platform.objectLocker) {
                                    if (currentNode.vehicle != null && currentNode.vehicle.equals(vehicle))
                                        currentNode.leave();
                                    else
                                        currentNode.emplace(currentNode.vehicle, previousLabel);
                                    currentNode = nextNode;
                                    currentNode.emplace(currentNode.vehicle, labelText);
                                }
                                sleep(waitAmount / 2);
                            }
                        }
                        
                        notThere = false;
                    } else {
                        if (currentNode.isParkingSpot)
                            moveTo(iterator.next());
                        else
                            currentNode.emplace(vehicle, labelText);
                        notThere = false;
                    }
                } else if (indexOfCrashPlatform < Garage.indexOf(platform)) {
                    isLookingForExit = true;
                    isLookingForParking = false;
                    int indexOfCurrentNode = platform.getCardinalIndex(currentNode);
                    if ((indexOfCurrentNode / GRID_WIDTH) == 1) {
                        PlatformNode upOne = platform.getPlatformPlace(0, indexOfCurrentNode % GRID_WIDTH);
                        waitForNextNode(upOne);
                        moveTo(upOne);
                    }
                    while (iterator.hasNext()) {
                        nextNode = iterator.next();
                        emergencyVehicleMove(nextNode);
                    }
                    iterator = extendRoute();
                    nextNode = iterator.next();
                    emergencyVehicleMove(nextNode);
                } else if (indexOfCrashPlatform > Garage.indexOf(platform)) {
                    isLookingForExit = false;
                    isLookingForParking = true;
                    int indexOfCurrentNode = platform.getCardinalIndex(currentNode);
                    if ((indexOfCurrentNode / GRID_WIDTH) == 0) {
                        PlatformNode downOne = platform.getPlatformPlace(1, indexOfCurrentNode % GRID_WIDTH);
                        waitForNextNode(downOne);
                        moveTo(downOne);
                    }
                    while (iterator.hasNext()) {
                        nextNode = iterator.next();
                        emergencyVehicleMove(nextNode);
                    }
                    iterator = extendRoute();
                    nextNode = iterator.next();
                    emergencyVehicleMove(nextNode);
                }
            }
        } catch (InterruptedException ignored) {
        }

    }


    private void emergencyVehicleMove(PlatformNode nextNode) throws InterruptedException {
        if (nextNode.isEmpty()) {

            moveTo(nextNode);
            sleep(waitAmount / 2);
        } else if (nextNode.nodeLabel.getText().contains("R")) {

            while (nextNode.nodeLabel.getText().contains("R"))
                sleep(waitAmount / 2);
            moveTo(nextNode);
            sleep(waitAmount / 2);
        } else {

            overtake(nextNode);
        }
    }

    private PlatformNode overtake(PlatformNode nextNode) {

        String previousLabel = nextNode.nodeLabel.getText();

        boolean overtakeOngoing = true;
        synchronized (platform.objectLocker) {
            if (currentNode.vehicle != null && currentNode.vehicle == vehicle)
                currentNode.leave();
            else
                currentNode.overtakeLeave();

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
        } catch (InterruptedException ignored) {
        }
        while (iterator.hasNext() && overtakeOngoing) {
            if (nextNode.nodeLabel.getText().contains("R")) {

                return nextNode;
            }
            synchronized (platform.objectLocker) {
                currentNode.emplace(currentNode.vehicle, previousLabel);
                previousLabel = nextNode.nodeLabel.getText();

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
            } catch (InterruptedException ignored) {
            }
        }
        if (overtakeOngoing) {
            if (nextNode.nodeLabel.getText().contains("R"))
                while (nextNode.nodeLabel.getText().contains("R")) {
                    try {
                        sleep(waitAmount / 2);
                    } catch (InterruptedException ignored) {
                    }
                }
            synchronized (platform.objectLocker) {
                currentNode.emplace(currentNode.vehicle, previousLabel);
                currentNode = nextNode;
                currentNode.emplace(currentNode.vehicle, labelText);
            }
            try {
                sleep(waitAmount / 2);
            } catch (InterruptedException ignored) {
            }
        }
        return null;
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
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    if (nextNode.isEmpty()) {
                        moveTo(nextNode);
                        try {
                            sleep(waitAmount);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                return platform.propagatedExitRoute.listIterator(platform.propagatedExitRoute.indexOf(platform.getPlatformPlace(0, indexOfCurrentNode % GRID_WIDTH)));
            } else if ((indexOfCurrentNode == 0)) {

                if (Garage.indexOf(platform) == 0) {
                    return new ArrayList<PlatformNode>().iterator();
                } else {
                    platform.getPlatformVehicles().remove(this.vehicle);
                    platform.traversalNodes.remove(this);
                    platform = Garage.get(Garage.indexOf(platform) - 1);
                    platform.getPlatformVehicles().add(this.vehicle);
                    platform.traversalNodes.add(this);
                    return platform.propagatedExitRoute.listIterator();
                }
            }
            if ((Garage.indexOf(platform) == (Garage.size() - 1)) && (indexOfCurrentNode == 15)) {
                return platform.propagatedExitRoute.listIterator(0);
            }
        }
        if (isLookingForParking) {
            if ((Garage.indexOf(platform) == (Garage.size() - 1)) && (indexOfCurrentNode == 15)) {
                if (!rotationIsOn()) {
                    isLookingForParking = false;
                    isLookingForExit = true;
                }
                return platform.propagatedExitRoute.listIterator(0);
            } else if (indexOfCurrentNode == 22) {
                PlatformNode nextNode = platform.getPlatformPlace(1, indexOfCurrentNode % GRID_WIDTH);
                return platform.fullPlatformParkingRoute.listIterator(platform.fullPlatformParkingRoute.indexOf(nextNode));
            } else if (indexOfCurrentNode == 18) {
                PlatformNode nextNode = platform.getPlatformPlace(1, indexOfCurrentNode % GRID_WIDTH);
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
            } catch (InterruptedException ignored) {
            }
        }
    }

    public boolean isMoving() {
        return isMoving;
    }

}



















