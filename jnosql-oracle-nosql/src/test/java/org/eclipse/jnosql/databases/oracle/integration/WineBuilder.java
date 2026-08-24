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
package org.eclipse.jnosql.databases.oracle.integration;

import java.util.List;
import java.util.Map;

public class WineBuilder {
    private String id;
    private List<String> comments;
    private List<Crew> crew;
    private Map<String, Object> data;

    public WineBuilder id(String id) {
        this.id = id;
        return this;
    }

    public WineBuilder comments(List<String> comments) {
        this.comments = comments;
        return this;
    }

    public WineBuilder crew(List<Crew> crew) {
        this.crew = crew;
        return this;
    }

    public WineBuilder data(Map<String, Object> data) {
        this.data = data;
        return this;
    }

    public Wine build() {
        return new Wine(id, comments, crew, data);
    }
}