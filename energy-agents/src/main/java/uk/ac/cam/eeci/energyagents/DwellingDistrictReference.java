package uk.ac.cam.eeci.energyagents;

import uk.ac.cam.eeci.framework.Reference;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DwellingDistrictReference extends Reference<DwellingDistrict> {

    public DwellingDistrictReference(DwellingDistrict referent) {
        super(referent);
    }

    public CompletableFuture<Map<DwellingReference, Double>> getAllCurrentAirTemperatures() {
        return this.referent.getAllCurrentAirTemperatures()
                .thenApplyAsync((values) -> values, pool.currentExecutor());
    }

    public CompletableFuture<Map<DwellingReference, Double>> getAllCurrentThermalPowers() {
        return this.referent.getAllCurrentThermalPowers()
                .thenApplyAsync((values) -> values, pool.currentExecutor());
    }

}
