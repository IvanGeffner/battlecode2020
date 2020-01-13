package old;

import battlecode.common.RobotType;

public class BuildingManager {

    static final int MAX_ROUND_VAPORIZER = 500;

    static final int ECO = 0;
    static final int RUSH = 1;
    static final int EMERGENCY = 2;

    static RobotType getNextBuilding(Comm comm){
        int gameState = getState(comm);
        switch(gameState) {
            case ECO:
                return getNextBuildingEco(comm);
            default:
                return getNextBuildingEco(comm);
        }
    }

    static RobotType getNextBuildingEco(Comm comm){
        if (comm.buildings[RobotType.FULFILLMENT_CENTER.ordinal()] == 0) {
            System.out.println(RobotType.FULFILLMENT_CENTER.name());
            return RobotType.FULFILLMENT_CENTER;
        }
        if (!comm.seenLandscaper && comm.rc.getRoundNum() <= MAX_ROUND_VAPORIZER) {
            if (nVaporators(comm.maxSoup) > comm.buildings[RobotType.VAPORATOR.ordinal()]) {
                if (Constants.DEBUG == 1) System.out.println(RobotType.VAPORATOR.name());
                return RobotType.VAPORATOR;
            }
        }
        if (comm.buildings[RobotType.DESIGN_SCHOOL.ordinal()] == 0) {
            System.out.println(RobotType.DESIGN_SCHOOL.name());
            return RobotType.DESIGN_SCHOOL;
        }
        return null;
    }

    static int getState(Comm comm){
        return ECO;
    }

    static int nVaporators(int soup){
        int extraSoup = (soup%300) + (soup/300)*230;
        return extraSoup/RobotType.VAPORATOR.cost;
    }

    static boolean shouldBuildDrone(Comm comm){
        if (!comm.upToDate()) return false;
        RobotType r = getNextBuilding(comm);
        if (r == null) return true;
        if (r != RobotType.VAPORATOR) return false;
        int vapor = comm.buildings[RobotType.VAPORATOR.ordinal()], drones = comm.buildings[RobotType.DELIVERY_DRONE.ordinal()];
        switch(vapor){
            case 0:
                return false;
            case 1:
                return false;
            case 2:
                return drones < 1;
            case 3:
                return drones < 2;
            default:
                return drones < vapor;
        }
    }
}
