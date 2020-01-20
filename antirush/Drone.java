package antirush;

import battlecode.common.*;

public class Drone extends MyRobot {

    RobotController rc;
    Comm comm;
    ExploreDrone exploreDrone;
    DroneBugPath droneBugPath;
    BuildingZone buildingZone;
    Direction[] dirs = Direction.values();

    RobotInfo robotHeld;

    int round;

    Drone(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        //myLoc = rc.getLocation();
        buildingZone = new BuildingZone(rc);
        exploreDrone = new ExploreDrone(rc, comm, buildingZone);
        droneBugPath = new DroneBugPath(rc, comm);
    }

    void play(){

        //UPDATE STUFF
        if (comm.singleMessage()) comm.readMessages();
        round = rc.getRoundNum();
        exploreDrone.update();
        exploreDrone.checkComm();


        MapLocation w = getClosestWater();
        //if (w != null) rc.setIndicatorDot(w, 255, 255, 255);
        //else System.out.println("NO WATER");
        tryGrab();
        tryDropEnemy();
        tryDropAlly();
        MapLocation target = getTarget();
        if (target != null){
            droneBugPath.updateGuns(exploreDrone.cantMove);
            droneBugPath.moveTo(target);
        }
        if (comm.wallMes != null) buildingZone.update(comm.wallMes);
        buildingZone.run();
        comm.readMessages();
    }

    MapLocation getTarget(){
        if (!rc.isReady()) return null;
        if (rc.isCurrentlyHoldingUnit()){
            if (robotHeld == null || robotHeld.getTeam() != rc.getTeam()) {
                if (exploreDrone.closestWater != null) return exploreDrone.closestWater;
                if (comm.water != null) return comm.water;
                return exploreDrone.exploreTarget();
            }
            else{
                if (shouldPrepareClutch()){
                    if (exploreDrone.closestEnemyBuilding != null) return exploreDrone.closestEnemyBuilding;
                    MapLocation enemyHQ = comm.getEnemyHQLoc();
                    if (enemyHQ != null) return enemyHQ;
                    return exploreDrone.exploreTarget();
                }
                if (exploreDrone.closestFinishedWall != null) return exploreDrone.closestFinishedWall;
                else return exploreDrone.exploreTarget();
            }
        }
        if (exploreDrone.closestLandscaper != null) return exploreDrone.closestLandscaper.location;
        if (exploreDrone.closestMiner != null) return exploreDrone.closestMiner.location;
        if (buildingZone.HQloc != null && comm.isRush()) return buildingZone.HQloc;
        if (exploreDrone.stuckAlly != null && exploreDrone.closestFinishedWall != null){
            return exploreDrone.stuckAlly.location;
        }
        if (shouldPrepareClutch() && exploreDrone.closestLandscaperMyTeam != null) return exploreDrone.closestLandscaperMyTeam.location;
        MapLocation enemyHQ = comm.getEnemyHQLoc();
        if (enemyHQ != null) return enemyHQ;
        MapLocation loc = getBestGuess();
        if (loc != null) return loc;
        return exploreDrone.exploreTarget();
    }

    MapLocation getBestGuess(){
        MapLocation ans = null;
        int bestDist = 0;
        MapLocation myLoc = rc.getLocation();
        MapLocation hor = comm.getHorizontal(), ver = comm.getVertical(), rot = comm.getRotational();
        if (hor != null){
            int t = myLoc.distanceSquaredTo(hor);
            if (ans == null || t < bestDist){
                ans = hor;
                bestDist = t;
            }
        }
        if (ver != null){
            int t = myLoc.distanceSquaredTo(ver);
            if (ans == null || t < bestDist){
                ans = ver;
                bestDist = t;
            }
        }
        if (rot != null){
            int t = myLoc.distanceSquaredTo(rot);
            if (ans == null || t < bestDist){
                ans = rot;
                bestDist = t;
            }
        }
        return ans;
    }

