/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.communication.driver;

import org.eclipse.jnosql.communication.ValueWriter;
import org.eclipse.jnosql.communication.ValueWriterDecorator;

import java.util.List;
import java.util.Objects;

/**
 * A composite {@link ValueWriter} that delegates type checks and writing operations
 * to a chain of custom writers before falling back to the system default.
 */
@SuppressWarnings("rawtypes")
public final class CompositeValueWriter<T, S> implements ValueWriter<T, S> {

    private static final ValueWriter DEFAULT = ValueWriterDecorator.getInstance();


    private final List<ValueWriter> customWriters;

    public CompositeValueWriter(ValueWriter... customWriters) {
        this.customWriters = List.of(Objects.requireNonNull(customWriters));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean test(Class<?> type) {
        for (var writer : customWriters) {
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
            for (var writer : customWriters) {
                if (writer.test(clazz)) {
                    return (S) writer.write(type);
                }
            }
        }
        return (S) DEFAULT.write(type);
    }
}