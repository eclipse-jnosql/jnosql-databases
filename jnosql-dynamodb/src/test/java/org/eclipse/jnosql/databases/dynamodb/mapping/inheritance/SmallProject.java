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
@DiscriminatorValue("small_project")
public class SmallProject extends Project {

    @Column
    private String smallProjectName;

    public static SmallProject of(String name, String smallProjectName) {
        return of(UUID.randomUUID().toString(), name, smallProjectName);
    }

    public static SmallProject of(String id, String name, String smallProjectName) {
        return new SmallProject(id, name, smallProjectName);
    }

    public SmallProject() {
    }

    public SmallProject(String id, String name, String smallProjectName) {
        super(id, name);
        this.smallProjectName = smallProjectName;
    }

    public String getSmallProjectName() {
        return smallProjectName;
    }

    public void setSmallProjectName(String smallProjectName) {
        this.smallProjectName = smallProjectName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SmallProject that = (SmallProject) o;
        return Objects.equals(this.getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass().hashCode(), super.hashCode());
    }
}
