package rushplus;

import battlecode.common.Clock;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class BuildingZone {

    static final int BUILDING_AREA = 1;
    static final int WALL = 2;

    int[][] map;
    int[] message = null;

    int row = 0;
    final int h,w;

    final int[] X = new int[]{0,-1,0,0,1,-1,-1,1,1,-2,0,0,2,-2,-2,-1,-1,1,1,2,2,-2,-2,2,2,-3,0,0,3,-3,-3,-1,-1,1,1,3,3,-3,-3,-2,-2,2,2,3,3,-4,0,0,4,-4,-4,-1,-1,1,1,4,4,-3,-3,3,3,-4,-4,-2,-2,2,2,4,4,-5,-4,-4,-3,-3,0,0,3,3,4,4,5,-5,-5,-1,-1,1,1,5,5,-5,-5,-2,-2,2,2,5,5,-4,-4,4,4,-5,-5,-3,-3,3,3,5,5,-6,0,0,6,-6,-6,-1,-1,1,1,6,6,-6,-6,-2,-2,2,2,6,6,-5,-5,-4,-4,4,4,5,5,-6,-6,-3,-3,3,3,6,6};
    final int[] Y = new int[]{0,0,-1,1,0,-1,1,-1,1,0,-2,2,0,-1,1,-2,2,-2,2,-1,1,-2,2,-2,2,0,-3,3,0,-1,1,-3,3,-3,3,-1,1,-2,2,-3,3,-3,3,-2,2,0,-4,4,0,-1,1,-4,4,-4,4,-1,1,-3,3,-3,3,-2,2,-4,4,-4,4,-2,2,0,-3,3,-4,4,-5,5,-4,4,-3,3,0,-1,1,-5,5,-5,5,-1,1,-2,2,-5,5,-5,5,-2,2,-4,4,-4,4,-3,3,-5,5,-5,5,-3,3,0,-6,6,0,-1,1,-6,6,-6,6,-1,1,-2,2,-6,6,-6,6,-2,2,-4,4,-5,5,-5,5,-4,4,-3,3,-6,6,-6,6,-3,3};

    MapLocation HQloc = null;

    RobotController rc;

    int cont = X.length, wallCont = X.length;
    boolean shouldComputeWall = false;

    BuildingZone(RobotController rc){
        this.rc = rc;
        w = rc.getMapWidth();
        h = rc.getMapHeight();
        map = new int[w][0];
        shouldComputeWall = rc.getType() == RobotType.LANDSCAPER;
    }

    void update(int[] message){
        this.message = message;
        HQloc = new MapLocation((message[0] >>>16)&63, (message[0] >>>10)&63);
        //if (Constants.DEBUG == 1) System.out.println(message[0] + " " + message[1] + " " + message[2] + " " + message[3] + " " + message[4] + " " + message[5]);
    }

    void run(){
        while (row < map.length){
            if (Clock.getBytecodesLeft() <= 300) return;
            map[row] = new int[h];
            ++row;
        }
        if (message == null) return;
        while (cont >= 0){
            if (Clock.getBytecodesLeft() <= 300) return;
            int bit = message[cont/32 + 1]&(1 << (cont%32));
            if (bit != 0){
                int x = HQloc.x + X[cont], y = HQloc.y + Y[cont];
                map[x][y] = 1;
            }
            cont--;
        }
        if (!shouldComputeWall) return;
        while (wallCont >= 0){
            if (Clock.getBytecodesLeft() <= 700) return;
            System.out.print(Clock.getBytecodeNum());
            int bit = message[wallCont/32 + 1]&(1 << (wallCont%32));
            if (bit != 0){
                int x = HQloc.x + X[wallCont], y = HQloc.y + Y[wallCont];
                int i = 9;
                while (--i >= 0){
                    int newX = x + X[i], newY = y + Y[i];
                    if (0 > newX) continue;
                    if (0 > newY) continue;
                    if (newX >= w) continue;
                    if (newY >= h) continue;
                    if (map[newX][newY] == 0){
                        map[newX][newY] = 2;
                        if (Constants.DEBUG == 1) rc.setIndicatorDot(new MapLocation(newX, newY), 0, 255, 0);
                    }
                }
            }
            wallCont--;
            System.out.print(Clock.getBytecodeNum());
        }
    }

    boolean finished(){
        if (cont >= 0) return false;
        if (!shouldComputeWall) return true;
        return wallCont < 0;
    }

    void debugPrint(){
        if (Constants.DEBUG != 1) return;
        System.out.println("Debug printing zone!");
        int i = X.length;
        while (--i >= 0){
            MapLocation loc = new MapLocation(HQloc.x + X[i], HQloc.y + Y[i]);
            if (loc.x < 0) continue;
            if (loc.x >= map.length) continue;
            if (loc.y < 0) continue;
            if (loc.y >= h) continue;
            if (map[loc.x][loc.y] == 1) rc.setIndicatorDot(loc, 0, 0, 255);
        }
    }

}
