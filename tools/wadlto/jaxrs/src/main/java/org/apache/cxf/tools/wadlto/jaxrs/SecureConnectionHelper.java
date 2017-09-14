package org.apache.cxf.tools.wadlto.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.cxf.common.util.Base64Utility;

public final class SecureConnectionHelper {
    private SecureConnectionHelper() {
        
    }
    
    public static InputStream getStreamFromSecureConnection(URL url, String authorizationValue) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        String encodedAuth = "Basic " + Base64Utility.encode(authorizationValue.getBytes());
        conn.setRequestProperty("Authorization", encodedAuth);
        return conn.getInputStream();
    }
}
