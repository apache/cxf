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
import java.net.ServerSocket;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.attachment.AttachmentDataSource;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.io.AbstractWrappedOutputStream;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.AbstractMultiplexDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.http.policy.PolicyUtils;
import org.apache.cxf.transport.https.CertConstraints;
import org.apache.cxf.transport.https.CertConstraintsInterceptor;
import org.apache.cxf.transport.https.SSLUtils;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.policy.Assertor;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

/**
 * Common base for HTTP Destination implementations.
 */
public abstract class AbstractHTTPDestination extends AbstractMultiplexDestination implements Configurable,
    Assertor {
    
    public static final String HTTP_REQUEST = "HTTP.REQUEST";
    public static final String HTTP_RESPONSE = "HTTP.RESPONSE";
    public static final String HTTP_CONTEXT = "HTTP.CONTEXT";
    public static final String HTTP_CONFIG = "HTTP.CONFIG";
    public static final String PROTOCOL_HEADERS_CONTENT_TYPE = Message.CONTENT_TYPE.toLowerCase();
        
    public static final String PARTIAL_RESPONSE = AbstractMultiplexDestination.class.getName()
        + ".partial.response";
    public static final String RESPONSE_COMMITED = "http.response.done";
    public static final String REQUEST_REDIRECTED = "http.request.redirected";
    
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractHTTPDestination.class);
    
    private static final long serialVersionUID = 1L;

    protected final Bus bus;
    protected final ConduitInitiator conduitInitiator;

    // Configuration values
    protected HTTPServerPolicy server;
    protected String contextMatchStrategy = "stem";
    protected boolean fixedParameterOrder;
    protected boolean multiplexWithAddress;
    protected CertConstraints certConstraints;
    
    /**
     * Constructor
     * 
     * @param b the associated Bus
     * @param ci the associated conduit initiator
     * @param ei the endpoint info of the destination 
     * @param dp true for adding the default port if it is missing
     * @throws IOException
     */    
    public AbstractHTTPDestination(Bus b,
                                   ConduitInitiator ci,
                                   EndpointInfo ei,
                                   boolean dp)
        throws IOException {
        super(b, getTargetReference(getAddressValue(ei, dp), b), ei);  
        bus = b;
        conduitInitiator = ci;
        
        initConfig();
    }
    
    

    /**
     * Cache HTTP headers in message.
     * 
     * @param message the current message
     */
    protected void setHeaders(Message message) {
        Map<String, List<String>> requestHeaders = new HashMap<String, List<String>>();
        copyRequestHeaders(message, requestHeaders);
        message.put(Message.PROTOCOL_HEADERS, requestHeaders);

        if (requestHeaders.containsKey("Authorization")) {
            List<String> authorizationLines = requestHeaders.get("Authorization"); 
            String credentials = authorizationLines.get(0);
            if (credentials != null && !StringUtils.isEmpty(credentials.trim())) {
                String authType = credentials.split(" ")[0];
                if ("Basic".equals(authType)) {
                    String authEncoded = credentials.split(" ")[1];
                    try {
                        String authDecoded = new String(Base64Utility.decode(authEncoded));
                        String authInfo[] = authDecoded.split(":");
                        String username = (authInfo.length > 0) ? authInfo[0] : "";
                        // Below line for systems that blank out password after authentication;
                        // see CXF-1495 for more info
                        String password = (authInfo.length > 1) ? authInfo[1] : "";
                        AuthorizationPolicy policy = new AuthorizationPolicy();
                        policy.setUserName(username);
                        policy.setPassword(password);
                        
                        message.put(AuthorizationPolicy.class, policy);
                    } catch (Base64Exception ex) {
                        //ignore, we'll leave things alone.  They can try decoding it themselves
                    }
                }
            }
        }
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Request Headers: " + requestHeaders.toString());
        }
           
    }
    
    protected void updateResponseHeaders(Message message) {
        Map<String, List<String>> responseHeaders =
            CastUtils.cast((Map)message.get(Message.PROTOCOL_HEADERS));
        if (responseHeaders == null) {
            responseHeaders = new HashMap<String, List<String>>();
            message.put(Message.PROTOCOL_HEADERS, responseHeaders);         
        }
        setPolicies(responseHeaders);
    }
    
    /** 
     * @param message the message under consideration
     * @return true iff the message has been marked as oneway
     */    
    protected final boolean isOneWay(Message message) {
        Exchange ex = message.getExchange();
        return ex == null ? false : ex.isOneWay();
    }
    
    /**
     * @return the associated conduit initiator
     */
    protected ConduitInitiator getConduitInitiator() {
        return conduitInitiator;
    }

    /**
     * Copy the request headers into the message.
     * 
     * @param message the current message
     * @param headers the current set of headers
     */
    protected void copyRequestHeaders(Message message, Map<String, List<String>> headers) {
        HttpServletRequest req = (HttpServletRequest)message.get(HTTP_REQUEST);
        
        //TODO how to deal with the fields        
        for (Enumeration e = req.getHeaderNames(); e.hasMoreElements();) {
            String fname = (String)e.nextElement();
            String mappedName = HttpHeaderHelper.getHeaderKey(fname);
            List<String> values;
            if (headers.containsKey(mappedName)) {
                values = headers.get(mappedName);
            } else {
                values = new ArrayList<String>();
                headers.put(mappedName, values);
            }
            for (Enumeration e2 = req.getHeaders(fname); e2.hasMoreElements();) {
                String val = (String)e2.nextElement();
                values.add(val);
            }
        }
        headers.put(Message.CONTENT_TYPE, Collections.singletonList(req.getContentType()));
    }

    /**
     * Copy the response headers into the response.
     * 
     * @param message the current message
     * @param headers the current set of headers
     */
    protected void copyResponseHeaders(Message message, HttpServletResponse response) {
        String ct  = (String)message.get(Message.CONTENT_TYPE);
        String enc = (String)message.get(Message.ENCODING);
        
        if (null != ct 
            && null != enc
            && ct.indexOf("charset=") == -1
            && !ct.toLowerCase().contains("multipart/related")) {
            ct = ct + "; charset=" + enc;
        }

        Map<?, ?> headers = (Map<?, ?>)message.get(Message.PROTOCOL_HEADERS);
        if (null != headers) {
            
            if (!headers.containsKey(Message.CONTENT_TYPE)) {
                response.setContentType(ct);
            }
            
            for (Iterator<?> iter = headers.keySet().iterator(); iter.hasNext();) {
                String header = (String)iter.next();
                List<?> headerList = (List<?>)headers.get(header);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < headerList.size(); i++) {
                    sb.append(headerList.get(i));
                    if (i + 1 < headerList.size()) {
                        sb.append(',');
                    }
                }
                response.addHeader(header, sb.toString());
            }
        } else {
            response.setContentType(ct);
        }
    }
    
    protected void setupMessage(Message inMessage,
                                final ServletContext context, 
                                final HttpServletRequest req, 
                                final HttpServletResponse resp) throws IOException {
        setupMessage(inMessage, null, context, req, resp);
    }
    
    protected void setupMessage(Message inMessage,
                                final ServletConfig config,
                                final ServletContext context, 
                                final HttpServletRequest req, 
                                final HttpServletResponse resp) throws IOException {

        DelegatingInputStream in = new DelegatingInputStream(req.getInputStream());
        inMessage.setContent(DelegatingInputStream.class, in);
        inMessage.setContent(InputStream.class, in);
        inMessage.put(HTTP_REQUEST, req);
        inMessage.put(HTTP_RESPONSE, resp);
        inMessage.put(HTTP_CONTEXT, context);
        inMessage.put(HTTP_CONFIG, config);
        
        inMessage.put(Message.HTTP_REQUEST_METHOD, req.getMethod());
        inMessage.put(Message.REQUEST_URI, req.getRequestURI());
        String contextPath = req.getContextPath();
        if (contextPath == null) {
            contextPath = "";
        }
        inMessage.put(Message.PATH_INFO, contextPath + req.getPathInfo());
        
        String contentType = req.getContentType();
        String enc = HttpHeaderHelper.findCharset(contentType);
        if (enc == null) {
            enc = req.getCharacterEncoding();
        }
        // work around a bug with Jetty which results in the character
        // encoding not being trimmed correctly.
        if (enc != null && enc.endsWith("\"")) {
            enc = enc.substring(0, enc.length() - 1);
        }
        if (enc != null || "POST".equals(req.getMethod()) || "PUT".equals(req.getMethod())) {
            //allow gets/deletes/options to not specify an encoding
            String normalizedEncoding = HttpHeaderHelper.mapCharset(enc);
            if (normalizedEncoding == null) {
                String m = new org.apache.cxf.common.i18n.Message("INVALID_ENCODING_MSG",
                                                                  LOG, enc).toString();
                LOG.log(Level.WARNING, m);
                throw new IOException(m);   
            }
            inMessage.put(Message.ENCODING, normalizedEncoding);
        }
        
        inMessage.put(Message.QUERY_STRING, req.getQueryString());
        inMessage.put(Message.CONTENT_TYPE, contentType);
        inMessage.put(Message.ACCEPT_CONTENT_TYPE, req.getHeader("Accept"));
        String basePath = getBasePath(contextPath);
        if (!StringUtils.isEmpty(basePath)) {
            inMessage.put(Message.BASE_PATH, basePath);
        }
        inMessage.put(Message.FIXED_PARAMETER_ORDER, isFixedParameterOrder());
        inMessage.put(Message.ASYNC_POST_RESPONSE_DISPATCH, Boolean.TRUE);
        inMessage.put(SecurityContext.class, new SecurityContext() {
            public Principal getUserPrincipal() {
                return req.getUserPrincipal();
            }
            public boolean isUserInRole(String role) {
                return req.isUserInRole(role);
            }
        });
        
        setHeaders(inMessage);
        
        SSLUtils.propogateSecureSession(req, inMessage);

        inMessage.put(CertConstraints.class.getName(), certConstraints);
        inMessage.put(Message.IN_INTERCEPTORS,
                Arrays.asList(new Interceptor[] {CertConstraintsInterceptor.INSTANCE}));

    }
    
    protected String getBasePath(String contextPath) throws IOException {
        if (StringUtils.isEmpty(endpointInfo.getAddress())) {
            return "";
        }
        return new URL(endpointInfo.getAddress()).getPath();
    }
    
    protected static EndpointInfo getAddressValue(EndpointInfo ei) {       
        return getAddressValue(ei, true);
    } 
    
    protected static EndpointInfo getAddressValue(EndpointInfo ei, boolean dp) {       
        if (dp) {
            
            String eiAddress = ei.getAddress();
            if (eiAddress == null) {
                try {
                    ServerSocket s = new ServerSocket(0);
                    ei.setAddress("http://localhost:" + s.getLocalPort());
                    s.close();
                    return ei;
                } catch (IOException ex) {
                    // problem allocating a random port, go to the default one
                    ei.setAddress("http://localhost");
                }
            }
            
            String addr = StringUtils.addDefaultPortIfMissing(ei.getAddress());
            if (addr != null) {
                ei.setAddress(addr);
            }
        } 
        return ei;
    }
    
    /**
     * @param inMessage the incoming message
     * @return the inbuilt backchannel
     */
    protected Conduit getInbuiltBackChannel(Message inMessage) {
        HttpServletResponse response = (HttpServletResponse)inMessage.get(HTTP_RESPONSE);
        return new BackChannelConduit(response);
    }
    
    /**
     * Mark message as a partial message.
     * 
     * @param partialResponse the partial response message
     * @param the decoupled target
     * @return true iff partial responses are supported
     */
    protected final boolean markPartialResponse(Message partialResponse,
                                       EndpointReferenceType decoupledTarget) {
        // setup the outbound message to for 202 Accepted
        partialResponse.put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_ACCEPTED);
        partialResponse.getExchange().put(EndpointReferenceType.class, decoupledTarget);
        partialResponse.put(PARTIAL_RESPONSE, Boolean.TRUE);
        return true;
    }
    
    protected boolean isPartialResponse(Message m) {
        return Boolean.TRUE.equals(m.get(PARTIAL_RESPONSE));
    }

    private void initConfig() {
        PolicyEngine engine = bus.getExtension(PolicyEngine.class);
        // for a decoupled endpoint there is no service info
        if (null != engine && engine.isEnabled() 
            && null != endpointInfo.getService()) {
            server = PolicyUtils.getServer(engine, endpointInfo, this);
        }
        if (null == server) {
            server = endpointInfo.getTraversedExtensor(
                    new HTTPServerPolicy(), HTTPServerPolicy.class);
        }
    }
    private static List<String> createMutableList(String val) {
        return new ArrayList<String>(Arrays.asList(new String[] {val}));
    }
    void setPolicies(Map<String, List<String>> headers) {
        HTTPServerPolicy policy = server; 
        if (policy.isSetCacheControl()) {
            headers.put("Cache-Control",
                        createMutableList(policy.getCacheControl().value()));
        }
        if (policy.isSetContentLocation()) {
            headers.put("Content-Location",
                        createMutableList(policy.getContentLocation()));
        }
        if (policy.isSetContentEncoding()) {
            headers.put("Content-Encoding",
                        createMutableList(policy.getContentEncoding()));
        }
        if (policy.isSetContentType()) {
            headers.put(HttpHeaderHelper.CONTENT_TYPE,
                        createMutableList(policy.getContentType()));
        }
        if (policy.isSetServerType()) {
            headers.put("Server",
                        createMutableList(policy.getServerType()));
        }
        if (policy.isSetHonorKeepAlive() && !policy.isHonorKeepAlive()) {
            headers.put("Connection",
                        createMutableList("close"));
        } else if (policy.isSetKeepAliveParameters()) {
            headers.put("Keep-Alive", createMutableList(policy.getKeepAliveParameters()));
        }
        
    
        
    /*
     * TODO - hook up these policies
    <xs:attribute name="SuppressClientSendErrors" type="xs:boolean" use="optional" default="false">
    <xs:attribute name="SuppressClientReceiveErrors" type="xs:boolean" use="optional" default="false">
    */
    }
  
   
    /**
     * On first write, we need to make sure any attachments and such that are still on the incoming stream 
     * are read in.  Otherwise we can get into a deadlock where the client is still trying to send the 
     * request, but the server is trying to send the response.   Neither side is reading and both blocked 
     * on full buffers.  Not a good situation.    
     * @param outMessage
     */
    private void cacheInput(Message outMessage) {
        if (outMessage.getExchange() == null) {
            return;
        }
        Message inMessage = outMessage.getExchange().getInMessage();
        if (inMessage == null) {
            return;
        }
        Collection<Attachment> atts = inMessage.getAttachments();
        if (atts != null) {
            for (Attachment a : atts) {
                if (a.getDataHandler().getDataSource() instanceof AttachmentDataSource) {
                    try {
                        ((AttachmentDataSource)a.getDataHandler().getDataSource()).cache();
                    } catch (IOException e) {
                        throw new Fault(e);
                    }
                }
            }
        }
        DelegatingInputStream in = inMessage.getContent(DelegatingInputStream.class);
        if (in != null) {
            in.cacheInput();
        }
    }
    
    protected OutputStream flushHeaders(Message outMessage) throws IOException {
        if (isResponseRedirected(outMessage)) {
            return null;
        }
        
        cacheInput(outMessage);
        
        updateResponseHeaders(outMessage);
        Object responseObj = outMessage.get(HTTP_RESPONSE);
        OutputStream responseStream = null;
        boolean oneWay = isOneWay(outMessage);
        if (responseObj instanceof HttpServletResponse) {
            HttpServletResponse response = (HttpServletResponse)responseObj;

            Integer i = (Integer)outMessage.get(Message.RESPONSE_CODE);
            if (i != null) {
                int status = i.intValue();  
                if (HttpURLConnection.HTTP_INTERNAL_ERROR == i) {
                    Map<Object, Object> pHeaders = 
                        CastUtils.cast((Map)outMessage.get(Message.PROTOCOL_HEADERS));
                    if (null != pHeaders && pHeaders.containsKey(PROTOCOL_HEADERS_CONTENT_TYPE)) {
                        pHeaders.remove(PROTOCOL_HEADERS_CONTENT_TYPE);
                    }
                }
                response.setStatus(status);
            } else if (oneWay) {
                response.setStatus(HttpURLConnection.HTTP_ACCEPTED);
            } else {
                response.setStatus(HttpURLConnection.HTTP_OK);
            }

            copyResponseHeaders(outMessage, response);

            
            if (oneWay && !isPartialResponse(outMessage)) {
                response.setContentLength(0);
                response.flushBuffer();
                response.getOutputStream().close();
            } else {
                responseStream = response.getOutputStream();                
            }
        } else if (null != responseObj) {
            String m = (new org.apache.cxf.common.i18n.Message("UNEXPECTED_RESPONSE_TYPE_MSG",
                LOG, responseObj.getClass())).toString();
            LOG.log(Level.WARNING, m);
            throw new IOException(m);   
        } else {
            String m = (new org.apache.cxf.common.i18n.Message("NULL_RESPONSE_MSG", LOG)).toString();
            LOG.log(Level.WARNING, m);
            throw new IOException(m);
        }

        if (oneWay) {
            outMessage.remove(HTTP_RESPONSE);
        }
        return responseStream;
    }
    
    private boolean isResponseRedirected(Message outMessage) {
        return Boolean.TRUE.equals(outMessage.get(REQUEST_REDIRECTED));
    }
    
    /**
     * Backchannel conduit.
     */
    public class BackChannelConduit
        extends AbstractDestination.AbstractBackChannelConduit {

        protected HttpServletResponse response;

        BackChannelConduit(HttpServletResponse resp) {
            response = resp;
        }

        /**
         * Send an outbound message, assumed to contain all the name-value
         * mappings of the corresponding input message (if any). 
         * 
         * @param message the message to be sent.
         */
        public void prepare(Message message) throws IOException {
            message.put(HTTP_RESPONSE, response);
            OutputStream os = message.getContent(OutputStream.class);
            if (os == null) {
                message.setContent(OutputStream.class, 
                               new WrappedOutputStream(message, response));
            }
        }
        
        @Override
        public void close(Message msg) throws IOException {
            super.close(msg);
            if (msg.getExchange() == null) {
                return;
            }
            Message m = msg.getExchange().getInMessage();
            if (m == null || msg.getExchange().isOneWay()) {
                return;
            }
            InputStream is = m.getContent(InputStream.class);
            if (is != null) {
                try {
                    is.close();
                    m.removeContent(InputStream.class);
                } catch (IOException ioex) {
                    //ignore
                }
            }
        }
    }

    /**
     * Wrapper stream responsible for flushing headers and committing outgoing
     * HTTP-level response.
     */
    private class WrappedOutputStream extends AbstractWrappedOutputStream {

        protected HttpServletResponse response;
        private Message outMessage;
        
        WrappedOutputStream(Message m, HttpServletResponse resp) {
            super();
            this.outMessage = m;
            response = resp;
        }

        /**
         * Perform any actions required on stream flush (freeze headers,
         * reset output stream ... etc.)
         */
        protected void onFirstWrite() throws IOException {
            OutputStream responseStream = flushHeaders(outMessage);
            if (null != responseStream) {
                wrappedStream = responseStream;
            }            
        }

        /**
         * Perform any actions required on stream closure (handle response etc.)
         */
        public void close() throws IOException {
            if (wrappedStream == null) {
                OutputStream responseStream = flushHeaders(outMessage);
                if (null != responseStream) {
                    wrappedStream = responseStream;
                }
            }
            if (wrappedStream != null) {
                wrappedStream.close();
                response.flushBuffer();
            }
            /*
            try {
                //make sure the input stream is also closed in this 
                //case so that any resources it may have is cleaned up
                Message m = outMessage.getExchange().getInMessage();
                if (m != null) {
                    InputStream ins = m.getContent(InputStream.class);
                    if (ins != null) {
                        ins.close();
                    }
                }
            } catch (IOException ex) {
                //ignore
            }
            */
        }
        
        public void flush() throws IOException {
            //ignore until we close 
            // or we'll force chunking and cause all kinds of network packets
        }
    }

    protected boolean contextMatchOnExact() {
        return "exact".equals(contextMatchStrategy);
    }    

    public String getBeanName() {
        String beanName = null;
        if (endpointInfo.getName() != null) {
            beanName = endpointInfo.getName().toString() + ".http-destination";
        }
        return beanName;
    }

    /*
     * Implement multiplex via the address URL to avoid the need for ws-a.
     * Requires contextMatchStrategy of stem.
     * 
     * @see org.apache.cxf.transport.AbstractMultiplexDestination#getAddressWithId(java.lang.String)
     */
    public EndpointReferenceType getAddressWithId(String id) {
        EndpointReferenceType ref = null;

        if (isMultiplexWithAddress()) {
            String address = EndpointReferenceUtils.getAddress(reference);
            ref = EndpointReferenceUtils.duplicate(reference);
            if (address.endsWith("/")) {
                EndpointReferenceUtils.setAddress(ref, address + id);
            } else {
                EndpointReferenceUtils.setAddress(ref, address + "/" + id);
            }
        } else {
            ref = super.getAddressWithId(id);
        }
        return ref;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.transport.AbstractMultiplexDestination#getId(java.util.Map)
     */
    public String getId(Map context) {
        String id = null;

        if (isMultiplexWithAddress()) {
            String address = (String)context.get(Message.PATH_INFO);
            if (null != address) {
                int afterLastSlashIndex = address.lastIndexOf("/") + 1;
                if (afterLastSlashIndex > 0 
                        && afterLastSlashIndex < address.length()) {
                    id = address.substring(afterLastSlashIndex);
                }
            } else {
                getLogger().log(Level.WARNING,
                    new org.apache.cxf.common.i18n.Message(
                            "MISSING_PATH_INFO", LOG).toString());
            }
        } else {
            return super.getId(context);
        }   
        return id;
    }

    public String getContextMatchStrategy() {
        return contextMatchStrategy;
    }

    public void setContextMatchStrategy(String contextMatchStrategy) {
        this.contextMatchStrategy = contextMatchStrategy;
    }

    public boolean isFixedParameterOrder() {
        return fixedParameterOrder;
    }

    public void setFixedParameterOrder(boolean fixedParameterOrder) {
        this.fixedParameterOrder = fixedParameterOrder;
    }

    public boolean isMultiplexWithAddress() {
        return multiplexWithAddress;
    }

    public void setMultiplexWithAddress(boolean multiplexWithAddress) {
        this.multiplexWithAddress = multiplexWithAddress;
    }

    public HTTPServerPolicy getServer() {
        return server;
    }

    public void setServer(HTTPServerPolicy server) {
        this.server = server;
    }
    
    public void assertMessage(Message message) {
        PolicyUtils.assertServerPolicy(message, server); 
    }

    public boolean canAssert(QName type) {
        return PolicyUtils.HTTPSERVERPOLICY_ASSERTION_QNAME.equals(type); 
    }
    
}
