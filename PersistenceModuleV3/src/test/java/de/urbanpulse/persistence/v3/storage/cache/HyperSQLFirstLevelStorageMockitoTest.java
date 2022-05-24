
package de.urbanpulse.persistence.v3.storage.cache;

import de.urbanpulse.outbound.QueryConfig;
import de.urbanpulse.persistence.v3.outbound.BatchSender;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class HyperSQLFirstLevelStorageMockitoTest {

    private static final String SID1 = UUID.randomUUID().toString();
    private static final String SID2 = UUID.randomUUID().toString();

    // Need this on initialization, so mocking the old fashioned way
    private Vertx vertx = mock(Vertx.class);

    @Mock
    private BatchSender batchSender;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityTransaction entityTransaction;

    @Mock
    private Query sidsToDeleteQuery;

    @Mock
    private Query deleteQuery;

    private final JsonObject storageConfig = new JsonObject();

    @InjectMocks
    private final HyperSQLFirstLevelStorage storage = new HyperSQLFirstLevelStorage(vertx, storageConfig);

    @Before
    public void setUp() {
        given(entityManager.getTransaction()).willReturn(entityTransaction);
        given(entityManager.createNativeQuery(HyperSQLFirstLevelStorage.SELECT_SIDS_TO_DELETE_QUERY)).willReturn(sidsToDeleteQuery);
        given(entityManager.createNativeQuery(HyperSQLFirstLevelStorage.DELETE_STATEMENT)).willReturn(deleteQuery);

        given(deleteQuery.setParameter(anyInt(), any())).willReturn(deleteQuery);
    }

    @Test
    public void testWillCleanupIfDue() {
        given(sidsToDeleteQuery.getResultList()).willReturn(Arrays.asList(SID1, SID2));

        ZonedDateTime aMomentAgo = ZonedDateTime.now().minusSeconds(10);
        HyperSQLFirstLevelStorage.NEXT_CLEANUP_DUE_TIME.set(aMomentAgo);

        storage.persist(Collections.emptyList());

        verify(sidsToDeleteQuery, times(1)).getResultList();
        verify(deleteQuery, times(2)).executeUpdate();
    }

}
