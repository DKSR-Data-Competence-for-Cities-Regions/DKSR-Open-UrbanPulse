package de.urbanpulse.urbanpulsemanagement.transfer;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@XmlRootElement
public class UpdateListenersWrapperTO {

    private List<UpdateListenerTO> listeners;

    public UpdateListenersWrapperTO() {
        listeners = new LinkedList<>();
    }

    public void setListeners(List<UpdateListenerTO> listeners) {
        this.listeners = listeners;
    }

    public List<UpdateListenerTO> getListeners() {
        return listeners;
    }

    /**
     * @param listeners a list of UpdateListeners
     * @throws IllegalArgumentException null list
     */
    public UpdateListenersWrapperTO(List<UpdateListenerTO> listeners) {
        if (listeners == null) {
            throw new IllegalArgumentException("listeners must not be null");
        }

        this.listeners = listeners;
    }

}
