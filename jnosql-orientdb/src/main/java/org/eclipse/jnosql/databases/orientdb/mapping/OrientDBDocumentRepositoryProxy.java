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
package org.eclipse.jnosql.databases.orientdb.mapping;


import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.query.AbstractRepository;
import org.eclipse.jnosql.mapping.core.repository.DynamicReturn;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.driver.ParamUtil;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.eclipse.jnosql.mapping.semistructured.query.AbstractSemiStructuredRepositoryProxy;
import org.eclipse.jnosql.mapping.semistructured.query.SemiStructuredRepositoryProxy;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.eclipse.jnosql.mapping.core.repository.DynamicReturn.toSingleResult;


class OrientDBDocumentRepositoryProxy<T, K> extends AbstractSemiStructuredRepositoryProxy<T, K> {

    private final Class<T> typeClass;

    private final OrientDBTemplate template;

    private final AbstractRepository<T, K> repository;

    private final Class<?> repositoryType;

    private final Converters converters;

    private final EntitiesMetadata entitiesMetadata;

    private final EntityMetadata entityMetadata;


    OrientDBDocumentRepositoryProxy(OrientDBTemplate template, Class<?> repositoryType,
                                    Converters converters,
                                    EntitiesMetadata entitiesMetadata) {
        this.template = template;
        this.typeClass = Class.class.cast(ParameterizedType.class.cast(repositoryType.getGenericInterfaces()[0])
                .getActualTypeArguments()[0]);
        this.repositoryType = repositoryType;
        this.converters = converters;
        this.entitiesMetadata = entitiesMetadata;
        this.entityMetadata = entitiesMetadata.get(typeClass);
        this.repository = SemiStructuredRepositoryProxy.SemiStructuredRepository.of(template, entityMetadata);
    }


    @Override
    protected AbstractRepository<T, K>  repository() {
        return repository;
    }

    @Override
    protected Class<?> repositoryType() {
        return repositoryType;
    }

    @Override
    protected Converters converters() {
        return converters;
    }

    @Override
    protected EntitiesMetadata entitiesMetadata() {
        return entitiesMetadata;
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

            if (args == null || args.length == 0) {
                result = template.sql(sql.value());
            } else {
                Map<String, Object> params = ParamUtil.INSTANCE.getParams(args, method);
                if (params.isEmpty()) {
                    result = template.sql(sql.value(), args);
                } else {
                    result = template.sql(sql.value(), params);
                }
            }
            return DynamicReturn.builder()
                    .classSource(typeClass)
                    .methodSource(method).result(() -> result)
                    .singleResult(toSingleResult(method).apply(() -> result))
                    .build().execute();
        }
        return super.invoke(instance, method, args);
    }



}
