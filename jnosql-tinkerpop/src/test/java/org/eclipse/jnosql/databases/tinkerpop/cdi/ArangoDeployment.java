package org.eclipse.jnosql.databases.tinkerpop.cdi;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

enum ArangoDeployment {
    INSTANCE;

    private final GenericContainer<?> arangodb =
            new GenericContainer<>("arangodb/arangodb:latest")
                    .withExposedPorts(8529)
                    .withEnv("ARANGO_NO_AUTH", "1")
                    .waitingFor(Wait.forHttp("/")
                            .forStatusCode(200));

    {
        arangodb.start();
    }

    public GenericContainer<?> getContainer() {
        return arangodb;
    }
}
