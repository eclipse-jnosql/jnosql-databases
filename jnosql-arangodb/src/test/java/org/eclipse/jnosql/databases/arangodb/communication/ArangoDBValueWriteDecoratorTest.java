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

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;


@SuppressWarnings("rawtypes")
class ArangoDBValueWriteDecoratorTest {

    private final ArangoDBValueWriteDecorator<Object, String> valueWriter = new ArangoDBValueWriteDecorator<>();

    @Test
    void shouldTestUUIDType() {
        assertTrue(valueWriter.test(UUID.class));
    }

}
