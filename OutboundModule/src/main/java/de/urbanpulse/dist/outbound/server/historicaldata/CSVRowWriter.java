package de.urbanpulse.dist.outbound.server.historicaldata;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import com.googlecode.jcsv.*;
import com.googlecode.jcsv.writer.CSVEntryConverter;
import com.googlecode.jcsv.writer.CSVWriter;
import com.googlecode.jcsv.writer.internal.CSVWriterBuilder;
import java.io.IOException;
import java.io.StringWriter;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CSVRowWriter {

    private static final CSVStrategy CSV_STRATEGY = new CSVStrategy(';', '\"', '#', false, true);

    /**
     * converts the field names of a JSON event into a CSV header row
     */
    private static class HeaderConverter implements CSVEntryConverter<JsonObject> {

        private final Set<String> headerFields;

        public HeaderConverter(Set<String> headerFields) {
            this.headerFields = headerFields;
        }

        @Override
        public String[] convertEntry(JsonObject event) {
            String[] header = new String[headerFields.size()];
            headerFields.toArray(header);
            return header;
        }
    }

    /**
     * converts the field values of a JSON event into CSV row values
     */
    private static class EventEntryConverter implements CSVEntryConverter<JsonObject> {

        private final Set<String> headerFields;

        public EventEntryConverter(Set<String> headerFields) {
            this.headerFields = headerFields;
        }

        @Override
        public String[] convertEntry(JsonObject event) {
            String[] values = new String[headerFields.size()];
            int index = 0;
            for (String fieldName : headerFields) {
                String value = "" + event.getValue(fieldName, "");
                values[index] = value;
                index++;
            }

            return values;
        }

    }

    /**
     *
     * @param isFirst must be set to true for the first event, will be set to false during the first call
     * @param response the http response object
     * @param event flat JsonObject
     * @param headerFields shall be an empty set when called for the first event, it will then be filled
     * with the field names of that event, which will then be used also for subsequent events
     * (missing fields result in empty CSV cells, extraneous fields in following events will be skipped)
     */
    void writeCsvEvent(AtomicBoolean isFirst, HttpServerResponse response, JsonObject event, CSVHeaderSet headerFields) {
        try {
            if (isFirst.compareAndSet(true, false)) {
                headerFields.addAll(event.fieldNames());

                StringWriter headerWriter = new StringWriter();
                CSVWriter<JsonObject> csvWriter = new CSVWriterBuilder(headerWriter).entryConverter(
                        new HeaderConverter(headerFields)).strategy(CSV_STRATEGY).build();
                csvWriter.write(event);
                response.write(headerWriter.toString(), "UTF-8");
            }

            StringWriter rowWriter = new StringWriter();
            CSVWriter<JsonObject> csvWriter = new CSVWriterBuilder(rowWriter).entryConverter(
                    new EventEntryConverter(headerFields)).strategy(CSV_STRATEGY).build();
            csvWriter.write(event);
            response.write(rowWriter.toString(), "UTF-8");
        } catch (IOException ex) {

        }
    }
}
