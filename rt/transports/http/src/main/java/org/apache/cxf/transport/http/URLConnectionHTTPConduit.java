/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.https.HttpsURLConnectionFactory;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * 
 */
public class URLConnectionHTTPConduit extends HTTPConduit {

    /**
     * This field holds the connection factory, which primarily is used to 
     * factor out SSL specific code from this implementation.
     * <p>
     * This field is "protected" to facilitate some contrived UnitTesting so
     * that an extended class may alter its value with an EasyMock URLConnection
     * Factory. 
     */
    protected HttpsURLConnectionFactory connectionFactory;
        
    
    public URLConnectionHTTPConduit(Bus b, EndpointInfo ei) throws IOException {
        super(b, ei);
        connectionFactory = new HttpsURLConnectionFactory();
        CXFAuthenticator.addAuthenticator();
    }

    public URLConnectionHTTPConduit(Bus b, EndpointInfo ei, EndpointReferenceType t) throws IOException {
        super(b, ei, t);
        connectionFactory = new HttpsURLConnectionFactory();
        CXFAuthenticator.addAuthenticator();
    }
    
    /**
     * Close the conduit
     */
    public void close() {
        super.close();
        if (defaultEndpointURI != null) {
            try {
                URLConnection connect = defaultEndpointURI.toURL().openConnection();
                if (connect instanceof HttpURLConnection) {
                    ((HttpURLConnection)connect).disconnect();
                }
            } catch (IOException ex) {
                //ignore
            }
            //defaultEndpointURL = null;
        }
    }    
    
