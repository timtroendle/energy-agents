package uk.ac.eeci;

import org.javatuples.Pair;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class DwellingSet {

    private final List<DwellingReference> dwellings;

    public DwellingSet(Collection<DwellingReference> dwellings) {
        this.dwellings = new ArrayList<>(dwellings);
    }

    public List<Double> getTemperatures() {
        Map<DwellingReference, Double> temperatures = new ConcurrentHashMap<>();
        CompletableFuture<Void>[] temperaturesUpdates = new CompletableFuture[this.dwellings.size()];
        for(int i = 0; i < this.dwellings.size(); ++i) {
            temperaturesUpdates[i] = this.getTemperature(this.dwellings.get(i)).thenAccept(pair -> temperatures.put(pair.getValue0(), pair.getValue1()));
        }
        try {
            CompletableFuture.allOf(temperaturesUpdates).get();
        } catch (InterruptedException e) {
            e.printStackTrace(); // FIXME proper exception handling necessary
        } catch (ExecutionException e) {
            e.printStackTrace(); // FIXME proper exception handling necessary
        }
        return this.mapToSortedList(temperatures);
    }

    public List<DwellingReference> getDwellings() {
        return this.dwellings;
    }

    private CompletableFuture<Pair<DwellingReference, Double>> getTemperature(DwellingReference dwelling) {
        return dwelling.getTemperature().thenApplyAsync(temp -> new Pair<>(dwelling, temp));
    }

    private List<Double> mapToSortedList(Map<DwellingReference, Double> valueMap) {
        List<Double> temperatures = new ArrayList<>(this.dwellings.size());
        for (DwellingReference dwelling : this.dwellings) {
            temperatures.add(valueMap.get(dwelling));
        }
        return temperatures;
    }
}
