package de.urbanpulse.dist.jee.entities;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * entity for event processor statements
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Table(name = "up_statements", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Entity
public class StatementEntity extends AbstractUUIDEntity {

    private static final long serialVersionUID = 2L;

    private String name;

    @Lob
    private String query;

    @OneToMany(mappedBy = "statement")
    private List<UpdateListenerEntity> updateListeners;

    private String comment;

    public List<UpdateListenerEntity> getUpdateListeners() {
        return updateListeners;
    }

    public void setUpdateListeners(List<UpdateListenerEntity> updateListeners) {
        this.updateListeners = updateListeners;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
