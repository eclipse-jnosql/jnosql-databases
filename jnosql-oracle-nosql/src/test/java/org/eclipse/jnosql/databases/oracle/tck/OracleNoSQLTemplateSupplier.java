/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
 *   Alessandro Moscatelli
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.oracle.tck;

import ee.jakarta.tck.nosql.TemplateSupplier;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.nosql.Template;
import org.eclipse.jnosql.databases.oracle.communication.Database;
import org.eclipse.jnosql.databases.oracle.communication.OracleNoSQLConfigurations;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;


public class OracleNoSQLTemplateSupplier implements TemplateSupplier {

    static {
        System.setProperty(OracleNoSQLConfigurations.HOST.get(), Database.INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "jakartanosqltck");
        SeContainerInitializer.newInstance().initialize();
    }

    @Override
    public Template get() {
        return CDI.current().select(DocumentTemplate.class).get();
    }
}
