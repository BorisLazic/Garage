package garage;

import garage.Vehicles.Vehicle;
import javafx.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static garage.Administrator.Garage;
import static garage.UserMode.GRID_WIDTH;


public class Traveler extends Thread implements Serializable {

    private static int start = 1;
    private int index;
    private static long waitAmount = 250;

    private PlatformNode currentNode;
    private Vehicle vehicle;
    private Platform platform;
    private String labelText;
    private ArrayList<PlatformNode> currentRoute;
    private Iterator<PlatformNode> iterator;
    private boolean isLookingForParking = false;//false indicates intention to exit
    private boolean isLookingForExit = false;
    private boolean isMoving;


    public Traveler(PlatformNode currentNode, Platform platform) {
        this.currentNode = currentNode;
        if (!currentNode.isParkingSpot) {
            isLookingForParking = true;
        } else {
            isLookingForExit = true;
        }
        this.vehicle = currentNode.vehicle;
        labelText = currentNode.nodeLabel.getText();
        this.platform = platform;
        Pair<ArrayList<PlatformNode>, Integer> row = this.platform.getPlatformNodeRow(currentNode);
        PlatformNode nextNode;
        if (isLookingForParking) {
            if (platform.freeParkingSpots > 0) {
                currentRoute = platform.freePlatformParkingRoute;
                iterator = currentRoute.listIterator(1);
            } else {
                currentRoute = platform.fullPlatformParkingRoute;
                iterator = currentRoute.listIterator(1);
            }
        } else if (isLookingForExit) {
            if ((row.getValue() % 4) == 0) {
                nextNode = row.getKey().get(row.getValue() + 1);
                currentRoute = new ArrayList<>(platform.findRoute(nextNode));
                iterator = currentRoute.listIterator(currentRoute.indexOf(nextNode));
            } else if ((row.getValue() % 4) == 3) {
                nextNode = row.getKey().get(row.getValue() - 1);
                currentRoute = new ArrayList<>(platform.findRoute(nextNode));
                iterator = currentRoute.listIterator(currentRoute.indexOf(nextNode));
            }
        }
    }

    public String toString() {
        return "Traveler" + index;
    }

    @Override
    public void run() {
        isMoving = true;
        while (true) {
            if (iterator.hasNext()) {
                while (iterator.hasNext()) {
                    PlatformNode nextNode = iterator.next();
                    setupNextCycle(nextNode);
                    if (isLookingForParking)
                        park();
                    try {
                        sleep(waitAmount * 2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            findNewRoute();
        }
    }

    private void setupNextCycle(PlatformNode nextNode) {
        waitForNextNode(nextNode);
        synchronized (platform.objectLocker) {
            currentNode.leave();
            currentNode = nextNode;
            currentNode.emplace(vehicle, labelText);
        }
    }

    private void waitForNextNode(PlatformNode nextNode) {
        while (!nextNode.isEmpty()) {
            try {
                sleep(waitAmount);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void park() {
        Pair<ArrayList<PlatformNode>, Integer> row = platform.getPlatformNodeRow(currentNode);
        if ((row.getValue() % 4) == 1) {
            PlatformNode parkingNode;
            if ((parkingNode = row.getKey().get(row.getValue() - 1)).isEmpty() && parkingNode.isParkingSpot) {
                iterator = (Collections.singletonList(parkingNode).listIterator(0));
                isLookingForParking = false;
                isLookingForExit = true;
                platform.freeParkingSpots--;
            } else if ((parkingNode = row.getKey().get(row.getValue() + 2)).isEmpty() && parkingNode.isParkingSpot) {
                iterator = Arrays.asList(row.getKey().get(row.getValue() + 1), row.getKey().get(row.getValue() + 2)).listIterator(0);
                isLookingForParking = false;
                isLookingForExit = true;
                platform.freeParkingSpots--;
            }
        }
    }

    private void findNewRoute() {
        int indexOfCurrentNode = platform.getCardinalIndex(currentNode);
        if (isLookingForExit) {
            if (((indexOfCurrentNode % 4) == 2) && ((indexOfCurrentNode / GRID_WIDTH) == 2)) {
                setupNextCycle(platform.getPlatformPlace(1, indexOfCurrentNode % GRID_WIDTH));
                try {
                    sleep(waitAmount * 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                iterator = platform.propagatedExitRoute.listIterator(platform.propagatedExitRoute
                        .indexOf(platform.getPlatformPlace(0, indexOfCurrentNode % GRID_WIDTH)));
                waitForRightSide(indexOfCurrentNode % GRID_WIDTH);
            } else if ((indexOfCurrentNode == 0)) {
                if (Garage.indexOf(platform) == 0) {
                    System.out.println("We done, charge me \n");
                    return;
                }
                platform = Garage.get(Garage.indexOf(platform) - 1);
                iterator = platform.propagatedExitRoute.listIterator();
            }
        }
        if(isLookingForParking){
            if(Garage.indexOf(platform) == (Garage.size() - 1)){
                iterator = platform.propagatedExitRoute.listIterator(0);
                return;
            }

        }
    }

    private void waitForRightSide(int horizontalIndex) {
        ArrayList<PlatformNode> prioritizedNodes = new ArrayList<>(Arrays.asList(platform.getPlatformPlace(0, horizontalIndex - 1),
                platform.getPlatformPlace(0, horizontalIndex), platform.getPlatformPlace(0, horizontalIndex + 1)));
        while (!prioritizedNodes.get(0).isEmpty() && !prioritizedNodes.get(1).isEmpty() && !prioritizedNodes.get(2).isEmpty()) {
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



















