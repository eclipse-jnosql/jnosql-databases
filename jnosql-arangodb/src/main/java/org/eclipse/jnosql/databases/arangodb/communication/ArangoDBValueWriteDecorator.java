/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.arangodb.communication;

import org.eclipse.jnosql.communication.ValueWriter;
import org.eclipse.jnosql.communication.ValueWriterDecorator;
import org.eclipse.jnosql.communication.driver.CompositeValueWriter;
import org.eclipse.jnosql.communication.driver.UUIDValueWriter;

import java.util.UUID;

final class ArangoDBValueWriteDecorator<T, S> implements ValueWriter<T, S> {

    @SuppressWarnings("rawtypes")
    static final ValueWriter ARANGO_DB_VALUE_WRITER = new ArangoDBValueWriteDecorator();

    @SuppressWarnings("rawtypes")
    private final ValueWriter delegate = new CompositeValueWriter(
            new UUIDValueWriter()
    );

    @SuppressWarnings("unchecked")
    @Override
    public boolean test(Class<?> type) {
        return delegate.test(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public S write(T type) {
        return (S) delegate.write(type);
    }
}
