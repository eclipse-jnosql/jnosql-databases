/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.valkey.communication;

/**
 * Represents an element of a sorted set, composed of a member and its associated score.
 * <p>
 * In data stores such as Valkey, a sorted set maintains elements ordered
 * by a numeric value (commonly referred to as a <em>score</em>). This interface models
 * that concept in a domain-oriented way, abstracting the underlying storage details
 * while preserving ordering semantics.
 * </p>
 *
 * <p>
 * <strong>Design note:</strong> This type is a value object and should be treated as immutable.
 * Implementations are expected to provide consistent {@code equals} and {@code hashCode}
 * semantics based on both member and score.
 * </p>
 *
 * @see <a href="https://valkey.io/">Valkey documentation</a>
 */
public interface Ranking {

    /**
     * Returns the numeric score associated with this member.
     * <p>
     * The score determines the ordering of elements within the sorted set.
     * </p>
     *
     * @return the score used for ordering; never {@code null}
     */
    Number getPoints();

    /**
     * Returns the unique member identifier within the sorted set.
     *
     * @return the member identifier; never {@code null}
     */
    String getMember();

    /**
     * Creates a new {@link Ranking} instance with the given member and score.
     * <p>
     * This factory method provides a consistent way to create immutable
     * {@code Ranking} instances.
     * </p>
     *
     * @param member the member identifier; must not be {@code null}
     * @param points the score associated with the member; must not be {@code null}
     * @return a new {@link Ranking} instance
     * @throws NullPointerException if either {@code member} or {@code points} is {@code null}
     */
    static Ranking of(String member, Number points) throws NullPointerException {
        return new DefaultRanking(member, points);
    }
}