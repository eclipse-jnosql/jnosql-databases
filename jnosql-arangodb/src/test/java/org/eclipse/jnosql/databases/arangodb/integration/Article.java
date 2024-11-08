/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.arangodb.integration;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.Objects;

@Entity
public class Article {

    @Id("_key")
    private String id;

    @Column("title")
    private String title;

    @Column("edition")
    private int edition;

    public Article(String id, String title, int edition) {
        this.id = id;
        this.title = title;
        this.edition = edition;
    }

    Article() {
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public int edition() {
        return edition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Article magazine = (Article) o;
        return edition == magazine.edition
                && Objects.equals(id, magazine.id)
                && Objects.equals(title, magazine.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, edition);
    }

    @Override
    public String toString() {
        return "Book{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", edition=" + edition +
                '}';
    }
}
