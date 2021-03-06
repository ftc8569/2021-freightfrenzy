package org.firstinspires.ftc.teamcode.Controllers;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

/**
 * A simple P based controller to try to combat axial drift due to wheel/motor/weight inconsistencies.
 * Currently still a prototype and probably has the opportunity to break. In the case of unwanted
 * movement, tapping your rotation button should reset the heading error and things should work
 * again. The idea of the controller is that while you are not pressing any buttons for rotation,
 * your robot should not be rotating, so we can correct any drift with a feedback controller.
 */
public class TeleopHeadingDriftController {
    final int TURNSTOPTIMEOUT = 500;
    Pose2d currentPose;
    Pose2d targetPose;
    double poseTolerance;
    double outputDeadzone;
    boolean lastTurning = true;
    PIDFCoefficients pidfCoefficients;
    boolean enabled = true;
    long stoppedTurning = 0;
    boolean timerstarted = false;
    boolean firstStart = true;
    double output;
    MiniPID pid;


    public TeleopHeadingDriftController(double poseTolerance, double outputDeadzone, PIDFCoefficients pid, int bufferSize) {
        this.poseTolerance = poseTolerance;
        this.pidfCoefficients = pid;
        this.pid = new MiniPID(pid, bufferSize);
        this.outputDeadzone = outputDeadzone;
    }


    public Pose2d control(Pose2d currentPose, Pose2d drivePower) {
        this.currentPose = currentPose;
        double turn = drivePower.getHeading();
        boolean turning = Math.abs(turn) > .05;
        if(firstStart) {
            targetPose = currentPose;
            firstStart = false;
        }
        if(enabled) {
            if (turning) {
                lastTurning = true;
                timerstarted = false;
                return drivePower;
            } else {
                if (lastTurning) {
                    if(timerstarted) {
                        if(System.currentTimeMillis() - stoppedTurning >= TURNSTOPTIMEOUT) {
                            targetPose = currentPose;
                            pid.setSetpoint(targetPose.getHeading());
                            lastTurning = false;
                            timerstarted = false;
                            return drivePower;
                        } else return drivePower;
                    } else {
                        stoppedTurning = System.currentTimeMillis();
                        timerstarted = true;
                        return drivePower;
                    }
                } else {
                    output = pid.getOutputAngles(Math.toDegrees(currentPose.getHeading()), Math.toDegrees(targetPose.getHeading()));
                    if(Math.abs(output) >= outputDeadzone) {
                        if (Math.abs(pid.getError()) >= poseTolerance)
                            return drivePower.minus(new Pose2d(0, 0, output));
                            // possibly * -1 because now that I think about it I think this is actually x and y
                            // swapped so instead of doing rotation-controller.control() in setting your
                            // power it will be +
                        else {
                            pid.reset();
                            return drivePower;
                        }
                    } else return drivePower;
                }
            }
        } else return drivePower;
    }

    public double getError() {
        return pid.getError();
    }

    public double getSetpoint() {
       return pid.getSetpoint();
    }

    public void setTargetPose(double targetPose) {
        this.targetPose = new Pose2d(0, 0, targetPose);
        pid.setSetpoint(this.targetPose.getHeading());
    }

    public double getTargetPose() {
        return Math.toDegrees(targetPose.getHeading());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void reset(Pose2d currentPose) {
        targetPose = currentPose;
        this.pid.reset();
        lastTurning = true;
        timerstarted = false;
    }

    public void setPIDF(PIDFCoefficients pidf) {
        this.pidfCoefficients = pidf;
        this.pid.setPID(pidf.p, pidf.i, pidf.d, pidf.f);
    }

    public void setOutputScale(double scale) {
        pid.setOutputLimits(scale);
    }

    public PIDFCoefficients getPidfCoefficients() {
        return this.pidfCoefficients;
    }

    public void setOutputFilter(double filter) {
        pid.setOutputFilter(filter);
    }

    public double getLastOutput() {
        return output;
    }

    public void setRamping(double val) {
        pid.setOutputRampRate(val);
    }

    public double[] getBufferAvgs() {
        return pid.getBufferAvgs();
    }
}


