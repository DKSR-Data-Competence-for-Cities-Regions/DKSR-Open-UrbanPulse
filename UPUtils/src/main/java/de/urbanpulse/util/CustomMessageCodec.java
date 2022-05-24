package de.urbanpulse.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @param <T> generic input for CustomMessageCodec
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CustomMessageCodec<T extends CustomMessageCodecEntity> implements MessageCodec<T, T> {

    private final Class<T> typeArgumentClass;
    private final String name;

    public CustomMessageCodec(T... name) {
        this.name = name.getClass().getComponentType().getSimpleName();
        this.typeArgumentClass = (Class<T>) getClass().getGenericSuperclass();
    }

    @Override
    public void encodeToWire(Buffer buffer, T eventTO) {
        String jsonToStr = eventTO.toJson().encode();
        int length = jsonToStr.getBytes().length;
        buffer.appendInt(length);
        buffer.appendString(jsonToStr);
    }

    @Override
    public T decodeFromWire(int pos, Buffer buffer) {
        int _pos = pos;
        int length = buffer.getInt(_pos);
        String jsonStr = buffer.getString(_pos += 4, _pos += length);
        T customMessageObj;
        try {
            customMessageObj = typeArgumentClass.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(CustomMessageCodec.class.getName()).log(Level.SEVERE, "Cannot decode JSON to class in CustomMessageCodec", ex);
            return null;
        }
        customMessageObj.fromJson(new JsonObject(jsonStr));
        return customMessageObj;
    }

    @Override
    public T transform(T obj) {
        return obj;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }

}
