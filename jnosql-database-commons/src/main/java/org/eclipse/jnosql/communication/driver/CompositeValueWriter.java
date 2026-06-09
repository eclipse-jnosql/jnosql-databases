package org.eclipse.jnosql.communication.driver;

import org.eclipse.jnosql.communication.ValueWriter;
import org.eclipse.jnosql.communication.ValueWriterDecorator;

import java.util.List;
import java.util.Objects;

/**
 * A composite {@link ValueWriter} that delegates type checks and writing operations
 * to a chain of custom writers before falling back to the system default.
 */
public final class CompositeValueWriter<T, S> implements ValueWriter<T, S> {

    @SuppressWarnings("rawtypes")
    private static final ValueWriter DEFAULT = ValueWriterDecorator.getInstance();

    @SuppressWarnings("rawtypes")
    private final List<ValueWriter> customWriters;

    @SuppressWarnings("rawtypes")
    public CompositeValueWriter(ValueWriter... customWriters) {
        this.customWriters = List.of(Objects.requireNonNull(customWriters));
    }

    @Override
    public boolean test(Class<?> type) {
        for (@SuppressWarnings("rawtypes") ValueWriter writer : customWriters) {
            if (writer.test(type)) {
                return true;
            }
        }
        return DEFAULT.test(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public S write(T type) {
        if (type != null) {
            Class<?> clazz = type.getClass();
            for (@SuppressWarnings("rawtypes") ValueWriter writer : customWriters) {
                if (writer.test(clazz)) {
                    return (S) writer.write(type);
                }
            }
        }
        return (S) DEFAULT.write(type);
    }
}