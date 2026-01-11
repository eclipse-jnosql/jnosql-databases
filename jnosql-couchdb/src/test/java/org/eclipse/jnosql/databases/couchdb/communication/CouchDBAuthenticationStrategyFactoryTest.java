package org.eclipse.jnosql.databases.couchdb.communication;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("CouchDBAuthenticationStrategyFactory")
class CouchDBAuthenticationStrategyFactoryTest {


    @Test
    @DisplayName("should return Basic strategy when username and password are provided")
    void shouldReturnBasicStrategy() {
        CouchDBAuthenticationStrategy strategy =
                CouchDBAuthenticationStrategyFactory.of("admin", "secret", null);

        assertThat(strategy)
                .isInstanceOf(CouchDBAuthenticationStrategyFactory.Basic.class);
    }

    @Test
    @DisplayName("should return Bearer strategy when token is provided and username/password are null")
    void shouldReturnBearerStrategy() {
        CouchDBAuthenticationStrategy strategy =
                CouchDBAuthenticationStrategyFactory.of(null, null, "jwt-token");

        assertThat(strategy)
                .isInstanceOf(CouchDBAuthenticationStrategyFactory.Bearer.class);
    }

    @Test
    @DisplayName("should return None strategy when no authentication data is provided")
    void shouldReturnNoneStrategy() {
        CouchDBAuthenticationStrategy strategy =
                CouchDBAuthenticationStrategyFactory.of(null, null, null);

        assertThat(strategy)
                .isInstanceOf(CouchDBAuthenticationStrategyFactory.None.class);
    }

    @Test
    @DisplayName("Basic strategy should apply Authorization header using Basic scheme")
    void shouldApplyBasicAuthorizationHeader() {
        CouchDBAuthenticationStrategy strategy =
                CouchDBAuthenticationStrategyFactory.of("admin", "secret", null);

        HttpUriRequest request = Mockito.mock(HttpUriRequest.class);

        strategy.apply(request);

        verify(request)
                .setHeader(Mockito.eq(HttpHeaders.AUTHORIZATION), Mockito.startsWith("Basic "));
    }

    @Test
    @DisplayName("Bearer strategy should apply Authorization header using Bearer scheme")
    void shouldApplyBearerAuthorizationHeader() {
        CouchDBAuthenticationStrategy strategy =
                CouchDBAuthenticationStrategyFactory.of(null, null, "jwt-token");

        HttpUriRequest request = Mockito.mock(HttpUriRequest.class);

        strategy.apply(request);

        verify(request)
                .setHeader(HttpHeaders.AUTHORIZATION, "Bearer jwt-token");
    }

    @Test
    @DisplayName("None strategy should not interact with the HTTP request")
    void shouldNotApplyAnyAuthorizationHeader() {
        CouchDBAuthenticationStrategy strategy =
                CouchDBAuthenticationStrategyFactory.of(null, null, null);

        HttpUriRequest request = Mockito.mock(HttpUriRequest.class);

        strategy.apply(request);

        verifyNoInteractions(request);
    }
}