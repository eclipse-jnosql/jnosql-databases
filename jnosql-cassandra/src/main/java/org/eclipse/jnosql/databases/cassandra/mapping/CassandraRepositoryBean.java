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
package org.eclipse.jnosql.databases.cassandra.mapping;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.util.AnnotationLiteral;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.spi.AbstractBean;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;


class CassandraRepositoryBean<T, K> extends AbstractBean<CassandraRepository<T, K>> {

    private final Class<T> type;

    private final Set<Type> types;

    private final Set<Annotation> qualifiers = Collections.singleton(new AnnotationLiteral<Default>() {
    });

    CassandraRepositoryBean(Class<T> type) {
        this.type = type;
        this.types = Collections.singleton(type);
    }

    @Override
    public Class<?> getBeanClass() {
        return type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CassandraRepository<T, K> create(CreationalContext<CassandraRepository<T, K>> creationalContext) {
        CassandraTemplate template = getInstance(CassandraTemplate.class);
        Converters converters = getInstance(Converters.class);
        EntitiesMetadata entitiesMetadata = getInstance(EntitiesMetadata.class);
        CassandraRepositoryProxy<T, K> handler = new CassandraRepositoryProxy<>(template, type,
                converters, entitiesMetadata);
        return (CassandraRepository<T, K>) Proxy.newProxyInstance(type.getClassLoader(),
                new Class[]{type},
                handler);
    }


    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public String getId() {
        return type.getName() + "@cassandra";
    }

}