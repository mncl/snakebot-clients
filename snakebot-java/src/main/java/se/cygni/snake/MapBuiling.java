package se.cygni.snake;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

class MapBuiling {

    private MapBuiling() {/*non-instance*/}

    public static <K, V> Map<V, K> mapSwap(Map<K, V> parent) {
        Map<V, K> ret = new HashMap<>();
        Set<Map.Entry<K, V>> entries = parent.entrySet();
        for (Map.Entry<K, V> e : entries) {
            ret.put(e.getValue(), e.getKey());
        }
        return ret;
    }

    public static <K, V, R> Map<K, R> modMapValues(Map<K, V> parent, BiFunction<K, V, R> maping) {
        HashMap<K, R> ret = new HashMap<>();
        Set<Map.Entry<K, V>> entries = parent.entrySet();
        for (Map.Entry<K, V> e : entries) {
            K k = e.getKey();
            V v = e.getValue();
            ret.put(k, maping.apply(k, v));
        }
        return ret;
    }

    public interface MapBuilder<K, V> {
        MapBuilder<K, V> put(K key, V value);

        java.util.Map<K, V> build();
    }

    private static <K, V> MapBuilder<K, V> map(Collection<K> keys, Collection<V> values) {
        return new MapBuilder<K, V>() {
            @Override
            public MapBuilder<K, V> put(K key, V value) {
                ArrayList<K> ks = new ArrayList<>(keys);
                ks.add(key);
                ArrayList<V> vs = new ArrayList<>(values);
                vs.add(value);
                return map(ks, vs);
            }

            @Override
            public Map<K, V> build() {
                HashMap<K, V> ret = new HashMap<>();
                Iterator<K> ki = keys.iterator();
                Iterator<V> vi = values.iterator();
                while (ki.hasNext() && vi.hasNext()) {
                    ret.put(ki.next(), vi.next());
                }
                return ret;
            }
        };
    }

    public static <K, V> MapBuilder<K, V> map(K firstKey, V firstValue) {
        return new MapBuilder<K, V>() {
            @Override
            public MapBuilder<K, V> put(K key, V value) {
                return map(Arrays.asList(firstKey, key), Arrays.asList(firstValue, value));
            }

            @Override
            public Map<K, V> build() {
                HashMap<K, V> kvMap = new HashMap<>();
                kvMap.put(firstKey, firstValue);
                return kvMap;
            }
        };
    }

}
