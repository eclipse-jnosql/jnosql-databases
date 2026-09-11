/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 */
package org.eclipse.jnosql.databases.elasticsearch.communication;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryConverterSecurityTest {

    @Test
    void shouldEscapeQueryStringOperatorsInWildcardValues() {
        String payload = "* OR _exists_:secret ?";

        assertThat(QueryConverter.wildcardLiteral(payload))
                .isEqualTo("\\* OR _exists_:secret \\?");
        assertThat(QueryConverter.likePattern("%admin_" + payload))
                .isEqualTo("*admin?\\* OR ?exists?:secret \\?");
    }
}
