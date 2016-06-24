package org.jointheleague.erik.cleverrobot;

import android.os.SystemClock;
import android.util.Log;

import org.jointheleague.erik.cleverrobot.sensors.UltraSonicSensors;
import org.jointheleague.erik.irobot.IRobotAdapter;
import org.jointheleague.erik.irobot.IRobotInterface;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

public class Pilot extends IRobotAdapter {

    private static final String TAG = "Pilot";
    // The following measurements are taken from the interface specification
    private static final double WHEEL_DISTANCE = 235.0; //in mm
    private static final double WHEEL_DIAMETER = 72.0; //in mm
    private static final double ENCODER_COUNTS_PER_REVOLUTION = 508.8;

    private final Dashboard dashboard;
    public UltraSonicSensors sonar;
    int angle;
    private int startLeft;
    private int startRight;
    private int countsToGoWheelLeft;
    private int countsToGoWheelRight;
    private int directionLeft;
    private int directionRight;
    private static final int STRAIGHT_SPEED = 200;
    private static final int TURN_SPEED = 100;

    private int currentCommand = 0;
    private final boolean debug = true; // Set to true to get debug messages.

    public Pilot(IRobotInterface iRobot, Dashboard dashboard, IOIO ioio)
            throws ConnectionLostException {
        super(iRobot);
        sonar = new UltraSonicSensors(ioio);
        this.dashboard = dashboard;
        dashboard.log(dashboard.getString(R.string.hello));
    }

    /**
     * This method is executed when the robot first starts up.
     **/
    public void initialize() throws ConnectionLostException {
    }

    /**
     * This method is called repeatedly.
     **/
    //- + left & + - right
    /*
        160 reserved
        161 force field
        164 green
        165 green + force field
        168 red
        169 red + force field
        172 red + green
        173 red + green + force field
        0   idrk


         Home Base: 
        240 Reserved 
        248 Red Buoy
         244 Green Buoy
         242 Force Field
         252 Red Buoy and Green Buoy 
        250 Red Buoy and Force Field
         246 Green Buoy and Force Field 
        254 Red Buoy, Green Buoy and Force Field 
        255 No value received
         */

    //x=1 Senses nothing
    //x=2 Senses Green first then Red
    //x=3 Senses Red first then Green
    //x=4 Senses Green first Red second,then turns left until both buoys are sensed
    //x=5 Senses Red first Green second,then turns right until both buoys are sensed



    /*
    consider current state and infrared signal gotten
    read sensors
    check states

    sees nothing: curves

    green first:
    "sees green" goes forward
    "sees red" turns left
    "sees red-green" goes forward and stays inside the red-green zone

    or

    red first:
    "sees red" goes forward
    "sees green" turns right
    "sees red-green" goes forward and stays inside the red-green zone

    change state at the end of the loop
    check bump inside of each state
    */
    int x = 1;

