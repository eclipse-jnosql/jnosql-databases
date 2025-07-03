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
package org.eclipse.jnosql.databases.arangodb.mapping;


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

import static java.util.Collections.emptyMap;
import static org.eclipse.jnosql.mapping.core.repository.DynamicReturn.toSingleResult;

class ArangoDBDocumentRepositoryProxy<T, K> extends AbstractSemiStructuredRepositoryProxy<T, K> {

    private final Class<T> typeClass;

    private final ArangoDBTemplate template;

    private final AbstractRepository<?, ?> repository;

    private final Class<?> type;

    private final Converters converters;

    private final EntityMetadata entityMetadata;

    private final EntitiesMetadata entitiesMetadata;

    ArangoDBDocumentRepositoryProxy(ArangoDBTemplate template,
                                    Class<?> type,
                                    Converters converters,
                                    EntitiesMetadata entitiesMetadata) {
        this.template = template;
        this.typeClass = Class.class.cast(ParameterizedType.class.cast(type.getGenericInterfaces()[0])
                .getActualTypeArguments()[0]);
        this.type = type;
        this.converters = converters;
        this.entitiesMetadata = entitiesMetadata;
        this.entityMetadata = entitiesMetadata.get(typeClass);
        this.repository = SemiStructuredRepositoryProxy.SemiStructuredRepository.of(template, entityMetadata);
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
    protected EntitiesMetadata entitiesMetadata() {
        return this.entitiesMetadata;
    }

    @Override
    protected DocumentTemplate template() {
        return template;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object instance, Method method, Object[] args) throws Throwable {

        AQL aql = method.getAnnotation(AQL.class);
        if (Objects.nonNull(aql)) {
            Stream<T> result;
            Map<String, Object> params = ParamUtil.INSTANCE.getParams(args, method);
            if (params.isEmpty()) {
                result = template.aql(aql.value(), emptyMap());
            } else {
                result = template.aql(aql.value(), params);
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
