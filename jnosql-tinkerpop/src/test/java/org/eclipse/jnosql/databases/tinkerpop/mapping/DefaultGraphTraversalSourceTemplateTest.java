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
package org.eclipse.jnosql.databases.tinkerpop.mapping;

import jakarta.inject.Inject;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.databases.tinkerpop.mapping.spi.TinkerpopExtension;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, Transactional.class})
@AddPackages({MagazineRepository.class, Reflections.class, GraphProducer.class})
@AddExtensions({ReflectionEntityMetadataExtension.class, TinkerpopExtension.class})
class DefaultGraphTraversalSourceTemplateTest extends AbstractTinkerpopTemplateTest {

    @Inject
    private TinkerpopTemplate graphTemplate;

    @Inject
    private Graph graph;

    @Override
    protected Graph getGraph() {
        return graph;
    }

    @Override
    protected TinkerpopTemplate getGraphTemplate() {
        return graphTemplate;
    }
}
