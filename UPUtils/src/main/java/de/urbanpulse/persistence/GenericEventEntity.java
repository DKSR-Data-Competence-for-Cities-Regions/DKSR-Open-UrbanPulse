package de.urbanpulse.persistence;

import java.io.Serializable;
import java.util.Objects;

/**
 * immutable sensor event entity for use by the event persistence system
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class GenericEventEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String partitionKey;
    private final String rowKey;
    private final String sid;
    private final String json;

    /**
     *
     * @param partitionKey which may or may not be used for load balancing, depending on the implementation
     * @param rowKey the row key
     * @param sid (sensor ID) NOT the value of a field named "SID" in the event JSON, which is unrelated!
     * @param json event JSON string
     */
    public GenericEventEntity(String partitionKey, String rowKey, String sid, String json) {
        this.partitionKey = partitionKey;
        this.rowKey = rowKey;
        this.sid = sid;
        this.json = json;
    }

    /**
     * @return event JSON string
     * <p>
     * NOTE: may contain an "SID" field that is unrelated to {@link #getSid()}
     */
    public String getJson() {
        return json;
    }

    /**
     * @return partition key which may or may not be
     * used for load balancing, depending on the implementation
     */
    public String getPartitionKey() {
        return partitionKey;
    }

    public String getRowKey() {
        return rowKey;
    }

    /**
     * @return SID (sensor ID) NOT the value of a field named
     * "SID" in the event JSON!
     */
    public String getSid() {
        return sid;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.partitionKey);
        hash = 61 * hash + Objects.hashCode(this.rowKey);
        hash = 61 * hash + Objects.hashCode(this.sid);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GenericEventEntity other = (GenericEventEntity) obj;
        if (!Objects.equals(this.partitionKey, other.partitionKey)) {
            return false;
        }
        if (!Objects.equals(this.rowKey, other.rowKey)) {
            return false;
        }
        if (!Objects.equals(this.sid, other.sid)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.json;
    }

}
