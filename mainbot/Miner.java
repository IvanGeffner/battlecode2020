package mainbot;

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
    boolean buildOnWall = false;
    MapLocation myLoc;

    final int MAX_ELEVATION = 2;
    final int MIN_SOUP_FOR_REFINERY = 1000;
    final int MIN_DIST_FOR_REFINERY = 50;
    final int MAX_BUILD_TURNS = 6;
    final int DESPERATE_TURNS = 30;

    int maxRadius = 0;

    int tryToBuildTurns = 0;
    RobotType typeToBuild;

    Miner(RobotController rc){
        this.rc = rc;
        waterManager = new WaterManager(rc);
        comm = new Comm(rc);
        danger = new Danger();
        builder = comm.checkBuilder();
        buildingZone = new BuildingZone(rc);
        explore = new ExploreMiner(rc, comm, danger, buildingZone);
        bugPath = new BugPath(rc);
    }

    void play(){

        //UPDATE STUFF

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
        updateBuildingTurns();
        System.out.println(typeToBuild);

        if (Constants.DEBUG == 1) System.out.println("Bytecode post bugPath update " + Clock.getBytecodeNum());

        // TRY ESCAPING FROM DRONES

        boolean flee = false;
        if (bugPath.shouldFlee && WaterManager.closestSafeCell != null){
            bugPath.moveTo(WaterManager.closestSafeCell);
            flee = true;
        }

        if (Constants.DEBUG == 1) System.out.println("Bytecode post trying to flee water " + Clock.getBytecodeNum());

        //TRY TO BUILD
        tryBuilding();

        //IF I CAN STAY ON MY LOCATION, TRY BUILDING REFINERY OR TRY MINING
        Direction miningDir = getMiningDir();
        if (miningDir != null && bugPath.canMoveArray[Direction.CENTER.ordinal()]) {
            tryBuildRefinery();
            tryMine(miningDir);
        }

        if (Constants.DEBUG == 1) System.out.println("Bytecode post mining " + Clock.getBytecodeNum());

        //TRY DEPOSITING

        tryDeposit();

        if (Constants.DEBUG == 1) System.out.println("Bytecode post deposit/build " + Clock.getBytecodeNum());

        //CHOOSE TARGET AND MOVE

       if (!flee){
           MapLocation target;
           if (!builder) {
               target = getTarget();
           }
           else{
                target = explore.HQloc;
           }
           bugPath.moveTo(target);
       }


       //END OF TURN STUFF

       comm.readMessages();
       comm.getEnemyHQLoc();
       if (comm.wallMes != null) buildingZone.update(comm.wallMes);
       buildingZone.run();

    }

    void updateBuildingTurns(){
        RobotType type = BuildingManager.getNextBuilding(comm);
        if (type != null && type == typeToBuild && BuildingManager.haveSoupToSpawn(rc, type)){
            ++tryToBuildTurns;
        } else tryToBuildTurns = 0;
        typeToBuild = type;
    }

    MapLocation getTarget(){
        MapLocation ans;
        if (comm.shouldBuildVaporators()){
            ans = getTargetForVaporator();
            if (ans != null) return ans;
            return explore.HQloc;
        } else {
             ans = getBuildingTarget();
            if (ans != null) return ans;
        }
        if (rc.getSoupCarrying() >= rc.getType().soupLimit) return explore.closestRefineryLoc;
        if (rc.getSoupCarrying() > 0){
            if (explore.closestSoup != null) return explore.closestSoup;
            else return explore.closestRefineryLoc;
        }
        ans = explore.getBestTarget();
        if (ans != null) return ans;
        if (comm.latestMiningLoc != null){
            if (explore.map[comm.latestMiningLoc.x][comm.latestMiningLoc.y] == 0) return comm.latestMiningLoc;
        }
        return explore.exploreTarget();
    }

    MapLocation getTargetForVaporator(){
        return explore.HQloc;
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
        if (!comm.shouldBuildVaporators() && rc.getLocation().distanceSquaredTo(explore.HQloc) > Constants.DIST_TO_BUILD) return;
        RobotType type = BuildingManager.getNextBuilding(comm);
        if (type == null) return;
        if (!BuildingManager.haveSoupToSpawn(rc, type)) return;
        if (Constants.DEBUG == 1) System.out.println(type.name());
        if (rc.getTeamSoup() <= type.cost) return;
        build(type);
    }

    RobotType buildType;

    void build(RobotType type){
        try {
            buildType = type;
            myLoc = rc.getLocation();
            buildOnWall = comm.shouldBuildVaporators();

            if (Constants.DEBUG == 1) System.out.println("Trying to build " + type.name() + " " + buildOnWall);

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

            if (bestBuildingSpot != null){
                rc.setIndicatorDot(bestBuildingSpot.loc, 255, 255, 255);
                System.out.println("Best spot " + bestBuildingSpot.score() + " " + bestBuildingSpot.zone);
            }

            if (bestBuildingSpot != null && rc.canBuildRobot(buildType, bestBuildingSpot.dir)){
                if (bestBuildingSpot.score() >= 2) {
                    rc.buildRobot(buildType, bestBuildingSpot.dir);
                    comm.sendMessage(comm.BUILDING_TYPE, type.ordinal());
                } else if (bestBuildingSpot.score() >= 1){
                    if (tryToBuildTurns >= MAX_BUILD_TURNS){
                        rc.buildRobot(buildType, bestBuildingSpot.dir);
                        comm.sendMessage(comm.BUILDING_TYPE, type.ordinal());
                    }
                } else{
                    if (tryToBuildTurns >= DESPERATE_TURNS){
                        rc.buildRobot(buildType, bestBuildingSpot.dir);
                        comm.sendMessage(comm.BUILDING_TYPE, type.ordinal());
                    }
                }
            }
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
                    if (loc.distanceSquaredTo(buildingZone.HQloc) <= 2) effectiveHeight = MAX_ELEVATION;
                    //if (zone == BuildingZone.HOLE) computeWaterAdj();
                    //checkBuild();
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
                   if (buildOnWall){
                       switch(zone){
                           case BuildingZone.OUTER_WALL:
                               if (buildingZone.canBuild(loc) && height >= Constants.WALL_HEIGHT) score = 2;
                               else if (height >= Constants.WALL_HEIGHT) score = 1;
                               return score;
                           case BuildingZone.BUILDING_AREA:
                           case BuildingZone.NEXT_TO_WALL:
                               score = 1;
                               return score;
                           default:
                               score = 0;
                               return score;
                       }
                   } else{
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
                   }
               case FULFILLMENT_CENTER:
                   if (buildOnWall){
                       switch(zone){
                           case BuildingZone.OUTER_WALL:
                               if (buildingZone.canBuild(loc) && height >= Constants.WALL_HEIGHT) score = 2;
                               else if (height >= Constants.WALL_HEIGHT) score = 1;
                               return score;
                           case BuildingZone.BUILDING_AREA:
                           case BuildingZone.NEXT_TO_WALL:
                               score = 1;
                               return score;
                           default:
                               score = 0;
                               return score;
                       }
                   } else{
                       switch (zone) {
                           case BuildingZone.BUILDING_AREA:
                               score = 1;
                               return score;
                           case BuildingZone.NEXT_TO_WALL:
                               score = 2;
                               return score;
                           default:
                               score = 0;
                               return score;
                       }
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

        /*void checkBuild(){
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
        }*/

        boolean isBetter(BuildingSpot s){
            if (!canBuild) return false;
            if (s == null) return true;
            if (score() == 0) return false;
            if (s.score() == 0) return true;
            if (effectiveHeight > s.effectiveHeight) return true;
            if (s.effectiveHeight > effectiveHeight) return false;
            if (score() == s.score()) return height > s.height;
            return (score() > s.score());
        }

    }


}
