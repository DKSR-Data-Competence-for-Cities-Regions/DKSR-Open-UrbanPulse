package de.urbanpulse.urbanpulsemanagement.transfer;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryTO;
import java.util.LinkedList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CategoriesWrapperTO {

    public List<CategoryTO> categories;

    public CategoriesWrapperTO() {
        categories = new LinkedList<>();
    }

    /**
     * @param categories  a list of CategoryTO objects
     * @throws IllegalArgumentException null list
     */
    public CategoriesWrapperTO(List<CategoryTO> categories) {
        if (categories == null) {
            throw new IllegalArgumentException("categories must not be null");
        }

        this.categories = categories;
    }

    public JsonObject toJson() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (CategoryTO to : categories) {
            arrayBuilder.add(to.toJson());
        }
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("categories", arrayBuilder);
        return objectBuilder.build();
    }

}
