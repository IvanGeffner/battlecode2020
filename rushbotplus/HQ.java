package rushbotplus;

import battlecode.common.*;

public class HQ extends MyRobot {

    RobotController rc;
    Direction[] nonZeroDirs = new Direction[]{Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    MapLocation[] surroundings;

    HQWall hqWall;
    BuildingZone buildingZone;
    NetGunManager netGunManager;

    int myX, myY;
    MapLocation myLoc;
    Comm comm;
    int miners = 0;
    int maxSoup = 0;
    Direction dirToSoup = null;
    boolean rush;
    boolean everRush = false;

    final int BUILDER_SOUP = 1080;

    int[] X = new int[]{0,-1,0,0,1,-1,-1,1,1,-2,0,0,2,-2,-2,-1,-1,1,1,2,2,-2,-2,2,2,-3,0,0,3,-3,-3,-1,-1,1,1,3,3,-3,-3,-2,-2,2,2,3,3,-4,0,0,4,-4,-4,-1,-1,1,1,4,4,-3,-3,3,3,-4,-4,-2,-2,2,2,4,4,-5,-4,-4,-3,-3,0,0,3,3,4,4,5,-5,-5,-1,-1,1,1,5,5,-5,-5,-2,-2,2,2,5,5,-4,-4,4,4,-5,-5,-3,-3,3,3,5,5,-6,0,0,6,-6,-6,-1,-1,1,1,6,6,-6,-6,-2,-2,2,2,6,6,-5,-5,-4,-4,4,4,5,5,-6,-6,-3,-3,3,3,6,6};
    int[] Y = new int[]{0,0,-1,1,0,-1,1,-1,1,0,-2,2,0,-1,1,-2,2,-2,2,-1,1,-2,2,-2,2,0,-3,3,0,-1,1,-3,3,-3,3,-1,1,-2,2,-3,3,-3,3,-2,2,0,-4,4,0,-1,1,-4,4,-4,4,-1,1,-3,3,-3,3,-2,2,-4,4,-4,4,-2,2,0,-3,3,-4,4,-5,5,-4,4,-3,3,0,-1,1,-5,5,-5,5,-1,1,-2,2,-5,5,-5,5,-2,2,-4,4,-4,4,-3,3,-5,5,-5,5,-3,3,0,-6,6,0,-1,1,-6,6,-6,6,-1,1,-2,2,-6,6,-6,6,-2,2,-4,4,-5,5,-5,5,-4,4,-3,3,-6,6,-6,6,-3,3};

    HQ(RobotController rc){
        this.rc = rc;
        comm = new Comm(rc);
        surroundings = new MapLocation[8];
        for (int i = 0; i < 8; ++i) surroundings[i] = rc.getLocation().add(nonZeroDirs[i]);
        myLoc = rc.getLocation();
        myX = myLoc.x; myY = myLoc.y;
        buildingZone = new BuildingZone(rc);
        hqWall = new HQWall(rc);
        netGunManager = new NetGunManager(rc);
        //if (Constants.DEBUG == 1) System.out.println("I'm at (" + rc.getLocation().x + ", " + rc.getLocation().y + ")");
    }

    void play(){
        if (comm.singleMessage()) comm.readMessages();
        if (comm.maxSoup > maxSoup) maxSoup = comm.maxSoup;
        checkRush();


        netGunManager.tryShoot();
        getDirToSoup();
        if (shouldBuildMiner()) buildMiner(false);
        if (shouldBuildBuilder()){
            buildMiner(true);
        }

        comm.readMessages();

        hqWall.run();
        if (comm.wallMes != null) buildingZone.update(comm.wallMes);
        if (hqWall.finished()) comm.sendWall(hqWall.mes);
        buildingZone.run();
        if (buildingZone.finished()){
            comm.sendWallFinished();
        }


    }

    boolean shouldBuildMiner(){
        if (!rc.isReady()) return false;
        return miners < 3;
    }

    void buildMiner(boolean send){
        try {
            if (rc.getTeamSoup() < RobotType.MINER.cost) return;
            Direction dir = dirToSoup;
            if (dir == Direction.CENTER || dir == null) dir = Direction.NORTH;
            for (int i = 0; i < 8; ++i){
                if (rc.canBuildRobot(RobotType.MINER, dir)){
                    rc.buildRobot(RobotType.MINER, dir);
                    ++miners;
                    if (miners <= 1) comm.sendMessage(comm.BUILDER_TYPE, 0);
                    return;
                }
                dir = dir.rotateLeft();
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    //TODO improve this crap
    void getDirToSoup(){
        try {
            int soup = 0;
            dirToSoup = null;
            for (int i = 0; i < X.length; ++i) {
                MapLocation loc = new MapLocation(myX + X[i], myY + Y[i]);
                if (myLoc.distanceSquaredTo(loc) > rc.getCurrentSensorRadiusSquared()) break;
                if (!rc.canSenseLocation(loc)) continue;
                if (rc.senseSoup(loc) > 0 && !rc.senseFlooding(loc)){
                    soup += rc.senseSoup(loc);
                    if (dirToSoup == null) dirToSoup = myLoc.directionTo(loc);
                }
            }
            if (soup > maxSoup) maxSoup = soup;
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    boolean shouldBuildBuilder(){
        RobotType r = BuildingManager.getNextBuilding(comm);
        if (r == null) return false;
        if (minerNearby()) return false;
        if (rc.getTeamSoup() < r.cost + RobotType.MINER.cost + Constants.SAFETY_SOUP) return false;
        return true;
    }

    boolean minerNearby(){
        RobotInfo[] r = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam());
        for (RobotInfo ri : r){
            if (ri.getType() == RobotType.MINER) return true;
        }
        return false;
    }

    void checkRush(){
        rush = false;
        if (isRush()){
            rush = true;
            everRush = true;
            comm.sendRush();
        }
        else comm.sendRushEnd();
    }

    boolean isRush(){
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        for (RobotInfo r : enemies){
            switch (r.type){
                case MINER:
                case LANDSCAPER:
                case NET_GUN:
                case DESIGN_SCHOOL:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

}
