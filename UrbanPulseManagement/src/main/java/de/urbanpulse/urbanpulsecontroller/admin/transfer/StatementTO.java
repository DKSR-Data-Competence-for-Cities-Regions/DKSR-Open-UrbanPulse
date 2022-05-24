package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.StatementEntity;
import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * transfer object for event processor statements
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@XmlRootElement
@ApiModel
public class StatementTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String query;
    private String id;
    private String comment;

    public StatementTO() {
    }

    /**
     * @param entity (requires its ID and all referenced IDs to be set to non-null values!)
     */
    public StatementTO(StatementEntity entity) {
        id = entity.getId();
        name = entity.getName();
        query = entity.getQuery();
        comment = entity.getComment();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (name != null ? name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof StatementTO)) {
            return false;
        }
        StatementTO other = (StatementTO) object;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ id=" + id + " ]";
    }
}
