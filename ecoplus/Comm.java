package ecoplus;

import battlecode.common.*;

public class Comm {

    RobotController rc;

    MapLocation enemyHQLoc = null;
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
    static final int VERTICAL_SYMMETRY = 11;
    static final int HORIZONTAL_SYMMETRY = 12;
    static final int ROTATIONAL_SYMMETRY = 13;

    final int[] X = new int[]{0,-1,0,0,1,-1,-1,1,1,-2,0,0,2,-2,-2,-1,-1,1,1,2,2,-2,-2,2,2,-3,0,0,3,-3,-3,-1,-1,1,1,3,3,-3,-3,-2,-2,2,2,3,3};
    final int[] Y = new int[]{0,0,-1,1,0,-1,1,-1,1,0,-2,2,0,-1,1,-2,2,-2,2,-1,1,-2,2,-2,2,0,-3,3,0,-1,1,-3,3,-3,3,-1,1,-2,2,-3,3,-3,3,-2,2};

    int[] buildings;
    int[] wallMes = null;

    int[][] map;

    DangerDrone dangerDrone;

    int MASK = 4534653;
    boolean seenLandscaper = false;
    boolean seenUnit = false;
    boolean terrestrial = false;

    boolean vertical = true, horizontal = true, rotational = true;

    MapLocation water = null;

    MapLocation HQLoc;

    boolean imdrone;
    int w,h;

    MapLocation latestMiningLoc;
    int latestMiningLocTurn;

    Comm(RobotController rc){
        this.rc = rc;
        imdrone = rc.getType() == RobotType.DELIVERY_DRONE;
        MASK += rc.getTeam().ordinal();
        buildings = new int[RobotType.values().length];
        w = rc.getMapWidth(); h = rc.getMapHeight();
        map = new int[w][h];
        if (imdrone) dangerDrone = new DangerDrone(rc);
    }

    boolean singleMessage(){
        return turn == rc.getRoundNum()-1;
    }

    boolean upToDate(){
        return turn == rc.getRoundNum();
    }

    void readMessages(){
        try {
            if (imdrone) dangerDrone.complete();
            int r = rc.getRoundNum();
            while (turn < r && Clock.getBytecodesLeft() >= Constants.SAFETY_BYTECODE_MESSAGES) {
                Transaction[] transactions = rc.getBlock(turn);
                for (Transaction t : transactions){
                    if (t == null){
                        //++turn;
                        continue; // Can this happen? wtf
                    }
                    if ((MASK^t.getMessage()[6]) != turn){
                        //++turn;
                        continue;
                    }
                    int type = t.getMessage()[0]&1023;
                    switch(type){
                        case HQ_TYPE:
                            int code = t.getMessage()[1];
                            enemyHQLoc = new MapLocation((code >>> 6) & 63, (code & 63));
                            if (map[enemyHQLoc.x][enemyHQLoc.y] == 0){
                                map[enemyHQLoc.x][enemyHQLoc.y] = 1;
                                if (imdrone) dangerDrone.addDanger(enemyHQLoc);
                            }
                            terrestrial = (code >>> 12) > 0;
                            break;
                        case SOUP_TYPE:
                            int soup = t.getMessage()[1];
                            if (soup > maxSoup){
                                maxSoup = soup;
                                code = t.getMessage()[0];
                                latestMiningLoc = new MapLocation ((code >>> 16)&63, (code >>> 10)&63);
                                latestMiningLocTurn = turn;
                            }
                            break;
                        case BUILDING_TYPE:
                            int index = t.getMessage()[1];
                            if (0 <= index && index < buildings.length) ++buildings[index];
                            break;
                        case WALL:
                            if (wallMes == null){
                                wallMes = t.getMessage();
                                code = t.getMessage()[0];
                                HQLoc = new MapLocation ((code >>> 16)&63, (code >>> 10)&63);
                            }
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
                                if (imdrone) dangerDrone.addDanger(droneLoc);
                            }
                            break;
                        case GUN_DESTROYED:
                            code = t.getMessage()[1];
                            droneLoc = new MapLocation((code >>> 6)&63, (code&63));
                            if (map[droneLoc.x][droneLoc.y] == 1) {
                                map[droneLoc.x][droneLoc.y] = 0;
                                if (imdrone) dangerDrone.removeDanger(droneLoc);
                            }
                            break;
                        case HORIZONTAL_SYMMETRY:
                            horizontal = false;
                            break;
                        case VERTICAL_SYMMETRY:
                            vertical = false;
                            break;
                        case ROTATIONAL_SYMMETRY:
                            rotational = false;
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
        if (terrestrial == 0 && enemyHQLoc != null) return;
        if (!upToDate()) return;
        sendMessage(HQ_TYPE, (loc.x << 6) | loc.y | (terrestrial << 12));
    }

    void sendMaxSoup(int soup, MapLocation loc){
        if (soup <= maxSoup) return;
        if (!upToDate()) return;
        sendMessage(SOUP_TYPE | (loc.x << 16) | (loc.y << 10), soup);
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

    void sendHorizontal(){
        if (!upToDate()) return;
        if (!horizontal) return;
        sendMessage(HORIZONTAL_SYMMETRY, 0);
    }

    void sendVertical(){
        if (!upToDate()) return;
        if (!vertical) return;
        sendMessage(VERTICAL_SYMMETRY, 0);
    }

    void sendRotational(){
        if (!upToDate()) return;
        if (!rotational) return;
        sendMessage(ROTATIONAL_SYMMETRY, 0);
    }

    MapLocation getEnemyHQLoc(){
        try {
            if (enemyHQLoc != null) return enemyHQLoc;
            if (!upToDate()) return null;
            MapLocation hor = getHorizontal();
            if (hor != null && rc.canSenseLocation(hor)) {
                RobotInfo r = rc.senseRobotAtLocation(hor);
                if (r == null || r.team == rc.getTeam() || r.type != RobotType.HQ) sendHorizontal();
            }
            MapLocation ver = getVertical();
            if (ver != null && rc.canSenseLocation(ver)) {
                RobotInfo r = rc.senseRobotAtLocation(ver);
                if (r == null || r.team == rc.getTeam() || r.type != RobotType.HQ) sendVertical();
            }
            MapLocation rot = getRotational();
            if (rot != null && rc.canSenseLocation(rot)) {
                RobotInfo r = rc.senseRobotAtLocation(rot);
                if (r == null || r.team == rc.getTeam() || r.type != RobotType.HQ) sendRotational();
            }
            if (!horizontal && !vertical) return rot;
            if (!vertical && !rotational) return hor;
            if (!rotational && !horizontal) return ver;
        } catch(Throwable t){
            t.printStackTrace();
        }
        return null;
    }

    MapLocation getHorizontal(){
        if (HQLoc == null) return null;
        if (!horizontal) return null;
        return new MapLocation(w - HQLoc.x - 1, HQLoc.y);
    }

    MapLocation getVertical(){
        if (HQLoc == null) return null;
        if (!vertical) return null;
        return new MapLocation( HQLoc.x, h - HQLoc.y - 1);
    }

    MapLocation getRotational(){
        if (HQLoc == null) return null;
        if (!rotational) return null;
        return new MapLocation(w - HQLoc.x - 1, h - HQLoc.y - 1);
    }

    MapLocation getBestMiningLoc(){
        if (latestMiningLoc == null) return null;
        if (latestMiningLocTurn < rc.getRoundNum() - 5) return null;
        return latestMiningLoc;
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
            if (transactions.length < GameConstants.NUMBER_OF_TRANSACTIONS_PER_BLOCK) return 1;
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



}
