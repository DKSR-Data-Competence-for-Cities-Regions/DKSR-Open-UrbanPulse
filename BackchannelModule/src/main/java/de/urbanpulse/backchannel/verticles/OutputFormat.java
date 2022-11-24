package de.urbanpulse.backchannel.verticles;

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
