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

import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBConfigurations;
import org.eclipse.jnosql.databases.dynamodb.communication.DynamoDBTestUtils;
import org.eclipse.jnosql.databases.dynamodb.mapping.DynamoDBExtension;
import org.eclipse.jnosql.databases.dynamodb.mapping.DynamoDBTemplate;
import org.eclipse.jnosql.databases.dynamodb.mapping.PartiQL;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@EnableAutoWeld
@AddPackages({Database.class,
        EntityConverter.class,
        DocumentTemplate.class,
        DynamoDBTemplate.class})
@AddPackages(PartiQL.class)
@AddPackages(Project.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, DocumentExtension.class, DynamoDBExtension.class})
public class InheritanceWithCustomRepositoryTest {

    static {
        DynamoDBTestUtils.CONFIG.setupSystemProperties(Settings.builder()
                .put(DynamoDBConfigurations.CREATE_TABLES, "true"));
    }

    @Inject
    @Database(DatabaseType.DOCUMENT)
    Projects projects;

    @BeforeEach
    @AfterEach
    void clearData() {
        projects.findAll().forEach(projects::delete);
    }

    @Test
    void shouldInsert() {
        List<SmallProject> smallProjectList = new ArrayList<>();
        SmallProject smallProjectA = Project.smallProject("s-project A", "Small Project A");
        smallProjectList.add(smallProjectA);
        smallProjectList.add(Project.smallProject("s-project B", "Small Project B"));
        smallProjectList.add(Project.smallProject("s-project C", "Small Project C"));

        smallProjectList.forEach(projects::save);

        List<BigProject> bigProjectList = new ArrayList<>();
        bigProjectList.add(Project.bigProject("b-project A", "Big Project A"));
        BigProject bigProjectB = Project.bigProject("b-project B", "Big Project B");
        bigProjectList.add(bigProjectB);

        bigProjectList.forEach(projects::save);

        SoftAssertions.assertSoftly(softly -> {

            softly.assertThat(projects.findById(smallProjectA.getId()))
                    .isNotEmpty()
                    .contains(smallProjectA);

            softly.assertThat(projects.findById(bigProjectB.getId()))
                    .isNotEmpty()
                    .contains(bigProjectB);

            softly.assertThat(projects.findAllSmallProjects())
                    .isNotEmpty()
                    .hasSize(smallProjectList.size())
                    .containsExactlyInAnyOrderElementsOf(smallProjectList);

            softly.assertThat(projects.findAllBigProjects())
                    .isNotEmpty()
                    .hasSize(bigProjectList.size())
                    .containsExactlyInAnyOrderElementsOf(bigProjectList);

            Stream<Project> all = projects.findAll();
            softly.assertThat(all)
                    .isNotEmpty()
                    .hasSize(smallProjectList.size() + bigProjectList.size())
                    .containsExactlyInAnyOrderElementsOf(Stream.concat(smallProjectList.stream(), bigProjectList.stream()).toList());

        });


    }


}

