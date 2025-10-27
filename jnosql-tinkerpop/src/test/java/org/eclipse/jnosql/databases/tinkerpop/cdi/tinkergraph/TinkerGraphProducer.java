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
 *   Michele Rastelli
 */
package org.eclipse.jnosql.databases.tinkerpop.cdi.tinkergraph;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.databases.tinkerpop.cdi.TestGraphSupplier;

import java.util.function.Supplier;


@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
public class TinkerGraphProducer implements Supplier<Graph> {

    @Produces
    @ApplicationScoped
    @Override
    public Graph get() {
        return TestGraphSupplier.TINKER_GRAPH.get();
    }

    public void dispose(@Disposes Graph graph) throws Exception {
        graph.close();
    }

}
