package dev.creategmf.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/** Immutable values, including lists, so temporary profiles cannot alias the previous configuration. */
public record SettingsSnapshot<K>(Map<K, Object> values) {
    public SettingsSnapshot {
        Map<K, Object> copied = new LinkedHashMap<>();
        values.forEach((key, value) -> copied.put(key, value instanceof List<?> list ? List.copyOf(list) : value));
        values = Map.copyOf(copied);
    }
    public void restore(java.util.function.BiConsumer<K, Object> setter) { values.forEach(setter); }
}
