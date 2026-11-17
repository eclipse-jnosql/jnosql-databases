/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.elasticsearch.communication;

/**
 * Thrown when {@link ElasticsearchDocumentManager} needs the {@code _id} element of an entity, for example to
 * update it, and the entity does not have one. The message contains the offending entity.
 */
public class ElasticsearchKeyFoundException extends RuntimeException {

    ElasticsearchKeyFoundException(String message) {
        super("The entity was not found at: " + message);
    }
}
