package uk.ac.eeci;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DataLogger {

    private final DwellingSetReference dwellingSet;
    private final List<DwellingReference> dwellings;
    private final List<Double> temperatures;


    /** Logs data points during the simulation. */
    public DataLogger(DwellingSetReference dwellingSet) throws ExecutionException, InterruptedException {
        this.dwellingSet = dwellingSet;
        this.dwellings = this.dwellingSet.getDwellings().get();
        this.temperatures = new ArrayList<>();
    }

    public void step() {
        try {
            this.storeTemperatures(this.dwellingSet.getTemperatures().get());
        } catch (InterruptedException e) {
            e.printStackTrace(); // FIXME proper exception handling necessary
        } catch (ExecutionException e) {
            e.printStackTrace(); // FIXME proper exception handling necessary
        }
    }

    public Map<DwellingReference, List<Double>> getTemperatureRecord(){
        List<List<Double>> temperatureRecord = new ArrayList<>();
        for (DwellingReference dwelling : this.dwellings) {
            int dwellingIndex = this.dwellings.indexOf(dwelling);
            temperatureRecord.add(dwellingIndex, new ArrayList<>());
        }
        for (int i=0; i < this.temperatures.size(); i++) {
            int listIndex = i % this.dwellings.size();
            temperatureRecord.get(listIndex).add(this.temperatures.get(i));
        }
        Map<DwellingReference, List<Double>> temperatureRecordMap = new HashMap<>();
        for (DwellingReference dwelling : this.dwellings) {
            int dwellingIndex = this.dwellings.indexOf(dwelling);
            temperatureRecordMap.put(this.dwellings.get(dwellingIndex), temperatureRecord.get(dwellingIndex));
        }
        return temperatureRecordMap;
    }

    private void storeTemperatures(List<Double> currentTemperatures) {
        this.temperatures.addAll(currentTemperatures);
    }


}
