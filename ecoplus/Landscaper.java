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

    Direction[] dirs = Direction.values();

    AdjacentSpot bestSpotDig, bestSpotDeposit;
    int sight;

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
        if (Constants.DEBUG == 1 && exploreLandscaper.closestWallToBuild != null) rc.setIndicatorLine(rc.getLocation(), exploreLandscaper.closestWallToBuild, 0, 255, 0);
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
        tryDigAndDeposit();
        if (Constants.DEBUG == 1) System.out.println("Bytecode post dig/bury " + Clock.getBytecodeNum());
        if (!flee){
            MapLocation target = getTarget();
            bugPath.moveTo(target);
            //if (rc.isReady() && !bugPath.canMoveArray[Direction.CENTER.ordinal()]) bugPath.moveSafe();
        }
        tryDig();
        if (comm.wallMes != null) buildingZone.update(comm.wallMes);
        buildingZone.run();
        comm.readMessages();
    }

    MapLocation getTarget(){
        if (!rc.isReady()) return null;
        if (exploreLandscaper.closestEnemyBuilding != null) return exploreLandscaper.closestEnemyBuilding;
        if (exploreLandscaper.closestWallToBuild != null) return exploreLandscaper.closestWallToBuild;
        MapLocation enemyHQ = comm.getEnemyHQLoc();
        if (enemyHQ != null) return enemyHQ;
        MapLocation loc = getBestGuess();
        return loc;
        //return exploreLandscaper.exploreTarget();
    }

    void tryMoveToWall(){
        if (!rc.isReady()) return;
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
            myLoc = rc.getLocation();
            if (myLoc.distanceSquaredTo(exploreLandscaper.closestWallToBuild) > 2) return;
            Direction dir = myLoc.directionTo(exploreLandscaper.closestWallToBuild);
            if (rc.canDepositDirt(dir)){
                rc.depositDirt(dir);
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void tryDig(){
        try {
            if (!rc.isReady()) return;
            if (!buildingZone.finished()) return;
            if (rc.getDirtCarrying() >= RobotType.LANDSCAPER.dirtLimit) return;
            if (bestSpotDig != null && bestSpotDig.scoreDig() > 0){
                rc.digDirt(bestSpotDig.dir);
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void tryDigAndDeposit(){
        try {
            if (!rc.isReady()) return;
            if (!buildingZone.finished()) return;
            if(Constants.DEBUG == 1) System.out.println("Before adjacent analysis " + Clock.getBytecodeNum());
            bestSpotDig = null; bestSpotDeposit = null;
            myLoc = rc.getLocation();
            sight = rc.getCurrentSensorRadiusSquared();


            AdjacentSpot s1 = new AdjacentSpot(Direction.NORTH);
            AdjacentSpot s2 = new AdjacentSpot(Direction.NORTHEAST);
            AdjacentSpot s3 = new AdjacentSpot(Direction.EAST);
            AdjacentSpot s4 = new AdjacentSpot(Direction.SOUTHEAST);
            AdjacentSpot s5 = new AdjacentSpot(Direction.SOUTH);
            AdjacentSpot s6 = new AdjacentSpot(Direction.SOUTHWEST);
            AdjacentSpot s7 = new AdjacentSpot(Direction.WEST);
            AdjacentSpot s8 = new AdjacentSpot(Direction.NORTHWEST);
            AdjacentSpot s9 = new AdjacentSpot(Direction.CENTER);

            if(Constants.DEBUG == 1) System.out.println("After adjacent analysis " + Clock.getBytecodeNum());

            if (rc.getDirtCarrying() > 0){
                if (s1.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s1;
                if (s2.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s2;
                if (s3.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s3;
                if (s4.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s4;
                if (s5.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s5;
                if (s6.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s6;
                if (s7.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s7;
                if (s8.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s8;
                if (s9.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s9;

                if (bestSpotDeposit != null && bestSpotDeposit.depositScore() >= 3) {
                    rc.depositDirt(bestSpotDeposit.dir);
                    return;
                }
            }

            if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                if (s1.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s1;
                if (s2.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s2;
                if (s3.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s3;
                if (s4.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s4;
                if (s5.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s5;
                if (s6.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s6;
                if (s7.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s7;
                if (s8.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s8;
                if (s9.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s9;

                if (bestSpotDig != null && bestSpotDig.scoreDig() > 0) {
                    if (bestSpotDig.scoreDig() >= 3 || rc.getDirtCarrying() == 0) {
                        rc.digDirt(bestSpotDig.dir);
                        return;
                    }
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    class AdjacentSpot {

        Direction dir;
        boolean canDig, canDeposit;
        int zone;
        MapLocation loc;
        int elevation;
        boolean flooded;
        boolean waterAdj;

        int scoreDig = -1, scoreDeposit = -1;

        //TODO: what if can't sense?
        AdjacentSpot(Direction dir){
            try {
                this.dir = dir;
                loc = myLoc.add(dir);
                if (rc.canSenseLocation(loc)){
                    canDig = rc.canDigDirt(dir);
                    canDeposit = rc.canDepositDirt(dir);
                    zone = buildingZone.getZone(loc);
                    elevation = rc.senseElevation(loc);
                    flooded = rc.senseFlooding(loc);
                    if (sight >= 8){
                        if (Constants.DEBUG == 1) System.out.println("Before compute Adj: " + Clock.getBytecodeNum());
                        computeWaterAdj();
                        if (Constants.DEBUG == 1) System.out.println("After compute Adj: " + Clock.getBytecodeNum());
                    }

                }
            } catch(Throwable t){
                t.printStackTrace();
            }
        }

        void computeWaterAdj(){
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

            } catch(Throwable t){
                t.printStackTrace();
            }
        }

        int scoreDig(){
            if (scoreDig >= 0) return scoreDig;
            if (flooded && elevation >= Constants.MIN_DEPTH){
                scoreDig = 0;
                return scoreDig;
            }
            if (waterAdj && elevation <= WaterManager.waterLevel + Constants.SAFETY_WALL){
                scoreDig = 0;
                return scoreDig;
            }
            switch(zone){
                case BuildingZone.WALL:
                case BuildingZone.OUTER_WALL:
                    if (elevation > Constants.WALL_HEIGHT){
                        if (elevation > Constants.WALL_HEIGHT + Constants.MAX_DIFF_HEIGHT) scoreDig = 4;
                        else scoreDig = 5;
                    }
                    else scoreDig = 1;
                    break;
                case BuildingZone.BUILDING_AREA:
                case BuildingZone.NEXT_TO_WALL:
                    scoreDig = 2;
                    break;
                case BuildingZone.HOLE:
                    scoreDig = 3;
                    break;
                default:
                    scoreDig = 4;
                    break;
            }
            return scoreDig;
        }

        int depositScore(){
            if (scoreDeposit >= 0) return scoreDeposit;
            if (flooded && elevation >= Constants.MIN_DEPTH){
                scoreDeposit = 4;
                return scoreDeposit;
            }
            if (waterAdj && elevation >= Constants.MIN_DEPTH && elevation < WaterManager.waterLevel + Constants.SAFETY_WALL){
                scoreDeposit = 4;
                return scoreDeposit;
            }
            switch(zone){
                case BuildingZone.WALL:
                case BuildingZone.OUTER_WALL:
                    if (elevation >= Constants.WALL_HEIGHT) scoreDeposit = 1;
                    else scoreDeposit = 3;
                    break;
                case BuildingZone.HOLE:
                    scoreDeposit = 2;
                    break;
                case BuildingZone.BUILDING_AREA:
                case BuildingZone.NEXT_TO_WALL:
                default:
                    scoreDeposit = 0;
                    break;
            }
            return scoreDeposit;
        }



        boolean isBetterDiggingTargetThan(AdjacentSpot d){
            if (!canDig) return false;
            if (d == null) return true;
            return scoreDig() > d.scoreDig();
        }

        boolean isBetterDepositTargetThan(AdjacentSpot d){
            if (!canDeposit) return false;
            if (d == null) return true;
            return depositScore() > d.depositScore();
        }


    }


}
