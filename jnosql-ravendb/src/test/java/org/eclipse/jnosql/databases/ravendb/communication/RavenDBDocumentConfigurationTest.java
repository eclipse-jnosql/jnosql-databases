/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jnosql.databases.ravendb.communication;

import org.eclipse.jnosql.communication.Settings;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class RavenDBDocumentConfigurationTest {

    @Test
    public void shouldCreateDocumentManagerFactoryByMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("ravendb-server-host-1", "172.17.0.2:8080");
        RavenDBDocumentConfiguration configuration = new RavenDBDocumentConfiguration();
        var managerFactory = configuration.apply(Settings.of(map));
        assertThat(managerFactory).isNotNull();
    }


    @Test
    public void shouldReturnErrorWhenSettingsIsNull() {
        var configuration = new RavenDBDocumentConfiguration();
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> configuration.apply(null));
    }


}
