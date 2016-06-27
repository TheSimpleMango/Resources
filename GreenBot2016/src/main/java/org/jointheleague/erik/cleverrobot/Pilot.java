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


    int x = 0;
    public void initialize() throws ConnectionLostException {
    }/*
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
    boolean y = true;
    public void loop() throws ConnectionLostException {
        readSensors(SENSORS_GROUP_ID6);
        dashboard.log("" + getInfraredByte());
        // nothing so far
        if (x == 0){
            if (y){
                driveDirect(400, 300);
            } else if (!y){
                driveDirect(300, 400);
            }
            if (isBumpLeft() && isBumpRight()) {
                if (getWallSignal() > 40) {
                    driveDirect(-500, 500);
                    SystemClock.sleep(100);
                } else {
                    driveDirect(500, -500);
                    SystemClock.sleep(100);
                }
            } else if (isBumpRight()) {
                driveDirect(-500, -500);
                SystemClock.sleep(100);
                driveDirect(-500, 500);
                SystemClock.sleep(376);
                y = !y;
            } else if (isBumpLeft()) {
                driveDirect(-500, -500);
                SystemClock.sleep(100);
                driveDirect(500, -500);
                SystemClock.sleep(376);
                y = !y;
            }
        }
        // green first
        if (getInfraredByte() == 244 || getInfraredByte() == 246 && x == 0){
            dashboard.log("green first: " + getInfraredByte());
            x = 1;
            dashboard.log("x changed to " + x);
            while (getInfraredByte() != 248 && getInfraredByte() != 250) {
                readSensors(SENSORS_GROUP_ID6);
                dashboard.log("goes forward until red: " + getInfraredByte());
                driveDirect(50, 50);
                if (Math.abs(getCurrent()) > 1200 || isBumpLeft() || isBumpRight()) {
                    SystemClock.sleep(500);
                }
            }
            while (getInfraredByte() != 252 && getInfraredByte() != 254){
                readSensors(SENSORS_GROUP_ID6);
                dashboard.log("turns until middle: " + getInfraredByte());
                driveDirect(-50, 50);
                if (Math.abs(getCurrent()) > 1200 || isBumpLeft() || isBumpRight()) {
                    SystemClock.sleep(500);
                }
            }
            x = 3;
            dashboard.log("x changed to " + x);
        }
        // red first
        if (getInfraredByte() == 248 || getInfraredByte() == 250 && x == 0){
            dashboard.log("red first");
            x = 2;
            dashboard.log("x changed to " + x);
            while (getInfraredByte() != 244 && getInfraredByte() != 246){
                readSensors(SENSORS_GROUP_ID6);
                dashboard.log("goes forward until green: " + getInfraredByte());
                driveDirect(50, 50);
                if (Math.abs(getCurrent()) > 1200 || isBumpLeft() || isBumpRight()) {
                    SystemClock.sleep(500);
                }
            }
            while (getInfraredByte() != 252 && getInfraredByte() != 254){
                readSensors(SENSORS_GROUP_ID6);
                dashboard.log("turns until middle: " + getInfraredByte());
                driveDirect(50, -50);
                if (Math.abs(getCurrent()) > 1200 || isBumpLeft() || isBumpRight()) {
                    SystemClock.sleep(500);
                }
            }
            x = 3;
            dashboard.log("x changed to " + x);
        }
        if (x == 3){
            dashboard.log("starts 3");
            driveDirect(100,100);
            while (getInfraredByte() == 244 || getInfraredByte() == 246){
                dashboard.log("curving right");
                readSensors(SENSORS_GROUP_ID6);
                driveDirect(100,50);
            }
            while (getInfraredByte() == 248 || getInfraredByte() == 250){
                dashboard.log("curving left");
                readSensors(SENSORS_GROUP_ID6);
                driveDirect(50,100);
            }
            if (Math.abs(getCurrent()) > 1200 || isBumpLeft() || isBumpRight()) {
                SystemClock.sleep(500);
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
