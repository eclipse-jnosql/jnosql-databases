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
package org.eclipse.jnosql.databases.mongodb.communication;

import org.eclipse.jnosql.communication.ValueWriter;
import org.eclipse.jnosql.communication.ValueWriterDecorator;
import org.eclipse.jnosql.communication.driver.UUIDValueWriter;

import java.util.UUID;

final class MongoDBValueWriteDecorator<T, S> implements ValueWriter<T, S> {

    @SuppressWarnings("rawtypes")
    static final ValueWriter MONGO_DB_VALUE_WRITER = new MongoDBValueWriteDecorator();

    @SuppressWarnings("rawtypes")
    private static final ValueWriter DEFAULT = ValueWriterDecorator.getInstance();

    private static final UUIDValueWriter UUID_VALUE_WRITER = new UUIDValueWriter();


    @Override
    public boolean test(Class<?> type) {
        return UUID_VALUE_WRITER.test(type) || DEFAULT.test(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public S write(T type) {
        if(type != null && UUID_VALUE_WRITER.test(type.getClass())) {
            return (S) UUID_VALUE_WRITER.write((UUID) type);
        } else {
            return (S) DEFAULT.write(type);
        }
    }

}