    private HttpURLConnection createConnection(Message message, Address address, HTTPClientPolicy csPolicy)
        throws IOException {
        URL url = address.getURL();
        URI uri = address.getURI();
        Proxy proxy = proxyFactory.createProxy(csPolicy , uri);
        message.put("http.scheme", uri.getScheme());
        // check tlsClientParameters from message header
        TLSClientParameters clientParameters = message.get(TLSClientParameters.class);
        if (clientParameters == null) {
            clientParameters = tlsClientParameters;
        }
        return connectionFactory.createConnection(clientParameters, proxy, url);
    }
    protected void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy) throws IOException {
        HttpURLConnection connection = createConnection(message, address, csPolicy);
        connection.setDoOutput(true);       
        
        int ctimeout = determineConnectionTimeout(message, csPolicy);
        connection.setConnectTimeout(ctimeout);
        
        int rtimeout = determineReceiveTimeout(message, csPolicy);
        connection.setReadTimeout(rtimeout);
        
        connection.setUseCaches(false);
        // We implement redirects in this conduit. We do not
        // rely on the underlying URLConnection implementation
        // because of trust issues.
        connection.setInstanceFollowRedirects(false);

        // If the HTTP_REQUEST_METHOD is not set, the default is "POST".
        String httpRequestMethod = 
            (String)message.get(Message.HTTP_REQUEST_METHOD);
        if (httpRequestMethod == null) {
            httpRequestMethod = "POST";
            message.put(Message.HTTP_REQUEST_METHOD, "POST");
        }
        connection.setRequestMethod(httpRequestMethod);
        
        // We place the connection on the message to pick it up
        // in the WrappedOutputStream.
        message.put(KEY_HTTP_CONNECTION, connection);
        message.put(KEY_HTTP_CONNECTION_ADDRESS, address);
    }

    
    protected OutputStream createOutputStream(Message message, 
                                              boolean needToCacheRequest, 
                                              boolean isChunking,
                                              int chunkThreshold) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)message.get(KEY_HTTP_CONNECTION);
        
        if (isChunking && chunkThreshold <= 0) {
            chunkThreshold = 0;
            connection.setChunkedStreamingMode(-1);                    
        }
        try {
            return new URLConnectionWrappedOutputStream(message, connection,
                                           needToCacheRequest, 
                                           isChunking,
                                           chunkThreshold,
                                           getConduitName());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
    
    private static URI computeURI(Message message, HttpURLConnection connection) throws URISyntaxException {
        Address address = (Address)message.get(KEY_HTTP_CONNECTION_ADDRESS);
        return address != null ? address.getURI() : connection.getURL().toURI();
    }
    
    class URLConnectionWrappedOutputStream extends WrappedOutputStream {
        HttpURLConnection connection;
        public URLConnectionWrappedOutputStream(Message message, HttpURLConnection connection,
                                                boolean needToCacheRequest, boolean isChunking,
                                                int chunkThreshold, String conduitName) throws URISyntaxException {
            super(message, needToCacheRequest, isChunking,
                  chunkThreshold, conduitName,
                  computeURI(message, connection));
            this.connection = connection;
        }
        
        // This construction makes extending the HTTPConduit more easier 
        protected URLConnectionWrappedOutputStream(URLConnectionWrappedOutputStream wos) {
            super(wos);
            this.connection = wos.connection;
        }
        protected void setupWrappedStream() throws IOException {
            // If we need to cache for retransmission, store data in a
            // CacheAndWriteOutputStream. Otherwise write directly to the output stream.
            OutputStream cout = null;
            try {
                cout = connection.getOutputStream();
            } catch (SocketException e) {
                if ("Socket Closed".equals(e.getMessage())) {
                    connection.connect();
                    cout = connection.getOutputStream();
                } else {
                    throw e;
                }
            }
            if (cachingForRetransmission) {
                cachedStream =
                    new CacheAndWriteOutputStream(cout);
                wrappedStream = cachedStream;
            } else {
                wrappedStream = cout;
            }
        }

        @Override
        public void thresholdReached() {
            if (chunking) {
                connection.setChunkedStreamingMode(
                    URLConnectionHTTPConduit.this.getClient().getChunkLength());
            }
        }
        @Override
        protected void onFirstWrite() throws IOException {
            super.onFirstWrite();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Sending "
                    + connection.getRequestMethod() 
                    + " Message with Headers to " 
                    + url
                    + " Conduit :"
                    + conduitName
                    + "\n");
            }
        }
        protected void setProtocolHeaders() throws IOException {
            new Headers(outMessage).setProtocolHeadersInConnection(connection);
        }

        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
            connection.connect();
            return new HttpsURLConnectionInfo(connection);
        }
        protected void updateResponseHeaders(Message inMessage) {
            Headers h = new Headers(inMessage);
            h.readFromConnection(connection);
            inMessage.put(Message.CONTENT_TYPE, connection.getContentType());
            cookies.readFromHeaders(h);
        }
        protected void handleResponseAsync() throws IOException {
            handleResponseOnWorkqueue(true, false);
        }
        protected void updateCookiesBeforeRetransmit() {
            Headers h = new Headers();
            h.readFromConnection(connection);
            cookies.readFromHeaders(h);
        }
        
        protected InputStream getInputStream() throws IOException {
            InputStream in = null;
            if (getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                in = connection.getErrorStream();
                if (in == null) {
                    try {
                        // just in case - but this will most likely cause an exception
                        in = connection.getInputStream();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            } else {
                in = connection.getInputStream();
            }
            return in;
        }

        
        protected void closeInputStream() throws IOException {
            //try and consume any content so that the connection might be reusable
            InputStream ins = connection.getErrorStream();
            if (ins == null) {
                ins = connection.getInputStream();
            }
            if (ins != null) {
                IOUtils.consume(ins);
                ins.close();
            }
        }
        protected int getResponseCode() throws IOException {
            return connection.getResponseCode();
        }
        protected String getResponseMessage() throws IOException {
            return connection.getResponseMessage();
        }
        protected InputStream getPartialResponse() throws IOException {
            return ChunkedUtil.getPartialResponse(connection, connection.getResponseCode());
        }
        protected boolean usingProxy() {
            return connection.usingProxy();
        }
        protected void setFixedLengthStreamingMode(int i) {
            // [CXF-6227] do not call connection.setFixedLengthStreamingMode(i)
            // to prevent https://bugs.openjdk.java.net/browse/JDK-8044726
        }
        protected void handleNoOutput() throws IOException {
            if ("POST".equals(getMethod())) {
                connection.getOutputStream().close();
            }
        }
        protected void setupNewConnection(String newURL) throws IOException {
            HTTPClientPolicy cp = getClient(outMessage);
            Address address;
            try {
                URI nurl = new URI(newURL);
                address = new Address(nurl);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            setupConnection(outMessage, address, cp);
            this.url = address.getURI();
            connection = (HttpURLConnection)outMessage.get(KEY_HTTP_CONNECTION);
        }

        @Override
        protected void retransmitStream() throws IOException {
            OutputStream out = connection.getOutputStream();
            cachedStream.writeCacheTo(out);
        }
    }
    
}
