package com.adorsys.webank.security.extractor;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class CachingRequestBodyWrapper extends HttpServletRequestWrapper {
    private final byte[] cachedBody;

    public CachingRequestBodyWrapper(HttpServletRequest request) throws IOException {
        super(request);
        
        // Cache the body
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        
        try (ServletInputStream inputStream = request.getInputStream()) {
            while ((length = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
        }
        
        cachedBody = baos.toByteArray();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedServletInputStream();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    private class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream cachedInputStream = new ByteArrayInputStream(cachedBody);

        @Override
        public boolean isFinished() {
            return cachedInputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Async reading not supported");
        }

        @Override
        public int read() throws IOException {
            return cachedInputStream.read();
        }
    }
}
