package trumpplayer;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class BugPath {

    RobotController rc;

    BugPath(RobotController rc){
        this.rc = rc;
    }

    void moveTo(MapLocation location){
        try {
            if (location == null || !rc.isReady()) return;
            MapLocation myLoc = rc.getLocation();
            if (myLoc.distanceSquaredTo(location) == 0) return;
            Direction dir = rc.getLocation().directionTo(location);
            for (int i = 0; i < 8; ++i) {
                if (rc.canMove(dir)) {
                    MapLocation newLoc = myLoc.add(dir);
                    if (rc.canSenseLocation(newLoc) && !rc.senseFlooding(newLoc)) {
                        rc.move(dir);
                        return;
                    }
                }
                dir = dir.rotateLeft();
            }
        } catch(Throwable t){
            t.printStackTrace();
        }
    }

}
