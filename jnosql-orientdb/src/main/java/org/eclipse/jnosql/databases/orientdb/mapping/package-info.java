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

/**
 * OrientDB is an open source NoSQL database management system written in Java. It is a multi-model database,
 * supporting graph, document, key/value, and object models, but the relationships are managed as in graph databases
 * with direct connections between records. It supports schema-less, schema-full and schema-mixed modes.
 * It has a strong security profiling system based on users and roles and supports querying with Gremlin
 * along with SQL extended for graph traversal. OrientDB uses several indexing mechanisms based
 * on B-tree and Extendible hashing, the last one is known as "hash index", there are plans to implement
 * LSM-tree and Fractal tree index based indexes. Each record has Surrogate key which indicates position
 * of record inside of Array list , links between records are stored either as single value of record's
 * position stored inside of referrer or as B-tree of record positions (so-called record IDs or RIDs)
 * which allows fast traversal (with O(1) complexity) of one-to-many relationships and fast addition/removal
 * of new links. OrientDB is the second most popular graph database according to the DB-Engines graph database ranking.
 */
package org.eclipse.jnosql.databases.orientdb.mapping;