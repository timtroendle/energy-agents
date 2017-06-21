package uk.ac.cam.eeci.energyagents;

import org.javatuples.Pair;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An urban district comprising of several dwellings.
 */
public class DwellingDistrict {

    private final List<DwellingReference> dwellings;

    public DwellingDistrict(Set<DwellingReference> dwellings) {
        this.dwellings = new ArrayList<>(dwellings);
        if (dwellings.size() == 0){
            throw new IllegalArgumentException("DwellingDistrict must contain at least one dwelling.");
        }
    }

    public CompletableFuture<Map<DwellingReference, Double>> getAllCurrentAirTemperatures() {
        Map<DwellingReference, Double> values = new ConcurrentHashMap<>();
        CompletableFuture<Void>[] updates = new CompletableFuture[this.dwellings.size()];
        for (int i = 0; i < this.dwellings.size(); ++i) {
            updates[i] = this.getAirTemperature(this.dwellings.get(i))
                    .thenAccept(pair -> values.put(pair.getValue0(), pair.getValue1()));
        }
        return CompletableFuture.allOf(updates)
                .thenApply(nothing -> values);
    }

    public CompletableFuture<Map<DwellingReference, Double>> getAllCurrentThermalPowers() {
        Map<DwellingReference, Double> values = new ConcurrentHashMap<>();
        CompletableFuture<Void>[] updates = new CompletableFuture[this.dwellings.size()];
        for (int i = 0; i < this.dwellings.size(); ++i) {
            updates[i] = this.getThermalPower(this.dwellings.get(i))
                    .thenAccept(pair -> values.put(pair.getValue0(), pair.getValue1()));
        }
        return CompletableFuture.allOf(updates)
                .thenApply(nothing -> values);
    }

    private CompletableFuture<Pair<DwellingReference, Double>> getAirTemperature(DwellingReference dwelling) {
        return dwelling.getCurrentAirTemperature().thenApplyAsync(temp -> new Pair<>(dwelling, temp));
    }

    private CompletableFuture<Pair<DwellingReference, Double>> getThermalPower(DwellingReference dwelling) {
        return dwelling.getCurrentThermalPower().thenApplyAsync(temp -> new Pair<>(dwelling, temp));
    }
}
