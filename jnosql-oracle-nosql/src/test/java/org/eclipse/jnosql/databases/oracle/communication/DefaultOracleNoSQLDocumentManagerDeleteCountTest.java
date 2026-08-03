/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.oracle.communication;

import jakarta.json.bind.Jsonb;
import oracle.nosql.driver.NoSQLHandle;
import oracle.nosql.driver.ops.DeleteRequest;
import oracle.nosql.driver.ops.DeleteResult;
import oracle.nosql.driver.ops.PrepareRequest;
import oracle.nosql.driver.ops.PrepareResult;
import oracle.nosql.driver.ops.PreparedStatement;
import oracle.nosql.driver.ops.QueryRequest;
import oracle.nosql.driver.ops.QueryResult;
import oracle.nosql.driver.values.FieldValue;
import oracle.nosql.driver.values.MapValue;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultOracleNoSQLDocumentManagerDeleteCountTest {

    private static final String TABLE = "database";
    private static final String ENTITY = "person";
    private static final String NUM_ROWS_DELETED = "numRowsDeleted";

    private NoSQLHandle handle;
    private PreparedStatement preparedStatement;
    private DefaultOracleNoSQLDocumentManager manager;

    @BeforeEach
    void setUp() {
        handle = mock(NoSQLHandle.class);
        preparedStatement = mock(PreparedStatement.class);
        manager = new DefaultOracleNoSQLDocumentManager(TABLE, handle, mock(Jsonb.class));
    }

    @Test
    void shouldExposeCountedDeleteOnProviderInterfaceForCdiProxies() throws NoSuchMethodException {
        assertThat(OracleNoSQLDocumentManager.class.getDeclaredMethod("deleteAndCount", DeleteQuery.class))
                .isNotNull();
    }

    @Test
    void shouldCountOnlySuccessfulDirectIdDeletes() {
        when(handle.delete(any(DeleteRequest.class)))
                .thenReturn(new DeleteResult().setSuccess(true), new DeleteResult().setSuccess(false));

        DeleteQuery query = delete().from(ENTITY)
                .where("_id").in(List.of("one", "two"))
                .build();

        long deleted = manager.deleteAndCount(query);

        ArgumentCaptor<DeleteRequest> requests = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(handle, times(2)).delete(requests.capture());
        assertThat(requests.getAllValues())
                .allSatisfy(request -> assertThat(request.getTableName()).isEqualTo(TABLE))
                .extracting(request -> request.getKey().getString("id"))
                .containsExactlyInAnyOrder("person:one", "person:two");
        assertThat(deleted).isEqualTo(1L);
        verify(handle, never()).prepare(any(PrepareRequest.class));
    }

    @Test
    void shouldSumNativeSqlDeleteCountsAcrossBatches() {
        prepareSqlDelete(List.of(
                List.of(deleteCount(1L)),
                List.of(deleteCount(2L))));

        long deleted = manager.deleteAndCount(delete().from(ENTITY).build());

        ArgumentCaptor<PrepareRequest> request = ArgumentCaptor.forClass(PrepareRequest.class);
        verify(handle).prepare(request.capture());
        assertThat(request.getValue().getStatement())
                .isEqualTo("DELETE from database WHERE database.entity= 'person'");
        assertThat(deleted).isEqualTo(3L);
        verify(handle, times(2)).query(any(QueryRequest.class));
        verify(handle, never()).delete(any(DeleteRequest.class));
    }

    @Test
    void shouldBindConditionalSqlDeleteAndReturnNativeCount() {
        prepareSqlDelete(List.of(List.of(new MapValue().put(NUM_ROWS_DELETED, 2))));

        long deleted = manager.deleteAndCount(delete().from(ENTITY)
                .where("type").eq("V")
                .build());

        ArgumentCaptor<FieldValue> parameter = ArgumentCaptor.forClass(FieldValue.class);
        verify(preparedStatement).setVariable(eq(1), parameter.capture());
        assertThat(parameter.getValue().asString().getValue()).isEqualTo("V");

        ArgumentCaptor<PrepareRequest> request = ArgumentCaptor.forClass(PrepareRequest.class);
        verify(handle).prepare(request.capture());
        assertThat(request.getValue().getStatement())
                .contains("DELETE from database WHERE database.entity= 'person' AND")
                .contains("database.content.type")
                .contains(" =  ?");
        assertThat(deleted).isEqualTo(2L);
    }

    @Test
    void shouldPreserveVoidDeleteExecution() {
        prepareSqlDelete(List.of(List.of(deleteCount(3L))));

        manager.delete(delete().from(ENTITY).build());

        verify(handle).prepare(any(PrepareRequest.class));
        verify(handle).query(any(QueryRequest.class));
    }

    private void prepareSqlDelete(List<List<MapValue>> resultBatches) {
        when(handle.prepare(any(PrepareRequest.class)))
                .thenReturn(new PrepareResult().setPreparedStatement(preparedStatement));

        AtomicInteger nextBatch = new AtomicInteger();
        when(handle.query(any(QueryRequest.class))).thenAnswer(invocation -> {
            QueryRequest request = invocation.getArgument(0);
            int batch = nextBatch.getAndIncrement();
            request.setContKey(batch + 1 < resultBatches.size() ? new byte[]{1} : null);
            return new QueryResult(request).setResults(resultBatches.get(batch));
        });
    }

    private static MapValue deleteCount(long count) {
        return new MapValue().put(NUM_ROWS_DELETED, count);
    }
}
