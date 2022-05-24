
package de.urbanpulse.dist.outbound.server.historicaldata;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
enum OutputFormat {
    JSON("application/json"), CSV("text/csv");

    private final String contentType;

    public boolean matches(String contentType) {
        return this.contentType.equalsIgnoreCase(contentType);
    }

    public String getContentType() {
        return contentType;
    }

    OutputFormat(String contentType) {
        this.contentType = contentType;
    }
}
