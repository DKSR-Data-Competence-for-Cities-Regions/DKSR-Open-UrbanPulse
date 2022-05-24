package de.urbanpulse.urbanpulsemanagement.transfer;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.StatementTO;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@XmlRootElement
public class StatementsWrapperTO {

    public List<StatementTO> statements;

    public StatementsWrapperTO() {
        statements = new LinkedList<>();
    }

    /**
     * @param statements  a list of statements
     * @throws IllegalArgumentException null list
     */
    public StatementsWrapperTO(List<StatementTO> statements) {
        if (statements == null) {
            throw new IllegalArgumentException("statements must not be null");
        }

        this.statements = statements;
    }

}
