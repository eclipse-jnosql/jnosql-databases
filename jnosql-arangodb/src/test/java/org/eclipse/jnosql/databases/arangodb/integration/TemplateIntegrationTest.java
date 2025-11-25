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


import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.databases.arangodb.communication.ArangoDBConfigurations;
import org.eclipse.jnosql.databases.arangodb.communication.DocumentDatabase;
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
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.databases.arangodb.integration.StepTransitionReason.REPEAT;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, DocumentTemplate.class})
@AddPackages(Magazine.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class TemplateIntegrationTest {

    @Inject
    private DocumentTemplate template;

    static {
        DocumentDatabase.INSTANCE.get("library");
        System.setProperty(ArangoDBConfigurations.HOST.get() + ".1", DocumentDatabase.INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "library");
    }

    @BeforeEach
    void setup() {
        template.deleteAll(WorkflowStep.class);
    }

    @Test
    void shouldInsert() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        template.insert(magazine);
        Optional<Magazine> optional = template.find(Magazine.class, magazine.id());
        assertThat(optional).isNotNull().isNotEmpty()
                .get().isEqualTo(magazine);
    }

    @Test
    void shouldUpdate() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        Magazine updated = new Magazine(magazine.id(), magazine.title() + " updated", 2);

        assertThat(template.update(updated))
                .isNotNull()
                .isNotEqualTo(magazine);

        assertThat(template.find(Magazine.class, magazine.id()))
                .isNotNull().get().isEqualTo(updated);

    }

    @Test
    void shouldFindById() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        assertThat(template.find(Magazine.class, magazine.id()))
                .isNotNull().get().isEqualTo(magazine);
    }

    @Test
    void shouldDelete() {
        Magazine magazine = new Magazine(randomUUID().toString(), "Effective Java", 1);
        assertThat(template.insert(magazine))
                .isNotNull()
                .isEqualTo(magazine);

        template.delete(Magazine.class, magazine.id());
        assertThat(template.find(Magazine.class, magazine.id()))
                .isNotNull().isEmpty();
    }

    @Test
    void shouldUpdateEmbeddable() {
        String id = UUID.randomUUID().toString();
        var workflowStep = WorkflowStep.builder()
                .id(id)
                .workflowSchemaKey("workflowSchemaKey")
                .stepName("stepName")
                .mainStepType(MainStepType.MAIN)
                .stepNo(1)
                .componentConfigurationKey("componentConfigurationKey")
                .relationTypeKey("relationTypeKey")
                .availableTransitions(List.of(new Transition("TEST_WORKFLOW_STEP_KEY", REPEAT,
                        null, List.of("ADMIN"))))
                .build();
        var result = this.template.insert(workflowStep);

        SoftAssertions.assertSoftly(soft ->{
            soft.assertThat(result).isNotNull();
            soft.assertThat(result.id()).isEqualTo(id);
            soft.assertThat(result.workflowSchemaKey()).isEqualTo("workflowSchemaKey");
            soft.assertThat(result.stepName()).isEqualTo("stepName");
            soft.assertThat(result.mainStepType()).isEqualTo(MainStepType.MAIN);
            soft.assertThat(result.stepNo()).isEqualTo(1);
            soft.assertThat(result.componentConfigurationKey()).isEqualTo("componentConfigurationKey");
            soft.assertThat(result.relationTypeKey()).isEqualTo("relationTypeKey");
            soft.assertThat(result.availableTransitions()).hasSize(1);
            soft.assertThat(result.availableTransitions().get(0).targetWorkflowStepKey()).isEqualTo("TEST_WORKFLOW_STEP_KEY");
            soft.assertThat(result.availableTransitions().get(0).stepTransitionReason()).isEqualTo(REPEAT);
            soft.assertThat(result.availableTransitions().get(0).mailTemplateKey()).isNull();
            soft.assertThat(result.availableTransitions().get(0).restrictedRoleGroups()).hasSize(1);
            soft.assertThat(result.availableTransitions().get(0).restrictedRoleGroups().get(0)).isEqualTo("ADMIN");
        });
    }

    @Test
    void shouldUpdateEmbeddable2() {
        String id = UUID.randomUUID().toString();
        var workflowStep = WorkflowStep.builder()
                .id(id)
                .workflowSchemaKey("workflowSchemaKey")
                .stepName("stepName")
                .mainStepType(MainStepType.MAIN)
                .stepNo(null)
                .componentConfigurationKey("componentConfigurationKey")
                .relationTypeKey("relationTypeKey")
                .availableTransitions(null)
                .build();
        var result = this.template.insert(workflowStep);

        SoftAssertions.assertSoftly(soft ->{
            soft.assertThat(result).isNotNull();
            soft.assertThat(result.id()).isEqualTo(id);
            soft.assertThat(result.workflowSchemaKey()).isEqualTo("workflowSchemaKey");
            soft.assertThat(result.stepName()).isEqualTo("stepName");
            soft.assertThat(result.mainStepType()).isEqualTo(MainStepType.MAIN);
            soft.assertThat(result.stepNo()).isNull();
            soft.assertThat(result.componentConfigurationKey()).isEqualTo("componentConfigurationKey");
            soft.assertThat(result.relationTypeKey()).isEqualTo("relationTypeKey");
            soft.assertThat(result.availableTransitions()).isNull();
        });
    }

    @Test
    void shouldExecuteWithoutKey(){
        var workflowStep = WorkflowStep.builder()
                .id("id")
                .workflowSchemaKey("workflowSchemaKey")
                .stepName("stepName")
                .mainStepType(MainStepType.MAIN)
                .stepNo(null)
                .componentConfigurationKey("componentConfigurationKey")
                .relationTypeKey("relationTypeKey")
                .availableTransitions(null)
                .build();
        var result = this.template.insert(workflowStep);

        SoftAssertions.assertSoftly(soft ->{
            soft.assertThat(result).isNotNull();
            soft.assertThat(result.id()).isEqualTo("id");
            soft.assertThat(result.workflowSchemaKey()).isEqualTo("workflowSchemaKey");
            soft.assertThat(result.stepName()).isEqualTo("stepName");
            soft.assertThat(result.mainStepType()).isEqualTo(MainStepType.MAIN);
            soft.assertThat(result.stepNo()).isNull();
            soft.assertThat(result.componentConfigurationKey()).isEqualTo("componentConfigurationKey");
            soft.assertThat(result.relationTypeKey()).isEqualTo("relationTypeKey");
            soft.assertThat(result.availableTransitions()).isNull();
        });
    }

    @Test
    void shouldExecuteWithoutKId(){
        var workflowStep = WorkflowStep.builder()
                .workflowSchemaKey("workflowSchemaKey")
                .stepName("stepName")
                .mainStepType(MainStepType.MAIN)
                .stepNo(null)
                .componentConfigurationKey("componentConfigurationKey")
                .relationTypeKey("relationTypeKey")
                .availableTransitions(null)
                .build();
        var result = this.template.insert(workflowStep);

        SoftAssertions.assertSoftly(soft ->{
            soft.assertThat(result).isNotNull();
            soft.assertThat(result.id()).isNotNull();
            soft.assertThat(result.workflowSchemaKey()).isEqualTo("workflowSchemaKey");
            soft.assertThat(result.stepName()).isEqualTo("stepName");
            soft.assertThat(result.mainStepType()).isEqualTo(MainStepType.MAIN);
            soft.assertThat(result.stepNo()).isNull();
            soft.assertThat(result.componentConfigurationKey()).isEqualTo("componentConfigurationKey");
            soft.assertThat(result.relationTypeKey()).isEqualTo("relationTypeKey");
            soft.assertThat(result.availableTransitions()).isNull();
        });
    }


}
