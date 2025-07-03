/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.dynamodb.mapping.inheritance;

import jakarta.nosql.Column;
import jakarta.nosql.DiscriminatorValue;
import jakarta.nosql.Entity;

import java.util.Objects;
import java.util.UUID;

@Entity
@DiscriminatorValue("big_project")
public class BigProject extends Project {

    @Column
    private String bigProjectName;

    public static BigProject of(String name, String bigProjectName) {
        return of(UUID.randomUUID().toString(), name, bigProjectName);
    }

    public static BigProject of(String id, String name, String bigProjectName) {
        return new BigProject(id, name, bigProjectName);
    }

    public BigProject() {}

    public BigProject(String id, String name, String bigProjectName) {
        super(id, name);
        this.bigProjectName = bigProjectName;
    }

    public String getBigProjectName() {
        return bigProjectName;
    }

    public void setBigProjectName(String bigProjectName) {
        this.bigProjectName = bigProjectName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass().hashCode(), super.hashCode());
    }

}
