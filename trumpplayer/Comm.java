package trumpplayer;

import battlecode.common.*;

public class Comm {

    RobotController rc;

    MapLocation EnemyHQLoc = null;
    int maxSoup = 0;
    int turn = 1;

    final int FIRST_ROUND = 1;

    final int HQ_TYPE = 1;
    final int SOUP_TYPE = 2;
    int MASK = 4534653;

    final int BYTECODE_LEFT = 300;

    Comm(RobotController rc){
        this.rc = rc;
        MASK += rc.getTeam().ordinal();
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
                    switch(t.getMessage()[0]){
                        case HQ_TYPE:
                            int code = t.getMessage()[1];
                            EnemyHQLoc = new MapLocation((code >>> 6), (code&0x0000003F));
                            break;
                        case SOUP_TYPE:
                            int soup = t.getMessage()[1];
                            if (soup > maxSoup) maxSoup = soup;
                            break;
                    }
                }
                turn++;
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
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

    void sendMessage(int type, int code){
        try {
            int b = getBidValue();
            int[] message = new int[]{type, code, 0, 0, 0, 0, rc.getRoundNum() ^ MASK};
            if (rc.canSubmitTransaction(message, b)) rc.submitTransaction(message, b);
        } catch (Throwable t){
            t.printStackTrace();
        }
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



}
