package rushbotplus;

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
    final int MAX_BUILD_TURNS = 3;

    int maxRadius = 0;
    boolean built = false;
    boolean netgun = false;

    int tryToBuildTurns = 0;

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
        explore.updateMiner();
        explore.checkComm();
        bugPath.update();
        if (explore.dronesFound){
            if (!builder || built) bugPath.updateDrones(danger);
            if (explore.shouldBuildNetGun()) build(RobotType.NET_GUN);
        }

        if (Constants.DEBUG == 1) System.out.println("Bytecode post bugPath update " + Clock.getBytecodeNum());

        if (builder) actRush();
        else actNormally();

        if (Constants.DEBUG == 1) System.out.println("Bytecode post trying to flee water " + Clock.getBytecodeNum());


       //END OF TURN STUFF

       comm.readMessages();
       comm.getEnemyHQLoc();
       if (comm.wallMes != null) buildingZone.update(comm.wallMes);
       buildingZone.run();

    }

    void actRush(){
        boolean flee = false;
        if (bugPath.shouldFlee && WaterManager.closestSafeCell != null){
            bugPath.moveTo(WaterManager.closestSafeCell);
            flee = true;
        }

        tryBuildRush();


        MapLocation target = comm.getEnemyHQLoc();
        if (target == null) target = getBestGuess();
        else if (rc.getLocation().distanceSquaredTo(target) <= 2) target = rc.getLocation();

        if (!flee) bugPath.moveTo(target);

    }

    void tryBuildRush() {
        try {
            if (explore.enemyHQ == null) return;
            checkNetGun();
            if (built) return;
            for (Direction dir : dirs) {
                MapLocation loc = rc.getLocation().add(dir);
                if (loc.distanceSquaredTo(explore.enemyHQ) > 2) continue;
                if (!rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) continue;
                rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
                built = true;
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void checkNetGun(){
        try {
            if (netgun) return;
            if (rc.getLocation().distanceSquaredTo(explore.enemyHQ) > 48) return;
            MapLocation loc = getClosestEnemyDrone();
            if (loc == null) return;
            if (rc.getLocation().distanceSquaredTo(loc) > 5) return;
            int minDist = 0;
            Direction bestDir = null;
            for (Direction dir : dirs) {
                if (!rc.canBuildRobot(RobotType.NET_GUN, dir)) continue;
                MapLocation newLoc = rc.getLocation().add(dir);
                int d = newLoc.distanceSquaredTo(loc);
                if (bestDir == null || d < minDist) {
                    bestDir = dir;
                    minDist = d;
                }
            }
            if (bestDir != null) {
                rc.buildRobot(RobotType.NET_GUN, bestDir);
                netgun = true;
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    MapLocation getClosestEnemyDrone(){
        MapLocation ans = null;
        int d = 0;
        RobotInfo[] ri = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        for (RobotInfo r : ri){
            if (r.getType() == RobotType.DELIVERY_DRONE || r.getType() == RobotType.FULFILLMENT_CENTER){
                int dist = r.location.distanceSquaredTo(rc.getLocation());
                if (ans == null || dist < d){
                    ans = r.location;
                    d = dist;
                }
            }
        }
        return ans;
    }

    void actNormally(){
        // TRY ESCAPING FROM DRONES

        boolean flee = false;
        if (bugPath.shouldFlee && WaterManager.closestSafeCell != null){
            bugPath.moveTo(WaterManager.closestSafeCell);
            flee = true;
        }

        //IF I CAN STAY ON MY LOCATION, TRY BUILDING REFINERY OR TRY MINING
        Direction miningDir = getMiningDir();
        if (miningDir != null && bugPath.canMoveArray[Direction.CENTER.ordinal()]) {
            //tryBuildRefinery();
            tryMine(miningDir);
        }

        if (Constants.DEBUG == 1) System.out.println("Bytecode post mining " + Clock.getBytecodeNum());

        //TRY DEPOSITING OR BUILDING

        tryDeposit();
        //tryBuilding();

        if (Constants.DEBUG == 1) System.out.println("Bytecode post deposit/build " + Clock.getBytecodeNum());

        //CHOOSE TARGET AND MOVE

        if (!flee){
            MapLocation target = getTarget();
            bugPath.moveTo(target);
        }
    }

    MapLocation getTarget(){
        MapLocation ans;
        /*if (comm.shouldBuildVaporators()){
            ans = getTargetForVaporator();
            if (ans != null) return ans;
            return explore.HQloc;
        } else {
             ans = getBuildingTarget();
            if (ans != null) {
                ++tryToBuildTurns;
                return ans;
            } else tryToBuildTurns = 0;
        }*/
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
            buildOnWall = comm.shouldBuildVaporators();

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

            if (bestBuildingSpot != null && rc.canBuildRobot(buildType, bestBuildingSpot.dir) && bestBuildingSpot.score() > 0){
                if (tryToBuildTurns > MAX_BUILD_TURNS || bestBuildingSpot.score() >= 2) {
                    rc.buildRobot(buildType, bestBuildingSpot.dir);
                    comm.sendMessage(comm.BUILDING_TYPE, type.ordinal());
                }
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
               case FULFILLMENT_CENTER:
               case DESIGN_SCHOOL:
               case VAPORATOR:
                   if (buildOnWall){
                       switch(zone){
                           case BuildingZone.OUTER_WALL:
                               if (buildingZone.canBuild(loc) && height >= Constants.WALL_HEIGHT) score = 2;
                               else if (height >= Constants.WALL_HEIGHT) score = 1;
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
