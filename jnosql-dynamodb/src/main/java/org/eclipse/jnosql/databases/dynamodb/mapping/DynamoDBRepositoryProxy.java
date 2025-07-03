/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */

package org.eclipse.jnosql.databases.dynamodb.mapping;

import jakarta.data.repository.Param;
import jakarta.inject.Inject;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.query.AbstractRepository;
import org.eclipse.jnosql.mapping.core.repository.DynamicReturn;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.eclipse.jnosql.mapping.semistructured.SemiStructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.query.AbstractSemiStructuredRepositoryProxy;
import org.eclipse.jnosql.mapping.semistructured.query.SemiStructuredRepositoryProxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.eclipse.jnosql.mapping.core.repository.DynamicReturn.toSingleResult;

class DynamoDBRepositoryProxy<T, K> extends AbstractSemiStructuredRepositoryProxy<T, K> {

    private final DynamoDBTemplate template;

    private final Class<?> type;

    private final Converters converters;

    private final Class<T> typeClass;

    private final EntitiesMetadata entitiesMetadata;

    private final EntityMetadata entityMetadata;

    private final AbstractRepository<?, ?> repository;

    @Inject
    @SuppressWarnings("unchecked")
    DynamoDBRepositoryProxy(DynamoDBTemplate template,
                            Class<?> type,
                            Converters converters,
                            EntitiesMetadata entitiesMetadata) {

        this.template = template;
        this.type = type;
        this.typeClass = (Class<T>) ((ParameterizedType) type.getGenericInterfaces()[0]).getActualTypeArguments()[0];
        this.converters = converters;
        this.entitiesMetadata = entitiesMetadata;
        this.entityMetadata = entitiesMetadata.get(typeClass);
        this.repository = SemiStructuredRepositoryProxy.SemiStructuredRepository.of(template, entityMetadata);
    }

    /**
     * Required by CDI/Reflection/Test purposes
     * Don't use it
     */
    DynamoDBRepositoryProxy() {
        this.template = null;
        this.type = null;
        this.typeClass = null;
        this.converters = null;
        this.entitiesMetadata = null;
        this.entityMetadata = null;
        this.repository = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object instance, Method method, Object[] args) throws Throwable {
        PartiQL sql = method.getAnnotation(PartiQL.class);
        if (Objects.nonNull(sql)) {

            Class<T> returnType = (Class<T>) method.getReturnType();
            List<Object> params = getParams(args, method);
            Supplier<Stream<T>> resultSupplier = () -> template.partiQL(sql.value(), typeClass, params.toArray());

            return DynamicReturn.builder()
                    .classSource(typeClass)
                    .methodSource(method)
                    .result(resultSupplier)
                    .singleResult(toSingleResult(method).apply(resultSupplier::get))
                    .build().execute();
        }
        return super.invoke(instance, method, args);
    }

    private List<Object> getParams(Object[] args, Method method) {

        List<Object> params = new ArrayList<>();
        Annotation[][] annotations = method.getParameterAnnotations();

        for (int index = 0; index < annotations.length; index++) {

            final Object arg = args[index];

            Optional<Param> param = Stream.of(annotations[index])
                    .filter(Param.class::isInstance)
                    .map(Param.class::cast)
                    .findFirst();
            param.ifPresent(p -> params.add(arg));

        }

        return params;
    }

    @Override
    protected Converters converters() {
        return converters;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected AbstractRepository repository() {
        return repository;
    }

    @Override
    protected Class<?> repositoryType() {
        return type;
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
    protected SemiStructuredTemplate template() {
        return template;
    }
}
