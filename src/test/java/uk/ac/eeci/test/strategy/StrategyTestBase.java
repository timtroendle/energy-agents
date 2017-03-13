package uk.ac.eeci.test.strategy;

import org.junit.Before;
import org.junit.runners.Parameterized;
import uk.ac.eeci.HeatingControlStrategy;
import uk.ac.eeci.Person;
import uk.ac.eeci.PersonReference;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.ac.eeci.test.utils.Utils.cartesian;

public class StrategyTestBase {

    private final static ZoneId TIME_ZONE = ZoneId.of("Europe/Paris");
    private final static ZonedDateTime MONDAY_AM = ZonedDateTime.of(2017, 3, 13, 10, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime TUESDAY_PM = ZonedDateTime.of(2017, 3, 14, 16, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime SATURDAY_AM = ZonedDateTime.of(2017, 3, 11, 9, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime SUNDAY_PM = ZonedDateTime.of(2017, 3, 12, 15, 0, 0, 0, TIME_ZONE);

    HeatingControlStrategy strategy;
    PersonReference person1 = mock(PersonReference.class);
    PersonReference person2 = mock(PersonReference.class);
    Set<PersonReference> people;


    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[] activities = Person.Activity.values();
        Object[] timeStamps = new Object[]{MONDAY_AM, TUESDAY_PM, SATURDAY_AM, SUNDAY_PM};
        return cartesian(activities, activities, timeStamps);
    }

    @Parameterized.Parameter
    public Person.Activity activityPerson1;

    @Parameterized.Parameter(1)
    public Person.Activity activityPerson2;

    @Parameterized.Parameter(2)
    public ZonedDateTime timeStamp;

    @Before
    public void setUp() {
        this.people = new HashSet<>();
        this.people.add(person1);
        this.people.add(person2);
        CompletableFuture<Person.Activity> activityPerson1 = new CompletableFuture<>();
        CompletableFuture<Person.Activity> activityPerson2 = new CompletableFuture<>();
        activityPerson1.complete(this.activityPerson1);
        activityPerson2.complete(this.activityPerson2);
        when(this.person1.getCurrentActivity()).thenReturn(activityPerson1);
        when(this.person2.getCurrentActivity()).thenReturn(activityPerson2);
    }
}
