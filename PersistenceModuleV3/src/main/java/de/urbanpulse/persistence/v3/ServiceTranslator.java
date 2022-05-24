package de.urbanpulse.persistence.v3;

import de.urbanpulse.persistence.v3.storage.StorageService;
import java.util.HashMap;
import java.util.Map;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ServiceTranslator {

    private final Map<String,String> conversionMap = new HashMap<>();

    public ServiceTranslator() {
        conversionMap.put("de.urbanpulse.persistence.v3.storage.AzureTableStoreSecondLevelStorage",
                "de.urbanpulse.persistence.v3.storage.AzureTableStoreSecondLevelStorageServiceImpl");
        conversionMap.put("de.urbanpulse.persistence.v3.storage.ElasticSearchSecondLevelStorageVerticle",
                "de.urbanpulse.persistence.v3.storage.ElasticSearchSecondLevelStorageServiceImpl");
        conversionMap.put("de.urbanpulse.persistence.v3.storage.JPASecondLevelStorage",
                "de.urbanpulse.persistence.v3.storage.JPASecondLevelStorageServiceImpl");
    }

    public String convert(String impl) {
        String storageServiceClass = conversionMap.getOrDefault(impl, impl);
        if (isValidStorageService(storageServiceClass)) {
            return storageServiceClass;
        } else {
            throw new IllegalArgumentException(storageServiceClass + " is not a valid storage service");
        }
    }

    private boolean isValidStorageService(String storageServiceClass) {
        Class<?> storageServiceClazz;
        try {
            storageServiceClazz = Class.forName(storageServiceClass);
        } catch (ClassNotFoundException ex) {
            return false;
        }
        return StorageService.class.isAssignableFrom(storageServiceClazz);
    }

}
