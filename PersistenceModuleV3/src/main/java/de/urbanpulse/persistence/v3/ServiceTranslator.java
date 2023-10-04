package de.urbanpulse.persistence.v3;

import de.urbanpulse.persistence.v3.storage.StorageService;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Christian Müller <christian.mueller@the-urban-institute.de>
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