    public void loop() throws ConnectionLostException {
        readSensors(SENSORS_GROUP_ID6);
        dashboard.log("" + getInfraredByte());
        if (Math.abs(getCurrent()) > 1000) {
            dashboard.log("Stuck");
            driveDirect(-500, -500);
            SystemClock.sleep(100);
            driveDirect(-500, 500);
            SystemClock.sleep(376);
        }
        if (isBumpLeft() && isBumpRight()&& x==1) { // What if x != 1?
            if (getWallSignal() > 50) {
                driveDirect(-500, 500);
            } else {
                driveDirect(500, -500);
            }
        } else if (isBumpRight()) {
            driveDirect(-500, -500);
            SystemClock.sleep(100);
            driveDirect(-500, 500);
            SystemClock.sleep(376);
        } else if (isBumpLeft()) {
            driveDirect(-500, -500);
            SystemClock.sleep(100);
            driveDirect(500, -500);
            SystemClock.sleep(376);
        }
        // red buoy and green buoy / red buoy green buoy and force field / sees green first
        if (getInfraredByte() == 252 || getInfraredByte() == 254 && x == 2) {
            x = 4;
            // read sensors
            //while (not seeing just green) {//rotate left and read sensors}
            //
        }
        // red buoy and green buoy / red buoy green buoy and force field / sees red first
        else if (getInfraredByte() == 252 || getInfraredByte() == 254 && x == 3) {
            x = 5;
        }
        // green buoy or green buoy and force field
        else if (getInfraredByte() == 244 || getInfraredByte() == 246) {
            x = 2;
        }
        // red buoy or red buoy and force field first
        else if (getInfraredByte() == 248 || getInfraredByte() == 250) {
            x = 3;
        }
            // what happens after everything finishes and it has to go back to roaming mode

    /*
         Home Base: 
        240 Reserved 
        248 Red Buoy
         244 Green Buoy
         242 Force Field
         252 Red Buoy and Green Buoy 
        250 Red Buoy and Force Field
         246 Green Buoy and Force Field 
        254 Red Buoy, Green Buoy and Force Field 
        255 No value received
                */
        if (x == 1) {
            driveDirect(500, 350);
        } else if (x == 2) {
            driveDirect(500, 500);
            if (getInfraredByte() == 248) {
                driveDirect(-500, 500);
            }
        } else if (x == 3) {
            driveDirect(500, 500);
            if (getInfraredByte() == 248) {
                driveDirect(500, -500);
            }
        } else if (x == 4) {
            if (getInfraredByte() == 252) {
                driveDirect(250, 500);
                SystemClock.sleep(1000);
                driveDirect(500, 250);
            }
        } else if (x == 5) {
            if (getInfraredByte() == 252) {
                driveDirect(250, 500);
                SystemClock.sleep(1000);
                driveDirect(500, 250);
            }
        }



    }

    /**
     * This method determines where to go next. This is a very simple Tortoise-like
     * implementation, but a more advanced implementation could take into account
     * sensory input, maze mapping, and other.
     *
     * @throws ConnectionLostException
     */
    private void nextCommand() throws ConnectionLostException {
//        try {
//            sonar.read();
//            int front = sonar.getDistanceFront();
//            if ( front < 50 ) {
//                currentCommand = 4; // shutdown if distance to object in front is less than 5 cm
//            }
//        } catch (InterruptedException e) {
//            dashboard.log(e.getMessage());
//        }
        dashboard.log("currentCommand = " + currentCommand);
        switch (currentCommand) {
            case 0:
                goStraight(1000);
                break;
            case 1:
                turnLeft(180);
                break;
            case 2:
                goStraight(1000);
                break;
            case 3:
                turnRight(180);
                break;
            case 4:
                shutDown();
                break;
            default:
        }
        currentCommand++;
    }

    private void shutDown() throws ConnectionLostException {
        dashboard.log("Shutting down... Bye!");
        stop();
        closeConnection();
    }

    /**
     * This method determines where to go next. This is a very simple Tortoise-like
     * implementation, but a more advanced implementation could take into account
     * sensory input, maze mapping, and other.
     *
     * @throws ConnectionLostException
     */
    private void nextCommandBis() throws ConnectionLostException {
        if (currentCommand < 8) {
            if (currentCommand % 2 == 0) {
                goStraight(1000);
            } else {
                turnRight(90);
            }
            currentCommand++;
        } else if (currentCommand == 8) {
            shutDown();
        }
    }

    /**
     * Moves the robot in a straight line. Note: Unexpected behavior may occur if distance
     * is larger than 14567mm.
     *
     * @param distance the distance to go in mm. Must be &le; 14567.
     */
    private void goStraight(int distance) throws ConnectionLostException {
        countsToGoWheelLeft = (int) (distance * ENCODER_COUNTS_PER_REVOLUTION
                / (Math.PI * WHEEL_DIAMETER));
        countsToGoWheelRight = countsToGoWheelLeft;
        if (debug) {
            String msg = String.format("Going straight  L: %d  R: %d",
                    countsToGoWheelLeft, countsToGoWheelRight);
            Log.d(TAG, msg);
            dashboard.log(msg);
        }
        directionLeft = 1;
        directionRight = 1;
        recordEncodersAndDrive(directionLeft * STRAIGHT_SPEED, directionRight * STRAIGHT_SPEED);
    }


