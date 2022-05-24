/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import io.vertx.core.json.JsonObject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public interface ModuleSetup {

    public JsonObject createModuleSetup(UPModuleEntity module, JsonObject setup);
    public UPModuleType getModuleType();

}
