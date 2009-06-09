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

package org.apache.cxf.javascript;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.jdom.input.DOMBuilder;
import org.jdom.output.XMLOutputter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.ScriptableObject;

/**
 * Implementation of XMLHttpRequest for Rhino. This might be given knowledge of
 * CXF 'local' URLs if the author is feeling frisky.
 */
public class JsXMLHttpRequest extends ScriptableObject {
    private static final Logger LOG = LogUtils.getL7dLogger(JsXMLHttpRequest.class);
    private static Charset utf8 = Charset.forName("utf-8");
    private static Set<String> validMethods;
    static {
        validMethods = new HashSet<String>();
        validMethods.add("GET");
        validMethods.add("POST");
        validMethods.add("HEAD");
        validMethods.add("PUT");
        validMethods.add("OPTIONS");
        validMethods.add("DELETE");
    }

    
    private static String[] invalidHeaders = {"Accept-Charset", "Accept-Encoding", "Connection",
                                              "Content-Length", "Content-Transfer-Encoding", "Date",
                                              "Expect", "Host", "Keep-Alive", "Referer", "TE", "Trailer",
                                              "Transfer-Encoding", "Upgrade", "Via"};
    private int readyState = jsGet_UNSENT();
    private Object readyStateChangeListener;
    private Map<String, String> requestHeaders;
    private String storedMethod;
    private String storedUser;
    private String storedPassword;
    private boolean sendFlag;
    private URI uri;
    private URL url;
    private boolean storedAsync;
    private URLConnection connection;
    private HttpURLConnection httpConnection;
    private Map<String, List<String>> responseHeaders;
    private int httpResponseCode;
    private String httpResponseText;
    private String responseText;
    private JsSimpleDomNode responseXml;
    private boolean errorFlag;

    public JsXMLHttpRequest() {
        requestHeaders = new HashMap<String, String>();
        storedMethod = null;
    }
    
