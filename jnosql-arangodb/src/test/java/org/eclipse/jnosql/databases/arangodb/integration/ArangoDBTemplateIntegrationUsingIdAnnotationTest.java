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


import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.databases.arangodb.communication.ArangoDBConfigurations;
import org.eclipse.jnosql.databases.arangodb.mapping.ArangoDBTemplate;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.databases.arangodb.communication.DocumentDatabase.INSTANCE;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(Article.class)
@AddPackages(ArangoDBTemplate.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class ArangoDBTemplateIntegrationUsingIdAnnotationTest {

    @Inject
    private ArangoDBTemplate template;

    static {
        INSTANCE.get("library");
        System.setProperty(ArangoDBConfigurations.HOST.get() + ".1", INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "library");
    }

    @BeforeEach
    void setUp() {
        this.template.delete(Article.class).execute();
    }

    @Test
    void shouldInsert() {
        var article = new Article(randomUUID().toString(), "Effective Java", 1);
        template.insert(article);
        Optional<Article> optional = template.find(Article.class, article.id());
        assertThat(optional).isNotNull().isNotEmpty()
                .get().isEqualTo(article);
    }

    @Test
    void shouldUpdate() {
        var article = new Article(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(article))
                .isNotNull()
                .isEqualTo(article);

         Article updated = new Article(article.id(), article.title() + " updated", 2);

        assertThat(template.update(updated))
                .isNotNull()
                .isNotEqualTo(article);

        assertThat(template.find(Article.class, article.id()))
                .isNotNull().get().isEqualTo(updated);

    }

    @Test
    void shouldFindById() {
        Article article = new Article(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(article))
                .isNotNull()
                .isEqualTo(article);

        assertThat(template.find(Article.class, article.id()))
                .isNotNull().get().isEqualTo(article);
    }

    @Test
    void shouldDelete() {
        var article = new Article(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(article))
                .isNotNull()
                .isEqualTo(article);

        template.delete(Article.class, article.id());
        assertThat(template.find(Article.class, article.id()))
                .isNotNull().isEmpty();
    }

    @Test
    void shouldDeleteAll() {
        for (int index = 0; index < 20; index++) {
            var article = new Article(randomUUID().toString(), "Effective Java", 1);
            assertThat(template.insert(article))
                    .isNotNull()
                    .isEqualTo(article);
        }

        template.delete(Article.class).execute();
        assertThat(template.select(Article.class).result()).isEmpty();
    }


    @Test
    void shouldUpdateNullValues() {
        var article = new Article(randomUUID().toString(), "Effective Java", 1);
        template.insert(article);
        template.update(new Article(article.id(), null, 2));
        Optional<Article> optional = template.select(Article.class).where("_key")
                .eq(article.id()).singleResult();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(optional).isPresent();
            softly.assertThat(optional).get().extracting(Article::title).isNull();
            softly.assertThat(optional).get().extracting(Article::edition).isEqualTo(2);
        });
    }

    @Test
    void shouldExecuteLimit() {

        for (int index = 1; index < 10; index++) {
            var article = new Article(randomUUID().toString(), "Effective Java", index);
            template.insert(article);
        }

        List<Article> articles = template.select(Article.class).orderBy("edition")
                .asc().limit(4).result();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(articles).hasSize(4);
            var editions = articles.stream().map(Article::edition).toList();
            soft.assertThat(editions).hasSize(4).contains(1, 2, 3, 4);
        });

    }

    @Test
    void shouldExecuteSkip() {
        for (int index = 1; index < 10; index++) {
            var book = new Article(randomUUID().toString(), "Effective Java", index);
            template.insert(book);
        }

        List<Article> articles = template.select(Article.class).orderBy("edition")
                .asc().skip(4).result();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(articles).hasSize(5);
            var editions = articles.stream().map(Article::edition).toList();
            soft.assertThat(editions).hasSize(5).contains(5, 6, 7, 8, 9);
        });
    }

    @Test
    void shouldExecuteLimitStart() {
        for (int index = 1; index < 10; index++) {
            var article = new Article(randomUUID().toString(), "Effective Java", index);
            template.insert(article);
        }

        List<Article> articles = template.select(Article.class).orderBy("edition")
                .asc().skip(4).limit(3).result();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(articles).hasSize(3);
            var editions = articles.stream().map(Article::edition).toList();
            soft.assertThat(editions).hasSize(3).contains(5, 6, 7);
        });
    }

    @Test
    void shouldSelectCursorSize() {
        for (int index = 1; index < 10; index++) {
            var article = new Article(randomUUID().toString(), "Effective Java", index);
            template.insert(article);
        }
        var select = SelectQuery.select().from("Article").orderBy("edition").asc()
                .skip(4).limit(3).build();
        var pageRequest = PageRequest.ofSize(3);
        CursoredPage<Article> entities = template.selectCursor(select, pageRequest);

        SoftAssertions.assertSoftly(soft -> {
            var content = entities.content();
            soft.assertThat(content).hasSize(3);
            var editions = content.stream().map(Article::edition).toList();
            soft.assertThat(editions).hasSize(3).contains(1, 2, 3);
        });
    }
}
