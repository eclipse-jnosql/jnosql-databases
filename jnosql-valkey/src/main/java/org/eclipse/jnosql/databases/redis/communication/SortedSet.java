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
import java.util.List;

/**
 * Sorted sets are a data type which is similar to a mix between a Set and a Hash.
 * Like sets, sorted sets are composed of unique, non-repeating string elements,
 * so in some sense a sorted set is a set as well.
 */
public interface SortedSet {

    /**
     * Adds all the specified members with the specified scores to the sorted set stored at key.
     * Creates a score.
     *
     * @param member the name
     * @param value  the value
     * @throws NullPointerException when either member or value are null
     */
    void add(String member, Number value) throws NullPointerException;


    /**
     * Adds all the specified members with the specified scores to the sorted set stored at key.
     * Creates a score.
     *
     * @param ranking the element
     * @throws NullPointerException when ranking is null
     */
    void add(Ranking ranking) throws NullPointerException;

    /**
     * Increments the score of member in the sorted set stored at member by increment.
     *
     * @param member the member
     * @param value  the vaue
     * @return the new score member
     * @throws NullPointerException when either member or value are null
     */
    Number increment(String member, Number value) throws NullPointerException;

    /**
     * Increments the score of member in the sorted set stored at member by increment.
     *
     * @param member the member
     * @param value  the vaue
     * @return the new score member
     * @throws NullPointerException when either member or value are null
     */
    Number decrement(String member, Number value) throws NullPointerException;

    /**
     * Removes a member
     *
     * @param member the member
     * @throws NullPointerException when member is null
     */
    void remove(String member) throws NullPointerException;

    /**
     * @return the number of members on this
     */
    int size();

    /**
     * @return Returns true if this SortedSet contains no elements.
     */
    boolean isEmpty();

    /**
     * Delete this SortedSet
     *
     * @deprecated As of release 0.0.5, replaced by {@link #clear()}
     */
    @Deprecated
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

    /**
     * Returns the specified range of elements in the sorted set stored at key.
     * The elements are considered to be ordered from the lowest to the highest score.
     * Lexicographical order is used for elements with equal score.
     * Both start and stop are zero-based indexes
     *
     * @param start the index to start
     * @param end   the index to end
     * @return the Ranking
     */
    List<Ranking> range(long start, long end);

    /**
     * Returns the specified range of elements in the sorted set
     * stored at key. The elements are considered to be ordered from the
     * highest to the lowest score. Descending lexicographical order is used for elements with equal score.
     * Both start and stop are zero-based indexes
     *
     * @param start the index to start
     * @param end   the index to end
     * @return the Ranking
     */
    List<Ranking> revRange(long start, long end);

    /**
     * Returns all elements using {@link SortedSet#range(long, long)}
     *
     * @return the rankings
     * @see SortedSet#range(long, long)
     */
    List<Ranking> getRanking();

    /**
     * Returns all elements using {@link SortedSet#revRange(long, long)}
     *
     * @return the rankings
     * @see SortedSet#revRange(long, long)
     */
    List<Ranking> getRevRanking();

    /**
     * Removes all of the elements from this sortedset
     */
    void clear();
}