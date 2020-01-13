package old;

import battlecode.common.*;

public class Comm {

    RobotController rc;

    MapLocation EnemyHQLoc = null;
    int maxSoup = 0;
    int turn = 1;

    final int FIRST_ROUND = 1;

    static final int HQ_TYPE = 1;
    static final int SOUP_TYPE = 2;
    static final int BUILDER_TYPE = 3;
    static final int BUILDING_TYPE = 4;
    static final int WALL = 5;
    static final int EMERGENCY = 6;
    static final int ENEMY_UNIT = 7;
    static final int WATER = 8;
    static final int GUN = 9;
    static final int GUN_DESTROYED = 10;

    final int[] X = new int[]{0,-1,0,0,1,-1,-1,1,1,-2,0,0,2,-2,-2,-1,-1,1,1,2,2,-2,-2,2,2,-3,0,0,3,-3,-3,-1,-1,1,1,3,3,-3,-3,-2,-2,2,2,3,3};
    final int[] Y = new int[]{0,0,-1,1,0,-1,1,-1,1,0,-2,2,0,-1,1,-2,2,-2,2,-1,1,-2,2,-2,2,0,-3,3,0,-1,1,-3,3,-3,3,-1,1,-2,2,-3,3,-3,3,-2,2};

    int[] buildings;
    int[] wallMes = null;

    int[][] map, dangerMap;

    int MASK = 4534653;
    boolean seenLandscaper = false;
    boolean seenUnit = false;
    boolean terrestrial = false;

    MapLocation water = null;

    int BYTECODE_LEFT = 300;

    final int BYTECODE_LEFT_DRONE = 1000;

    boolean imdrone;
    int w,h;

    Comm(RobotController rc){
        this.rc = rc;
        imdrone = rc.getType() == RobotType.DELIVERY_DRONE;
        MASK += rc.getTeam().ordinal();
        buildings = new int[RobotType.values().length];
        w = rc.getMapWidth(); h = rc.getMapHeight();
        map = new int[w][h];
        if (imdrone){
            dangerMap = new int[rc.getMapWidth()][rc.getMapHeight()];
            BYTECODE_LEFT = BYTECODE_LEFT_DRONE;
        }
    }

    boolean singleMessage(){
        return turn == rc.getRoundNum()-1;
    }

    boolean upToDate(){
        return turn == rc.getRoundNum();
    }

