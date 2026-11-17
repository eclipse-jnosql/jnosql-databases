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
 *   Lucas Furlaneto
 */
package org.eclipse.jnosql.databases.orientdb.communication;

/**
 * Receives the updated record of an OrientDB live query. It is invoked each time a matching record is updated.
 * Pass it to {@link OrientDBLiveCallback} to subscribe through
 * {@link OrientDBDocumentManager#live(String, OrientDBLiveCallback, Object...)}.
 *
 * <pre>{@code
 * OrientDBLiveUpdateCallback<CommunicationEntity> callback = entity -> System.out.println(entity);
 * }</pre>
 *
 * @param <T> the record type
 */
@FunctionalInterface
public interface OrientDBLiveUpdateCallback<T> {
/**
 * Handles the updated record.
 *
 * @param t the updated record
 */
    void accept(T t);
}
