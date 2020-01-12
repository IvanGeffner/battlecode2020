package trumpplayer;

import battlecode.common.RobotType;

public class BuildingManager {

    static final int MAX_ROUND_VAPORIZER = 500;

    static RobotType getNextBuilding(Comm comm){
        if (!comm.emergency && comm.rc.getRoundNum() <= MAX_ROUND_VAPORIZER){
            int extraSoup = (comm.maxSoup%300) + (comm.maxSoup/300)*230;
            if (extraSoup/RobotType.VAPORATOR.cost > comm.buildings[RobotType.VAPORATOR.ordinal()]) return RobotType.VAPORATOR;
        }
        if (comm.buildings[RobotType.DESIGN_SCHOOL.ordinal()] == 0) return RobotType.DESIGN_SCHOOL;
        if (comm.buildings[RobotType.FULFILLMENT_CENTER.ordinal()] == 0) return RobotType.FULFILLMENT_CENTER;
        return null;
    }


}
