package uk.ac.eeci.test;

import org.junit.Before;
import org.junit.Test;
import uk.ac.eeci.DataPoint;
import uk.ac.eeci.TimeSeries;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TestDataPoint {

    private static class DataPointValueSource {

        private Double value;

        public DataPointValueSource(double initialValue) {
            this.value = initialValue;
        }

        private CompletableFuture<Double> getDataPointValue() {
            return CompletableFuture.completedFuture(this.value);
        }

    }

    private final static ZonedDateTime INITIAL_TIME = ZonedDateTime.of(2017, 2, 23, 17, 16, 0, 0, ZoneOffset.UTC);
    private final static Duration TIME_STEP_SIZE = Duration.ofMinutes(1);
    private final static String DATA_POINT_NAME = "testDataPoint";

    private DataPointValueSource source1;
    private DataPointValueSource source2;
    private Map<Integer, DataPointValueSource> indexedSources;
    private DataPoint<DataPointValueSource, Double> dataPoint;

    @Before
    public void setUp() {
        this.source1 = new DataPointValueSource(4.0);
        this.source2 = new DataPointValueSource(5.0);
        this.indexedSources = new HashMap<>();
        this.indexedSources.put(1, this.source1);
        this.indexedSources.put(2, this.source2);

        this.dataPoint = new DataPoint<>(
                DATA_POINT_NAME,
                this.indexedSources,
                DataPointValueSource::getDataPointValue
        );
    }

    @Test
    public void returnsOwnName() {
        assertThat(this.dataPoint.getName(), is(equalTo(DATA_POINT_NAME)));
    }

    @Test
    public void recordContainsEmptyTimeSeriesBeforeStepping() {
        Map<Integer, TimeSeries<Double>> record = this.dataPoint.getRecord();

        assertThat(record.get(1).size(), is(equalTo(0)));
        assertThat(record.get(2).size(), is(equalTo(0)));
    }

    @Test
    public void recordContainsEntryForEachSource() throws ExecutionException, InterruptedException {
        this.dataPoint.step(INITIAL_TIME).get();
        Map<Integer, TimeSeries<Double>> record = this.dataPoint.getRecord();

        assertThat(record.size(), is(equalTo(2)));
    }

    @Test
    public void recordsInitialValue() throws ExecutionException, InterruptedException {
        this.dataPoint.step(INITIAL_TIME).get();
        Map<Integer, TimeSeries<Double>> record = this.dataPoint.getRecord();

        assertThat(record.get(1).getValues().get(0), is(equalTo(4.0)));
        assertThat(record.get(2).getValues().get(0), is(equalTo(5.0)));
    }

    @Test
    public void recordsInitialTimeStamp() throws ExecutionException, InterruptedException {
        this.dataPoint.step(INITIAL_TIME).get();
        Map<Integer, TimeSeries<Double>> record = this.dataPoint.getRecord();

        assertThat(record.get(1).getIndex().get(0), is(equalTo(INITIAL_TIME)));
        assertThat(record.get(2).getIndex().get(0), is(equalTo(INITIAL_TIME)));
    }

    @Test
    public void recordsLaterValues() throws ExecutionException, InterruptedException {
        this.dataPoint.step(INITIAL_TIME).get();
        this.source1.value = 6.0;
        this.source2.value = 3.0;
        this.dataPoint.step(INITIAL_TIME.plus(TIME_STEP_SIZE)).get();
        Map<Integer, TimeSeries<Double>> record = this.dataPoint.getRecord();

        assertThat(record.get(1).getValues().get(1), is(equalTo(6.0)));
        assertThat(record.get(2).getValues().get(1), is(equalTo(3.0)));
    }

    @Test
    public void recordsLaterTimeStamps() throws ExecutionException, InterruptedException {
        this.dataPoint.step(INITIAL_TIME).get();
        ZonedDateTime nextTimeStamp = INITIAL_TIME.plus(TIME_STEP_SIZE);
        this.dataPoint.step(nextTimeStamp).get();
        Map<Integer, TimeSeries<Double>> record = this.dataPoint.getRecord();

        assertThat(record.get(1).getIndex().get(1), is(equalTo(nextTimeStamp)));
        assertThat(record.get(2).getIndex().get(1), is(equalTo(nextTimeStamp)));
    }

    @Test
    public void timeSeriesIsAsLongAsNumberOfSteps() throws ExecutionException, InterruptedException {
        this.dataPoint.step(INITIAL_TIME).get();
        this.dataPoint.step(INITIAL_TIME.plus(TIME_STEP_SIZE.multipliedBy(1))).get();
        this.dataPoint.step(INITIAL_TIME.plus(TIME_STEP_SIZE.multipliedBy(2))).get();
        this.dataPoint.step(INITIAL_TIME.plus(TIME_STEP_SIZE.multipliedBy(3))).get();
        this.dataPoint.step(INITIAL_TIME.plus(TIME_STEP_SIZE.multipliedBy(4))).get();

        Map<Integer, TimeSeries<Double>> record = this.dataPoint.getRecord();

        assertThat(record.get(1).size(), is(equalTo(5)));
        assertThat(record.get(2).size(), is(equalTo(5)));
    }

    @Test
    public void conservesArbitraryIdsInRecord() {
        Map<Integer, DataPointValueSource> arbitrarilyIndexedSources = new HashMap<>();
        arbitrarilyIndexedSources.put(100023, this.source2);
        arbitrarilyIndexedSources.put(56, this.source1);
        DataPoint<DataPointValueSource, Double> secondDataPoint = new DataPoint<>(
                "testDataPoint2",
                arbitrarilyIndexedSources,
                DataPointValueSource::getDataPointValue
        );

        assertThat(secondDataPoint.getRecord().keySet(), containsInAnyOrder(100023, 56));
    }


}
