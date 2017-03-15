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

public class TimeScheduleControlStrategy extends HeatingControlStrategy {

    public enum DayOfWeek {
        WEEKDAY, WEEKEND;

        public static DayOfWeek fromTimeZone(ZonedDateTime timeStamp) {
            java.time.DayOfWeek dayOfWeek = timeStamp.getDayOfWeek();
            switch (dayOfWeek) {
                case MONDAY:
                case TUESDAY:
                case WEDNESDAY:
                case THURSDAY:
                case FRIDAY:
                    return DayOfWeek.WEEKDAY;
                case SATURDAY:
                case SUNDAY:
                    return DayOfWeek.WEEKEND;
                    default:
                        throw new IllegalStateException(String.format("Received unknown day of week: %s.", dayOfWeek));
            }
        }
    }

    public static class TimeSlot {

        private final DayOfWeek dayOfWeek;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final double heatingSetPoint;

        public TimeSlot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, double heatingSetPoint) {
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            if (endTime != LocalTime.MIDNIGHT)
                this.endTime = endTime;
            else
                this.endTime = endTime.minus(Duration.ofMillis(1)); // so that this time is considered same day
            this.heatingSetPoint = heatingSetPoint;
        }

        public boolean startsWithinOtherTimeSlot(TimeSlot other) {
            return this.dayOfWeek == other.dayOfWeek && this.startTime.isAfter(other.startTime)
                    && this.startTime.isBefore(other.endTime);
        }
    }

    private final List<TimeSlot> timeSlots;
    private final ZoneId zoneId;

    public TimeScheduleControlStrategy(List<TimeSlot> timeSlots, ZoneId zoneId) {
        if (TimeScheduleControlStrategy.timeSlotsOverlap(timeSlots)) {
            String msg = "Passed time slots are overlapping which must not be the case.";
            throw new IllegalArgumentException(msg);
        }
        this.timeSlots = timeSlots;
        this.zoneId = zoneId;
    }

    @Override
    public CompletableFuture<Optional<Double>> heatingSetPoint(ZonedDateTime timeStamp, Set<PersonReference> peopleInDwelling) {
        Optional<TimeSlot> currentTimeSlot = this.chooseCurrentTimeSlot(timeStamp);
        if (currentTimeSlot.isPresent()) {
            return CompletableFuture.completedFuture(Optional.of(currentTimeSlot.get().heatingSetPoint));
        }
        else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    private Optional<TimeSlot> chooseCurrentTimeSlot(ZonedDateTime currentTime) {
        DayOfWeek dayOfWeek = DayOfWeek.fromTimeZone(currentTime);
        LocalTime currentLocalTime = currentTime.withZoneSameInstant(this.zoneId).toLocalTime();
        for (TimeSlot timeSlot : this.timeSlots) {
            if (timeSlot.dayOfWeek == dayOfWeek && timeSlot.startTime.isBefore(currentLocalTime)
                    && timeSlot.endTime.isAfter(currentLocalTime))
                return Optional.of(timeSlot);
        }
        return Optional.empty();
    }

    private static boolean timeSlotsOverlap(List<TimeSlot> timeSlots) {
        List<TimeSlot> remainingTimeSlots = new ArrayList<>(timeSlots);
        for (TimeSlot timeSlot : timeSlots) {
            remainingTimeSlots.remove(timeSlot);
            boolean overlap = remainingTimeSlots
                    .stream()
                    .anyMatch((other) ->
                            (timeSlot.startsWithinOtherTimeSlot(other) || other.startsWithinOtherTimeSlot(timeSlot)));
            if (overlap){
                return true;
            }
        }
        return false;
    }


}
