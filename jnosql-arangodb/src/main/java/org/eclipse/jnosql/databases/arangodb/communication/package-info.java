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
 * <p>ArangoDB is a NoSQL multi-model database developed by triAGENS GmbH. It has been referred to as the most popular NoSQL database available that has an open source license.
 * It has also been referred to as a universal database. Its creators refer to it as a "native multi-model" database to indicate that it was designed specifically
 * to allow key/value, document, and graph data to be stored together and queried with a common language.</p>
 * <p>ArangoDB has a low resource consumption and high performance, as shown in the latest open-source NoSQL performance test.</p>
 * <p>ArangoDB provides scalable, highly efficient queries when working with graph data. The database uses JSON as a default storage format,
 * but internally it uses ArangoDB's VelocyPack - a fast and compact binary format for serialization and storage. ArangoDB can natively store a nested
 * JSON object as a data entry inside a collection. Therefore, there is no need to disassemble the resulting JSON objects.
 * Thus, the stored data would simply inherit the tree structure of the XML data.</p>
 * <p>ArangoDB works in a distributed cluster unlike some other existing graph databases and it is the first DBMS being
 * certified for the Distributed Cluster Operating System (DC/OS).[12] DC/OS allows to deploy ArangoDB on most of the existing ecosystems:
 * Amazon Web Services (AWS), Google Compute Engine and Microsoft Azure. Moreover, it also provides you a single click deployment in your own cluster.</p>
 * <p>ArangoDB provides native integration of the JavaScript microservices directly on top of the DBMS using the Foxx framework,
 * which is an analogue of the multithreaded NodeJS.</p>
 * <p>The database has both AQL query language and provides GraphQL to write flexible native web services directly on top of the DBMS</p>
 */
package org.eclipse.jnosql.databases.arangodb.communication;