package clutch;

import battlecode.common.*;

public class Miner extends MyRobot {

    RobotController rc;
    Direction[] dirs = Direction.values();
    BugPath bugPath;
    ExploreMiner explore;
    WaterManager waterManager;
    Comm comm;
    BuildingZone buildingZone;
    Danger danger;
    boolean builder;
    MapLocation myLoc;

    final int MAX_ELEVATION = 2;
    final int MIN_SOUP_FOR_REFINERY = 1000;
    final int MIN_DIST_FOR_REFINERY = 50;

    int maxRadius = 0;

    Miner(RobotController rc){
        this.rc = rc;
        waterManager = new WaterManager(rc);
        comm = new Comm(rc);
        danger = new Danger();
        builder = comm.checkBuilder();
        explore = new ExploreMiner(rc, comm, danger);
        bugPath = new BugPath(rc);
        buildingZone = new BuildingZone(rc);
    }

    void play(){
        if (comm.singleMessage()) comm.readMessages();
        waterManager.update();
        if (!builder) explore.updateMiner();
        else explore.updateBuilder();
        explore.checkComm();
        bugPath.update();
        if (explore.dronesFound){
            bugPath.updateDrones(danger);
            if (explore.shouldBuildNetGun()) build(RobotType.NET_GUN);
        }

        updateMaxRadius();

        if (Constants.DEBUG == 1) System.out.println("Bytecode post bugPath update " + Clock.getBytecodeNum());
        //if (Constants.DEBUG == 1 && comm.EnemyHQLoc != null) rc.setIndicatorLine(rc.getLocation(), comm.EnemyHQLoc, 0, 255, 0);

        //if (rc.getRoundNum() == 320 && buildingZone.finished()) buildingZone.debugPrint();
        boolean flee = false;
        if (bugPath.shouldFlee && WaterManager.closestSafeCell != null){
            bugPath.moveTo(WaterManager.closestSafeCell);
            flee = true;
        }
        if (Constants.DEBUG == 1) System.out.println("Bytecode post trying to flee water " + Clock.getBytecodeNum());
        Direction miningDir = getMiningDir();
        if (miningDir != null && bugPath.canMoveArray[Direction.CENTER.ordinal()]) {
            tryBuildRefinery();
            tryMine(miningDir);
        }
        if (Constants.DEBUG == 1) System.out.println("Bytecode post mining " + Clock.getBytecodeNum());
        tryDeposit();
        tryBuilding();
        if (Constants.DEBUG == 1) System.out.println("Bytecode post deposit/build " + Clock.getBytecodeNum());
       if (!flee){
           MapLocation target;
           if (!builder) {
               target = getTarget();
               //if (target != null) rc.setIndicatorLine(rc.getLocation(), target, 255, 255, 255);
           }
           else{
                target = explore.HQloc;
           }
           bugPath.moveTo(target);
       }
       comm.readMessages();
       comm.getEnemyHQLoc();
       if (comm.wallMes != null) buildingZone.update(comm.wallMes);
       buildingZone.run();
    }

    void updateMaxRadius () {

    }

    MapLocation getTarget(){
        if (rc.getSoupCarrying() >= rc.getType().soupLimit) return explore.closestRefineryLoc;
        if (rc.getSoupCarrying() > 0){
            if (explore.closestSoup != null) return explore.closestSoup;
            else return explore.closestRefineryLoc;
        }
        MapLocation ans = explore.getBestTarget();
        if (ans != null) return ans;
        ans = getBuildingTarget();
        if (ans != null) return ans;
        if (comm.latestMiningLoc != null){
            if (explore.map[comm.latestMiningLoc.x][comm.latestMiningLoc.y] == 0) return comm.latestMiningLoc;
        }
        //ans = comm.getEnemyHQLoc();
        //if (ans != null && rc.getRoundNum() >= Constants.RUSH_TURN) return ans;
        return explore.exploreTarget();
    }

    MapLocation getBuildingTarget() {
        if (explore.HQloc == null) return null;
        RobotType r = BuildingManager.getNextBuilding(comm);
        if (r != null && rc.getTeamSoup() >= r.cost){
            if (rc.getLocation().distanceSquaredTo(explore.HQloc) <= Constants.DIST_TO_BUILD) return explore.HQloc;
        }
        return null;
    }


    boolean tryMine(Direction dir){
        try {
            if (!rc.isReady()) return false;
            if (rc.getSoupCarrying() >= rc.getType().soupLimit) return false;
            rc.mineSoup(dir);
        } catch (Throwable t){
            t.printStackTrace();
        }
        return false;
    }

