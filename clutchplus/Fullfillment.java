package clutchplus;

import battlecode.common.*;

public class Fullfillment extends MyRobot {

    RobotController rc;
    Comm comm;
    MapLocation myLoc;
    boolean[] cantMove;
    Direction[] dirs = Direction.values();

    Fullfillment(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        myLoc = rc.getLocation();
    }

    void play(){
        if (comm.singleMessage()) comm.readMessages();
        if (shouldBuildDrone()) {
            updateCantSpawn();
            build(RobotType.DELIVERY_DRONE);
        }
        BuildingManager.printDebug(comm);
        comm.readMessages();
    }

    boolean shouldBuildDrone(){
        if (visibleLandscaper()) return true;
        if (!BuildingManager.haveSoupToSpawn(rc, RobotType.DELIVERY_DRONE)) return false;
        if (!comm.upToDate()) return false;
        return BuildingManager.shouldBuildDrone(comm, rc);
    }

    void build (RobotType r){
        try{
            for (Direction dir : dirs){
                if (cantMove[dir.ordinal()]) continue;
                MapLocation loc = myLoc.add(dir);
                if (rc.canSenseLocation(loc)){
                    if (rc.canBuildRobot(r, dir)){
                        rc.buildRobot(r, dir);
                        comm.sendMessage(Comm.BUILDING_TYPE, r.ordinal());
                        return;
                    }
                }
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

    void updateCantSpawn(){
        cantMove = new boolean[9];
        RobotInfo[] visibleRobots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        for (RobotInfo r : visibleRobots){
            if (r.getType() == RobotType.NET_GUN || r.getType() == RobotType.HQ) addDanger(r.location);
        }
    }

    void addDanger(MapLocation loc){
        //System.out.println("Adding danger! " + loc);
        MapLocation newLoc = myLoc.add(Direction.NORTH);
        if (newLoc.distanceSquaredTo(loc) <= 13) cantMove[Direction.NORTH.ordinal()] = true;
        newLoc = myLoc.add(Direction.NORTHWEST);
        if (newLoc.distanceSquaredTo(loc) <= 13) cantMove[Direction.NORTHWEST.ordinal()] = true;
        newLoc = myLoc.add(Direction.WEST);
        if (newLoc.distanceSquaredTo(loc) <= 13) cantMove[Direction.WEST.ordinal()] = true;
        newLoc = myLoc.add(Direction.SOUTHWEST);
        if (newLoc.distanceSquaredTo(loc) <= 13) cantMove[Direction.SOUTHWEST.ordinal()] = true;
        newLoc = myLoc.add(Direction.SOUTH);
        if (newLoc.distanceSquaredTo(loc) <= 13) cantMove[Direction.SOUTH.ordinal()] = true;
        newLoc = myLoc.add(Direction.SOUTHEAST);
        if (newLoc.distanceSquaredTo(loc) <= 13) cantMove[Direction.SOUTHEAST.ordinal()] = true;
        newLoc = myLoc.add(Direction.EAST);
        if (newLoc.distanceSquaredTo(loc) <= 13) cantMove[Direction.EAST.ordinal()] = true;
        newLoc = myLoc.add(Direction.NORTHEAST);
        if (newLoc.distanceSquaredTo(loc) <= 13) cantMove[Direction.NORTHEAST.ordinal()] = true;
        newLoc = myLoc.add(Direction.CENTER);
        if (newLoc.distanceSquaredTo(loc) <= 13) cantMove[Direction.CENTER.ordinal()] = true;
    }

}