    public static void register(ScriptableObject scope) {
        try {
            ScriptableObject.defineClass(scope, JsXMLHttpRequest.class);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getClassName() {
        return "XMLHttpRequest";
    }

    private void notifyReadyStateChangeListener() {
        if (readyStateChangeListener instanceof Function) {
            LOG.fine("notify " + readyState);
            // for now, call with no args.
            Function listenerFunction = (Function)readyStateChangeListener;
            listenerFunction.call(Context.getCurrentContext(), getParentScope(), null, new Object[] {});
        }
    }

    private void doOpen(String method, String urlString, boolean async, String user, String password) {
        // ignoring auth for now.
        LOG.fine("doOpen " + method + " " + urlString + " " + Boolean.toString(async));

        storedAsync = async;
        responseText = null;
        responseXml = null;
        // see 4
        method = method.toUpperCase();
        // 1 check method
        if (!validMethods.contains(method)) {
            LOG.fine("Invalid method syntax error.");
            throwError("SYNTAX_ERR");
        }
        // 2 security check (we don't have any)
        // 3 store method
        storedMethod = method;
        // 4 we already mapped it to upper case.
        // 5 make a URL, dropping any fragment.
        uri = null;
        try {
            URI tempUri = new URI(urlString);
            if (tempUri.isOpaque()) { 
                LOG.fine("Relative URL syntax error.");
                throwError("SYNTAX_ERR");
            }
            
            uri = new URI(tempUri.getScheme(), tempUri.getUserInfo(), tempUri.getHost(), tempUri.getPort(),
                          tempUri.getPath(), tempUri.getQuery(), null /*
                                                                         * no
                                                                         * fragment
                                                                         */);
            url = uri.toURL();
        } catch (URISyntaxException e) {
            LOG.log(Level.SEVERE, "URI syntax error", e);
            throwError("SYNTAX_ERR");
        } catch (MalformedURLException e) {
            LOG.log(Level.SEVERE, "URI isn't URL", e);
            throwError("SYNTAX_ERR");
        }
        // 6 deal with relative URLs. We don't have a base. This is a limitation
        // on browser compatibility.
        if (!uri.isAbsolute()) {
            throwError("SYNTAX_ERR");
        }
        // 7 scheme check. Well, for now ...
        if (!uri.getScheme().equals("http") && !uri.getScheme().equals("https")) {
            LOG.severe("Not http " + uri.toString());
            throwError("NOT_SUPPORTED_ERR");
        }
        // 8 user:password is OK for HTTP.
        // 9, 10 user/password parsing
        if (uri.getUserInfo() != null) {
            String[] userAndPassword = uri.getUserInfo().split(":");
            storedUser = userAndPassword[0];
            if (userAndPassword.length == 2) {
                storedPassword = userAndPassword[1];
            }
        }
        // 11 cross-scripting check. We don't implement it.
        // 12 default async. Already done.
        // 13 check user for syntax. Not Our Job.
        // 14 encode the user. We think we can leave this for the Http code we
        // use below
        // 15, 16, 17, 18 more user/password glop.
        // 19: abort any pending activity.
        // TODO: abort
        // 20 cancel network activity.
        // TODO: cancel
        // 21 set state to OPENED and fire the listener.
        readyState = jsGet_OPENED();
        sendFlag = false;
        notifyReadyStateChangeListener();
    }

    private void doSetRequestHeader(String header, String value) {
        // 1 check state
        if (readyState != jsGet_OPENED()) {
            LOG.severe("setRequestHeader invalid state " + readyState);
            throwError("INVALID_STATE_ERR");
        }
        // 2 check flag
        if (sendFlag) {
            LOG.severe("setRequestHeader send flag set.");
            throwError("INVALID_STATE_ERR");
        }
        // 3 check field-name production.
        // 4 ignore null values.
        if (value == null) {
            return;
        }
        // 5 check value
        // 6 check for bad headers
        for (String invalid : invalidHeaders) {
            if (header.equalsIgnoreCase(invalid)) {
                LOG.severe("setRequestHeader invalid header.");
                throwError("SECURITY_ERR");
            }
        }
        // 7 check for proxy
        String headerLower = header.toLowerCase();
        if (headerLower.startsWith("proxy-")) {
            LOG.severe("setRequestHeader proxy header.");
            throwError("SECURITY_ERR");
        }
        // 8, 9, handle appends.
        String previous = requestHeaders.get(header);
        if (previous != null) {
            value = previous + ", " + value;
        }
        requestHeaders.put(header, value);
    }

    private void doSend(byte[] dataToSend, boolean xml) {
        // avoid warnings on stuff we arent using yet.
        if (storedUser != null || storedPassword != null) {
            //
        }
        // 1 check state
        if (readyState != jsGet_OPENED()) {
            LOG.severe("send state != OPENED.");
            throwError("INVALID_STATE_ERR");
        }
        // 2 check flag
        if (sendFlag) {
            LOG.severe("send sendFlag set.");
            throwError("INVALID_STATE_ERR");
        }
        // 3
        sendFlag = storedAsync;
        // 4 preprocess data. Handled on the way in here, we're called with
        // UTF-8 bytes.
        if (xml && !requestHeaders.containsKey("Content-Type")) {
            requestHeaders.put("Content-Type", "application/xml;charset=utf-8");
        }
        
        // 5 talk to the server.
        try {
            connection = url.openConnection();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "send connection failed.", e);
            throwError("CONNECTION_FAILED");
        }
        connection.setDoInput(true);
        connection.setUseCaches(false); // Enable tunneling.
        boolean post = false;
        httpConnection = null;
        if (connection instanceof HttpURLConnection) {
            httpConnection = (HttpURLConnection)connection;
            try {
                httpConnection.setRequestMethod(storedMethod);
                if ("POST".equalsIgnoreCase(storedMethod)) {
                    httpConnection.setDoOutput(true);
                    post = true;
                }
                for (Map.Entry<String, String> headerEntry : requestHeaders.entrySet()) {
                    httpConnection.setRequestProperty(headerEntry.getKey(), headerEntry.getValue());
                }
            } catch (ProtocolException e) {
                LOG.log(Level.SEVERE, "send http protocol exception.", e);
                throwError("HTTP_PROTOCOL_ERROR");
            }
        }
        
        if (post) {
            OutputStream outputStream = null;
            try {
                outputStream = connection.getOutputStream(); // implicitly connects?
                if (dataToSend != null) {
                    outputStream.write(dataToSend);
                    outputStream.flush();
                }
                outputStream.close();
            } catch (IOException e) {
                errorFlag = true;
                LOG.log(Level.SEVERE, "send output error.", e);
                throwError("NETWORK_ERR");
                try {
                    outputStream.close();
                } catch (IOException e1) {
                    //
                }
            }
        }
        // 6
        notifyReadyStateChangeListener();
        
        if (storedAsync) {
            new Thread() {
                public void run() {
                    try {
                        Context cx = Context.enter();
                        communicate(cx);
                    } finally {
                        Context.exit();
                    }
                }
            } .start();
        } else {
            communicate(Context.getCurrentContext());
        }
    }

    private void communicate(Context cx) {
        try {
            InputStream is = connection.getInputStream();
            httpResponseCode = -1;
            // this waits, I hope, for a response.
            responseHeaders = connection.getHeaderFields();
            readyState = jsGet_HEADERS_RECEIVED();
            notifyReadyStateChangeListener();
            
            if (httpConnection != null) {
                httpResponseCode = httpConnection.getResponseCode();
                httpResponseText = httpConnection.getResponseMessage();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            boolean notified = false;
            while ((read = is.read(buffer)) != -1) {
                if (!notified) {
                    readyState = jsGet_LOADING();
                    notifyReadyStateChangeListener();
                }
                baos.write(buffer, 0, read);
            }
            is.close();
            
            // For a one-way message or whatever, there may not be a content type.
            // throw away any encoding modifier.
            String contentType = "";
            String connectionContentType = connection.getContentType();
            String contentEncoding = null;
            if (connectionContentType != null) {
                contentEncoding = HttpHeaderHelper
                    .mapCharset(HttpHeaderHelper.findCharset(connectionContentType));
                contentType = connectionContentType.split(";")[0];
            }
            if (contentEncoding == null || contentEncoding.length() == 0) {
                contentEncoding = "iso-8859-1";
            }
            
            /* We need all the text in a string, independent of the
             * XML parse. 
             */

            Charset contentCharset = Charset.forName(contentEncoding);
            byte[] contentBytes = baos.toByteArray();
            CharBuffer contentChars = 
                contentCharset.decode(ByteBuffer.wrap(contentBytes)); // not the most efficient way.
            responseText = contentChars.toString();
            LOG.fine(responseText);
            
            
            if ("text/xml".equals(contentType)
                || "application/xml".equals(contentType) 
                || contentType.endsWith("+xml")) {
                
                try {
                    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    documentBuilderFactory.setNamespaceAware(true);
                    DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
                    ByteArrayInputStream bais = new ByteArrayInputStream(contentBytes);
                    InputSource inputSource = new InputSource(bais);
                    inputSource.setEncoding(contentEncoding);
                    Document xmlDoc = builder.parse(inputSource);
                    responseXml = JsSimpleDomNode.wrapNode(getParentScope(), xmlDoc);
                } catch (ParserConfigurationException e) {
                    LOG.log(Level.SEVERE, "ParserConfigurationError", e);
                    responseXml = null;
                } catch (SAXException e) {
                    LOG.log(Level.SEVERE, "Error parsing XML response", e);
                    responseXml = null;
                }
            }

            readyState = jsGet_DONE();
            notifyReadyStateChangeListener();

            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        } catch (IOException ioException) {
            errorFlag = true;
            readyState = jsGet_DONE();
            if (!storedAsync) {
                LOG.log(Level.SEVERE, "IO error reading response", ioException);
                throwError("NETWORK_ERR");
                notifyReadyStateChangeListener();
            }
        }
    }

    private void throwError(String errorName) {
        LOG.info("Javascript throw: " + errorName);
        throw new JavaScriptException(Context.javaToJS(errorName, getParentScope()), "XMLHttpRequest", 0);
    }

    private byte[] utf8Bytes(String data) {
        ByteBuffer bb = utf8.encode(data);
        byte[] val = new byte[bb.limit()];
        bb.get(val);
        return val;
    }

    private byte[] domToUtf8(JsSimpleDomNode xml) {
        Node node = xml.getWrappedNode();
        Document document = (Document)node; // assume that we're given the
        // entire document.
        // if that's an issue, we could code something more complex.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.jdom.Document jDocument = new DOMBuilder().build(document);
        org.jdom.output.Format format = org.jdom.output.Format.getRawFormat();
        format.setEncoding("utf-8");
        try {
            new XMLOutputter(format).output(jDocument, baos);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "impossible IO exception serializing XML", e);
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
    
    public void doAbort() {
        // this is messy.
    }
    
    public String doGetAllResponseHeaders() {
        // 1 check state.
        if (readyState == jsGet_UNSENT() || readyState == jsGet_OPENED()) {
            LOG.severe("Invalid state");
            throwError("INVALID_STATE_ERR");
        }
        
        // 2 check error flag
        if (errorFlag) {
            LOG.severe("error flag set");
            return null;
        }
        
        // 3 pile up the headers.
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<String>> headersEntry : responseHeaders.entrySet()) {
            if (headersEntry.getKey() == null) {
                // why does the HTTP connection return a null key with the response code and text?
                continue;
            }
            builder.append(headersEntry.getKey());
            builder.append(": ");
            for (String value : headersEntry.getValue()) {
                builder.append(value);
                builder.append(", ");
            }
            builder.setLength(builder.length() - 2); // trim extra comma/space
            builder.append("\r\n");
        }
        return builder.toString();
    }
    
    public String doGetResponseHeader(String header) {
        // 1 check state.
        if (readyState == jsGet_UNSENT() || readyState == jsGet_OPENED()) {
            LOG.severe("invalid state");
            throwError("INVALID_STATE_ERR");
        }
        
        // 2 check header format, we don't do it.
        
        // 3 check error flag
        if (errorFlag) {
            LOG.severe("error flag");
            return null;
        }
        
        //4 -- oh, it's CASE-INSENSITIVE. Well, we do it the hard way.
        for (Map.Entry<String, List<String>> headersEntry : responseHeaders.entrySet()) {
            if (header.equalsIgnoreCase(headersEntry.getKey())) {
                StringBuilder builder = new StringBuilder();
                for (String value : headersEntry.getValue()) {
                    builder.append(value);
                    builder.append(", ");
                }
                builder.setLength(builder.length() - 2); // trim extra comma/space
                return builder.toString();
            }
        }
        return null;
    }
    
    public String doGetResponseText() {
        // 1 check state.
        if (readyState == jsGet_UNSENT() || readyState == jsGet_OPENED()) {
            LOG.severe("invalid state " + readyState);
            throwError("INVALID_STATE_ERR");
        }
        
        // 2 return what we have.
        return responseText;
    }
    
    public Object doGetResponseXML() {
        // 1 check state.
        if (readyState == jsGet_UNSENT() || readyState == jsGet_OPENED()) {
            LOG.severe("invalid state");
            throwError("INVALID_STATE_ERR");
        }
        
        return responseXml;
    }
    
    public int doGetStatus() {
        if (httpResponseCode == -1) {
            LOG.severe("invalid state");
            throwError("INVALID_STATE_ERR");
        }
        return httpResponseCode;
            
    }
    
    public String doGetStatusText() {
        if (httpResponseText == null) {
            LOG.severe("invalid state");
            throwError("INVALID_STATE_ERR");
        }
        return httpResponseText;
    }

    // CHECKSTYLE:OFF

    public Object jsGet_onreadystatechange() {
        return readyStateChangeListener;
    }

    public void jsSet_onreadystatechange(Object listener) {
        readyStateChangeListener = listener;
    }

    public int jsGet_UNSENT() {
        return 0;
    }

    public int jsGet_OPENED() {
        return 1;
    }

    public int jsGet_HEADERS_RECEIVED() {
        return 2;
    }

    public int jsGet_LOADING() {
        return 3;
    }

    public int jsGet_DONE() {
        return 4;
    }

    public int jsGet_readyState() {
        return readyState;
    }

    public void jsFunction_open(String method, String url, Object asyncObj, Object user, Object password) {
        Boolean async;
        if (asyncObj == Context.getUndefinedValue()) {
            async = Boolean.TRUE;
        } else {
            async = (Boolean)Context.jsToJava(asyncObj, Boolean.class);
        }
        
        if (user == Context.getUndefinedValue()) {
            user = null;
        } else {
            user = Context.jsToJava(user, String.class);
        }
        if (password == Context.getUndefinedValue()) {
            password = null;
        } else {
            password = Context.jsToJava(password, String.class);
        }
        
        doOpen(method, url, async, (String)user, (String)password);
    }

    public void jsFunction_setRequestHeader(String header, String value) {
        doSetRequestHeader(header, value);
    }

    public void jsFunction_send(Object arg) {
        if (arg == Context.getUndefinedValue()) {
            doSend(null, false);
        } else if (arg instanceof String) {
            doSend(utf8Bytes((String)arg), false);
        } else if (arg instanceof JsSimpleDomNode) {
            doSend(domToUtf8((JsSimpleDomNode)arg), true);
        } else {
            throwError("INVALID_ARG_TO_SEND");
        }
    }


    public void jsFunction_abort() {
        doAbort();
    }

    public String jsFunction_getAllResponseHeaders() {
        return doGetAllResponseHeaders();
    }

    public String jsFunction_getResponseHeader(String header) {
        return doGetResponseHeader(header);
    }

    public String jsGet_responseText() {
        return doGetResponseText();
    }

    public Object jsGet_responseXML() {
        return doGetResponseXML();
    }

    public int jsGet_status() {
        return doGetStatus();
    }

    public String jsGet_statusText() {
        return doGetStatusText();
    }
}
 