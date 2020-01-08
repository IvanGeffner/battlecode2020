package trumpplayer;

import battlecode.common.*;

import java.rmi.server.ExportException;
import java.util.ArrayList;

public class Explore {

    final int MAX_SOUP_ARRAY = 50;
    final int MAX_SOUP_ARRAY_AUX = 49;
    final int SOUP_BIT = 2;
    final int EXPLORE_BIT = 1;
    final int INDEX_OFFSET = 2;
    final int RESET_BASE = 1;
    int[][] map;
    RobotController rc;
    MapLocation closestSoup;
    MapLocation closestRefineryLoc;
    MapLocation HQloc;
    Direction[] dirs = Direction.values();
    MapLocation[] soups =  new MapLocation[MAX_SOUP_ARRAY];
    int currentIndex = 0;
    MapLocation myLoc;
    MapLocation exploreTarget = null;

    int[] X = new int[]{0,-1,0,0,1,-1,-1,1,1,-2,0,0,2,-2,-2,-1,-1,1,1,2,2,-2,-2,2,2,-3,0,0,3,-3,-3,-1,-1,1,1,3,3,-3,-3,-2,-2,2,2,3,3,-4,0,0,4,-4,-4,-1,-1,1,1,4,4,-3,-3,3,3,-4,-4,-2,-2,2,2,4,4};
    int[] Y = new int[]{0,0,-1,1,0,-1,1,-1,1,0,-2,2,0,-1,1,-2,2,-2,2,-1,1,-2,2,-2,2,0,-3,3,0,-1,1,-3,3,-3,3,-1,1,-2,2,-3,3,-3,3,-2,2,0,-4,4,0,-1,1,-4,4,-4,4,-1,1,-3,3,-3,3,-2,2,-4,4,-4,4,-2,2};

    Explore(RobotController rc){
        this.rc = rc;
        map = new int[rc.getMapWidth()][rc.getMapHeight()];
        findHQ();
        closestRefineryLoc = HQloc;
    }

    void findHQ(){
        try {
            MapLocation myLoc = rc.getLocation();
            for (Direction dir : dirs) {
                MapLocation newLoc = myLoc.add(dir);
                RobotInfo r = rc.senseRobotAtLocation(newLoc);
                if (r != null && r.type == RobotType.HQ && r.team == rc.getTeam()){
                    HQloc = newLoc;
                    return;
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void update(){
        myLoc = rc.getLocation();
        checkRefinery();
        checkCells();
    }

    void checkRefinery(){
        try {
            if (rc.canSenseLocation(closestRefineryLoc)) {
                RobotInfo r = rc.senseRobotAtLocation(closestRefineryLoc);
                if (r.team != rc.getTeam() || r.type != RobotType.REFINERY) closestRefineryLoc = HQloc;
            }
            RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam());
            int bestDist = myLoc.distanceSquaredTo(HQloc);
            for (RobotInfo r : robots){
                switch (r.type){
                    case HQ:
                    case REFINERY:
                        if (r.team == rc.getTeam()){
                            int dist = myLoc.distanceSquaredTo(r.location);
                            if (dist < bestDist){
                                bestDist = dist;
                                closestRefineryLoc = r.location;
                            }
                        }
                        break;

                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void checkCells(){
        int sight = rc.getCurrentSensorRadiusSquared();
        int x = myLoc.x;
        int y = myLoc.y;
        closestSoup = null;
        try {
            for (int i = 0; i < X.length; ++i) {
                int newX = x + X[i], newY = y + Y[i];
                MapLocation newLoc = new MapLocation(newX, newY);
                if (!rc.canSenseLocation(newLoc)) break;
                if (!rc.onTheMap(newLoc)) continue;
                int prevNumber = map[newX][newY] | EXPLORE_BIT;
                if (rc.senseSoup(newLoc) > 0 && !rc.senseFlooding(newLoc)) {
                    if (closestSoup == null) closestSoup = newLoc;
                    if ((prevNumber & SOUP_BIT) == 0) {
                        prevNumber = prevNumber | SOUP_BIT;
                        soups[currentIndex] = newLoc;
                        prevNumber = prevNumber | (currentIndex << INDEX_OFFSET);
                        currentIndex = (currentIndex + 1) % MAX_SOUP_ARRAY;
                    }
                } else{
                    if ((prevNumber & SOUP_BIT) > 0) {
                        soups[(prevNumber >>> INDEX_OFFSET)] = null;
                        prevNumber = prevNumber & RESET_BASE;
                    }
                }
                map[newX][newY] = prevNumber;
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    MapLocation getBestTarget(){
        if (closestSoup != null) return closestSoup;
        int bestDist = Constants.INF;
        MapLocation ans = null;
        int aux = currentIndex;
        boolean found = false;
        for (int i = 1; i < 20; ++i){
            int index = (aux + MAX_SOUP_ARRAY - i)%MAX_SOUP_ARRAY;
            MapLocation soupLoc = soups[index];
            if (soupLoc == null){
                if (!found) currentIndex = index;
                continue;
            }
            found = true;
            int dist = myLoc.distanceSquaredTo(soupLoc);
            if (dist < bestDist){
                bestDist = dist;
                ans = soupLoc;
            }
        }
        return ans;
    }

    MapLocation exploreTarget(){
        if (exploreTarget != null){
            if (map[exploreTarget.x][exploreTarget.y] > 0) exploreTarget = null;
        }
        int bestDist = Constants.INF;
        if (exploreTarget != null){
            bestDist = myLoc.distanceSquaredTo(exploreTarget);
        }
        while (Clock.getBytecodesLeft() > 3000){
            int x = (int)(Math.random()*rc.getMapWidth());
            int y = (int)(Math.random()*rc.getMapHeight());
            MapLocation newLoc = new MapLocation(x,y);
            if (map[newLoc.x][newLoc.y] > 0) continue;
            int dist = myLoc.distanceSquaredTo(newLoc);
            if (dist < bestDist){
                bestDist = dist;
                exploreTarget = newLoc;
            }
        }
        return exploreTarget;
    }

    MapLocation getClosestSoup(){
        return closestSoup;
    }
}
