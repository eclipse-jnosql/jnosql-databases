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

import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Groups the callbacks of an OrientDB live query. Any callback may be {@code null} to ignore that event.
 *
 * <pre>{@code
 * OrientDBLiveCallback<CommunicationEntity> callbacks = new OrientDBLiveCallback<>(
 *         created -> System.out.println("created " + created), null, null);
 * manager.live("LIVE SELECT FROM Person", callbacks);
 * }</pre>
 *
 * @param <T> the record type
 */
public class OrientDBLiveCallback<T> {

    private final OrientDBLiveCreateCallback<T> createCallback;
    private final OrientDBLiveUpdateCallback<T> updateCallback;
    private final OrientDBLiveDeleteCallback<T> deleteCallback;

/**
 * Creates the callback group.
 *
 * @param createCallback called on creations, or {@code null}
 * @param updateCallback called on updates, or {@code null}
 * @param deleteCallback called on deletions, or {@code null}
 */
    public OrientDBLiveCallback(OrientDBLiveCreateCallback<T> createCallback,
                                OrientDBLiveUpdateCallback<T> updateCallback,
                                OrientDBLiveDeleteCallback<T> deleteCallback) {
        this.createCallback = createCallback;
        this.updateCallback = updateCallback;
        this.deleteCallback = deleteCallback;
    }

/**
 * Returns the create callback.
 *
 * @return the callback, or empty when none was set
 */
    public Optional<OrientDBLiveCreateCallback<T>> getCreateCallback() {
        return ofNullable(createCallback);
    }

/**
 * Returns the update callback.
 *
 * @return the callback, or empty when none was set
 */
    public Optional<OrientDBLiveUpdateCallback<T>> getUpdateCallback() {
        return ofNullable(updateCallback);
    }

/**
 * Returns the delete callback.
 *
 * @return the callback, or empty when none was set
 */
    public Optional<OrientDBLiveDeleteCallback<T>> getDeleteCallback() {
        return ofNullable(deleteCallback);
    }

}
