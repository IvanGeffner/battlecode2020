package trumpplayer;

import battlecode.common.*;

public class Drone extends MyRobot{

    RobotController rc;
    Comm comm;
    MapLocation myLoc;
    ExploreDrone exploreDrone;
    DroneBugPath droneBugPath;

    Drone(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        //myLoc = rc.getLocation();
        exploreDrone = new ExploreDrone(rc);
        droneBugPath = new DroneBugPath(rc);
    }

    void play(){
        if (comm.singleMessage()) comm.readMessages();
        exploreDrone.update();
        exploreDrone.checkComm(comm);
        MapLocation target = getTarget();
        if (target != null){
            droneBugPath.moveTo(target);
        }
        comm.readMessages();
    }

    MapLocation getTarget(){
        if (comm.EnemyHQLoc != null) return comm.EnemyHQLoc;
        return exploreDrone.exploreTarget();
    }

}
