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
package org.apache.cxf.rs.security.jose.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public final class JoseUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(JoseUtils.class);
    private static final String CLASSPATH_PREFIX = "classpath:";

    private JoseUtils() {

    }
    public static String[] getCompactParts(String compactContent) {
        if (compactContent.startsWith("\"") && compactContent.endsWith("\"")) {
            compactContent = compactContent.substring(1, compactContent.length() - 1);
        }
        return compactContent.split("\\.");
    }
    public static void setJoseContextProperty(JoseHeaders headers) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        String context = (String)message.get(JoseConstants.JOSE_CONTEXT_PROPERTY);
        if (context != null) {
            headers.setHeader(JoseConstants.JOSE_CONTEXT_PROPERTY, context);
        }
    }
    public static void setJoseMessageContextProperty(JoseHeaders headers, String value) {
        headers.setHeader(JoseConstants.JOSE_CONTEXT_PROPERTY, value);
        Message message = PhaseInterceptorChain.getCurrentMessage();
        message.put(JoseConstants.JOSE_CONTEXT_PROPERTY, value);
    }
    public static void setMessageContextProperty(JoseHeaders headers) {
        String context = (String)headers.getHeader(JoseConstants.JOSE_CONTEXT_PROPERTY);
        if (context != null) {
            Message message = PhaseInterceptorChain.getCurrentMessage();
            message.put(JoseConstants.JOSE_CONTEXT_PROPERTY, context);
        }
    }
    public static void validateRequestContextProperty(JoseHeaders headers) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        Object requestContext = message.get(JoseConstants.JOSE_CONTEXT_PROPERTY);
        Object headerContext = headers.getHeader(JoseConstants.JOSE_CONTEXT_PROPERTY);
        if (!Objects.equals(requestContext, headerContext)) {
            LOG.warning("Invalid JOSE context property");
            throw new JoseException();
        }
    }

    public static String checkContentType(String contentType, String defaultType) {
        if (contentType != null) {
            int paramIndex = contentType.indexOf(';');
            String typeWithoutParams = paramIndex == -1 ? contentType : contentType.substring(0, paramIndex);
            if (typeWithoutParams.indexOf('/') == -1) {
                contentType = "application/" + contentType;
            }
        } else {
            contentType = defaultType;
        }
        return contentType;
    }
    public static String expandContentType(String contentType) {
        int paramIndex = contentType.indexOf(';');
        String typeWithoutParams = paramIndex == -1 ? contentType : contentType.substring(0, paramIndex);
        if (typeWithoutParams.indexOf('/') == -1) {
            contentType = "application/" + contentType;
        }
        return contentType;
    }

    public static String decodeToString(String encoded) {
        return new String(decode(encoded), StandardCharsets.UTF_8);
    }
    public static byte[] decode(String encoded) {
        return CryptoUtils.decodeSequence(encoded);
    }

    public static boolean validateCriticalHeaders(JoseHeaders headers) {
        List<String> critical = headers.getCritical();
        if (critical == null) {
            return true;
        }
        // The "crit" value MUST NOT be empty "[]" or contain either duplicate values or "crit"
        if (critical.isEmpty()
            || detectDoubleEntry(critical)
            || critical.contains(JoseConstants.HEADER_CRITICAL)) {
            return false;
        }

        // Check that the headers contain these critical headers
        return headers.asMap().keySet().containsAll(critical);
    }
    private static boolean detectDoubleEntry(List<?> list) {
        Set<Object> inputSet = new HashSet<>(list);
        return list.size() > inputSet.size();
    }

    public static void traceHeaders(JoseHeaders headers) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        if (MessageUtils.getContextualBoolean(m, JoseConstants.JOSE_DEBUG, false)) {
            JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter(true);
            String thePrefix = headers instanceof JwsHeaders ? "JWS" : headers instanceof JweHeaders ? "JWE" : "JOSE";
            LOG.info(thePrefix + " Headers: \r\n" + writer.toJson(headers));
        }
    }

    public static boolean checkBooleanProperty(JoseHeaders headers, Properties props, Message m,
                                               String propertyName) {
        if (headers == null) {
            return false;
        }
        if (props.containsKey(propertyName)) {
            return PropertyUtils.isTrue(props.get(propertyName));
        }
        return MessageUtils.getContextualBoolean(m, propertyName, false);
    }

    //
    // <Start> Copied from JAX-RS RT FRONTEND ResourceUtils
    //

    public static InputStream getResourceStream(String loc, Bus bus) throws IOException {
        URL url = getResourceURL(loc, bus);
        return url == null ? null : url.openStream();
    }

    public static URL getResourceURL(String loc, Bus bus) throws IOException {
        if (loc == null) {
            return null;
        }
        URL url;
        if (loc.startsWith(CLASSPATH_PREFIX)) {
            String path = loc.substring(CLASSPATH_PREFIX.length());
            url = JoseUtils.getClasspathResourceURL(path, JoseUtils.class, bus);
        } else {
            try {
                url = new URL(loc);
            } catch (Exception ex) {
                // it can be either a classpath or file resource without a scheme
                url = JoseUtils.getClasspathResourceURL(loc, JoseUtils.class, bus);
                if (url == null) {
                    File file = new File(loc);
                    if (file.exists()) {
                        url = file.toURI().toURL();
                    }
                }
            }
        }
        if (url == null) {
            LOG.warning("No resource " + loc + " is available");
        }
        return url;
    }

    public static URL getClasspathResourceURL(String path, Class<?> callingClass, Bus bus) {
        URL url = ClassLoaderUtils.getResource(path, callingClass);
        return url == null ? getResource(path, URL.class, bus) : url;
    }

    public static <T> T getResource(String path, Class<T> resourceClass, Bus bus) {
        if (bus != null) {
            ResourceManager rm = bus.getExtension(ResourceManager.class);
            if (rm != null) {
                return rm.resolveResource(path, resourceClass);
            }
        }
        return null;
    }

    public static Properties loadProperties(String propertiesLocation, Bus bus) throws IOException {
        try (InputStream is = getResourceStream(propertiesLocation, bus)) {
            if (is == null) {
                throw new JoseException("The properties file " + propertiesLocation + " could not be read");
            }
            Properties props = new Properties();
            props.load(is);
            return props;
        }
    }
    
    public static AlgorithmParameterSpec createPSSParameterSpec(String size) {
        AlgorithmParameterSpec spec;
        switch (size) {
        case "256" : 
            spec = new PSSParameterSpec(MGF1ParameterSpec.SHA256.getDigestAlgorithm(), 
                                        "MGF1", MGF1ParameterSpec.SHA256, Integer.parseInt(size) / 8, 1);
            break;
        case "384" : 
            spec = new PSSParameterSpec(MGF1ParameterSpec.SHA384.getDigestAlgorithm(),  
                                        "MGF1", MGF1ParameterSpec.SHA384, Integer.parseInt(size) / 8, 1);
            break;
        case "512" : 
            spec = new PSSParameterSpec(MGF1ParameterSpec.SHA512.getDigestAlgorithm(), 
                                        "MGF1", MGF1ParameterSpec.SHA512, Integer.parseInt(size) / 8, 1);
            break;
        default : 
            spec = PSSParameterSpec.DEFAULT;
        }
        return spec;
    }

    //
    // <End> Copied from JAX-RS RT FRONTEND ResourceUtils
    //

}
