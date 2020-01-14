package rushplus;

import battlecode.common.*;

public class Landscaper extends MyRobot {

    RobotController rc;
    Comm comm;
    ExploreLandscaper exploreLandscaper;
    BugPath bugPath;
    WaterManager waterManager;
    BuildingZone buildingZone;
    Danger danger;

    RobotInfo robotHeld;
    Direction[] dirs = Direction.values();

    Landscaper(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        danger = new Danger();
        //myLoc = rc.getLocation();
        waterManager = new WaterManager(rc);
        exploreLandscaper = new ExploreLandscaper(rc, comm, danger);
        buildingZone = new BuildingZone(rc);
        bugPath = new BugPath(rc);
    }

    void play(){
        if (comm.singleMessage()) comm.readMessages();
        waterManager.update();
        exploreLandscaper.update();
        exploreLandscaper.checkComm();
        bugPath.update();
        if (exploreLandscaper.dronesFound) bugPath.updateDrones(danger);
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
        tryDig(false);
        if (Constants.DEBUG == 1) System.out.println("Bytecode post dig/bury " + Clock.getBytecodeNum());
        if (!flee){
            MapLocation target = getTarget();
            bugPath.moveTo(target);
            if (rc.isReady() && !bugPath.canMoveArray[Direction.CENTER.ordinal()]) bugPath.moveSafe();
        }
        tryDig(true);
        comm.readMessages();
        if (comm.wallMes != null) buildingZone.update(comm.wallMes);
        buildingZone.run();
    }

    MapLocation getTarget(){
        if (!rc.isReady()) return null;
        if (exploreLandscaper.closestEnemyBuilding != null) return exploreLandscaper.closestEnemyBuilding;
        MapLocation enemyHQ = comm.getEnemyHQLoc();
        if (enemyHQ != null) return enemyHQ;
        MapLocation loc = getBestGuess();
        if (loc != null) return loc;
        return exploreLandscaper.exploreTarget();
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

    void tryDig(boolean free){
        try {
            if (!rc.isReady()) return;
            if (!buildingZone.finished()) return;
            if (!free && rc.getDirtCarrying() > 0) return;
            if (!buildingZone.finished()) return;
            for (Direction dir : dirs){
                MapLocation newLoc = rc.getLocation().add(dir);
                if (!rc.canDigDirt(dir)) continue;
                if (!free && buildingZone.map[newLoc.x][newLoc.y] > 0) continue;
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
