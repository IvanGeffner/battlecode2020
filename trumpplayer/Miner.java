package trumpplayer;

import battlecode.common.*;

import java.util.ArrayList;

public class Miner extends MyRobot{

    RobotController rc;
    Direction[] dirs = Direction.values();
    BugPath bugPath;
    Explore explore;

    Miner(RobotController rc){
        this.rc = rc;
        explore = new Explore(rc);
        bugPath = new BugPath(rc);
    }

    void play(){
        if (Constants.DEBUG == 1) System.out.println("Turn starts: " + Clock.getBytecodeNum());
        explore.update();
        if (Constants.DEBUG == 1) System.out.println("Update Finishes: " + Clock.getBytecodeNum());
        tryMine();
        tryDeposit();
        if (Constants.DEBUG == 1) System.out.println("GetTarget Starts: " + Clock.getBytecodeNum());
        MapLocation target = getTarget();
        if (Constants.DEBUG == 1) System.out.println("GetTarget Finishes: " + Clock.getBytecodeNum());
        if (target != null) rc.setIndicatorDot(target, 255, 0, 0);
        bugPath.moveTo(target);
    }

    MapLocation getTarget(){
        if (rc.getSoupCarrying() >= rc.getType().soupLimit) return explore.closestRefineryLoc;
        if (rc.getSoupCarrying() > 0){
            if (explore.closestSoup != null) return explore.closestSoup;
            else return explore.closestRefineryLoc;
        }
        MapLocation ans = explore.getBestTarget();
        if (ans == null) ans = explore.exploreTarget();
        return ans;
    }

    boolean tryMine(){
        try {
            if (rc.getSoupCarrying() >= rc.getType().soupLimit) return false;
            MapLocation myLoc = rc.getLocation();
            for (Direction dir : dirs) {
                MapLocation newLoc = myLoc.add(dir);
                if (rc.canSenseLocation(newLoc) && rc.senseSoup(newLoc) > 0 && rc.canMineSoup(dir)) {
                    rc.mineSoup(dir);
                    return true;
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
        return false;
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



}
