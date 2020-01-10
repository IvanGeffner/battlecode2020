package trumpplayer;

import battlecode.common.*;

public class HQ extends MyRobot{

    RobotController rc;
    Direction[] nonZeroDirs = new Direction[]{Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    MapLocation[] surroundings;

    int myX, myY;
    MapLocation myLoc;

    int[] X = new int[]{0,-1,0,0,1,-1,-1,1,1,-2,0,0,2,-2,-2,-1,-1,1,1,2,2,-2,-2,2,2,-3,0,0,3,-3,-3,-1,-1,1,1,3,3,-3,-3,-2,-2,2,2,3,3,-4,0,0,4,-4,-4,-1,-1,1,1,4,4,-3,-3,3,3,-4,-4,-2,-2,2,2,4,4,-5,-4,-4,-3,-3,0,0,3,3,4,4,5,-5,-5,-1,-1,1,1,5,5,-5,-5,-2,-2,2,2,5,5,-4,-4,4,4,-5,-5,-3,-3,3,3,5,5,-6,0,0,6,-6,-6,-1,-1,1,1,6,6,-6,-6,-2,-2,2,2,6,6,-5,-5,-4,-4,4,4,5,5,-6,-6,-3,-3,3,3,6,6};
    int[] Y = new int[]{0,0,-1,1,0,-1,1,-1,1,0,-2,2,0,-1,1,-2,2,-2,2,-1,1,-2,2,-2,2,0,-3,3,0,-1,1,-3,3,-3,3,-1,1,-2,2,-3,3,-3,3,-2,2,0,-4,4,0,-1,1,-4,4,-4,4,-1,1,-3,3,-3,3,-2,2,-4,4,-4,4,-2,2,0,-3,3,-4,4,-5,5,-4,4,-3,3,0,-1,1,-5,5,-5,5,-1,1,-2,2,-5,5,-5,5,-2,2,-4,4,-4,4,-3,3,-5,5,-5,5,-3,3,0,-6,6,0,-1,1,-6,6,-6,6,-1,1,-2,2,-6,6,-6,6,-2,2,-4,4,-5,5,-5,5,-4,4,-3,3,-6,6,-6,6,-3,3};

    HQ(RobotController rc){
        this.rc = rc;
        surroundings = new MapLocation[8];
        for (int i = 0; i < 8; ++i) surroundings[i] = rc.getLocation().add(nonZeroDirs[i]);
        myLoc = rc.getLocation();
        myX = myLoc.x; myY = myLoc.y;
        if (Constants.DEBUG == 1) System.out.println("I'm at (" + rc.getLocation().x + ", " + rc.getLocation().y + ")");
    }



    int miners = 0;

    void play(){
        if (shouldBuildMiner()) buildMiner();
    }

    boolean shouldBuildMiner(){
        if (!rc.isReady()) return false;
        return miners <= 14;
    }

    void buildMiner(){
        try {
            if (rc.getTeamSoup() < RobotType.MINER.cost) return;
            Direction dir = getBestDirection();
            if (dir == Direction.CENTER) dir = Direction.NORTH;
            for (int i = 0; i < 8; ++i){
                if (rc.canBuildRobot(RobotType.MINER, dir)){
                    rc.buildRobot(RobotType.MINER, dir);
                    ++miners;
                    return;
                }
                dir = dir.rotateLeft();
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    //TODO improve this crap
    Direction getBestDirection(){
        try {
            int bestDist = Constants.INF;
            int bestIndex = -1;
            for (int i = 1; i < X.length; ++i) {
                MapLocation loc = new MapLocation(myX + X[i], myY + Y[i]);
                if (myLoc.distanceSquaredTo(loc) > rc.getCurrentSensorRadiusSquared()) break;
                if (rc.senseSoup(loc) > 0){
                    Direction dir = myLoc.directionTo(loc);
                    return dir;
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
        return Direction.NORTH;
    }

}
