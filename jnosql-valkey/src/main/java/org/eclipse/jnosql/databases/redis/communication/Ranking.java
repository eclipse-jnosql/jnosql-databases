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

/**
 * An SortedSet element
 */
public interface Ranking {

    /**
     * @return the point
     */
    Number getPoints();

    /**
     * @return the member name
     */
    String getMember();

    /**
     * Returns a {@link Ranking} instance
     *
     * @param member the member name
     * @param points the point value
     * @return the instance
     * @throws NullPointerException when either member and points are null
     */
    static Ranking of(String member, Number points) throws NullPointerException {
        return new DefaultRanking(member, points);
    }
}