package crunch;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class DangerDrone {

    RobotController rc;
    int[][] dangerMap;
    Direction[] dirPath =  new Direction[]{Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.CENTER};
    int contDanger = -1, contRemove = -1;
    MapLocation locDanger, locRemove;


    DangerDrone(RobotController rc){
        this.rc = rc;
        dangerMap = new int[rc.getMapWidth()][rc.getMapHeight()];
    }

    void addDanger(MapLocation loc){
        if (Constants.DEBUG == 1) System.out.println("Starting danger at " + Clock.getBytecodeNum());
        /*int x = loc.x, y = loc.y;
        int i = X.length;
        while (--i >= 0){
            int newX = x + X[i], newY = y + Y[i];
            if (newX < 0 || newX >= w) continue;
            if (newY < 0 || newY >= h) continue;
            ++dangerMap[newX][newY];
            if (Constants.DEBUG == 1) rc.setIndicatorDot(new MapLocation(newX, newY), 0, 0, 255);
        }*/

        contDanger = dirPath.length;
        locDanger = new MapLocation(loc.x, loc.y);
        while (--contDanger >= 0) {
            locDanger = locDanger.add(dirPath[contDanger]);
            if (rc.onTheMap(locDanger)){
                ++dangerMap[locDanger.x][locDanger.y];
                //if (Constants.DEBUG == 1) rc.setIndicatorDot(newLoc, 0, 0, 255);
            }
            if (Clock.getBytecodesLeft() <= Constants.SAFETY_BYTECODE_MESSAGES) return;
        }

        if (Constants.DEBUG == 1) System.out.println("Ending danger at " + Clock.getBytecodeNum());

    }

    void removeDanger(MapLocation loc){
        contRemove = dirPath.length;
        locRemove = new MapLocation(loc.x, loc.y);
        while (--contRemove >= 0) {
            locRemove = locRemove.add(dirPath[contRemove]);
            if (rc.onTheMap(locRemove)){
                --dangerMap[locRemove.x][locRemove.y];
                //if (Constants.DEBUG == 1) rc.setIndicatorDot(newLoc, 0, 0, 255);
            }
            if (Clock.getBytecodesLeft() <= Constants.SAFETY_BYTECODE_MESSAGES) return;
        }
    }

    void complete(){
        while (--contDanger >= 0) {
            locDanger = locDanger.add(dirPath[contDanger]);
            if (rc.onTheMap(locDanger)){
                ++dangerMap[locDanger.x][locDanger.y];
                //if (Constants.DEBUG == 1) rc.setIndicatorDot(newLoc, 0, 0, 255);
            }
            if (Clock.getBytecodesLeft() <= Constants.SAFETY_BYTECODE_MESSAGES) return;
        }
        while (--contRemove >= 0) {
            locRemove = locRemove.add(dirPath[contRemove]);
            if (rc.onTheMap(locRemove)){
                --dangerMap[locRemove.x][locRemove.y];
                //if (Constants.DEBUG == 1) rc.setIndicatorDot(newLoc, 0, 0, 255);
            }
            if (Clock.getBytecodesLeft() <= Constants.SAFETY_BYTECODE_MESSAGES) return;
        }
    }


}
