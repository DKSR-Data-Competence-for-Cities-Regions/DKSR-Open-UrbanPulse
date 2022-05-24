package de.urbanpulse.persistence.v3.jpa;

import java.util.Iterator;

import io.vertx.core.json.JsonObject;

/**
 * wraps the {@link Iterable} we get as JPA query result,
 * so we can iterate across {@link JsonObject}s
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
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
