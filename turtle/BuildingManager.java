package turtle;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class BuildingManager {

    static final int MAX_ROUND_VAPORIZER = 500;

    static final int ROUND_LANDSCAPERS_ECO = 350;

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

    static boolean shouldBuildDrone(Comm comm, RobotController rc){
        if (!comm.upToDate()) return false;
        RobotType r = getNextBuilding(comm);
        if (r == null || r.cost + RobotType.DELIVERY_DRONE.cost < rc.getTeamSoup()){
            //if (rc.getRoundNum() < ROUND_LANDSCAPERS_ECO) return true;
            if (rc.getTeamSoup() > RobotType.LANDSCAPER.cost + RobotType.DELIVERY_DRONE.cost) return true;
            int landscapers = comm.buildings[RobotType.LANDSCAPER.ordinal()], drones = comm.buildings[RobotType.DELIVERY_DRONE.ordinal()];
            return drones <= landscapers;
        }
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

    static boolean shouldBuildLandscaper(Comm comm, RobotController rc){
        if (!comm.upToDate()) return false;
        //if (rc.getRoundNum() < ROUND_LANDSCAPERS_ECO) return false;
        RobotType r = getNextBuilding(comm);
        int price = 0;
        if (r == null || r.cost + RobotType.LANDSCAPER.cost < rc.getTeamSoup()){
            if (rc.getTeamSoup() > RobotType.LANDSCAPER.cost + RobotType.DELIVERY_DRONE.cost) return true;
            int landscapers = comm.buildings[RobotType.LANDSCAPER.ordinal()], drones = comm.buildings[RobotType.DELIVERY_DRONE.ordinal()];
            return drones > landscapers;
        }
        if (r != RobotType.VAPORATOR) return false;
        int vapor = comm.buildings[RobotType.VAPORATOR.ordinal()], landscapers = comm.buildings[RobotType.LANDSCAPER.ordinal()];
        switch(vapor){
            case 0:
                return false;
            case 1:
                return false;
            case 2:
                return landscapers < 1;
            case 3:
                return landscapers < 2;
            default:
                return landscapers < vapor;
        }
    }
}
