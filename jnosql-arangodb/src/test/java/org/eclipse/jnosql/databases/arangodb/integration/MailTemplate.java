/*
 *   Copyright (c) 2025 Contributors to the Eclipse Foundation
 *    All rights reserved. This program and the accompanying materials
 *    are made available under the terms of the Eclipse Public License 2.0
 *    and Apache License v2.0 which accompanies this distribution.
 *    The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *    and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *    You may elect to redistribute this code under either of these licenses.
 *
 *    Contributors:
 *
 *    Otavio Santana
 */
package org.eclipse.jnosql.databases.arangodb.integration;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.Objects;
import java.util.UUID;

@Entity
public class MailTemplate {

    @Id
    private String id;

    @Column
    private String to;

    @Column
    private String from;

    @Column
    private MailCategory category;

    @Column
    private boolean isDefault;

    public String getId() {
        return id;
    }

    public String getTo() {
        return to;
    }

    public String getFrom() {
        return from;
    }

    public MailCategory getCategory() {
        return category;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MailTemplate that = (MailTemplate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "MailTemplate{" +
                "id=" + id +
                ", to='" + to + '\'' +
                ", from='" + from + '\'' +
                ", category=" + category +
                ", isDefault=" + isDefault +
                '}';
    }

    public static MailTemplateBuilder builder(){
        return new MailTemplateBuilder();
    }


    public static class MailTemplateBuilder{

        private String to;

        private String from;

        private MailCategory category;

        private boolean isDefault;

        public MailTemplateBuilder to(String to) {
            this.to = to;
            return this;
        }

        public MailTemplateBuilder from(String from) {
            this.from = from;
            return this;
        }

        public MailTemplateBuilder category(MailCategory category) {
            this.category = category;
            return this;
        }

        public MailTemplateBuilder isDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public MailTemplate build(){
            MailTemplate mailTemplate = new MailTemplate();
            mailTemplate.id = UUID.randomUUID().toString();
            mailTemplate.to = this.to;
            mailTemplate.from = this.from;
            mailTemplate.category = this.category;
            mailTemplate.isDefault = this.isDefault;
            return mailTemplate;
        }
    }
}
