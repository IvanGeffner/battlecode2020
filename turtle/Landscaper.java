package turtle;

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
    boolean spareDirt;
    boolean full;
    boolean alert = false;
    int myDirt;

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
            rc.setIndicatorLine(rc.getLocation(), exploreLandscaper.closestWallToBuild, 255, 255, 255);
        }*/
        exploreLandscaper.checkComm();
        bugPath.update();
        if (exploreLandscaper.dronesFound) bugPath.updateDrones(danger);
        if (Constants.DEBUG == 1) System.out.println("Bytecode post bugPath update " + Clock.getBytecodeNum());

        boolean flee = false;
        if (bugPath.shouldFlee && WaterManager.closestSafeCell != null){
            bugPath.moveTo(WaterManager.closestSafeCell);
            flee = true;
        }
        if (buildingZone.HQloc != null && rc.getLocation().distanceSquaredTo(buildingZone.HQloc) > 2){
            bugPath.moveTo(buildingZone.HQloc);
        } else if (buildingZone.HQloc != null){
            tryDigAndDeposit();
        }
        /*
        if (Constants.DEBUG == 1) System.out.println("Bytecode post trying to flee water " + Clock.getBytecodeNum());
        checkUrgentMoves();
        //tryMoveToWall();
        tryBury();
        tryDigAndDeposit();
        if (Constants.DEBUG == 1){
            if (bestSpotDig != null){
                System.out.println("Best Spot Dig " + bestSpotDig.scoreDig());
                rc.setIndicatorDot(bestSpotDig.loc, 255, 0, 0);
            }
            if (bestSpotDeposit != null){
                System.out.println("Best Spot deposit " + bestSpotDeposit.scoreDeposit());
                rc.setIndicatorDot(bestSpotDeposit.loc, 0, 255, 0);
            }
        }
        if (!flee){
            MapLocation target = getTarget();
            bugPath.moveTo(target);
        }*/
        if (comm.wallMes != null) buildingZone.update(comm.wallMes);
        buildingZone.run();
        comm.readMessages();
    }

    MapLocation getTarget(){
        if (!rc.isReady()) return null;
        if (exploreLandscaper.closestEnemyBuilding != null) return exploreLandscaper.closestEnemyBuilding;
        if ((comm.HQLoc == null || !comm.wallFinished || rc.getRoundNum() <= Constants.MIN_TURN_GO_TO_ENEMY) && exploreLandscaper.closestWallToBuild != null) return exploreLandscaper.closestWallToBuild;
        MapLocation enemyHQ = comm.getEnemyHQLoc();
        if (enemyHQ != null) return enemyHQ;
        MapLocation loc = getBestGuess();
        return loc;
    }

    void checkUrgentMoves(){
        try {
            if (exploreLandscaper.urgentFix != null){
                rc.setIndicatorDot(exploreLandscaper.urgentFix, 255, 255, 0);
                System.out.println("URGENT "+ exploreLandscaper.urgentFixType);
            }
            if (!rc.isReady()) return;
            if (!buildingZone.finished()) return;
            if (buildingZone.isWall(rc.getLocation())) return;
            if (exploreLandscaper.urgentFix != null) {
                int d = rc.getLocation().distanceSquaredTo(exploreLandscaper.urgentFix);
                if (d > 2) bugPath.moveTo(exploreLandscaper.urgentFix);
                return;
            }
            if (exploreLandscaper.buildingHurt != null) {
                int d = rc.getLocation().distanceSquaredTo(exploreLandscaper.buildingHurt);
                if (d > 2) bugPath.moveTo(exploreLandscaper.buildingHurt);
                else if (rc.isReady()) {
                    Direction dir = rc.getLocation().directionTo(exploreLandscaper.buildingHurt);
                    if (rc.canDigDirt(dir)) rc.digDirt(dir);
                }
                return;
            }
            if (exploreLandscaper.closestWallToBuild == null) return;
            if (buildingZone.isWall(rc.getLocation())) return;
            bugPath.moveTo(exploreLandscaper.closestWallToBuild);
        } catch (Throwable t){
            t.printStackTrace();
        }
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

    void tryDigAndDeposit(){
        try {
            if (!rc.isReady()) return;
            if (!buildingZone.finished()) return;
            if (!bugPath.canMoveArray[Direction.CENTER.ordinal()]) return;
            bestSpotDig = null; bestSpotDeposit = null;
            myLoc = rc.getLocation();
            sight = rc.getCurrentSensorRadiusSquared();
            spareDirt = rc.getDirtCarrying() > 1;
            full = rc.getDirtCarrying() >= RobotType.LANDSCAPER.dirtLimit;
            alert = false;
            myDirt = rc.getDirtCarrying();


            AdjacentSpot s1 = new AdjacentSpot(Direction.NORTH);
            AdjacentSpot s2 = new AdjacentSpot(Direction.NORTHEAST);
            AdjacentSpot s3 = new AdjacentSpot(Direction.EAST);
            AdjacentSpot s4 = new AdjacentSpot(Direction.SOUTHEAST);
            AdjacentSpot s5 = new AdjacentSpot(Direction.SOUTH);
            AdjacentSpot s6 = new AdjacentSpot(Direction.SOUTHWEST);
            AdjacentSpot s7 = new AdjacentSpot(Direction.WEST);
            AdjacentSpot s8 = new AdjacentSpot(Direction.NORTHWEST);
            AdjacentSpot s0 = new AdjacentSpot(Direction.CENTER);

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
                        rc.depositDirt(bestSpotDeposit.dir);
                        return;
                }
            }

            if (!full) {
                if (s8.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s8;
                if (s7.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s7;
                if (s6.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s6;
                if (s5.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s5;
                if (s4.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s4;
                if (s3.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s3;
                if (s2.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s2;
                if (s1.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s1;
                if (s0.isBetterDiggingTargetThan(bestSpotDig)) bestSpotDig = s0;


                if (bestSpotDig != null){
                        rc.digDirt(bestSpotDig.dir);
                        return;
                }
            }

        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    static final int FLOODED_INTERIOR = 0;
    static final int FLOODED_NEXT_TO_WALL = 1;
    static final int FLOODED_WALL = 2;
    static final int FLOODED_OUTER_WALL = 3;
    static final int FLOODED_HOLE = 4;
    static final int WALL_LOW = 5;
    static final int BUILDING_AREA = 6;
    static final int NEXT_TO_WALL = 7;
    static final int HOLE = 8;
    static final int WALL_HIGH = 9;
    static final int WALL_SUPER_HIGH = 10;


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
                    RobotInfo r = rc.senseRobotAtLocation(loc);
                    if (buildingZone.HQloc == null){
                        canDig = canDeposit = false;
                    } else{
                        int d = loc.distanceSquaredTo(buildingZone.HQloc);
                        if (d == 0 || d > 2){
                            canDeposit = false;
                        }
                        else canDig = false;
                    }
                    //TODO: wtf how to access building HP
                    if (r != null && r.getType().isBuilding()) elevation += 15;
                    if (sight >= 8){
                        //if (Constants.DEBUG == 1) System.out.println("Before compute Adj: " + Clock.getBytecodeNum());
                        computeWaterAdj();
                        //if (Constants.DEBUG == 1) System.out.println("After compute Adj: " + Clock.getBytecodeNum());
                    }

                }
            } catch(Throwable t){
                t.printStackTrace();
            }
        }

        boolean flood(int e){
            if (flooded || waterAdj && e < WaterManager.waterLevel + waterManager.safetyWall()) return true;
            return false;
        }

        int computeScore(int e, int d){
            int dd = 0;
            if (dir == Direction.CENTER) dd+= 150;
            return elevation-dd;
        }



        int scoreDig(){
            if (scoreDig < 0) scoreDig = computeScore(elevation-1, 0);
            return scoreDig;
        }

        int scoreDeposit(){
            if (scoreDeposit < 0) scoreDeposit = computeScore(elevation, 1);
            return scoreDeposit;
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
            return scoreDig() > d.scoreDig();
        }

        boolean isBetterDepositTargetThan(AdjacentSpot d){
            if (!canDeposit) return false;
            if (scoreDeposit() == 0) return false;
            if (d == null) return true;
            return scoreDeposit() < d.scoreDeposit();
        }


    }


}
