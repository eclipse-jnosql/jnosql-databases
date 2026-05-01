/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.redis.communication;


import java.time.Duration;

/**
 * The redis counter structure
 */
public interface Counter {

    /**
     * Returns the counter value
     *
     * @return The counter value
     */
    Number get();

    /**
     * Increments by one the counter
     *
     * @return the increment by one result
     */
    Number increment();

    /**
     * Increments the counter
     *
     * @param value the value to be increased
     * @return the increment result
     * @throws NullPointerException when value is null
     */
    Number increment(Number value) throws NullPointerException;

    /**
     * Decrements by one the counter
     *
     * @return the increment by one result
     */
    Number decrement();

    /**
     * Decrements
     *
     * @param value the value to be decreased
     * @return the decrement result
     */
    Number decrement(Number value);

    /**
     * Delete this Counter
     */
    void delete();


    /**
     * Defines a ttl to SortedSet
     *
     * @param ttl the ttl
     * @throws NullPointerException when either key and ttl are null
     */
    void expire(Duration ttl) throws NullPointerException;

    /**
     * Removes the ttl
     */
    void persist();
}
