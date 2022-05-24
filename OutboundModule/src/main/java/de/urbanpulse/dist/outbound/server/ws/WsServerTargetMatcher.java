package de.urbanpulse.dist.outbound.server.ws;

public class WsServerTargetMatcher {

    /**
     * @param targetPath (e.g. "/OutboundInterface/outbound/fooStatement"), MAY contain redundant slashes
     * @param basePathWithoutTrailingSlash (e.g. "/OutboundInterface/outbound"), MUST NOT contain redundant slashes!
     * @throws IllegalArgumentException either arg null, baseUrlWithoutTrailingSlash is empty or contains trailing slash
     * @return whether targetPath (stripped of any redundant slashes it may have) matches basePathWithoutTrailingSlash + "/" + aPotentialStatementName
     *
     * This code is published by DKSR Gmbh under the German Free Software License.
     * Please refer to the document in the link for usage, change and distribution information
     * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
     */
    public boolean matches(String targetPath, String basePathWithoutTrailingSlash) {
        if (null == targetPath) {
            throw new IllegalArgumentException("cannot match null target!");
        }

        if (null == basePathWithoutTrailingSlash) {
            throw new IllegalArgumentException("cannot match null base url!");
        }

        if (basePathWithoutTrailingSlash.isEmpty()) {
            throw new IllegalArgumentException("cannot match empty base url!");
        }

        if (basePathWithoutTrailingSlash.endsWith("/")) {
            throw new IllegalArgumentException("baseUrlWithoutTrailingSlash has trailing slash");
        }

        if (!targetPath.equals(targetPath.trim())) {
            return false;
        }

        String baseWithoutDuplicateSlashes = basePathWithoutTrailingSlash;
        while (baseWithoutDuplicateSlashes.contains("//")) {
            baseWithoutDuplicateSlashes = baseWithoutDuplicateSlashes.replace("//", "/");
        }

        String targetWithoutDuplicateSlashes = targetPath;
        while (targetWithoutDuplicateSlashes.contains("//")) {
            targetWithoutDuplicateSlashes = targetWithoutDuplicateSlashes.replace("//", "/");
        }

        if (!targetWithoutDuplicateSlashes.startsWith(baseWithoutDuplicateSlashes)) {
            return false;
        }

        String restAfterBaseWithoutSurroundingSlashes = targetWithoutDuplicateSlashes
                .substring(baseWithoutDuplicateSlashes.length());
        while (restAfterBaseWithoutSurroundingSlashes.startsWith("/")) {
            restAfterBaseWithoutSurroundingSlashes = restAfterBaseWithoutSurroundingSlashes.substring(1);
        }

        while (restAfterBaseWithoutSurroundingSlashes.endsWith("/")) {
            restAfterBaseWithoutSurroundingSlashes = restAfterBaseWithoutSurroundingSlashes.substring(0,
                    restAfterBaseWithoutSurroundingSlashes.length() - 2);
        }

        if (restAfterBaseWithoutSurroundingSlashes.isEmpty()) {
            return false;
        }

        return ! restAfterBaseWithoutSurroundingSlashes.contains("/");
    }

    /**
     * @param targetPath (e.g. "/OutboundInterface/outbound/fooStatement"), MAY contain redundant slashes
     * @param basePathWithoutTrailingSlash (e.g. "/OutboundInterface/outbound"), MUST NOT contain redundant slashes!
     * @return statement name (null if not found)
     */
    public String extractStatement(String targetPath, String basePathWithoutTrailingSlash) {
        if (matches(targetPath, basePathWithoutTrailingSlash)) {
            String targetWithoutDuplicateSlashes = targetPath;
            while (targetWithoutDuplicateSlashes.contains("//")) {
                targetWithoutDuplicateSlashes = targetWithoutDuplicateSlashes.replace("//", "/");
            }

            int baseUrlLength = basePathWithoutTrailingSlash.length();
            String tailOfPath = targetWithoutDuplicateSlashes.substring(baseUrlLength);
            return tailOfPath.replace("/", "");
        } else {
            return null;
        }
    }
}
