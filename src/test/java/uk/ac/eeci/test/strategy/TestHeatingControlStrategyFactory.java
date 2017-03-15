package uk.ac.eeci.test.strategy;

import org.junit.Before;
import org.junit.Test;
import uk.ac.eeci.strategy.ClimateChangingControlStrategy;
import uk.ac.eeci.strategy.HeatingControlStrategyFactory;
import uk.ac.eeci.strategy.PresenceBasedStrategy;
import uk.ac.eeci.strategy.TimeScheduleControlStrategy;

import static uk.ac.eeci.strategy.HeatingControlStrategyFactory.ControlStrategyType.*;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.time.LocalTime;
import java.time.ZoneId;

public class TestHeatingControlStrategyFactory {

    private final static double SET_POINT_HOME = 26;
    private final static double SET_POINT_ASLEEP = 18;
    private final static LocalTime WAKE_UP_TIME = LocalTime.of(6, 50);
    private final static LocalTime LEAVE_HOME_TIME = LocalTime.of(8, 40);
    private final static LocalTime COME_HOME_TIME = LocalTime.of(18, 20);
    private final static LocalTime BED_TIME = LocalTime.of(22, 10);
    private final static ZoneId TIME_ZONE = ZoneId.of("Europe/Paris");

    private HeatingControlStrategyFactory factory;

    @Before
    public void setUp() {
        this.factory = new HeatingControlStrategyFactory(
                SET_POINT_HOME, SET_POINT_ASLEEP,
                WAKE_UP_TIME, LEAVE_HOME_TIME,
                COME_HOME_TIME, BED_TIME,
                TIME_ZONE
        );
    }

    @Test
    public void buildsFlatStrategy() {
        assertThat(this.factory.build(FLAT), is(instanceOf(ClimateChangingControlStrategy.class)));
    }

    @Test
    public void buildsTimeTriggeredStrategy() {
        assertThat(this.factory.build(TIME_TRIGGERED), is(instanceOf(TimeScheduleControlStrategy.class)));
    }

    @Test
    public void buildsPresenceTriggeredStrategy() {
        assertThat(this.factory.build(PRESENCE_TRIGGERED), is(instanceOf(PresenceBasedStrategy.class)));
    }
}