    void readMessages(){
        try {
            int r = rc.getRoundNum();
            while (turn < r && Clock.getBytecodesLeft() >= BYTECODE_LEFT) {
                Transaction[] transactions = rc.getBlock(turn);
                for (Transaction t : transactions){
                    if (t == null) continue; // Can this happen? wtf
                    if ((MASK^t.getMessage()[6]) != turn) continue;
                    switch(t.getMessage()[0]&1023){
                        case HQ_TYPE:
                            int code = t.getMessage()[1];
                            EnemyHQLoc = new MapLocation((code >>> 6) & 63, (code & 63));
                            if (map[EnemyHQLoc.x][EnemyHQLoc.y] == 0){
                                map[EnemyHQLoc.x][EnemyHQLoc.y] = 1;
                                if (imdrone) addDanger(EnemyHQLoc);
                            }
                            terrestrial = (code >>> 12) > 0;
                            break;
                        case SOUP_TYPE:
                            int soup = t.getMessage()[1];
                            if (soup > maxSoup) maxSoup = soup;
                            break;
                        case BUILDING_TYPE:
                            int index = t.getMessage()[1];
                            if (0 <= index && index < buildings.length) ++buildings[index];
                            break;
                        case WALL:
                            if (wallMes == null) wallMes = t.getMessage();
                            break;
                        case EMERGENCY:
                            seenLandscaper = true;
                            break;
                        case ENEMY_UNIT:
                            seenUnit = true;
                            break;
                        case WATER:
                            code = t.getMessage()[1];
                            water = new MapLocation((code >>> 6)&63, (code&63));
                            break;
                        case GUN:
                            code = t.getMessage()[1];
                            MapLocation droneLoc = new MapLocation((code >>> 6)&63, (code&63));
                            if (map[droneLoc.x][droneLoc.y] == 0) {
                                map[droneLoc.x][droneLoc.y] = 1;
                                if (imdrone) addDanger(droneLoc);
                            }
                            break;
                        case GUN_DESTROYED:
                            code = t.getMessage()[1];
                            droneLoc = new MapLocation((code >>> 6)&63, (code&63));
                            if (map[droneLoc.x][droneLoc.y] == 1) {
                                map[droneLoc.x][droneLoc.y] = 0;
                                if (imdrone) removeDanger(droneLoc);
                            }
                            break;
                    }
                }
                turn++;
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    boolean checkBuilder(){
        try {
            int r = rc.getRoundNum();
            if (r <= FIRST_ROUND) return false;
            Transaction[] transactions = rc.getBlock(r-1);
            for (Transaction t : transactions){
                if (t == null) continue;
                if ((MASK^t.getMessage()[6]) == r-1){
                    if (t.getMessage()[0] == BUILDER_TYPE) return true;
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
        return false;
    }

    void sendHQLoc(MapLocation loc, int terrestrial){
        if (this.terrestrial) return;
        if (terrestrial == 0 && EnemyHQLoc != null) return;
        if (!upToDate()) return;
        sendMessage(HQ_TYPE, (loc.x << 6) | loc.y | (terrestrial << 12));
    }

    void sendMaxSoup(int soup){
        if (soup <= maxSoup) return;
        if (!upToDate()) return;
        sendMessage(SOUP_TYPE, soup);
    }

    void sendWall(int[] wall){
        if (wallMes != null) return;
        try {
            wall[6] = rc.getRoundNum() ^ MASK;
            int b = getBidValue();
            if (rc.canSubmitTransaction(wall, b)) rc.submitTransaction(wall, b);
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void sendWater(MapLocation loc){
        if (water != null) return;
        if (!upToDate()) return;
        sendMessage(WATER, (loc.x << 6) | loc.y);
    }

    void sendMessage(int type, int code){
        try {
            int b = getBidValue();
            int[] message = new int[]{type, code, 0, 0, 0, 0, rc.getRoundNum() ^ MASK};
            if (rc.canSubmitTransaction(message, b)) rc.submitTransaction(message, b);
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    void sendLandscaper(){
        if (seenLandscaper) return;
        if (!upToDate()) return;
        sendMessage(EMERGENCY, 0);
    }

    void sendEnemyUnit(){
        if (seenUnit) return;
        if (!upToDate()) return;
        sendMessage(ENEMY_UNIT, 0);
    }

    void sendGun(MapLocation loc){
        if (!upToDate()) return;
        if (map[loc.x][loc.y] == 1) return;
        sendMessage(GUN, (loc.x << 6) | loc.y);
    }

    void sendGunDestroyed(MapLocation loc){
        if (!upToDate()) return;
        if (map[loc.x][loc.y] == 0) return;
        sendMessage(GUN_DESTROYED, (loc.x << 6) | loc.y);
    }

    int getBidValue(){
        try {
            int r = rc.getRoundNum();
            if (r <= FIRST_ROUND) return 1;
            Transaction[] transactions = rc.getBlock(r-1);
            int ans = 1;
            if (transactions.length < GameConstants.MAX_BLOCKCHAIN_TRANSACTION_LENGTH) return 1;
            for (Transaction t : transactions){
                if (t == null) return 1;
                if ((MASK^t.getMessage()[6]) != r-1){
                    int b = t.getCost();
                    if (b >= ans) ans = b+1;
                }
            }
            return ans;
        } catch (Throwable t){
            t.printStackTrace();
        }
        return 1;
    }

    void addDanger(MapLocation loc){
        if (Constants.DEBUG == 1) System.out.println("Starting danger at " + Clock.getBytecodeNum());
        int x = loc.x, y = loc.y;
        int i = X.length;
        while (--i >= 0){
            int newX = x + X[i], newY = y + Y[i];
            if (newX < 0 || newX >= w) continue;
            if (newY < 0 || newY >= h) continue;
            ++dangerMap[newX][newY];
            if (Constants.DEBUG == 1) rc.setIndicatorDot(new MapLocation(newX, newY), 0, 0, 255);
        }
        if (Constants.DEBUG == 1) System.out.println("Ending danger at " + Clock.getBytecodeNum());

    }

    void removeDanger(MapLocation loc){
        int x = loc.x, y = loc.y;
        int i = X.length;
        while (--i >= 0){
            int newX = x + X[i], newY = y + Y[i];
            if (newX < 0 || newX >= w) continue;
            if (newY < 0 || newY >= h) continue;
            --dangerMap[newX][newY];
        }
    }



}
