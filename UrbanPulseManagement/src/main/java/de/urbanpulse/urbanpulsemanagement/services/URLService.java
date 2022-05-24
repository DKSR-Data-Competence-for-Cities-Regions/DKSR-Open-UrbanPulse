package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.dist.jee.entities.CategoryEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import city.ui.shared.commons.time.UPDateTimeFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class URLService {
    private String host;
    private String upmgmtPort;
    private String historicDataPort;

    @PostConstruct
    public void postConstruct() {
        try {
            InitialContext initialContext = new InitialContext();
            host = (String) initialContext.lookup("urbanpulse/external_host");
            historicDataPort = (String) initialContext.lookup("urbanpulse/historicdata_port");
            upmgmtPort = (String) initialContext.lookup("urbanpulse/up_management_port");
        } catch (NamingException ex) {
            Logger.getLogger(URLService.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    public String createUPApiUrl(Class clazz, Long id) {
        String url = "";
        if (clazz == CategoryEntity.class) {
            url = "https://" + host + ":" + upmgmtPort + "/UrbanPulseManagement/api/categories/" + id;
        } else if (clazz == SensorEntity.class) {
            url = "https://" + host + ":" + upmgmtPort + "/UrbanPulseManagement/api/sensors/" + id;
        } else {
            throw new IllegalArgumentException("Cannot create UPManagement URL for resource: " + clazz);
        }

        return url;
    }

    public String createUPApiUrl(Class clazz, String id) {
        String url = "";
        if (clazz == CategoryEntity.class) {
            url = "https://" + host + ":" + upmgmtPort + "/UrbanPulseManagement/api/categories/" + id;
        } else if (clazz == SensorEntity.class) {
            url = "https://" + host + ":" + upmgmtPort + "/UrbanPulseManagement/api/sensors/" + id;
        } else {
            throw new IllegalArgumentException("Cannot create UPManagement URL for resource: " + clazz);
        }

        return url;
    }

    public String createUPHistoricUrl(Long id) {
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime tMinus1Hour = utcNow.minusHours(1);
        String stringNow = UPDateTimeFormat.getFormatterWithZoneZ().format(utcNow);
        String stringTMinus1Hour = UPDateTimeFormat.getFormatterWithZoneZ().format(tMinus1Hour);
        String url = "https://" + host + ":" + historicDataPort + "/UrbanPulseData/historic/sensordata?SID=" + id + "&since="
                + stringTMinus1Hour + "&until=" + stringNow;
        return url;
    }

    public String createUPHistoricUrl(String id) {
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime tMinus1Hour = utcNow.minusHours(1);
        String stringNow = UPDateTimeFormat.getFormatterWithZoneZ().format(utcNow);
        String stringTMinus1Hour = UPDateTimeFormat.getFormatterWithZoneZ().format(tMinus1Hour);
        String url = "https://" + host + ":" + historicDataPort + "/UrbanPulseData/historic/sensordata?SID=" + id + "&since="
                + stringTMinus1Hour + "&until=" + stringNow;
        return url;
    }

}
