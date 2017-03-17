package uk.ac.eeci.strategy;

import uk.ac.eeci.HeatingControlStrategy;
import uk.ac.eeci.strategy.TimeScheduleControlStrategy.TimeSlot;
import static uk.ac.eeci.strategy.TimeScheduleControlStrategy.DayType.*;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * HeatingControlStrategyFactory builds HeatingControlStrategies.<br><br>
 *
 * It has all necessary parameters to build all possible control strategies.
 */
public class HeatingControlStrategyFactory {

    /**
     * A type of heating control strategy.
     */
    public enum ControlStrategyType {
        /**
         * No heating whatsoever.
         */
        OFF,
        /**
         * The same heating set point 24/7. Uses the "set point while at home".
         */
        FLAT,
        /**
         * Time triggered heating control strategy with the following schedule:
         * <br><br>
         * Weekday: <br>
         *     from bed time (day before) to "wake up time": "set point while asleep"<br>
         *     from "wake up time" till "leave home time": "set point while at home",<br>
         *     from "leave home time" till "come home time": heating system switched off<br>
         *     from "come home time" till "bed time": "set point while at home<br>
         *<br><br>
         * Weekend:<br>
         *     The same heating set point for 48h. Uses the "set point while at home".<br>
         */
        TIME_TRIGGERED,
        /**
         * Heating set points are triggered by occupants of the dwelling. <br><br>
         *
         * if no one is home -> heating system is switched off<br>
         * if at least one person is home and not asleep -> "set point while at home"<br>
         * if at least one person is home and all are asleep -> "set point while asleep"<br>
         */
        PRESENCE_TRIGGERED
    }

    private final double setPointWhileHome;
    private final double setPointWhileAsleep;
    private final LocalTime wakeUpTime;
    private final LocalTime leaveHomeTime;
    private final LocalTime comeHomeTime;
    private final LocalTime bedTime;
    private final ZoneId timeZone;

    /**
     *
     * @param setPointWhileHome heating set point while occupants are (assumed to be) home
     * @param setPointWhileAsleep heating set point while occupants are (assumed to be) asleep
     * @param wakeUpTime local time at which occupants wake up typically
     * @param leaveHomeTime local time in the morning at which occupants typically leave their dwelling (to work)
     * @param comeHomeTime local time in the evening at which occupants typically come back up
     * @param bedTime local time at which occupants typically go to bed
     * @param timeZone time zone on which occupants live
     */
    public HeatingControlStrategyFactory(double setPointWhileHome, double setPointWhileAsleep,
                                          LocalTime wakeUpTime, LocalTime leaveHomeTime,
                                          LocalTime comeHomeTime, LocalTime bedTime,
                                          ZoneId timeZone) {
        this.setPointWhileHome = setPointWhileHome;
        this.setPointWhileAsleep = setPointWhileAsleep;
        this.wakeUpTime = wakeUpTime;
        this.leaveHomeTime = leaveHomeTime;
        this.comeHomeTime = comeHomeTime;
        this.bedTime = bedTime;
        this.timeZone = timeZone;
    }

    /**
     * Builds a heating control strategy.
     *
     * @param type the type of strategy to be build
     * @return a new heating control strategy2
     */
    public HeatingControlStrategy build(ControlStrategyType type){
        switch (type) {
            case OFF:
                return this.noHeatingStrategy();
            case FLAT:
                return this.flatStrategy();
            case TIME_TRIGGERED:
                return this.timeTriggeredStrategy();
            case PRESENCE_TRIGGERED:
                return this.presenceTriggeredStrategy();
            default:
                String msg = String.format("Unknown control strategy type %s cannot be build.", type);
                throw new IllegalStateException(msg);
        }
    }

    private HeatingControlStrategy noHeatingStrategy() {
        return new NoHeatingStrategy();
    }

    private HeatingControlStrategy flatStrategy() {
        return new ClimateChangingControlStrategy(this.setPointWhileHome);
    }

    private HeatingControlStrategy timeTriggeredStrategy() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        timeSlots.add(new TimeSlot(
                WEEKDAY,
                LocalTime.MIDNIGHT,
                this.wakeUpTime,
                this.setPointWhileAsleep
        ));
        timeSlots.add(new TimeSlot(
                WEEKDAY,
                this.wakeUpTime,
                this.leaveHomeTime,
                this.setPointWhileHome
        ));
        timeSlots.add(new TimeSlot(
                WEEKDAY,
                this.comeHomeTime,
                this.bedTime,
                this.setPointWhileHome
        ));
        timeSlots.add(new TimeSlot(
                WEEKDAY,
                this.bedTime,
                LocalTime.MIDNIGHT,
                this.setPointWhileAsleep
        ));
        timeSlots.add(new TimeSlot(
                WEEKEND,
                LocalTime.MIDNIGHT,
                LocalTime.MIDNIGHT,
                this.setPointWhileHome
        ));
        return new TimeScheduleControlStrategy(timeSlots, this.timeZone);
    }

    private HeatingControlStrategy presenceTriggeredStrategy() {
        return new PresenceBasedStrategy(this.setPointWhileHome, this.setPointWhileAsleep);
    }
}
