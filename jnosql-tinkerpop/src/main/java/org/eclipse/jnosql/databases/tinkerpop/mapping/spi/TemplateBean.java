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
package org.eclipse.jnosql.databases.tinkerpop.mapping.spi;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.nosql.Template;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.databases.tinkerpop.mapping.GraphTemplateProducer;
import org.eclipse.jnosql.databases.tinkerpop.mapping.TinkerpopTemplate;
import org.eclipse.jnosql.mapping.DatabaseQualifier;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.eclipse.jnosql.mapping.core.spi.AbstractBean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

class TemplateBean extends AbstractBean<TinkerpopTemplate> {

    private static final Set<Type> TYPES = Set.of(TinkerpopTemplate.class, Template.class);

    private final String provider;

    private final Set<Annotation> qualifiers;

    /**
     * Constructor
     *
     * @param provider    the provider name, that must be a
     */
    public TemplateBean(String provider) {
        this.provider = provider;
        this.qualifiers = Collections.singleton(DatabaseQualifier.ofGraph(provider));
    }

    @Override
    public Class<?> getBeanClass() {
        return TinkerpopTemplate.class;
    }


    @Override
    public TinkerpopTemplate create(CreationalContext<TinkerpopTemplate> context) {

        GraphTemplateProducer producer = getInstance(GraphTemplateProducer.class);
        Graph graph = getGraph();
        return producer.apply(graph);
    }

    private Graph getGraph() {
        return getInstance(Graph.class, DatabaseQualifier.ofGraph(provider));
    }

    @Override
    public Set<Type> getTypes() {
        return TYPES;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }


    @Override
    public String getId() {
        return TinkerpopTemplate.class.getName() + DatabaseType.GRAPH + "-" + provider;
    }

}
