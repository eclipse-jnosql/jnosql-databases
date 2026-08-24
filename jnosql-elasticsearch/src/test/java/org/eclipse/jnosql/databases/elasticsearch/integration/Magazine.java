/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.elasticsearch.integration;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.Objects;
import java.util.UUID;

@Entity
public final class Magazine {
    @Id
    private final String id;
    @Column("title")
    private final String title;
    @Column("edition")
    private final int edition;
    @Column("author")
    private final Author author;

    public Magazine(
            @Id String id,
            @Column("title") String title,
            @Column("edition") int edition,
            @Column("author") Author author) {
        this.id = id;
        this.title = title;
        this.edition = edition;
        this.author = author;
    }

    public Magazine newEdition() {
        return new Magazine(UUID.randomUUID().toString(),
                this.title,
                this.edition + 1,
                this.author);
    }

    public Magazine updateEdition(int edition) {
        return new Magazine(this.id,
                this.title,
                edition,
                this.author);
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

    public Author author() {
        return author;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Magazine) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.title, that.title) &&
                this.edition == that.edition &&
                Objects.equals(this.author, that.author);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, edition, author);
    }

    @Override
    public String toString() {
        return "Book[" +
                "id=" + id + ", " +
                "title=" + title + ", " +
                "edition=" + edition + ", " +
                "author=" + author + ']';
    }


}
