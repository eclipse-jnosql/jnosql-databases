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
package org.eclipse.jnosql.databases.redis.communication;

/**
 * A member of a Redis sorted set together with its score. Sorted sets keep members ordered by score.
 * Instances are immutable and compare equal when member and score are equal.
 *
 * {@snippet lang="java" :
 * Ranking ranking = Ranking.of("ada", 10);
 * String member = ranking.getMember(); // "ada"
 * Number points = ranking.getPoints(); // 10
 * }
 *
 * @see SortedSet
 */
public interface Ranking {

    /**
     * Returns the score associated with this ranking.
     *
     * @return the point
     */
    Number getPoints();

    /**
     * Returns the ranked member name.
     *
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