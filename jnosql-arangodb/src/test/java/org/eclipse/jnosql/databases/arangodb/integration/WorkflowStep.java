/*
 *   Copyright (c) 2023 Contributors to the Eclipse Foundation
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

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.util.List;

@Entity("workflow_step")
public class WorkflowStep {

    @Id("_key")
    private String id;

    @Column
    private String workflowSchemaKey;

    @Column
    private String stepName;

    @Column
    private MainStepType mainStepType;

    @Column
    private Integer stepNo;

    @Column
    private String componentConfigurationKey;

    @Column
    private String relationTypeKey;

    @Column
    private List<Transition> availableTransitions;

    WorkflowStep(String id, String workflowSchemaKey,
                 String stepName, MainStepType mainStepType,
                 Integer stepNo, String componentConfigurationKey,
                 String relationTypeKey, List<Transition> availableTransitions) {
        this.id = id;
        this.workflowSchemaKey = workflowSchemaKey;
        this.stepName = stepName;
        this.mainStepType = mainStepType;
        this.stepNo = stepNo;
        this.componentConfigurationKey = componentConfigurationKey;
        this.relationTypeKey = relationTypeKey;
        this.availableTransitions = availableTransitions;
    }

    WorkflowStep() {
    }

    public static WorkflowStepBuilder builder() {
        return new WorkflowStepBuilder();
    }

    public String id() {
        return id;
    }

    public String workflowSchemaKey() {
        return workflowSchemaKey;
    }

    public String stepName() {
        return stepName;
    }

    public MainStepType mainStepType() {
        return mainStepType;
    }

    public Integer stepNo() {
        return stepNo;
    }

    public String componentConfigurationKey() {
        return componentConfigurationKey;
    }

    public String relationTypeKey() {
        return relationTypeKey;
    }

    public List<Transition> availableTransitions() {
        return availableTransitions;
    }
}