    /**
     * Turns in place rightwards. Note: Unexpected behavior may occur if degrees is
     * larger than 7103 degrees (a little less than 20 revolutions).
     *
     * @param degrees the number of degrees to turn. Must be &le; 7103.
     */
    private void turnRight(int degrees) throws ConnectionLostException {
        countsToGoWheelRight = (int) (degrees * WHEEL_DISTANCE * ENCODER_COUNTS_PER_REVOLUTION
                / (360.0 * WHEEL_DIAMETER));
        countsToGoWheelLeft = countsToGoWheelRight;
        directionLeft = 1;
        directionRight = -1;
        recordEncodersAndDrive(directionLeft * TURN_SPEED, directionRight * TURN_SPEED);
        if (debug) {
            String msg = String.format("Turning right  L: %d  R: %d",
                    countsToGoWheelLeft, countsToGoWheelRight);
            Log.d(TAG, msg);
            dashboard.log(msg);
        }
    }

    /**
     * Turns in place leftwards. Note: Unexpected behavior may occur if degrees is
     * larger than 7103 degrees (a little less than 20 revolutions).
     *
     * @param degrees the number of degrees to turn. Must be &le; 7103.
     */
    private void turnLeft(int degrees) throws ConnectionLostException {
        countsToGoWheelRight = (int) (degrees * WHEEL_DISTANCE * ENCODER_COUNTS_PER_REVOLUTION
                / (360.0 * WHEEL_DIAMETER));
        countsToGoWheelLeft = countsToGoWheelRight;
        if (debug) {
            String msg = String.format("Turning left  L: %d  R: %d",
                    countsToGoWheelLeft, countsToGoWheelRight);
            Log.d(TAG, msg);
            dashboard.log(msg);
        }
        directionLeft = -1;
        directionRight = 1;
        recordEncodersAndDrive(directionLeft * TURN_SPEED, directionRight * TURN_SPEED);
    }

    private void recordEncodersAndDrive(int leftVelocity, int rightVelocity) throws ConnectionLostException {
        readSensors(SENSORS_GROUP_ID101);
        startLeft = getEncoderCountLeft();
        startRight = getEncoderCountRight();
        driveDirect(leftVelocity, rightVelocity);
    }


    /**
     * Checks if the last command has been completed.
     *
     * @return true if the last command has been completed
     * @throws ConnectionLostException
     */
    private boolean checkDone() throws ConnectionLostException {
        readSensors(SENSORS_GROUP_ID101);
        int countLeft = getEncoderCountLeft();
        int countRight = getEncoderCountRight();
        boolean done = false;
        int doneLeft = (directionLeft * (countLeft - startLeft)) & 0xFFFF;
        int doneRight = (directionRight * (countRight - startRight)) & 0xFFFF;
        if (debug) {
            String msg = String.format("L: %d  R: %d  azimuth: %.2f",
                    doneLeft, doneRight, dashboard.getAzimuth());
            dashboard.log(msg);
            Log.d(TAG, msg);
        }
        if (countsToGoWheelLeft <= doneLeft && doneLeft < 0x7FFF ||
                countsToGoWheelRight <= doneRight && doneRight < 0x7FFF) {
            driveDirect(0, 0);
            waitForCompleteStop();
            done = true;
        }
        return done;
    }

    private void waitForCompleteStop() throws ConnectionLostException {
        boolean done = false;
        int prevCountLeft = -1;
        int prevCountRight = -1;
        while (!done) {
            readSensors(SENSORS_GROUP_ID101);
            int countLeft = getEncoderCountLeft();
            int countRight = getEncoderCountRight();
            if (debug) {
                String msg = String.format("Stopping  L: %d  R: %d", countLeft, countRight);
                Log.d(TAG, msg);
                dashboard.log(msg);
            }
            if (prevCountLeft == countLeft && prevCountRight == countRight) {
                done = true;
            } else {
                prevCountLeft = countLeft;
                prevCountRight = countRight;
            }
        }
    }


}
