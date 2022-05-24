package de.urbanpulse.dist.jee.upsecurityrealm.hmac;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * This Wrapper is used to be able to read the body stream of a requiest twice
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ServletRequestBodyWrapper extends HttpServletRequestWrapper {

    private static final Logger LOG = Logger.getLogger(CachedServletInputStream.class.getName());
    private ByteArrayOutputStream cacheBody;

    public ServletRequestBodyWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cacheBody == null) {
            cacheBody();
            LOG.info("caching");
        }

        return new CachedServletInputStream();
    }

    private void cacheBody() {
        cacheBody = new ByteArrayOutputStream();
        try {
            InputStream is = super.getInputStream();
            int read = is.read();
            while (read != -1) {
                cacheBody.write(read);
                read = is.read();
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error while caching request body", e);
        }
    }

    public class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;



        public CachedServletInputStream() {
            inputStream = new ByteArrayInputStream(cacheBody.toByteArray());
            LOG.info("InputStream created!");
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isReady() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

}
