package de.urbanpulse.persistence.v3.jpa;

import java.util.Iterator;

import io.vertx.core.json.JsonObject;

/**
 * wraps the {@link Iterable} we get as JPA query result,
 * so we can iterate across {@link JsonObject}s
 */
public class JPAWrapperIterator implements Iterator<JsonObject> {

    private final Iterator<JPAEventEntity> queryResultIterator;

    public JPAWrapperIterator(Iterable<JPAEventEntity> queryResult) {
        this.queryResultIterator = queryResult.iterator();
    }

    @Override
    public boolean hasNext() {
        return queryResultIterator.hasNext();
    }

    @Override
    public JsonObject next() {
        return new JsonObject(queryResultIterator.next().getJson());
    }
}
