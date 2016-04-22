package se.cygni.snake;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;


public class ValueMerge<T> implements Consumer<T>, java.util.function.Supplier<T> {

    private final BinaryOperator<T> merger;
    private final AtomicReference<T> ref;

    public static <T> ValueMerge<T> instance(T initialValue, BinaryOperator<T> merger) {
        AtomicReference<T> ref = new AtomicReference<>(initialValue);
        return new ValueMerge<>(merger, ref);
    }

    private ValueMerge(BinaryOperator<T> merger, AtomicReference<T> ref) {
        this.merger = merger;
        this.ref = ref;
    }

    @Override
    public void accept(T t) {
        ref.accumulateAndGet(t, merger);
    }

    @Override
    public T get() {
        return ref.get();
    }
}
