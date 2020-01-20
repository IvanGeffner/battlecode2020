package rushbot;

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

    MapLocation enemyHQloc = null;

    Direction[] dirs = Direction.values();

    AdjacentSpot bestSpotDig, bestSpotDeposit;
    int sight;
    boolean spareDirt;
    boolean full;
    boolean alert = false;
    int myDirt;
    MapLocation target;

    Landscaper(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        danger = new Danger();
        waterManager = new WaterManager(rc);
        buildingZone = new BuildingZone(rc);
        exploreLandscaper = new ExploreLandscaper(rc, comm, danger, buildingZone);
        bugPath = new BugPath(rc);
    }

    void play(){

        //UPDATE STUFF
        if (comm.singleMessage()) comm.readMessages();
        waterManager.update();
        exploreLandscaper.update();
        exploreLandscaper.checkComm();
        bugPath.update();
        checkEnemyHQ();
        //if (exploreLandscaper.dronesFound) bugPath.updateDrones(danger);
        if (Constants.DEBUG == 1) System.out.println("Bytecode post bugPath update " + Clock.getBytecodeNum());

        //TRY FLEEING FROM WATER

        boolean flee = false;
        if (bugPath.shouldFlee && WaterManager.closestSafeCell != null){
            bugPath.moveTo(WaterManager.closestSafeCell);
            flee = true;
        }

        if (Constants.DEBUG == 1) System.out.println("Bytecode post trying to flee water " + Clock.getBytecodeNum());

        //CHECK URGENCT MOVES: flooded cells, hurt buildings, enemy buildings, etc.

        target = null;
        checkUrgentMoves();
        if (target != null) bugPath.moveTo(target);


        tryBury();
        tryDigAndDeposit();

        //IF NOT FLEE GO TO TARGET
        /*if (!flee && target == null){
            target = getTarget();
            bugPath.moveTo(target);
        }*/

        //END OF TURN STUFF

        if (comm.wallMes != null) buildingZone.update(comm.wallMes);
        buildingZone.run();
        comm.readMessages();
    }

    void checkEnemyHQ(){
        if (enemyHQloc != null) return;
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        for (RobotInfo r : robots){
            if (r.type == RobotType.HQ && r.team != rc.getTeam()){
                enemyHQloc = r.location;
                return;
            }
        }
    }

    void checkUrgentMoves() {
        try {
            /*
            if (exploreLandscaper.urgentFix != null){
                rc.setIndicatorDot(exploreLandscaper.urgentFix, 255, 255, 0);
                System.out.println("URGENT "+ exploreLandscaper.urgentFixType);
            }*/
            if (!rc.isReady()) return;

            //CHECK ENEMY BUILDING
            if (enemyHQloc != null) {
                int d = rc.getLocation().distanceSquaredTo(enemyHQloc);
                if (d > 2) target = enemyHQloc;
                else target = rc.getLocation();
                return;
            }

            //CHECK BUILDING HURT
            if (exploreLandscaper.buildingHurt != null) {
                int d = rc.getLocation().distanceSquaredTo(exploreLandscaper.buildingHurt);
                if (d > 2) {
                    target = exploreLandscaper.buildingHurt;
                } else {
                    target = null;
                    if (rc.isReady()) {
                        Direction dir = rc.getLocation().directionTo(exploreLandscaper.buildingHurt);
                        if (rc.canDigDirt(dir)) rc.digDirt(dir);
                    }
                }
                return;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    void tryBury(){
        try {
            if (!rc.isReady()) return;
            if (enemyHQloc == null) return;
            if (enemyHQloc.distanceSquaredTo(rc.getLocation()) <= 2){
                Direction dir = rc.getLocation().directionTo(enemyHQloc);
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

        int score;

        //TODO: what if can't sense?
        AdjacentSpot(Direction dir){
            try {
                this.dir = dir;
                loc = myLoc.add(dir);
                if (rc.canSenseLocation(loc)){
                    canDig = rc.canDigDirt(dir);
                    if (exploreLandscaper.cantDig[dir.ordinal()]) canDig = false;
                    canDeposit = rc.canDepositDirt(dir);
                    zone = buildingZone.getZone(loc);
                    elevation = rc.senseElevation(loc);
                    flooded = rc.senseFlooding(loc);
                    RobotInfo r = rc.senseRobotAtLocation(loc);

                    score = 0;
                    if (r.type.isBuilding() && r.team != rc.getTeam()){
                        score++;
                        if (r.type == RobotType.HQ) ++score;
                    }

                }
            } catch(Throwable t){
                t.printStackTrace();
            }
        }


        boolean isBetterDiggingTargetThan(AdjacentSpot d){
            if (!canDig) return false;
            if (d == null) return true;
            return score < d.score;
        }

        boolean isBetterDepositTargetThan(AdjacentSpot d){
            if (!canDeposit) return false;
            if (d == null) return true;
            return score > d.score;
        }


    }


}
