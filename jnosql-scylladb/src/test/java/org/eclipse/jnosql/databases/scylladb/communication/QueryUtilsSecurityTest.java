/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 */
package org.eclipse.jnosql.databases.scylladb.communication;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryUtilsSecurityTest {

    @Test
    void shouldQuoteCqlIdentifierPayload() {
        assertThat(QueryUtils.getName("name\" = 'x' WHERE token(id) > 0 --"))
                .isEqualTo("\"name\"\" = 'x' WHERE token(id) > 0 --\"");
    }

    @Test
    void shouldQuoteCountQueryIdentifiers() {
        assertThat(QueryUtils.count("users; DROP TABLE secrets", "tenant"))
                .isEqualTo("select count(*) from tenant.\"users; DROP TABLE secrets\"");
    }
}
