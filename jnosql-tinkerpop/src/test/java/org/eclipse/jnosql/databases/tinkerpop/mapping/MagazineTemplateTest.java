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
package org.eclipse.jnosql.databases.tinkerpop.mapping;

import jakarta.inject.Inject;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Transaction.Status;
import org.eclipse.jnosql.databases.tinkerpop.cdi.arangodb.ArangoDBGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.neo4j.Neo4jGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.tinkergraph.TinkerGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Magazine;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.MagazineTemplate;
import org.eclipse.jnosql.databases.tinkerpop.mapping.spi.TinkerpopExtension;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.apache.tinkerpop.gremlin.structure.Transaction.Status.COMMIT;
import static org.apache.tinkerpop.gremlin.structure.Transaction.Status.ROLLBACK;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, TinkerpopTemplate.class})
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TinkerpopExtension.class})
abstract class MagazineTemplateTest {

    @AddPackages(ArangoDBGraphProducer.class)
    static class ArangoDBTest extends MagazineTemplateTest {
    }

    @AddPackages(Neo4jGraphProducer.class)
    static class Neo4jTest extends MagazineTemplateTest {
    }

    @AddPackages(TinkerGraphProducer.class)
    static class TinkerGraphTest extends MagazineTemplateTest {
    }

    @Inject
    private MagazineTemplate template;

    @Inject
    private Graph graph;

    @Test
    void shouldSaveWithTransaction() {
        assumeTrue("transactions not supported", graph.features().graph().supportsTransactions());

        AtomicReference<Status> status = new AtomicReference<>();

        Magazine magazine = Magazine.builder().withName("The Book").build();
        Transaction transaction = graph.tx();
        transaction.addTransactionListener(status::set);
        template.insert(magazine);
        assertFalse(transaction.isOpen());
        assertEquals(COMMIT, status.get());
    }

    @Test
    void shouldSaveWithRollback() {
        assumeTrue("transactions not supported", graph.features().graph().supportsTransactions());

        AtomicReference<Status> status = new AtomicReference<>();

        Magazine magazine = Magazine.builder().withName("The Book").build();
        Transaction transaction = graph.tx();
        transaction.addTransactionListener(status::set);
        try {
            template.insertException(magazine);
            assert false;
        }catch (Exception ignored){

        }

        assertFalse(transaction.isOpen());
        assertEquals(ROLLBACK, status.get());
    }

    @Test
    void shouldUseAutomaticNormalTransaction() {
        assumeTrue("transactions not supported", graph.features().graph().supportsTransactions());

        AtomicReference<Status> status = new AtomicReference<>();

        Magazine magazine = Magazine.builder().withName("The Book").build();
        Transaction transaction = graph.tx();
        transaction.addTransactionListener(status::set);
        assertNull(status.get());
        template.normalInsertion(magazine);
        assertEquals(COMMIT, status.get());
        assertFalse(transaction.isOpen());
    }
}

