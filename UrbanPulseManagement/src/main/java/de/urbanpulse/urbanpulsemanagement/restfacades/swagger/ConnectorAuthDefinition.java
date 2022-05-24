package de.urbanpulse.urbanpulsemanagement.restfacades.swagger;

import io.swagger.models.auth.AbstractSecuritySchemeDefinition;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ConnectorAuthDefinition extends AbstractSecuritySchemeDefinition {

    private final String AUTH_TYPE = "connector";

    @Override
    public String getType() {
        return AUTH_TYPE;
    }

    @Override
    public void setType(String type) {
        throw new UnsupportedOperationException("Setting of type not supported");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }

        ConnectorAuthDefinition that = (ConnectorAuthDefinition) other;

        return AUTH_TYPE.equals(that.AUTH_TYPE);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + AUTH_TYPE.hashCode();
        return result;
    }
}
