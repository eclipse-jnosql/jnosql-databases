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
package org.eclipse.jnosql.databases.hazelcast.mapping;

import org.eclipse.jnosql.mapping.core.query.AbstractRepository;
import org.eclipse.jnosql.mapping.core.repository.DynamicReturn;
import org.eclipse.jnosql.mapping.driver.ParamUtil;
import org.eclipse.jnosql.mapping.keyvalue.KeyValueTemplate;
import org.eclipse.jnosql.mapping.keyvalue.query.AbstractKeyValueRepositoryProxy;
import org.eclipse.jnosql.mapping.keyvalue.query.DefaultKeyValueRepository;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.eclipse.jnosql.mapping.core.repository.DynamicReturn.toSingleResult;

class HazelcastRepositoryProxy<T, K> extends AbstractKeyValueRepositoryProxy<T, K> {

    private final HazelcastTemplate template;

    private final AbstractRepository<T, K> repository;

    private final Class<T> typeClass;

    private final Class<?> repositoryType;

    private final EntityMetadata metadata;



    HazelcastRepositoryProxy(HazelcastTemplate template, Class<?> repositoryType, EntitiesMetadata entitiesMetadata) {
        this.template = template;
        this.typeClass = Class.class.cast(ParameterizedType.class.cast(repositoryType.getGenericInterfaces()[0])
                .getActualTypeArguments()[0]);
        this.metadata = entitiesMetadata.get(typeClass);
        this.repository = DefaultKeyValueRepository.of(template, metadata);
        this.repositoryType = repositoryType;
    }

    @Override
    protected AbstractRepository<T, K> repository() {
        return repository;
    }

    @Override
    protected KeyValueTemplate template() {
        return template;
    }

    @Override
    protected Class<T> type() {
        return typeClass;
    }

    @Override
    protected Class<?> repositoryType() {
        return repositoryType;
    }

    @Override
    protected EntityMetadata entityMetadata() {
        return metadata;
    }

    @Override
    public Object invoke(Object instance, Method method, Object[] args) throws Throwable {

        Query query = method.getAnnotation(Query.class);
        if (Objects.nonNull(query)) {
            Stream<T> result;
            Map<String, Object> params = ParamUtil.INSTANCE.getParams(args, method);
            if (params.isEmpty()) {
                result = template.<T>sql(query.value()).stream();
            } else {
                result = template.<T>sql(query.value(), params).stream();
            }

            return DynamicReturn.builder()
                    .classSource(typeClass)
                    .methodSource(method)
                    .result(() -> result)
                    .singleResult(toSingleResult(method).apply(() -> result))
                    .build().execute();
        }
        return super.invoke(instance, method, args);
    }


}