    boolean tryBuildRefinery(){
        try {
            if (!rc.isReady()) return false;
            if (rc.getTeamSoup() < RobotType.REFINERY.cost) return false;
            if (explore.soupCont <= MIN_SOUP_FOR_REFINERY) return false;
            MapLocation myLoc = rc.getLocation();
            if (explore.closestRefineryLoc.distanceSquaredTo(myLoc) <= MIN_DIST_FOR_REFINERY) return false;
            build(RobotType.REFINERY);
        } catch (Throwable t){
            t.printStackTrace();
        }
        return false;
    }

    Direction getMiningDir(){
        try {
            MapLocation myLoc = rc.getLocation();
            for (Direction dir : dirs) {
                MapLocation newLoc = myLoc.add(dir);
                if (rc.canSenseLocation(newLoc) && rc.senseSoup(newLoc) > 0 && rc.canMineSoup(dir)) {
                    return dir;
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
        return null;
    }

    boolean tryDeposit(){
        try {
            if (rc.getSoupCarrying() == 0) return false;
            if (explore.closestRefineryLoc.distanceSquaredTo(rc.getLocation()) <= 2){
                Direction dir = rc.getLocation().directionTo(explore.closestRefineryLoc);
                if (rc.canDepositSoup(dir)) rc.depositSoup(dir, rc.getSoupCarrying());
                return true;
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
        return false;

    }

    void tryBuilding(){
        if (!rc.isReady()) return;
        if (!comm.upToDate()) return;
        if (!buildingZone.finished()) return;
        if (rc.getLocation().distanceSquaredTo(explore.HQloc) > Constants.DIST_TO_BUILD) return;
        RobotType type = BuildingManager.getNextBuilding(comm);
        if (type == null) return;
        if (Constants.DEBUG == 1) System.out.println(type.name());
        if (rc.getTeamSoup() <= type.cost) return;
        build(type);
    }

    RobotType buildType;

    void build(RobotType type){
        try {
            if (Constants.DEBUG == 1) System.out.println("Trying to build " + type.name());
            buildType = type;
            myLoc = rc.getLocation();

            BuildingSpot bestBuildingSpot = null;

            BuildingSpot s1 = new BuildingSpot(Direction.NORTH);
            BuildingSpot s2 = new BuildingSpot(Direction.NORTHEAST);
            BuildingSpot s3 = new BuildingSpot(Direction.EAST);
            BuildingSpot s4 = new BuildingSpot(Direction.SOUTHEAST);
            BuildingSpot s5 = new BuildingSpot(Direction.SOUTH);
            BuildingSpot s6 = new BuildingSpot(Direction.SOUTHWEST);
            BuildingSpot s7 = new BuildingSpot(Direction.WEST);
            BuildingSpot s8 = new BuildingSpot(Direction.NORTHWEST);

            if (s1.isBetter(bestBuildingSpot)) bestBuildingSpot = s1;
            if (s2.isBetter(bestBuildingSpot)) bestBuildingSpot = s2;
            if (s3.isBetter(bestBuildingSpot)) bestBuildingSpot = s3;
            if (s4.isBetter(bestBuildingSpot)) bestBuildingSpot = s4;
            if (s5.isBetter(bestBuildingSpot)) bestBuildingSpot = s5;
            if (s6.isBetter(bestBuildingSpot)) bestBuildingSpot = s6;
            if (s7.isBetter(bestBuildingSpot)) bestBuildingSpot = s7;
            if (s8.isBetter(bestBuildingSpot)) bestBuildingSpot = s8;

            if (bestBuildingSpot != null && rc.canBuildRobot(buildType, bestBuildingSpot.dir)){
                rc.buildRobot(buildType, bestBuildingSpot.dir);
                comm.sendMessage(comm.BUILDING_TYPE, type.ordinal());
            }







            /*Direction bestDir = null;
            int bestHeight = 0;
            Direction dir = Direction.NORTH;
            MapLocation myLoc = rc.getLocation();
            boolean notNextToWall = type == RobotType.FULFILLMENT_CENTER || type == RobotType.DESIGN_SCHOOL;
            for (int i = 0; i < 8; ++i) {
                if (rc.canBuildRobot(type, dir)) {
                    MapLocation newLoc = myLoc.add(dir);
                    if (rc.canSenseLocation(newLoc) && !rc.senseFlooding(newLoc)) {
                        int zone = buildingZone.map[newLoc.x][newLoc.y];
                        if (inside && zone != buildingZone.BUILDING_AREA && zone != buildingZone.NEXT_TO_WALL){
                            dir = dir.rotateLeft();
                            continue;
                        }
                        if (notNextToWall && buildingZone.map[newLoc.x][newLoc.y] != buildingZone.BUILDING_AREA){
                            dir = dir.rotateLeft();
                            continue;
                        }
                        else rc.setIndicatorDot(newLoc, 0, 0, 255);
                        int h = rc.senseElevation(newLoc);
                        if (bestDir == null) {
                            bestDir = dir;
                            bestHeight = h;
                        }
                    }
                }
                dir = dir.rotateLeft();
            }
            if (bestDir != null) {
                rc.buildRobot(type, bestDir);
                comm.sendMessage(comm.BUILDING_TYPE, type.ordinal());
            }*/
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    class BuildingSpot{

        Direction dir;
        MapLocation loc;
        int zone;
        boolean flooded;
        boolean canBuild;
        int height;
        int effectiveHeight;
        boolean waterAdj;

        int score = -1;

        BuildingSpot(Direction dir){
            try {
                this.dir = dir;
                loc = myLoc.add(dir);
                if (rc.canSenseLocation(loc)) {
                    canBuild = rc.canBuildRobot(buildType, dir);
                    zone = buildingZone.getZone(loc);
                    flooded = rc.senseFlooding(loc);
                    height = rc.senseElevation(loc);
                    effectiveHeight = height;
                    if (effectiveHeight > MAX_ELEVATION) effectiveHeight = MAX_ELEVATION;
                    if (zone == BuildingZone.HOLE) computeWaterAdj();
                    checkBuild();
                }
            } catch (Throwable t){
                t.printStackTrace();
            }

        }

        int score(){
           if (score >= 0) return score;
           switch(buildType) {
               case DESIGN_SCHOOL:
               case VAPORATOR:
                   switch (zone) {
                       case BuildingZone.BUILDING_AREA:
                           score = 2;
                           return score;
                       case BuildingZone.NEXT_TO_WALL:
                           score = 1;
                           return score;
                       default:
                           score = 0;
                           return score;
                   }
               case FULFILLMENT_CENTER:
                   switch (zone) {
                       case BuildingZone.NEXT_TO_WALL:
                           score = 2;
                           return score;
                       case BuildingZone.BUILDING_AREA:
                           score = 1;
                           return score;
                       default:
                           score = 0;
                           return score;
                   }
               default:
                   score = 2;
                   return score;
           }
        }

        void computeWaterAdj() {
            try {
                MapLocation newLoc = loc.add(Direction.NORTH);
                if (rc.canSenseLocation(newLoc) && rc.senseFlooding(newLoc)) {
                    waterAdj = true;
                    return;
                }
                newLoc = loc.add(Direction.NORTHEAST);
                if (rc.canSenseLocation(newLoc) && rc.senseFlooding(newLoc)) {
                    waterAdj = true;
                    return;
                }
                newLoc = loc.add(Direction.EAST);
                if (rc.canSenseLocation(newLoc) && rc.senseFlooding(newLoc)) {
                    waterAdj = true;
                    return;
                }
                newLoc = loc.add(Direction.SOUTHEAST);
                if (rc.canSenseLocation(newLoc) && rc.senseFlooding(newLoc)) {
                    waterAdj = true;
                    return;
                }
                newLoc = loc.add(Direction.SOUTH);
                if (rc.canSenseLocation(newLoc) && rc.senseFlooding(newLoc)) {
                    waterAdj = true;
                    return;
                }
                newLoc = loc.add(Direction.SOUTHWEST);
                if (rc.canSenseLocation(newLoc) && rc.senseFlooding(newLoc)) {
                    waterAdj = true;
                    return;
                }
                newLoc = loc.add(Direction.WEST);
                if (rc.canSenseLocation(newLoc) && rc.senseFlooding(newLoc)) {
                    waterAdj = true;
                    return;
                }
                newLoc = loc.add(Direction.NORTHWEST);
                if (rc.canSenseLocation(newLoc) && rc.senseFlooding(newLoc)) {
                    waterAdj = true;
                    return;
                }
            } catch (Throwable t){
                t.printStackTrace();
            }
        }

        void checkBuild(){
            if (buildType == RobotType.FULFILLMENT_CENTER || buildType == RobotType.DESIGN_SCHOOL || buildType == RobotType.VAPORATOR) {
                switch (zone) {
                    case BuildingZone.HOLE:
                        canBuild = canBuild && (buildingZone.HQloc.distanceSquaredTo(loc) <= maxRadius);
                        return;
                    case BuildingZone.OUTER_WALL:
                        canBuild = canBuild && height >= Constants.WALL_HEIGHT && (buildingZone.HQloc.distanceSquaredTo(loc) <= maxRadius) && buildingZone.canBuild(loc);
                        return;
                    case BuildingZone.WALL:
                        canBuild = false;
                        return;
                }
            }
        }

        boolean isBetter(BuildingSpot s){
            if (!canBuild) return false;
            if (s == null) return true;
            if (effectiveHeight > s.effectiveHeight) return true;
            if (s.effectiveHeight > effectiveHeight) return false;
            if (score() == s.score()) return height > s.height;
            return (score() > s.score());
        }

    }


}
