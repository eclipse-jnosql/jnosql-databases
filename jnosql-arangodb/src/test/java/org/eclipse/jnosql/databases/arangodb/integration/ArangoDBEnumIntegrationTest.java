/*
 *   Copyright (c) 2025 Contributors to the Eclipse Foundation
 *    All rights reserved. This program and the accompanying materials
 *    are made available under the terms of the Eclipse Public License v1.0
 *    and Apache License v2.0 which accompanies this distribution.
 *    The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *    and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *    You may elect to redistribute this code under either of these licenses.
 *
 *    Contributors:
 *
 *    Otavio Santana
 */
package org.eclipse.jnosql.databases.arangodb.integration;


import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.databases.arangodb.communication.ArangoDBConfigurations;
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
import java.util.function.Predicate;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.databases.arangodb.communication.DocumentDatabase.INSTANCE;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(Magazine.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class ArangoDBEnumIntegrationTest {


    static {
        INSTANCE.get("library");
        System.setProperty(ArangoDBConfigurations.HOST.get() + ".1", INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "library");
    }

    @Inject
    private DocumentTemplate template;


    @Inject
    private MailTemplateRepository repository;

    @BeforeEach
    void setup() {
        template.deleteAll(MailTemplate.class);
    }

    @Test
    void shouldCreateAndQueryByEnumAndDefaultTrue() {
        template.insert(MailTemplate.builder()
                .category(MailCategory.TIMER)
                .from("otavio@email.com")
                .to("mateusz@email.com")
                .isDefault(true)
                .build());

        List<MailTemplate> result = template.select(MailTemplate.class) .where("category")
                .eq(MailCategory.TIMER).result();

        SoftAssertions.assertSoftly(soft -> {
            Predicate<MailTemplate> isTimer = m -> m.getCategory().equals(MailCategory.TIMER);
            Predicate<MailTemplate> isTrue = m -> m.isDefault();
            soft.assertThat(result).allMatch(isTimer.and(isTrue));
        });
    }

    @Test
    void shouldCreateAndQueryByEnumAndDefaultFalse() {
        var emailTemplate = MailTemplate.builder()
                .category(MailCategory.TRANSITION_ALTERNATIVE)
                .from("otavio@email.com")
                .to("mateusz@email.com")
                .isDefault(false)
                .build();

        template.insert(emailTemplate);

        List<MailTemplate> result = template.select(MailTemplate.class)
                .where("category")
                .eq(MailCategory.TRANSITION_ALTERNATIVE)
                .and("isDefault")
                .eq(false).result();

        SoftAssertions.assertSoftly(soft -> {
            Predicate<MailTemplate> isAlternative = m -> m.getCategory().equals(MailCategory.TRANSITION_ALTERNATIVE);
            Predicate<MailTemplate> isFalse = m -> !m.isDefault();
            soft.assertThat(result).hasSize(1).allMatch(
                    isAlternative.and(isFalse));
        });
    }

    @Test
    void shouldQueryUsingRepository() {
        repository.save(MailTemplate.builder()
                .category(MailCategory.TIMER)
                .from("otavio@email.com")
                .to("mateusz@email.com")
                .isDefault(true)
                .build());

        repository.save(MailTemplate.builder()
                .category(MailCategory.TRANSITION_ALTERNATIVE)
                .from("otavio@email.com")
                .to("mateusz@email.com")
                .isDefault(true)
                .build());

        List<MailTemplate> categoryAndIsDefaultTrue = repository.findByCategoryAndIsDefaultTrue(MailCategory.TIMER);

        SoftAssertions.assertSoftly(soft -> {
            Predicate<MailTemplate> isTimer = m -> m.getCategory().equals(MailCategory.TIMER);
            Predicate<MailTemplate> isTrue = m -> m.isDefault();
            soft.assertThat(categoryAndIsDefaultTrue).hasSize(1).allMatch(
                    isTimer.and(isTrue));
        });
    }
}
