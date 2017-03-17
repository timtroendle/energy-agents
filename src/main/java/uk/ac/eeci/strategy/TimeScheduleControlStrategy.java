package uk.ac.eeci.strategy;

import uk.ac.eeci.HeatingControlStrategy;
import uk.ac.eeci.PersonReference;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * A heating control strategy of piecewise constant heating set points based purely on current time.
 *
 * The strategy has either exactly one, or none heating set point which is valid at a single point in time,
 * resulting in a piecewise constant function of time. Durations during which heating set points are constant
 * can be chosen freely which the following restrictions:
 * <ul>
 *     <li>weekdays share the same schedule</li>
 *     <li>weekends share the same schedule</li>
 *     <li>no other form of days possible (bank holidays, vacation, etc.)</li>
 * </ul>
 */
public class TimeScheduleControlStrategy extends HeatingControlStrategy {

    public enum DayType {
        /** Monday, Tuesday, Wednesday, Thursday, or Friday */
        WEEKDAY,
        /** Saturday, or Sunday. */
        WEEKEND;

        private static DayType fromTimeZone(ZonedDateTime timeStamp) {
            java.time.DayOfWeek dayOfWeek = timeStamp.getDayOfWeek();
            switch (dayOfWeek) {
                case MONDAY:
                case TUESDAY:
                case WEDNESDAY:
                case THURSDAY:
                case FRIDAY:
                    return DayType.WEEKDAY;
                case SATURDAY:
                case SUNDAY:
                    return DayType.WEEKEND;
                default:
                    throw new IllegalStateException(String.format("Received unknown day of week: %s.", dayOfWeek));
            }
        }
    }

    /**
     * A period of time during which a single heating set point is valid.
     */
    public static class TimeSlot {

        private final DayType dayType;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final double heatingSetPoint;

        /**
         *
         * @param dayType the type of the day of this period, e.g. weekday
         * @param startTime the start time of the period as local time of the day
         * @param endTime the end time of the period as local time of the day
         * @param heatingSetPoint the heating set point valid during the period
         */
        public TimeSlot(DayType dayType, LocalTime startTime, LocalTime endTime, double heatingSetPoint) {
            this.dayType = dayType;
            this.startTime = startTime;
            if (endTime != LocalTime.MIDNIGHT)
                this.endTime = endTime;
            else
                this.endTime = endTime.minus(Duration.ofMillis(1)); // so that this time is considered same day
            this.heatingSetPoint = heatingSetPoint;
            if (this.endTime.isBefore(this.startTime)) {
                String msg = String.format("Start time must be before end time, but they were: start %s, end %s.",
                        startTime, endTime);
                throw new IllegalArgumentException(msg);
            }
        }

        private boolean overlapsWith(TimeSlot other) {
            return this.startsWithinOtherTimeSlot(other) || other.startsWithinOtherTimeSlot(this);
        }

        private boolean startsWithinOtherTimeSlot(TimeSlot other) {
            return this.dayType == other.dayType && this.startTime.isAfter(other.startTime)
                    && this.startTime.isBefore(other.endTime);
        }
    }

    private final List<TimeSlot> timeSlots;
    private final ZoneId zoneId;

    /**
     *
     * @param timeSlots all valid time slots of the strategy; must not overlap but can be incomplete
     * @param zoneId the timezone for which local times in the time slots are valid
     */
    public TimeScheduleControlStrategy(List<TimeSlot> timeSlots, ZoneId zoneId) {
        if (TimeScheduleControlStrategy.atLeastTwoTimeSlotsOverlap(timeSlots)) {
            String msg = "Passed time slots are overlapping which must not be the case.";
            throw new IllegalArgumentException(msg);
        }
        this.timeSlots = timeSlots;
        this.zoneId = zoneId;
    }

    @Override
    public CompletableFuture<Optional<Double>> heatingSetPoint(ZonedDateTime timeStamp,
                                                               Set<PersonReference> peopleInDwelling) {
        Optional<TimeSlot> currentTimeSlot = this.chooseCurrentTimeSlot(timeStamp);
        if (currentTimeSlot.isPresent()) {
            return CompletableFuture.completedFuture(Optional.of(currentTimeSlot.get().heatingSetPoint));
        }
        else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    private Optional<TimeSlot> chooseCurrentTimeSlot(ZonedDateTime currentTime) {
        DayType dayType = DayType.fromTimeZone(currentTime);
        LocalTime currentLocalTime = currentTime.withZoneSameInstant(this.zoneId).toLocalTime();
        List<TimeSlot> matchingTimeSlots = this.timeSlots.stream()
                .filter(timeSlot -> timeSlot.dayType == dayType && timeSlot.startTime.isBefore(currentLocalTime)
                                    && timeSlot.endTime.isAfter(currentLocalTime))
                .collect(Collectors.toList());
        assert matchingTimeSlots.size() <= 1;
        if (matchingTimeSlots.size() == 1)
            return Optional.of(matchingTimeSlots.get(0));
        else
            return Optional.empty();
    }

    private static boolean atLeastTwoTimeSlotsOverlap(List<TimeSlot> timeSlots) {
        List<TimeSlot> remainingTimeSlots = new ArrayList<>(timeSlots);
        for (TimeSlot timeSlot : timeSlots) {
            remainingTimeSlots.remove(timeSlot);
            boolean overlap = remainingTimeSlots
                    .stream()
                    .anyMatch(timeSlot::overlapsWith);
            if (overlap){
                return true;
            }
        }
        return false;
    }
}
