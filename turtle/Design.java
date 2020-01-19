package turtle;

import battlecode.common.*;

public class Design extends MyRobot {

    RobotController rc;
    Comm comm;
    MapLocation myLoc;
    BuildingZone buildingZone;

    Design(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        myLoc = rc.getLocation();
        buildingZone = new BuildingZone(rc);
    }

    void play(){
        if (comm.singleMessage()) comm.readMessages();
        if (shouldBuildLandscaper()) {
            build(RobotType.LANDSCAPER);
        }
        comm.readMessages();
        if (comm.wallMes != null) buildingZone.update(comm.wallMes);
        if (!buildingZone.finished()) buildingZone.run();
    }

    boolean shouldBuildLandscaper(){
        if (!comm.upToDate()) return false;
        if (visibleLandscaper()) return false;
        return BuildingManager.shouldBuildLandscaper(comm, rc);
    }

    void build (RobotType r){
        try{
            if (Constants.DEBUG == 1) System.out.println("Trying to build landscaper!");
            Direction dir = Direction.NORTH;
            if (!buildingZone.finished()) return;
            LandscaperBuildingSpot bestSpot = null;
            for (int i = 0; i < 8; ++i){
                LandscaperBuildingSpot spot = new LandscaperBuildingSpot(dir);
                if (spot.isBetterThan(bestSpot)) bestSpot = spot;
                dir = dir.rotateLeft();
            }
            if (bestSpot != null){
                rc.buildRobot(r, bestSpot.dir);
                comm.sendMessage(Comm.BUILDING_TYPE, r.ordinal());
                return;
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

    class LandscaperBuildingSpot{
        Direction dir;
        boolean wall;
        boolean canBuild;

        LandscaperBuildingSpot(Direction dir){
            canBuild = rc.canBuildRobot(RobotType.LANDSCAPER, dir);
            if (canBuild){
                this.dir = dir;
                wall = buildingZone.isWall(myLoc.add(dir));
            }
        }

        boolean isBetterThan(LandscaperBuildingSpot s){
            if (!canBuild) return false;
            if (s == null) return true;
            if (wall) return true;
            return false;
        }

    }

}