    void tryGrab(){
        if (!rc.isReady()) return;
        if (rc.isCurrentlyHoldingUnit()) return;
        try {
            if (exploreDrone.closestLandscaper != null && rc.canPickUpUnit(exploreDrone.closestLandscaper.getID())) {
                rc.pickUpUnit(exploreDrone.closestLandscaper.getID());
                robotHeld = exploreDrone.closestLandscaper;
                return;
            }
            if (exploreDrone.closestMiner != null && rc.canPickUpUnit(exploreDrone.closestMiner.getID())) {
                rc.pickUpUnit(exploreDrone.closestMiner.getID());
                robotHeld = exploreDrone.closestMiner;
                return;
            }
            if (!comm.isRush() && exploreDrone.stuckAlly != null && exploreDrone.closestFinishedWall != null && rc.canPickUpUnit(exploreDrone.stuckAlly.getID())) {
                rc.pickUpUnit(exploreDrone.stuckAlly.getID());
                robotHeld = exploreDrone.stuckAlly;
                return;
            }
            if (shouldPrepareClutch()){
                if (exploreDrone.closestLandscaperMyTeam != null && rc.canPickUpUnit(exploreDrone.closestLandscaperMyTeam.getID())) {
                    rc.pickUpUnit(exploreDrone.closestLandscaperMyTeam.getID());
                    robotHeld = exploreDrone.closestLandscaperMyTeam;
                    return;
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void tryDropEnemy(){
        if (!rc.isReady()) return;
        if (!rc.isCurrentlyHoldingUnit()) return;
        if (robotHeld != null && robotHeld.team == rc.getTeam()) return;
        MapLocation myLoc = rc.getLocation();
        try {
            for (Direction dir : dirs){
                MapLocation newLoc = myLoc.add(dir);
                if (!rc.canDropUnit(dir)) continue;
                if (rc.canSenseLocation(newLoc) && rc.senseFlooding(newLoc)){
                    rc.dropUnit(dir);
                    robotHeld = null;
                    return;
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void tryDropAlly(){
        if (!rc.isReady()) return;
        if (!rc.isCurrentlyHoldingUnit()) return;
        if (robotHeld == null || robotHeld.team != rc.getTeam()) return;
        if (!buildingZone.finished()) return;
        //boolean dropOnEnemyBuilding = rc.getRoundNum() >= Constants.MIN_TURN_CLUTCH;
        if (robotHeld.type == RobotType.LANDSCAPER && shouldPrepareClutch()){
            tryDropNextToBuilding();
            return;
        }
        MapLocation myLoc = rc.getLocation();
        try {
            Direction dir = Direction.NORTH;
            for (int i = 0; i < 8; ++i){
                MapLocation newLoc = myLoc.add(dir);
                if (!rc.canDropUnit(dir)){
                    dir = dir.rotateLeft();
                    continue;
                }
                if (rc.canSenseLocation(newLoc) && buildingZone.isWall(newLoc) && rc.senseElevation(newLoc) == Constants.WALL_HEIGHT){
                    rc.dropUnit(dir);
                    robotHeld = null;
                    return;
                }
                dir = dir.rotateLeft();
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void tryDropNextToBuilding(){
        if (exploreDrone.closestEnemyBuilding == null) return;
        MapLocation myLoc = rc.getLocation();
        try {
            Direction dir = Direction.NORTH;
            for (int i = 0; i < 8; ++i){
                MapLocation newLoc = myLoc.add(dir);
                if (!rc.canDropUnit(dir)){
                    dir = dir.rotateLeft();
                    continue;
                }
                if (exploreDrone.closestEnemyBuilding.distanceSquaredTo(newLoc) <= 2){
                    rc.dropUnit(dir);
                    robotHeld = null;
                    return;
                }
                dir = dir.rotateLeft();
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    boolean shouldClutch(){
        return round >= Constants.MIN_TURN_CLUTCH;
    }

    boolean shouldPrepareClutch(){
        return round >= Constants.MIN_TURN_PREPARE_CLUTCH;
    }

    MapLocation getClosestWater(){
        if (exploreDrone.closestWater != null){
            return exploreDrone.closestWater;
        }
        return comm.water;
    }

}
