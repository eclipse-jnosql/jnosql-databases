/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.mapping;


import jakarta.inject.Inject;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.query.AbstractRepository;
import org.eclipse.jnosql.mapping.core.repository.DynamicReturn;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.eclipse.jnosql.mapping.semistructured.query.AbstractSemiStructuredRepositoryProxy;
import org.eclipse.jnosql.mapping.semistructured.query.SemiStructuredRepositoryProxy;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.eclipse.jnosql.mapping.core.repository.DynamicReturn.toSingleResult;

class OracleDocumentRepositoryProxy<T, K> extends AbstractSemiStructuredRepositoryProxy<T, K> {

    private final Class<T> typeClass;

    private final OracleNoSQLTemplate template;

    private final AbstractRepository<?, ?> repository;

    private final Class<?> type;

    private final Converters converters;

    private final EntityMetadata entityMetadata;

    @Inject
    OracleDocumentRepositoryProxy(OracleNoSQLTemplate template,
                                  Class<?> type,
                                  Converters converters,
                                  EntitiesMetadata entitiesMetadata) {
        this.template = template;
        this.typeClass = Class.class.cast(ParameterizedType.class.cast(type.getGenericInterfaces()[0])
                .getActualTypeArguments()[0]);
        this.type = type;
        this.converters = converters;
        this.entityMetadata = entitiesMetadata.get(typeClass);
        this.repository = SemiStructuredRepositoryProxy.SemiStructuredRepository.of(template, entityMetadata);
    }

    OracleDocumentRepositoryProxy() {
        this.typeClass = null;
        this.template = null;
        this.repository = null;
        this.type = null;
        this.converters = null;
        this.entityMetadata = null;
    }

    @Override
    protected AbstractRepository repository() {
        return repository;
    }

    @Override
    protected Class<?> repositoryType() {
        return type;
    }

    @Override
    protected Converters converters() {
        return converters;
    }

    @Override
    protected EntityMetadata entityMetadata() {
        return entityMetadata;
    }

    @Override
    protected DocumentTemplate template() {
        return template;
    }

    @Override
    public Object invoke(Object instance, Method method, Object[] args) throws Throwable {

        SQL sql = method.getAnnotation(SQL.class);
        if (Objects.nonNull(sql)) {
            Stream<T> result;
            List<Object> params = ParamUtil.getParams(args, method);
            if (params.isEmpty()) {
                result = template.sql(sql.value());
            } else {
                result = template.sql(sql.value(), params.toArray());
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
