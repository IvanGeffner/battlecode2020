package trumpplayer;

import battlecode.common.*;

public class Fullfillment extends MyRobot{

    RobotController rc;
    Comm comm;
    MapLocation myLoc;

    Fullfillment(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        myLoc = rc.getLocation();
    }

    void play(){
        if (comm.singleMessage()) comm.readMessages();
        if (shouldBuildDrone()) {
            build(RobotType.DELIVERY_DRONE);
        }
        comm.readMessages();
    }

    boolean shouldBuildDrone(){
        if (!comm.upToDate()) return false;
        return BuildingManager.shouldBuildDrone(comm);
    }

    void build (RobotType r){
        try{
            Direction dir = Direction.NORTH;
            for (int i = 0; i < 8; ++i){
                MapLocation loc = myLoc.add(dir);
                if (rc.canSenseLocation(loc) && !rc.senseFlooding(loc)){
                    if (rc.canBuildRobot(r, dir)){
                        rc.buildRobot(r, dir);
                        comm.sendMessage(Comm.BUILDING_TYPE, r.ordinal());
                        return;
                    }
                }
                dir = dir.rotateLeft();
            }
        } catch(Throwable t){
            t.printStackTrace();
        }
    }

}
