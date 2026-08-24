/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 * and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */

package org.eclipse.jnosql.databases.dynamodb.mapping;

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

class DynamoDBRepositoryBean<T, K> extends AbstractBean<DynamoDBRepository<T, K>> {

    private final Class<T> type;

    private final Set<Type> types;

    private final Set<Annotation> qualifiers = Collections.singleton(new AnnotationLiteral<Default>() {
    });

    @SuppressWarnings({"rawtypes", "unchecked"})
    DynamoDBRepositoryBean(Class type) {
        this.type = type;
        this.types = Collections.singleton(type);
    }

    @Override
    public Class<?> getBeanClass() {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DynamoDBRepository<T, K> create(CreationalContext<DynamoDBRepository<T, K>> creationalContext) {

        DynamoDBTemplate template = getInstance(DynamoDBTemplate.class);
        Converters converters = getInstance(Converters.class);
        EntitiesMetadata entitiesMetadata = getInstance(EntitiesMetadata.class);

        DynamoDBRepositoryProxy<T, K> handler = new DynamoDBRepositoryProxy<>(
                template, type, converters, entitiesMetadata);

        return (DynamoDBRepository<T, K>) Proxy.newProxyInstance(type.getClassLoader(),
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
        return type.getName() + '@' + "dynamodb";
    }

}
