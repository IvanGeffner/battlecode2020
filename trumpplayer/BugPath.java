package trumpplayer;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class BugPath {

    RobotController rc;

    BugPath(RobotController rc){
        this.rc = rc;
    }

    boolean rotateRight = true; //if I should rotate right or left
    MapLocation lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToTarget = Constants.INF; //minimum distance I've been to the enemy while going around an obstacle
    MapLocation minLocationToTarget = null;
    MapLocation prevTarget = null; //previous target

    final int MIN_DIST_RESET = 3;

    void moveTo(MapLocation target){
        //No target? ==> bye!
        if (!rc.isReady()) return;
        if (target == null) return;

        //if (rc.getID() == 13977) System.out.println("Moving to " + target.x + " " + target.y);

        //different target? ==> previous data does not help!
        if (prevTarget == null){
            //if (rc.getID() == 13977) System.out.println("Previous Target was null");
            resetPathfinding();
        }
        else {
            int distTargets = target.distanceSquaredTo(prevTarget);
            if (distTargets > 0) {
                if (distTargets >= MIN_DIST_RESET){
                    //if (rc.getID() == 13977) System.out.println("Change of targets!");
                    resetPathfinding();
                }
                else softReset(target);
            }
        }

        //If I'm at a minimum distance to the target, I'm free!
        MapLocation myLoc = rc.getLocation();
        int d = myLoc.distanceSquaredTo(target);
        if (d <= minDistToTarget){
            resetPathfinding();
            minDistToTarget = d;
            minLocationToTarget = myLoc;
        }

        //Update data
        prevTarget = target;

        //If there's an obstacle I try to go around it [until I'm free] instead of going to the target directly
        Direction dir = myLoc.directionTo(target);
        if (lastObstacleFound != null) dir = myLoc.directionTo(lastObstacleFound);


        int sight = rc.getCurrentSensorRadiusSquared();
        boolean blind = sight < 2;

        try {
            //This should not happen for a single unit, but whatever
            if (rc.canMove(dir)){
                if (blind || !rc.senseFlooding(myLoc.add(dir))) resetPathfinding();
            }

            //I rotate clockwise or counterclockwise (depends on 'rotateRight'). If I try to go out of the map I change the orientation
            //Note that we have to try at most 16 times since we can switch orientation in the middle of the loop. (It can be done more efficiently)
            int i = 16;
            while (i-- >= 0) {
                MapLocation newLoc = myLoc.add(dir);
                if (rc.canMove(dir)) {
                    if (blind || !rc.senseFlooding(newLoc)) {
                        rc.move(dir);
                        return;
                    }
                }
                if (!rc.canSenseLocation(newLoc)) rotateRight = !rotateRight;
                    //If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
                else lastObstacleFound = newLoc;
                if (rotateRight) dir = dir.rotateRight();
                else dir = dir.rotateLeft();
            }

            if (rc.canMove(dir)){
                if (blind || !rc.senseFlooding(myLoc.add(dir))) {
                    rc.move(dir);
                    return;
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    //clear some of the previous data
    void resetPathfinding(){
        //if (rc.getID() == 13977) System.out.println("reset!");
        lastObstacleFound = null;
        minDistToTarget = Constants.INF;
    }

    void softReset(MapLocation target){
        //if (rc.getID() == 13977) System.out.println("soft reset!");
        if (minLocationToTarget != null) minDistToTarget = minLocationToTarget.distanceSquaredTo(target);
        else resetPathfinding();
    }

}
