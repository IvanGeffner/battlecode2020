package rushbot;

import battlecode.common.*;

public class Design extends MyRobot {

    RobotController rc;
    Comm comm;
    MapLocation myLoc;
    BuildingZone buildingZone;
    boolean needDrone;
    boolean enemyNetGunNearby;
    Direction[] dirs = Direction.values();

    MapLocation enemyHQloc = null;

    Design(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        myLoc = rc.getLocation();
        buildingZone = new BuildingZone(rc);
    }

    void play(){
        if (comm.singleMessage()) comm.readMessages();
        checkEnemyHQ();
        build(RobotType.LANDSCAPER);
        //BuildingManager.printDebug(comm);
        comm.readMessages();
        if (comm.wallMes != null) buildingZone.update(comm.wallMes);
        if (!buildingZone.finished()) buildingZone.run();
    }

    boolean shouldBuildLandscaper(){
        if (!comm.upToDate()) return false;
        if (!BuildingManager.haveSoupToSpawn(rc, RobotType.LANDSCAPER)) return false;
        if (comm.rush && enemyNetGunNearby && comm.buildings[RobotType.LANDSCAPER.ordinal()] == 0) return true;
        if (needDrone) return false;
        if (comm.rush) return true;
        return BuildingManager.shouldBuildLandscaper(comm, rc);
    }

    void build (RobotType r){
        try{
            if (enemyHQloc == null) return;
            LandscaperBuildingSpot bestSpot = null;
            for (Direction dir : dirs){
                LandscaperBuildingSpot spot = new LandscaperBuildingSpot(dir);
                if (spot.isBetterThan(bestSpot)) bestSpot = spot;
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

    /*boolean visibleLandscaper(){
        RobotInfo[] visibleRobots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        for (RobotInfo r : visibleRobots){
            if (r.getType() == RobotType.LANDSCAPER) return true;
        }
        return false;
    }*/

    class LandscaperBuildingSpot{
        Direction dir;
        boolean canBuild;
        MapLocation loc;
        int zone;
        int dist;

        int buildingScore = -1;

        int score(){
            if (buildingScore >= 0) return buildingScore;
            buildingScore = loc.distanceSquaredTo(enemyHQloc);
            return buildingScore;
        }

        LandscaperBuildingSpot(Direction dir){
            this.dir = dir;
            loc = myLoc.add(dir);
            if (rc.canSenseLocation(loc)){
                canBuild = rc.canBuildRobot(RobotType.LANDSCAPER, dir);
                zone = buildingZone.getZone(loc);
            }
        }

        boolean isBetterThan(LandscaperBuildingSpot s){
            if (!canBuild) return false;
            if (s == null) return true;
            return score() < s.score();
        }

    }

    void checkUnits(){
        enemyNetGunNearby = false;
        needDrone = false;
        int match = 0;
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r : robots){
            if (r.team != rc.getTeam()) {
                switch (r.type) {
                    case LANDSCAPER:
                    case MINER:
                        ++match;
                        break;
                    case NET_GUN:
                        enemyNetGunNearby = true;
                        break;
                }
            } else{
                switch (r.type) {
                    case DELIVERY_DRONE:
                        if (r.isCurrentlyHoldingUnit()){
                            --match;
                            break;
                        }
                    default:
                        break;
                }
            }
        }
        needDrone = match > 0;
    }

    void checkEnemyHQ(){
        if (enemyHQloc != null) return;
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        for (RobotInfo r : robots){
            if (r.type == RobotType.HQ && r.team != rc.getTeam()){
                enemyHQloc = r.location;
                return;
            }
        }
    }

}
