/*
 *   Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.mongodb.integration;

import jakarta.nosql.Column;
import jakarta.nosql.Embeddable;

import java.util.List;

@Embeddable
public class Transition {

    @Column
    private String targetWorkflowStepKey;
    @Column
    private StepTransitionReason stepTransitionReason;
    @Column
    private String mailTemplateKey;
    @Column
    private List<String> restrictedRoleGroups;

    public Transition(String targetWorkflowStepKey,
                      StepTransitionReason stepTransitionReason,
                      String mailTemplateKey,
                      List<String> restrictedRoleGroups) {
        this.targetWorkflowStepKey = targetWorkflowStepKey;
        this.stepTransitionReason = stepTransitionReason;
        this.mailTemplateKey = mailTemplateKey;
        this.restrictedRoleGroups = restrictedRoleGroups;
    }

    public Transition() {
    }

    public String targetWorkflowStepKey() {
        return targetWorkflowStepKey;
    }

    public StepTransitionReason stepTransitionReason() {
        return stepTransitionReason;
    }

    public String mailTemplateKey() {
        return mailTemplateKey;
    }

    public List<String> restrictedRoleGroups() {
        return restrictedRoleGroups;
    }
}
