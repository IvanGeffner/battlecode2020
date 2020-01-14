package rush;

import battlecode.common.RobotController;

public class Gun extends MyRobot {

    RobotController rc;
    NetGunManager netGunManager;

    Gun(RobotController rc){
        this.rc = rc;
    }

    void play(){
        netGunManager.tryShoot();
    }

}
