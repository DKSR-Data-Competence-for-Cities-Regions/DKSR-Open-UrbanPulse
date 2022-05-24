package de.urbanpulse.urbanpulsecontroller.admin.entities.modules;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * holds information about the state of a UP vert.x module command transaction with respect to a participating connection
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Entity
@Table(name = "up_transactions", indexes = {
    @Index(columnList = "txId")
})
public class TransactionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // row ID (for a transaction involving three connections to module instances we would have three rows)
    @Id
    private String id;

    // transaction ID
    private String txId;

    private String connectionID;
    private String txState;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getConnectionID() {
        return connectionID;
    }

    public void setConnectionID(String connectionID) {
        this.connectionID = connectionID;
    }

    public String getTxState() {
        return txState;
    }

    public void setTxState(String txState) {
        this.txState = txState;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TransactionEntity)) {
            return false;
        }
        TransactionEntity other = (TransactionEntity) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return this.getClass().getName()+ "[ id=" + id + " ]";
    }

}
