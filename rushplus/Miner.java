package rushplus;

import battlecode.common.*;

public class Miner extends MyRobot{

    RobotController rc;
    Direction[] dirs = Direction.values();
    BugPath bugPath;
    ExploreMiner explore;
    WaterManager waterManager;
    Comm comm;
    BuildingZone buildingZone;
    Danger danger;
    boolean builder;

    final int MIN_SOUP_FOR_REFINERY = 1000;
    final int MIN_DIST_FOR_REFINERY = 50;

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
            if (explore.shouldBuildNetGun()) build(RobotType.NET_GUN, false);
        }
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
        ans = comm.getEnemyHQLoc();
        if (ans != null && rc.getRoundNum() >= Constants.RUSH_TURN) return ans;
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
            for (Direction dir : dirs) {
                MapLocation newLoc = myLoc.add(dir);
                if (rc.canSenseLocation(newLoc) && rc.senseSoup(newLoc) > 0) continue;
                if (rc.canBuildRobot(RobotType.REFINERY, dir)){
                    rc.buildRobot(RobotType.REFINERY, dir);
                    return true;
                }
            }
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
        build(type, true);
    }

    void build(RobotType type, boolean inside){
        try {
            if (Constants.DEBUG == 1) System.out.println("Trying to build " + type.name());
            Direction bestDir = null;
            int bestHeight = 0;
            Direction dir = Direction.NORTH;
            MapLocation myLoc = rc.getLocation();
            for (int i = 0; i < 8; ++i) {
                if (rc.canBuildRobot(type, dir)) {
                    MapLocation newLoc = myLoc.add(dir);
                    if (rc.canSenseLocation(newLoc) && !rc.senseFlooding(newLoc)) {
                        if (inside && buildingZone.map[newLoc.x][newLoc.y] != 1){
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
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }
}
