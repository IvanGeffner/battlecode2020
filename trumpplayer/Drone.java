package trumpplayer;

import battlecode.common.*;

public class Drone extends MyRobot{

    RobotController rc;
    Comm comm;
    MapLocation myLoc;
    ExploreDrone exploreDrone;
    DroneBugPath droneBugPath;

    RobotInfo robotHeld;

    Drone(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        //myLoc = rc.getLocation();
        exploreDrone = new ExploreDrone(rc, comm);
        droneBugPath = new DroneBugPath(rc, comm);
    }

    void play(){
        if (comm.singleMessage()) comm.readMessages();
        exploreDrone.update();
        exploreDrone.checkComm();
        tryGrabEnemy();
        tryDropEnemy();
        MapLocation target = getTarget();
        if (target != null){
            droneBugPath.moveTo(target);
        }
        comm.readMessages();
    }

    MapLocation getTarget(){
        if (!rc.isReady()) return null;
        if (rc.isCurrentlyHoldingUnit()){
            if (exploreDrone.closestWater != null) return exploreDrone.closestWater;
            if (comm.water != null) return comm.water;
            return exploreDrone.exploreTarget();
        }
        if (exploreDrone.closestLandscaper != null) return exploreDrone.closestLandscaper.location;
        if (exploreDrone.closestMiner != null) return exploreDrone.closestMiner.location;
        if (comm.EnemyHQLoc != null) return comm.EnemyHQLoc;
        return exploreDrone.exploreTarget();
    }

    void tryGrabEnemy(){
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
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void tryDropEnemy(){
        if (!rc.isReady()) return;
        if (!rc.isCurrentlyHoldingUnit()) return;
        MapLocation myLoc = rc.getLocation();
        try {
            Direction dir = Direction.NORTH;
            for (int i = 0; i < 8; ++i){
                MapLocation newLoc = myLoc.add(dir);
                if (!rc.canDropUnit(dir)){
                    dir = dir.rotateLeft();
                    continue;
                }
                if (rc.canSenseLocation(newLoc) && rc.senseFlooding(newLoc)){
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

}
