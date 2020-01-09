package trumpplayer;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class BugPath {

    RobotController rc;

    BugPath(RobotController rc){
        this.rc = rc;
    }

    Boolean rotateRight = null; //if I should rotate right or left
    Boolean rotateRightAux = null;
    MapLocation lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToTarget = Constants.INF; //minimum distance I've been to the enemy while going around an obstacle
    MapLocation minLocationToTarget = null;
    MapLocation prevTarget = null; //previous target
    Direction[] dirs = Direction.values();

    MapLocation myLoc;
    boolean[] canMoveArray;

    final int MIN_DIST_RESET = 3;

    void moveTo(MapLocation target){
        //No target? ==> bye!
        if (!rc.isReady()) return;
        if (target == null) return;


        //different target? ==> previous data does not help!
        if (prevTarget == null){
            resetPathfinding();
        }
        else {
            int distTargets = target.distanceSquaredTo(prevTarget);
            if (distTargets > 0) {
                if (distTargets >= MIN_DIST_RESET){
                    rotateRight = null;
                    rotateRightAux = null;
                    resetPathfinding();
                }
                else softReset(target);
            }
        }

        //Update data
        prevTarget = target;
        myLoc = rc.getLocation();
        updateArray();


        //If I'm at a minimum distance to the target, I'm free!
        int d = myLoc.distanceSquaredTo(target);
        if (d == 0) return;
        if (d <= minDistToTarget){
            resetPathfinding();
            minDistToTarget = d;
            minLocationToTarget = myLoc;
        }

        //If there's an obstacle I try to go around it [until I'm free] instead of going to the target directly
        Direction dir = myLoc.directionTo(target);
        if (lastObstacleFound == null){
            if (tryGreedyMove()){
                resetPathfinding();
                return;
            }
        }
        else dir = myLoc.directionTo(lastObstacleFound);

        try {

            //TODO: when obstacle moves
            if (canMoveArray[dir.ordinal()]){
                rc.move(dir);
                resetPathfinding();
                return;
            }

            checkRotate(dir);

            //I rotate clockwise or counterclockwise (depends on 'rotateRight'). If I try to go out of the map I change the orientation
            //Note that we have to try at most 16 times since we can switch orientation in the middle of the loop. (It can be done more efficiently)
            int i = 16;
            while (i-- >= 0) {
                MapLocation newLoc = myLoc.add(dir);
                if (canMoveArray[dir.ordinal()]) {
                    rc.move(dir);
                    return;
                }
                if (!rc.canSenseLocation(newLoc)) rotateRight = !rotateRight;
                    //If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
                else lastObstacleFound = newLoc;
                if (rotateRight) dir = dir.rotateRight();
                else dir = dir.rotateLeft();
            }

            if (canMoveArray[dir.ordinal()]){
                rc.move(dir);
                return;
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void updateArray(){
        canMoveArray = new boolean[9];
        try {
            boolean blind = rc.getCurrentSensorRadiusSquared() < 2;
            for (Direction dir : dirs) {
                if (rc.canMove(dir)) {
                    if (blind || !rc.senseFlooding(myLoc.add(dir))) canMoveArray[dir.ordinal()] = true;
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }

    }

    boolean tryGreedyMove(){
        try {
            MapLocation myLoc = rc.getLocation();
            Direction dir = myLoc.directionTo(prevTarget);
            if (canMoveArray[dir.ordinal()]) {
                rc.move(dir);
                return true;
            }
            int dist = myLoc.distanceSquaredTo(prevTarget);
            int dist1 = Constants.INF, dist2 = Constants.INF;
            Direction dir1 = dir.rotateRight();
            MapLocation newLoc = myLoc.add(dir1);
            if (canMoveArray[dir1.ordinal()]) dist1 = newLoc.distanceSquaredTo(prevTarget);
            Direction dir2 = dir.rotateLeft();
            newLoc = myLoc.add(dir2);
            if (canMoveArray[dir2.ordinal()]) dist2 = newLoc.distanceSquaredTo(prevTarget);
            if (dist1 < dist && dist1 < dist2) {
                rotateRightAux = true;
                rc.move(dir1);
                return true;
            }
            if (dist2 < dist && dist2 < dist1) {
                rotateRightAux = false;
                rc.move(dir2);
                return true;
            }
        } catch(Throwable t){
            t.printStackTrace();
        }
        return false;
    }

    //TODO: check remaining cases
    void checkRotate(Direction dir){
        if (rotateRight != null) return;
        if (rotateRightAux != null){
            rotateRight = rotateRightAux;
            return;
        }
        rotateRight = true;
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

    int getOrdinal(Direction dir){
        switch (dir){
            case CENTER:
                return 0;
            case NORTH:
                return 1;
            case NORTHWEST:
                return 2;
            case WEST:
                return 3;
            case SOUTHWEST:
                return 4;
            case SOUTH:
                return 5;
            case SOUTHEAST:
                return 6;
            case EAST:
                return 7;
            default:
                return 8;
        }
    }

}
