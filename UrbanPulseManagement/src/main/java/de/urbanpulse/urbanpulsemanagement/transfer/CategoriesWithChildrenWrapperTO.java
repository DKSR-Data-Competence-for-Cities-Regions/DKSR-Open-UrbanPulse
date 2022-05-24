package de.urbanpulse.urbanpulsemanagement.transfer;

import de.urbanpulse.urbanpulsecontroller.admin.transfer.CategoryWithChildrenTO;
import io.vertx.core.json.JsonObject;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CategoriesWithChildrenWrapperTO {

    public List<CategoryWithChildrenTO> categories;

    public CategoriesWithChildrenWrapperTO() {
        categories = new LinkedList<>();
    }

    /**
     * @param categories the list of the categories with children
     * @throws IllegalArgumentException null list
     */
    public CategoriesWithChildrenWrapperTO(List<CategoryWithChildrenTO> categories) {
        if (categories == null) {
            throw new IllegalArgumentException("categories must not be null");
        }

        this.categories = categories;
    }

    public JsonObject toJson() {
        List<JsonObject> categoriesJsons = categories.parallelStream()
                .map(CategoryWithChildrenTO::toJson)
                .collect(Collectors.toList());

        JsonObject job = new JsonObject();
        job.put("categories", categoriesJsons);
        return job;
    }

}
