package sk.uniba.fmph.dcs.terra_futura;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class ScoringMethod {
    public List<Resource> resources;

    public Points pointsPerCombination;
    public Optional<Points> calculatedTotal;

    // provides current resource counts from the game
    private final Supplier<Map<Resource, Integer>> resourceProvider;

    public ScoringMethod(final List<Resource> resources,
                         final Points pointsPerCombination,
                         final Supplier<Map<Resource, Integer>> resourceProvider) {
        if (resources == null || pointsPerCombination == null || resourceProvider == null) {
            throw new IllegalArgumentException("Arguments must be non-null");
        }

        this.resources = List.copyOf(resources);
        this.pointsPerCombination = pointsPerCombination;
        this.resourceProvider = resourceProvider;
        this.calculatedTotal = Optional.empty();
    }

    // compute total points from available resources
    public void selectThisMethodAndCalculate() {
        final Map<Resource, Integer> provided = resourceProvider.get();

        final EnumMap<Resource, Integer> available =
                new EnumMap<>(Resource.class);
        if (provided != null) {
            available.putAll(provided);
        }

        // how many of each resource is needed for one combo
        final EnumMap<Resource, Integer> needed =
                new EnumMap<>(Resource.class);
        for (Resource r : resources) {
            needed.merge(r, 1, Integer::sum);
        }

        int combinations = 0;

        if (!needed.isEmpty()) {
            combinations = Integer.MAX_VALUE;

            for (Map.Entry<Resource, Integer> e : needed.entrySet()) {
                final Resource r = e.getKey();
                final int need = e.getValue();
                final int have = available.getOrDefault(r, 0);
                final int fromThis = have / need;
                combinations = Math.min(combinations, fromThis);
            }
        }

        final Points total =
                new Points(combinations * pointsPerCombination.value());

        this.calculatedTotal = Optional.of(total);
    }

    // JSON view for UI / debugging
    public String state() {
        final JSONObject json = new JSONObject();

        final JSONArray patternArr = new JSONArray();
        for (Resource r : resources) {
            patternArr.put(r.name());
        }
        json.put("resources", patternArr);

        json.put("pointsPerCombination", pointsPerCombination.value());
        json.put("selected", calculatedTotal.isPresent());
        json.put("total",
                calculatedTotal.map(Points::value).orElse(0));

        return json.toString();
    }
}
