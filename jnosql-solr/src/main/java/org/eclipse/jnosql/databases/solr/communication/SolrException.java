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
package org.eclipse.jnosql.databases.solr.communication;

import org.eclipse.jnosql.communication.CommunicationException;

/**
 * Root of exception on Solr/Jakarta NoSQL integration
 */
public class SolrException extends CommunicationException {

    /**
     * Constructs a new runtime exception with the specified detail message and cause.
     *
     * @param message the message
     * @param cause   the cause
     */
    SolrException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     *
     * @param message the message
     */
    SolrException(String message) {
        super(message);
    }
}
