package trumpplayer;

import battlecode.common.*;

public class Design extends MyRobot{

    RobotController rc;
    Comm comm;
    MapLocation myLoc;

    Design(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        myLoc = rc.getLocation();
    }

    void play(){
        if (comm.singleMessage()) comm.readMessages();
        if (shouldBuildLandscaper()) {
            build(RobotType.LANDSCAPER);
        }
        comm.readMessages();
    }

    boolean shouldBuildLandscaper(){
        if (!comm.upToDate()) return false;
        if (visibleLandscaper()) return false;
        return BuildingManager.shouldBuildLandscaper(comm, rc);
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

    boolean visibleLandscaper(){
        RobotInfo[] visibleRobots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        for (RobotInfo r : visibleRobots){
            if (r.getType() == RobotType.LANDSCAPER) return true;
        }
        return false;
    }

}
