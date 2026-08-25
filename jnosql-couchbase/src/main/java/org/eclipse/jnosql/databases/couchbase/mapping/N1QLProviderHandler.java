/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.couchbase.mapping;

import com.couchbase.client.java.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.jnosql.mapping.ProviderQuery;
import org.eclipse.jnosql.mapping.core.repository.RepositoryMetadataUtils;
import org.eclipse.jnosql.mapping.metadata.repository.spi.ProviderQueryHandler;
import org.eclipse.jnosql.mapping.metadata.repository.spi.RepositoryInvocationContext;

import java.util.Map;
import java.util.stream.Stream;

@ApplicationScoped
@ProviderQuery("n1ql")
class N1QLProviderHandler implements ProviderQueryHandler {

    @Override
    public <T> T execute(RepositoryInvocationContext context) {
        var method = context.method();
        var parameters = context.parameters();
        var template = (CouchbaseTemplate) context.template();
        var sampleQueryProvider = method.annotations().stream()
                .filter(annotation -> N1QL.class.equals(annotation.annotation()))
                .findFirst().orElseThrow();

        Map<String, Object> attributes = sampleQueryProvider.attributes();
        var cql = (String) attributes.get("value");
        Map<String, Object> params = RepositoryMetadataUtils.INSTANCE.getParamsFromName(method, parameters);
        Stream<T> result;
        if (params.isEmpty()) {
            result = template.n1qlQuery(cql);
        } else {
            var jsonObject = JsonObject.create();
            params.forEach(jsonObject::put);
            result = template.n1qlQuery(cql, jsonObject);
        }
        return RepositoryMetadataUtils.INSTANCE.execute(context, result);
    }
}
