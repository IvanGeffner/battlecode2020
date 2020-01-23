package rushbotplus;

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
        int fulfillment = comm.buildings[RobotType.FULFILLMENT_CENTER.ordinal()];
        int design = comm.buildings[RobotType.DESIGN_SCHOOL.ordinal()];
        int vaporators = comm.buildings[RobotType.VAPORATOR.ordinal()];


        if (10*fulfillment <= vaporators) {
            System.out.println(RobotType.FULFILLMENT_CENTER.name());
            return RobotType.FULFILLMENT_CENTER;
        }
        if (10*design <= vaporators && vaporators > 0) {
            System.out.println(RobotType.DESIGN_SCHOOL.name());
            return RobotType.DESIGN_SCHOOL;
        }
        int extraCash = 0;
        if (comm.shouldBuildVaporators()){
            System.out.println("Should build vaps!");
            extraCash += comm.soupGeneratedByVaporators/2;
        }
        if (nVaporators(comm.maxSoup, extraCash) > comm.buildings[RobotType.VAPORATOR.ordinal()]) {
            if (Constants.DEBUG == 1) System.out.println(RobotType.VAPORATOR.name());
            return RobotType.VAPORATOR;
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

    static int nVaporators(int soup, int extra){
        int extraSoup = (soup%300) + (soup/300)*230 + extra;
        return extraSoup/RobotType.VAPORATOR.cost;
    }

    static boolean shouldBuildDrone(Comm comm, RobotController rc){
        if (!comm.upToDate()) return false;
        RobotType r = getNextBuilding(comm);
        int price = 0;
        if (r != null) price += r.cost;
        System.out.println("checking drone building! " + price + " " + rc.getTeamSoup());
        if (price + RobotType.DELIVERY_DRONE.cost <= rc.getTeamSoup()){
            //if (rc.getRoundNum() < ROUND_LANDSCAPERS_ECO) return true;
            if (rc.getTeamSoup() > price +  RobotType.LANDSCAPER.cost + RobotType.DELIVERY_DRONE.cost) return true;
            int landscapers = getLandscapers(comm), drones = getDrones(comm);
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
        if (r != null) price += r.cost;
        if (price + RobotType.LANDSCAPER.cost <= rc.getTeamSoup()){
            if (rc.getTeamSoup() > price + RobotType.LANDSCAPER.cost + RobotType.DELIVERY_DRONE.cost) return true;
            int landscapers = getLandscapers(comm), drones = getDrones(comm);
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

    static boolean haveSoupToSpawn(RobotController rc, RobotType r){
        return rc.getTeamSoup() > r.cost;
    }

    static int getDrones(Comm comm){
        return comm.buildings[RobotType.DELIVERY_DRONE.ordinal()];
    }

    static int getLandscapers(Comm comm){
        return comm.buildings[RobotType.LANDSCAPER.ordinal()] + comm.unitsPostClutch[RobotType.LANDSCAPER.ordinal()];
    }

    static void printDebug(Comm comm){
        if (Constants.DEBUG != 1) return;
        RobotType[] types = RobotType.values();
        for (RobotType t : types){
            System.out.println(t.name() + " " + comm.buildings[t.ordinal()]);
        }
    }

}
