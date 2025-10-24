/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.tinkerpop.communication;

import jakarta.data.exceptions.EmptyResultException;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.jnosql.communication.CommunicationException;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.graph.CommunicationEdge;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.tinkerpop.gremlin.process.traversal.Order.asc;
import static org.apache.tinkerpop.gremlin.process.traversal.Order.desc;

/**
 * Default implementation of {@link TinkerpopGraphDatabaseManager} that serves as an adapter to the TinkerPop
 * graph database provided by the Apache TinkerPop framework.
 * <p>
 * This implementation wraps a TinkerPop {@link Graph} instance and provides methods to interact with
 * the underlying graph database, execute graph traversals, and perform other graph-related operations.
 * </p>
 * <p>
 * Note that this implementation does not support certain operations such as insertions with a duration,
 * as indicated by the UnsupportedOperationException thrown by those methods.
 * </p>
 */
public class DefaultTinkerpopGraphDatabaseManager implements TinkerpopGraphDatabaseManager {

    private final Graph graph;

    DefaultTinkerpopGraphDatabaseManager(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Graph get() {
        return graph;
    }

    @Override
    public String name() {
        return "The tinkerpop graph database manager";
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        addVertex(entity);
        return entity;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration duration) {
        throw new UnsupportedOperationException("There is no support to insert with duration");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        entities.forEach(this::insert);
        return entities;
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> iterable, Duration duration) {
        throw new UnsupportedOperationException("There is no support to insert with duration");
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        Object id = entity.find(ID).map(Element::get)
                .orElseThrow(() -> new IllegalArgumentException("Entity must have an ID"));
        Iterator<Vertex> vertices = graph.vertices(id);
        if (!vertices.hasNext()) {
            throw new EmptyResultException("The entity does not exist with the id: " + id);
        }
        Vertex vertex = vertices.next();
        entity.elements().stream()
                .filter(it -> !ID.equals(it.name()))
                .forEach(e -> vertex.property(e.name(), ValueUtil.convert(e.value())));
        GraphTransactionUtil.transaction(graph);
        return entity;
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        Stream.of(entities).forEach(this::update);
        return entities;
    }

    @Override
    public void delete(DeleteQuery query) {
        Objects.requireNonNull(query, "delete is required");
        GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V().hasLabel(query.name());
        query.condition().ifPresent(c -> {
            GraphTraversal<Vertex, Vertex> predicate = TraversalExecutor.getPredicate(c);
            traversal.filter(predicate);
        });

        traversal.drop().iterate();
        GraphTransactionUtil.transaction(graph);
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        GraphTraversal<Vertex, Vertex> traversal = buildGraphTraversalOf(query);

        if (query.limit() > 0) {
            traversal.limit(query.limit());
        } else if (query.skip() > 0) {
            traversal.skip(query.skip());
        }
        query.sorts().forEach(
                s -> {
                    if (s.isAscending()) {
                        traversal.order().by(s.property(), asc);
                    } else {
                        traversal.order().by(s.property(), desc);
                    }
                });
        return traversal.toStream().map(CommunicationEntityConverter.INSTANCE);
    }

    @Override
    public long count(String entity) {
        Objects.requireNonNull(entity, "entity is required");
        GraphTraversal<Vertex, Long> count = graph.traversal().V().hasLabel(entity).count();
        return count.next();
    }

    @Override
    public long count(SelectQuery query) {
        GraphTraversal<Vertex, Vertex> traversal = buildGraphTraversalOf(query);
        return traversal.count().next();
    }

    private GraphTraversal<Vertex, Vertex> buildGraphTraversalOf(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V().hasLabel(query.name());
        query.condition().ifPresent(c -> {
            GraphTraversal<Vertex, Vertex> predicate = TraversalExecutor.getPredicate(c);
            traversal.filter(predicate);
        });
        return traversal;
    }

    @Override
    public void close() {
        try {
            graph.close();
        } catch (Exception e) {
            throw new CommunicationException("There is an issue when close the Graph connection", e);
        }
    }

    @Override
    public CommunicationEdge edge(CommunicationEntity source, String label, CommunicationEntity target, Map<String, Object> properties) {
        Objects.requireNonNull(source, "source is required");
        Objects.requireNonNull(target, "target is required");
        Objects.requireNonNull(label, "label is required");

        Vertex sourceVertex = findOrCreateVertex(source);

        Vertex targetVertex = findOrCreateVertex(target);

        var edge = sourceVertex.addEdge(label, targetVertex);
        properties.forEach(edge::property);

        GraphTransactionUtil.transaction(graph);

        return new TinkerpopCommunicationEdge(edge.id(), source, target, label, properties);
    }

    @Override
    public void remove(CommunicationEntity source, String label, CommunicationEntity target) {
        Objects.requireNonNull(source, "source is required");
        Objects.requireNonNull(target, "target is required");
        Objects.requireNonNull(label, "label is required");

        Vertex sourceVertex = findVertexById(source.find(ID)
                .orElseThrow(() -> new CommunicationException("Source entity must have an ID")).get())
                .orElseThrow(() -> new EmptyResultException("Source entity not found"));

        Vertex targetVertex = findVertexById(target.find(ID)
                .orElseThrow(() -> new CommunicationException("Target entity must have an ID")).get())
                .orElseThrow(() -> new EmptyResultException("Target entity not found"));

        Iterator<Edge> edges = sourceVertex.edges(Direction.OUT, label);
        while (edges.hasNext()) {
            Edge edge = edges.next();
            if (edge.inVertex().id().equals(targetVertex.id())) {
                edge.remove();
            }
        }

        GraphTransactionUtil.transaction(graph);
    }

    @Override
    public <K> void deleteEdge(K id) {
        Objects.requireNonNull(id, "The id is required");

        var traversal = graph.traversal().E(id);
        if (!traversal.hasNext()) {
            throw new EmptyResultException("Edge not found for ID: " + id);
        }

        traversal.next().remove();
        GraphTransactionUtil.transaction(graph);
    }

    @Override
    public <K> Optional<CommunicationEdge> findEdgeById(K id) {
        Objects.requireNonNull(id, "The id is required");

        var traversal = graph.traversal().E(id);
        if (!traversal.hasNext()) {
            return Optional.empty();
        }

        var edge = traversal.next();
        var source = CommunicationEntity.of(edge.outVertex().label());
        source.add(ID, edge.outVertex().id());

        var target = CommunicationEntity.of(edge.inVertex().label());
        target.add(ID, edge.inVertex().id());

        Map<String, Object> properties = new HashMap<>();
        edge.properties().forEachRemaining(p -> properties.put(p.key(), p.value()));

        return Optional.of(new TinkerpopCommunicationEdge(id, source, target, edge.label(), properties));
    }

    private Vertex addVertex(CommunicationEntity entity) {
        Object[] args = Stream.concat(
                Stream.of(T.label, entity.name()),
                entity.elements().stream().flatMap(it -> ID.equals(it.name()) ?
                        Stream.of(T.id, it.get()) :
                        Stream.of(it.name(), ValueUtil.convert(it.value())))
        ).toArray();
        Vertex vertex = graph.addVertex(args);
        entity.add(ID, vertex.id());
        return vertex;
    }

    private Vertex findOrCreateVertex(CommunicationEntity entity) {
        return entity.find(ID)
                .flatMap(id -> findVertexById(id.get()))
                .orElseGet(() -> addVertex(entity));
    }

    private Optional<Vertex> findVertexById(Object id) {
        Iterator<Vertex> vertices = graph.vertices(id);
        return vertices.hasNext() ? Optional.of(vertices.next()) : Optional.empty();
    }

}
