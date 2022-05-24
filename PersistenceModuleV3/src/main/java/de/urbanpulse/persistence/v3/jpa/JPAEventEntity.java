package de.urbanpulse.persistence.v3.jpa;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.*;

/**
 * entity for JPA events, table name can be overridden via {@link EclipseLinkTableNameSessionCustomizer}
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Table(name = "up_events",
        //This already creates an index for the unique columns - adding them to indexes has no effect
        uniqueConstraints = @UniqueConstraint(columnNames = {"partitionKey", "rowKey", "sid", "eventHash"}),
        indexes = {
                @Index(columnList = "partitionKey"),
                @Index(columnList = "rowKey"),
                @Index(columnList = "sid"),
                @Index(columnList = "eventHash")
        })
@Entity
@NamedQueries({
        @NamedQuery(name = JPAEventEntity.QUERY_COUNT_FOR_SID, query
                = "SELECT COUNT(e.id) FROM JPAEventEntity e WHERE e.sid=:sid"),
        @NamedQuery(name = JPAEventEntity.QUERY_FOR_SID_OLDEST_FIRST, query
                = "SELECT e FROM JPAEventEntity e WHERE e.sid=:sid ORDER BY e.rowKey ASC"),
        @NamedQuery(name = JPAEventEntity.QUERY_FOR_SID_YOUNGEST_FIRST, query
                = "SELECT e FROM JPAEventEntity e WHERE e.sid=:sid ORDER BY e.rowKey DESC"),
        @NamedQuery(name = JPAEventEntity.QUERY_ROW_FOR_SID, query
                = "SELECT e FROM JPAEventEntity e WHERE e.sid=:sid AND e.rowKey=:rowKey AND e.partitionKey=:partitionKey AND e.eventHash=:eventHash"),
        @NamedQuery(name = JPAEventEntity.QUERY_SINCE_UNTIL_FOR_SID, query
                = "SELECT e FROM JPAEventEntity e WHERE e.sid=:sid AND e.rowKey>=:since AND e.rowKey<=:until ORDER BY e.rowKey ASC")
})
public class JPAEventEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * named query to retrieve all events for a given sid within the range [since,until], oldest first
     */
    public static final String QUERY_COUNT_FOR_SID = "countForSid";
    public static final String QUERY_FOR_SID_OLDEST_FIRST = "queryForSidOldestFirst";
    public static final String QUERY_ROW_FOR_SID = "queryRowForSid";
    public static final String QUERY_SINCE_UNTIL_FOR_SID = "querySinceUntilForSid";
    public static final String QUERY_FOR_SID_YOUNGEST_FIRST = "queryForSidYoungestFirst";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Timestamp partitionKey;

    @Column(nullable = false)
    private Timestamp rowKey;

    @Column(nullable = false)
    private String sid;

    @Column(nullable = false)
    private String eventHash;

    @Column(nullable = false)
    @Lob
    private String json;

    /**
     * the no-arg constructor is only for JPA, do NOT use it directly!
     */
    public JPAEventEntity() {
    }

    /**
     * only to be used by {@link JPAEventEntityFactory}, do NOT use this directly!
     * <p>
     *
     * @param partitionKey MUST NOT be null (NOTE: despite this the partition key is pretty irrelevant for this entity,
     *                     it's mostly here for historical reasons)
     * @param rowKey       MUST NOT be null
     * @param sid          MUST NOT be null
     * @param json         MUST NOT be null
     * @throws IllegalArgumentException any null arg
     */
    JPAEventEntity(Timestamp partitionKey, Timestamp rowKey, String sid, String eventHash, String json) {
        if (partitionKey == null || rowKey == null || sid == null || eventHash == null || json == null) {
            throw new IllegalArgumentException("null constructor args for " + this.getClass().getSimpleName() + " not allowed!");
        }

        this.partitionKey = partitionKey;
        this.rowKey = rowKey;
        this.sid = sid;
        this.eventHash = eventHash;
        this.json = json;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public Timestamp getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(Timestamp partitionKey) {
        this.partitionKey = partitionKey;
    }

    public Timestamp getRowKey() {
        return rowKey;
    }

    public void setRowKey(Timestamp rowKey) {
        this.rowKey = rowKey;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getEventHash() {
        return eventHash;
    }

    public void setEventHash(String eventHash) {
        this.eventHash = eventHash;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof JPAEventEntity)) {
            return false;
        }
        JPAEventEntity other = (JPAEventEntity) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "JPAEventEntity{" + "id=" + id + ", partitionKey=" + partitionKey + ", rowKey=" + rowKey + ", sid=" + sid + ", json=" + json + '}';
    }


}
