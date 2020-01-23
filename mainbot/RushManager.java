package mainbot;

import battlecode.common.*;

public class RushManager {

    RobotController rc;
    Comm comm;
    Team myTeam, opponent;

    int[] enemiesSeen;
    int freeDrones = 0;
    int robotLength;
    MapLocation fullfillmentLocation = null;
    int closestMinerDist;
    MapLocation closestMiner;

    int turnEnemyBuilt = -100;
    boolean enemyBuildingsInSight;

    RushManager (RobotController rc, Comm comm){
        this.rc = rc;
        this.comm = comm;
        myTeam = rc.getTeam(); opponent = myTeam.opponent();
        robotLength = RobotType.values().length;
        if (rc.getType() == RobotType.FULFILLMENT_CENTER) fullfillmentLocation = rc.getLocation();
    }

    RobotType rushBuild(){
        if (!comm.isRush()) return null;
        check();
        int myLandscapers = comm.buildings[RobotType.LANDSCAPER.ordinal()];
        int myDrones = comm.buildings[RobotType.DELIVERY_DRONE.ordinal()];
        int theirLandscapers = enemiesSeen[RobotType.LANDSCAPER.ordinal()];
        int theirMiners = enemiesSeen[RobotType.MINER.ordinal()];

        //FULFILLMENT CANT BUILD
        if (fullfillmentCantBuild()){
            if (myLandscapers == 0) return RobotType.LANDSCAPER;
            if (enemyBuildingsInSight){
                if (myLandscapers <= theirLandscapers) return RobotType.LANDSCAPER;
            }
            if (enemiesSeen[RobotType.NET_GUN.ordinal()] == 0 && freeDrones < theirLandscapers + theirMiners) return RobotType.DELIVERY_DRONE;
            return null;
        }

        //TRY DRONE
        if (myDrones <= 0 || freeDrones < theirLandscapers + theirMiners) return RobotType.DELIVERY_DRONE;

        //TRY LANDSCAPERS
        if (myLandscapers == 0) return RobotType.LANDSCAPER;
        if (enemyBuildingsInSight){
            if (myLandscapers <= theirLandscapers) return RobotType.LANDSCAPER;
        }

        return null;
    }

    boolean fullfillmentCantBuild(){
        if (fullfillmentLocation == null) return true;
        if (enemiesSeen[RobotType.NET_GUN.ordinal()] > 0) return true;
        if (closestMinerDist < 0) return false;
        if (closestMinerDist > 13) return false;
        if (turnEnemyBuilt > rc.getRoundNum() - 5) return false;
        return true;
    }

    void check(){
        int[] enemyRobots = new int[robotLength];
        freeDrones = 0;
        closestMinerDist = -1;
        closestMiner = null;
        enemyBuildingsInSight = false;
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r : robots){
            if (r.getTeam() == opponent){
                ++enemyRobots[r.getType().ordinal()];
                switch(r.getType()) {
                    case MINER:
                        if (fullfillmentLocation != null) {
                            int d = fullfillmentLocation.distanceSquaredTo(r.location);
                            if (closestMinerDist < 0 || closestMinerDist > d) {
                                closestMinerDist = d;
                                closestMiner = r.location;
                            }
                        }
                        break;
                    case FULFILLMENT_CENTER:
                    case DESIGN_SCHOOL:
                    case NET_GUN:
                        enemyBuildingsInSight = true;
                        break;
                }
            } else{
                switch(r.getType()){
                    case DELIVERY_DRONE:
                        if (!r.isCurrentlyHoldingUnit())++freeDrones;
                        break;
                    case FULFILLMENT_CENTER:
                        fullfillmentLocation = r.location;
                        break;
                }
            }
        }
        if (enemiesSeen == null){
            enemiesSeen = enemyRobots;
            return;
        }
        int index = RobotType.LANDSCAPER.ordinal();
        if (enemyRobots[index] > enemiesSeen[index]){
            turnEnemyBuilt = rc.getRoundNum();
        }
        index = RobotType.DESIGN_SCHOOL.ordinal();
        if (enemyRobots[index] > enemiesSeen[index]){
            turnEnemyBuilt = rc.getRoundNum();
        }
        enemiesSeen = enemyRobots;
    }
}
