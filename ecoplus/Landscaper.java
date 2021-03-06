package ecoplus;

import battlecode.common.*;
import com.sun.source.tree.ForLoopTree;

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
    boolean spareDirt;
    boolean full;
    boolean alert = false;

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
        /*if (Constants.DEBUG == 1 && exploreLandscaper.closestWallToBuild != null){
            System.out.println(exploreLandscaper.closestWallToBuild);
            rc.setIndicatorDot(exploreLandscaper.closestWallToBuild, 255, 255, 255);
        }*/
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
        if (Constants.DEBUG == 1){
            if (bestSpotDig != null){
                System.out.println("Best Spot Dig " + bestSpotDig.score());
                rc.setIndicatorDot(bestSpotDig.loc, 255, 0, 0);
            }
            if (bestSpotDeposit != null){
                System.out.println("Best Spot deposit " + bestSpotDeposit.score());
                rc.setIndicatorDot(bestSpotDeposit.loc, 0, 255, 0);
            }
        }
        //if (Constants.DEBUG == 1) System.out.println("Bytecode post dig/bury " + Clock.getBytecodeNum());
        if (!flee){
            MapLocation target = getTarget();
            bugPath.moveTo(target);
            //if (rc.isReady() && !bugPath.canMoveArray[Direction.CENTER.ordinal()]) bugPath.moveSafe();
        }
        //tryDig();
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

    /*
    void tryDig(){
        try {
            if (!rc.isReady()) return;
            if (!buildingZone.finished()) return;
            if (rc.getDirtCarrying() >= RobotType.LANDSCAPER.dirtLimit) return;
            if (bestSpotDig != null && bestSpotDig.score() > FIXABLE_FLOODED){
                rc.digDirt(bestSpotDig.dir);
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }*/

    void tryDigAndDeposit(){
        try {
            if (!rc.isReady()) return;
            if (!buildingZone.finished()) return;
            //if(Constants.DEBUG == 1) System.out.println("Before adjacent analysis " + Clock.getBytecodeNum());
            bestSpotDig = null; bestSpotDeposit = null;
            myLoc = rc.getLocation();
            sight = rc.getCurrentSensorRadiusSquared();
            spareDirt = rc.getDirtCarrying() > 1;
            full = rc.getDirtCarrying() >= RobotType.LANDSCAPER.dirtLimit;
            alert = false;


            AdjacentSpot s1 = new AdjacentSpot(Direction.NORTH);
            AdjacentSpot s2 = new AdjacentSpot(Direction.NORTHEAST);
            AdjacentSpot s3 = new AdjacentSpot(Direction.EAST);
            AdjacentSpot s4 = new AdjacentSpot(Direction.SOUTHEAST);
            AdjacentSpot s5 = new AdjacentSpot(Direction.SOUTH);
            AdjacentSpot s6 = new AdjacentSpot(Direction.SOUTHWEST);
            AdjacentSpot s7 = new AdjacentSpot(Direction.WEST);
            AdjacentSpot s8 = new AdjacentSpot(Direction.NORTHWEST);
            AdjacentSpot s0 = new AdjacentSpot(Direction.CENTER);

            //if(Constants.DEBUG == 1) System.out.println("After adjacent analysis " + Clock.getBytecodeNum());

            if (s0.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s0;
            if (s1.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s1;
            if (s2.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s2;
            if (s3.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s3;
            if (s4.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s4;
            if (s5.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s5;
            if (s6.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s6;
            if (s7.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s7;
            if (s8.isBetterDepositTargetThan(bestSpotDeposit)) bestSpotDeposit = s8;

            if (rc.getDirtCarrying() > 0){
                if (bestSpotDeposit != null){
                    if (bestSpotDeposit.score() <= WALL_LOW) {
                        rc.depositDirt(bestSpotDeposit.dir);
                        return;
                    }
                    /*if (bestSpotDeposit.depositScore() >= DEPOSIT_HOLE && full){
                        rc.depositDirt(bestSpotDeposit.dir);
                        return;
                    }*/
                }
            }

            if (!full) {
                if (s0.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s0;
                if (s1.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s1;
                if (s2.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s2;
                if (s3.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s3;
                if (s4.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s4;
                if (s5.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s5;
                if (s6.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s6;
                if (s7.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s7;
                if (s8.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s8;


                if (bestSpotDig != null){
                    if (rc.getDirtCarrying() > 0){
                        if (bestSpotDig.score() >= HOLE){
                            rc.digDirt(bestSpotDig.dir);
                            return;
                        }
                    } else{
                        if (bestSpotDig.score() >= BUILDING_AREA){
                            rc.digDirt(bestSpotDig.dir);
                            return;
                        }
                    }
                }

                /*if (alert && bestSpotDig != null && bestSpotDig.score() >= FLOODED_OUTER_WALL){
                    rc.digDirt(bestSpotDig.dir);
                    return;
                }*/

                /*
                if (bestSpotDig != null && bestSpotDig.score() > FIXABLE_FLOODED) {
                    if (bestSpotDig.score() >= HOLE || rc.getDirtCarrying() == 0) {
                        rc.digDirt(bestSpotDig.dir);
                        return;
                    }
                }*/
            }

        } catch (Throwable t){
            t.printStackTrace();
        }
    }


    /*static final int WATER_INSIDE = 0;
    static final int WATER_WALL = 1;
    static final int WATER_HOLE = 2;
    static final int WALL_LOW = 3;
    static final int INSIDE_PRECISE = 4;
    static final int WALL_PRECISE = 5;
    static final int*/

    static final int FLOODED_INTERIOR = 0;
    static final int FLOODED_NEXT_TO_WALL = 1;
    static final int FLOODED_WALL = 2;
    static final int FLOODED_OUTER_WALL = 3;
    static final int FLOODED_HOLE = 4;
    static final int WALL_LOW = 5;
    static final int BUILDING_AREA_PRECISE = 6;
    static final int NEXT_TO_WALL_AREA_PRECISE = 7;
    static final int WALL_PRECISE = 8;
    static final int WALL_PERFECT_HEIGHT = 9;
    static final int OUTER_WALL_PERFECT_HEIGHT = 10;
    static final int HOLE_PRECISE = 11;
    static final int BUILDING_AREA = 12;
    static final int NEXT_TO_WALL = 13;
    static final int HOLE_FLOODED_LOW_DIRT = 14;
    static final int HOLE = 15;
    static final int WALL_HIGH = 16;
    static final int WALL_SUPER_HIGH = 17;


    //static final int MINIMUM_PRIORITY = 0;
    //static final int FIXABLE_FLOODED = 1;
    //static final int WALL_LOW = 2;
    //static final int WALL_PRECISE = 3;
    //static final int FIXED_FLOODED = 4;
    //static final int BUILDING_AREA = 5;
    //static final int NEXT_TO_WALL = 6;
    //static final int HOLE = 7;
    //static final int WALL_SUPER_HIGH = 8;
    //static final int WALL_HIGH = 9;
    //static final int MAXIMUM_PRIORITY = 10;


    class AdjacentSpot {

        Direction dir;
        boolean canDig, canDeposit;
        int zone;
        MapLocation loc;
        int elevation;
        boolean flooded;
        boolean waterAdj;

        int score = -1;

        //int scoreDig, scoreDeposit;

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
                    RobotInfo r = rc.senseRobotAtLocation(loc);
                    //TODO: wtf how to access building HP
                    if (r != null && r.getType().isBuilding()) elevation += 15;
                    if (sight >= 8){
                        //if (Constants.DEBUG == 1) System.out.println("Before compute Adj: " + Clock.getBytecodeNum());
                        computeWaterAdj();
                        //if (Constants.DEBUG == 1) System.out.println("After compute Adj: " + Clock.getBytecodeNum());
                    }

                }
                computeScore();
            } catch(Throwable t){
                t.printStackTrace();
            }
        }

        int flood(){
            if (flooded) return 1;
            if (!waterAdj) return -1;
            if (elevation == WaterManager.waterLevel + waterManager.safetyWall()) return 0;
            if (elevation < WaterManager.waterLevel + waterManager.safetyWall()) return 1;
            return -1;
        }

        int computeScore(){
            if (elevation < Constants.MIN_DEPTH && zone != BuildingZone.BUILDING_AREA && zone != BuildingZone.NEXT_TO_WALL && zone != BuildingZone.WALL){
                score = HOLE;
                return score;
            }
            int f = flood();
            switch(zone){
                case BuildingZone.BUILDING_AREA:
                    switch(f){
                        case 1:
                            score = FLOODED_INTERIOR;
                            break;
                        case 0:
                            score = BUILDING_AREA_PRECISE;
                            break;
                        default:
                            score = BUILDING_AREA;
                            break;
                    }
                    return score;
                case BuildingZone.NEXT_TO_WALL:
                    switch(f){
                        case 1:
                            score = FLOODED_NEXT_TO_WALL;
                            break;
                        case 0:
                            score = NEXT_TO_WALL_AREA_PRECISE;
                            break;
                        default:
                            score = NEXT_TO_WALL;
                            break;
                    }
                    return score;
                case BuildingZone.HOLE:
                    if (spareDirt) {
                        switch(f){
                            case 1:
                                score = FLOODED_HOLE;
                                break;
                            case 0:
                                score = HOLE_PRECISE;
                                break;
                            default:
                                score = HOLE;
                                break;
                        }
                    }
                    else {
                        switch (f){
                            case 1:
                            case 0:
                                score = HOLE_FLOODED_LOW_DIRT;
                                break;
                            default:
                                score = HOLE;
                        }
                    }
                    return score;
                case BuildingZone.OUTER_WALL:
                    if (elevation < Constants.WALL_HEIGHT){
                        score = WALL_LOW;
                        if (f > 0) score = FLOODED_OUTER_WALL;
                        return score;
                    }
                    if (elevation == Constants.WALL_HEIGHT){
                        score = OUTER_WALL_PERFECT_HEIGHT;
                        return score;
                    }
                    if (elevation > Constants.WALL_HEIGHT + Constants.MAX_DIFF_HEIGHT){
                        score = WALL_SUPER_HIGH;
                        return score;
                    }
                    score = WALL_HIGH;
                    return score;
                case BuildingZone.WALL:
                default:
                    if (elevation < Constants.WALL_HEIGHT){
                        score = WALL_LOW;
                        if (f > 0) score = FLOODED_WALL;
                        return score;
                    }
                    if (elevation == Constants.WALL_HEIGHT){
                        score = WALL_PERFECT_HEIGHT;
                        return score;
                    }
                    if (elevation > Constants.WALL_HEIGHT + Constants.MAX_DIFF_HEIGHT){
                        score = WALL_SUPER_HIGH;
                        return score;
                    }
                    score = WALL_HIGH;
                    return score;
            }
        }

        int score(){
            if (score >= 0) return score;
            int ans = computeScore();
            if (ans <= FLOODED_WALL) alert = true;
            return ans;
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



        boolean isBetterDiggingTargetThan(AdjacentSpot d){
            if (!canDig) return false;
            if (d == null) return true;
            return score() > d.score();
        }

        boolean isBetterDepositTargetThan(AdjacentSpot d){
            if (!canDeposit) return false;
            if (d == null) return true;
            return score() < d.score();
        }


    }


}
