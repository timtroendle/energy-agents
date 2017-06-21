package uk.ac.cam.eeci.energyagents.test;

import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.eeci.energyagents.Dwelling;
import uk.ac.cam.eeci.energyagents.DwellingDistrict;
import uk.ac.cam.eeci.energyagents.DwellingReference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDwellingDistrict {

    private final static Double INITIAL_TEMPERATURE_1 = 26.0;
    private final static Double INITIAL_TEMPERATURE_2 = 20.0;
    private final static Double INITIAL_THERMAL_POWER_1 = 100.0;
    private final static Double INITIAL_THERMAL_POWER_2 = 267.4;

    private DwellingDistrict district;
    private Dwelling dwelling1 = mock(Dwelling.class);
    private Dwelling dwelling2 = mock(Dwelling.class);

    @Before
    public void setUp(){
        when(this.dwelling1.getCurrentAirTemperature()).thenReturn(INITIAL_TEMPERATURE_1);
        when(this.dwelling1.getCurrentThermalPower()).thenReturn(INITIAL_THERMAL_POWER_1);
        when(this.dwelling2.getCurrentAirTemperature()).thenReturn(INITIAL_TEMPERATURE_2);
        when(this.dwelling2.getCurrentThermalPower()).thenReturn(INITIAL_THERMAL_POWER_2);
        district = new DwellingDistrict(new HashSet<>(
                Arrays.asList(new DwellingReference(dwelling1), new DwellingReference(dwelling2))));
    }

    @Test
    public void returnsAllTemperatureValues() throws ExecutionException, InterruptedException {
        Map<DwellingReference, Double> values = this.district.getAllCurrentAirTemperatures().get();
        assertThat(values.values(), containsInAnyOrder(INITIAL_TEMPERATURE_1, INITIAL_TEMPERATURE_2));
    }

    @Test
    public void returnsAllThermalPowerValues() throws ExecutionException, InterruptedException {
        Map<DwellingReference, Double> values = this.district.getAllCurrentThermalPowers().get();
        assertThat(values.values(), containsInAnyOrder(INITIAL_THERMAL_POWER_1, INITIAL_THERMAL_POWER_2));
    }

}
