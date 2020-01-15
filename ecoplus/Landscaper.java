package ecoplus;

import battlecode.common.*;

public class Landscaper extends MyRobot {

    RobotController rc;
    Comm comm;
    ExploreLandscaper exploreLandscaper;
    BugPath bugPath;
    WaterManager waterManager;
    BuildingZone buildingZone;
    Danger danger;
    MapLocation myLoc;

    RobotInfo robotHeld;
    Direction[] dirs = Direction.values();

    Landscaper(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        danger = new Danger();
        //myLoc = rc.getLocation();
        waterManager = new WaterManager(rc);
        buildingZone = new BuildingZone(rc);
        exploreLandscaper = new ExploreLandscaper(rc, comm, danger, buildingZone);
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
        tryMoveToWall();
        tryBury();
        tryBuildWall();
        tryDig(false);
        if (Constants.DEBUG == 1) System.out.println("Bytecode post dig/bury " + Clock.getBytecodeNum());
        if (!flee){
            MapLocation target = getTarget();
            bugPath.moveTo(target);
            //if (rc.isReady() && !bugPath.canMoveArray[Direction.CENTER.ordinal()]) bugPath.moveSafe();
        }
        tryDig(true);
        comm.readMessages();
        if (comm.wallMes != null) buildingZone.update(comm.wallMes);
        buildingZone.run();
    }

    MapLocation getTarget(){
        if (!rc.isReady()) return null;
        if (exploreLandscaper.closestEnemyBuilding != null) return exploreLandscaper.closestEnemyBuilding;
        if (exploreLandscaper.closestWallToBuild != null) return exploreLandscaper.closestWallToBuild;
        MapLocation enemyHQ = comm.getEnemyHQLoc();
        if (enemyHQ != null) return enemyHQ;
        MapLocation loc = getBestGuess();
        if (loc != null) return loc;
        return exploreLandscaper.exploreTarget();
    }

    void tryMoveToWall(){
        if (!buildingZone.finished()) return;
        if (buildingZone.isWall(rc.getLocation())) return;
        if (exploreLandscaper.closestWallToBuild == null) return;
        bugPath.moveTo(exploreLandscaper.closestWallToBuild);
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

    void tryBuildWall(){
        try {
            if (!rc.isReady()) return;
            if (exploreLandscaper.closestWallToBuild == null) return;
            if (exploreLandscaper.closestWallToBuild.distanceSquaredTo(rc.getLocation()) <= 2){
                Direction dir = rc.getLocation().directionTo(exploreLandscaper.closestWallToBuild);
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
            DiggingSpot bestSpot = null;
            myLoc = rc.getLocation();
            //if (!free && rc.getDirtCarrying() > 0) return;
            for (Direction dir : dirs){
                DiggingSpot d = new DiggingSpot(dir);
                if (d.isBetterThan(bestSpot)) bestSpot = d;
                /*MapLocation newLoc = rc.getLocation().add(dir);
                if (!rc.canDigDirt(dir)) continue;
                if (!free && buildingZone.map[newLoc.x][newLoc.y] > 0) continue;
                if (rc.canSenseLocation(newLoc) && buildingZone.map[newLoc.x][newLoc.y] == 0){
                    rc.digDirt(dir);
                    return;
                }*/
            }
            if (bestSpot != null){
                if (bestSpot.score() < 3 && !free && rc.getDirtCarrying() > 0) return;
                rc.digDirt(bestSpot.dir);
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    class DiggingSpot{

        Direction dir;
        boolean canDig;
        int zone;
        MapLocation loc;
        int elevation;

        int score = -1;

        DiggingSpot (Direction dir){
            try {
                this.dir = dir;
                canDig = rc.canDigDirt(dir);
                if (canDig) {
                    loc = myLoc.add(dir);
                    if (rc.canSenseLocation(loc)) {
                        zone = buildingZone.map[loc.x][loc.y];
                        if (zone == 0 && buildingZone.isWall(loc)) zone = BuildingZone.WALL;
                        elevation = rc.senseElevation(loc);
                    }
                }
            } catch(Throwable t){
                t.printStackTrace();
            }
        }

        int score(){
            if (score >= 0) return score;
            switch(zone){
                case BuildingZone.WALL:
                    if (elevation > Constants.WALL_HEIGHT) score = 3;
                    else score = 0;
                    break;
                case BuildingZone.BUILDING_AREA:
                case BuildingZone.NEXT_TO_WALL:
                    score = 1;
                    break;
                default:
                    score = 2;
                    break;
            }
            return score;
        }

        boolean isBetterThan(DiggingSpot d){
            if (!canDig) return false;
            if (d == null) return true;
            return score() > d.score();
        }


    }

}
