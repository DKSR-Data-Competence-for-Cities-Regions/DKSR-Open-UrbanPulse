package de.urbanpulse.dist.jee.entities;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * associates a statement with an update listener target URL
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Table(name = "up_update_listeners")
@Entity
public class UpdateListenerEntity extends AbstractUUIDEntity {

    private static final long serialVersionUID = 3L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STATEMENT")
    private StatementEntity statement;

    private String hmacKey;
    private String authJson;

    public String getAuthJson() {
        return authJson;
    }

    public void setAuthJson(String authJson) {
        this.authJson = authJson;
    }

    @Lob
    private String target;

    public String getKey() {
        return hmacKey;
    }

    public void setKey(String key) {
        hmacKey = key;
    }

    public StatementEntity getStatement() {
        return statement;
    }

    public void setStatement(StatementEntity statement) {
        this.statement = statement;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
