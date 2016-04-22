package se.cygni.snake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

public class BucketMerge<T, Key> implements Consumer<T>, Function<Key, T> {

    final Function<T, Key> bucketSelector;
    final Function<T, ValueMerge<T>> bucketSupplier;
    final Map<Key, ValueMerge<T>> existingBuckets;
    final Function<Key, T> defaultValues;

    public static <Key extends Enum, T> BucketMerge<T, Key> fromSegmentDefaultMerge(Function<T, Key> bucketSelector, Function<Key, T> defaultValues, BinaryOperator<T> merger) {
        Function<T, ValueMerge<T>> bucketSupplier = t -> ValueMerge.instance(defaultValues.apply(bucketSelector.apply(t)), merger);
        ConcurrentMap<Key, ValueMerge<T>> storage = new ConcurrentHashMap<>();
        return new BucketMerge<>(bucketSelector, bucketSupplier, storage, defaultValues);
    }

    BucketMerge(Function<T, Key> bucketSelector, Function<T, ValueMerge<T>> bucketSupplier, ConcurrentMap<Key, ValueMerge<T>> existingBuckets, Function<Key, T> defaultValues) {
        this.bucketSelector = bucketSelector;
        this.bucketSupplier = bucketSupplier;
        this.existingBuckets = existingBuckets;
        this.defaultValues = defaultValues;
    }

    private ValueMerge<T> initBucket(Key key) {
        existingBuckets.computeIfAbsent(key, key1 -> bucketSupplier.apply(defaultValues.apply(key)));
        return existingBuckets.get(key);
    }

    @Override
    public void accept(T t) {
        initBucket(bucketSelector.apply(t)).accept(t);
    }

    @Override
    public T apply(Key key) {
        return initBucket(key).get();
    }
}
