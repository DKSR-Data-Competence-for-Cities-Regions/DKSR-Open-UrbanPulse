package de.urbanpulse.urbanpulsecontroller.admin;

import java.util.LinkedList;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class TransferObjectFactoryTest {

    private TransferObjectFactory factory;

    private static final String EXPECTED_NAME = "Big Macintosh";
    private static final String EXPECTED_INNER_NAME = "Snaps";

    static class MyPlainTO {

        private String name;

        public MyPlainTO() {
        }

        public MyPlainTO(MyPlainEntity entity) {
            name = entity.getName();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class MyPlainEntity {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
    static class MyOuterTO {

        private MyInnerTO inner;

        public MyInnerTO getInner() {
            return inner;
        }

        public void setInner(MyInnerTO inner) {
            this.inner = inner;
        }

        public MyOuterTO() {
        }

        public MyOuterTO(MyOuterEntity entity, TransferObjectFactory factory) {
            inner = factory.create(entity.getInner(), MyInnerEntity.class, MyInnerTO.class);
        }
    }

    static class MyOuterEntity {

        private MyInnerEntity inner;

        public MyInnerEntity getInner() {
            return inner;
        }

        public void setInner(MyInnerEntity inner) {
            this.inner = inner;
        }

    }
    static class MyInnerTO {

        private String name;

        public MyInnerTO() {
        }

        public MyInnerTO(MyInnerEntity entity) {
            name = entity.getName();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class MyInnerEntity {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private MyPlainEntity plainEntity;
    private List<MyPlainEntity> plainEntities;

    private MyOuterEntity outerEntity;
    private MyInnerEntity innerEntity;

    @Before
    public void setUp() {
        innerEntity = new MyInnerEntity();
        innerEntity.setName(EXPECTED_NAME);

        outerEntity = new MyOuterEntity();
        outerEntity.setInner(innerEntity);

        plainEntity = new MyPlainEntity();
        plainEntity.setName(EXPECTED_NAME);

        plainEntities = new LinkedList<>();
        plainEntities.add(plainEntity);

        factory = new TransferObjectFactory();
    }

    @Test
    public void createWithDependencies_createsExpected() throws Exception {
        MyOuterTO transfer = factory.createWithDependencies(outerEntity, MyOuterEntity.class, MyOuterTO.class);

        assertEquals(EXPECTED_NAME, transfer.getInner().getName());
    }

    @Test
    public void create_createsExpected() throws Exception {
        MyPlainTO transfer = factory.create(plainEntity, MyPlainEntity.class, MyPlainTO.class);

        assertEquals(EXPECTED_NAME, transfer.getName());
    }

    @Test
    public void createList_createsExpected() throws Exception {
        List<MyPlainTO> transfers = factory.createList(plainEntities, MyPlainEntity.class, MyPlainTO.class);

        assertEquals(plainEntities.size(), transfers.size());

        MyPlainTO transfer = transfers.get(0);
        assertEquals(EXPECTED_NAME, transfer.getName());
    }
}
