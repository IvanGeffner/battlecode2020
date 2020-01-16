package ecoplayer;

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

    int[] buildings;
    int[] wallMes = null;

    int MASK = 4534653;
    boolean seenLandscaper = false;
    boolean seenUnit = false;

    final int BYTECODE_LEFT = 300;

    Comm(RobotController rc){
        this.rc = rc;
        MASK += rc.getTeam().ordinal();
        buildings = new int[RobotType.values().length];
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
                            EnemyHQLoc = new MapLocation((code >>> 6), (code&0x0000003F));
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

    void sendHQLoc(MapLocation loc){
        if (EnemyHQLoc != null) return;
        if (!upToDate()) return;
        sendMessage(HQ_TYPE, (loc.x << 6) | loc.y);
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
