package trumpplayer;

import battlecode.common.*;

public class Landscaper extends MyRobot{

    RobotController rc;
    Comm comm;
    ExploreLandscaper exploreLandscaper;
    BugPath bugPath;
    WaterManager waterManager;
    BuildingZone buildingZone;

    RobotInfo robotHeld;
    Direction[] dirs = Direction.values();

    Landscaper(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        //myLoc = rc.getLocation();
        waterManager = new WaterManager(rc);
        exploreLandscaper = new ExploreLandscaper(rc, comm);
        buildingZone = new BuildingZone(rc);
        bugPath = new BugPath(rc);
    }

    void play(){
        if (comm.singleMessage()) comm.readMessages();
        waterManager.update();
        exploreLandscaper.update();
        exploreLandscaper.checkComm();
        bugPath.update();
        if (exploreLandscaper.dronesFound) bugPath.updateDrones(exploreLandscaper.minDist);
        if (Constants.DEBUG == 1) System.out.println("Bytecode post bugPath update " + Clock.getBytecodeNum());
        //if (Constants.DEBUG == 1 && comm.EnemyHQLoc != null) rc.setIndicatorLine(rc.getLocation(), comm.EnemyHQLoc, 0, 255, 0);

        //if (rc.getRoundNum() == 320 && buildingZone.finished()) buildingZone.debugPrint();
        boolean flee = false;
        if (bugPath.shouldFlee && WaterManager.closestSafeCell != null){
            bugPath.moveTo(WaterManager.closestSafeCell);
            flee = true;
        }
        if (Constants.DEBUG == 1) System.out.println("Bytecode post trying to flee water " + Clock.getBytecodeNum());
        tryBury();
        tryDig();
        if (Constants.DEBUG == 1) System.out.println("Bytecode post dig/bury " + Clock.getBytecodeNum());
        if (!flee){
            MapLocation target = getTarget();
            bugPath.moveTo(target);
            if (rc.isReady() && !bugPath.canMoveArray[Direction.CENTER.ordinal()]) bugPath.moveSafe();
        }
        comm.readMessages();
        if (comm.wallMes != null) buildingZone.update(comm.wallMes);
        buildingZone.run();
    }

    MapLocation getTarget(){
        if (!rc.isReady()) return null;
        if (exploreLandscaper.closestEnemyBuilding != null) return exploreLandscaper.closestEnemyBuilding;
        if (comm.EnemyHQLoc != null) return comm.EnemyHQLoc;
        return exploreLandscaper.exploreTarget();
    }

    void tryBury(){
        try {
            if (!rc.isReady()) return;
            if (exploreLandscaper.closestEnemyBuilding == null) return;
            if (exploreLandscaper.closestEnemyBuilding.distanceSquaredTo(rc.getLocation()) <= 2){
                Direction dir = rc.getLocation().directionTo(exploreLandscaper.closestEnemyBuilding);
                if (rc.canDepositDirt(dir)) rc.depositDirt(dir);
                return;
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void tryDig(){
        try {
            if (!rc.isReady()) return;
            if (!buildingZone.finished()) return;
            for (Direction dir : dirs){
                MapLocation newLoc = rc.getLocation().add(dir);
                if (!rc.canDigDirt(dir)) continue;
                if (rc.canSenseLocation(newLoc) && buildingZone.map[newLoc.x][newLoc.y] == 0){
                    rc.digDirt(dir);
                    return;
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }
}
