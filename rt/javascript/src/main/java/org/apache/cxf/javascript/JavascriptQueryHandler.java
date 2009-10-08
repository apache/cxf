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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.UncheckedException;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.javascript.service.ServiceJavascriptBuilder;
import org.apache.cxf.javascript.types.SchemaJavascriptBuilder;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.DestinationWithEndpoint;
import org.apache.cxf.transport.http.UrlUtilities;
import org.apache.cxf.transports.http.StemMatchingQueryHandler;

public class JavascriptQueryHandler implements StemMatchingQueryHandler {
    private static final String JS_UTILS_PATH = "/org/apache/cxf/javascript/cxf-utils.js";
    private static final Logger LOG = LogUtils.getL7dLogger(JavascriptQueryHandler.class);
    private static final Charset UTF8 = Charset.forName("utf-8");
    private static final String NO_UTILS_QUERY_KEY = "nojsutils";
    private static final String CODE_QUERY_KEY = "js";
    private Bus bus;

    public JavascriptQueryHandler(Bus b) {
        bus = b;
        LOG.finest("Bus " + bus);
    }

    public String getResponseContentType(String fullQueryString, String ctx) {
        URI uri = URI.create(fullQueryString);
        Map<String, String> map = UrlUtilities.parseQueryString(uri.getQuery());
        if (map.containsKey(CODE_QUERY_KEY)) {
            return "application/javascript;charset=UTF-8";
        }
        return null;
    }

    public boolean isRecognizedQuery(String baseUri, String ctx, EndpointInfo endpointInfo,
                                     boolean contextMatchExact) {
        if (baseUri == null) {
            return false;
        }
        URI uri = URI.create(baseUri);
        Map<String, String> map = UrlUtilities.parseQueryString(uri.getQuery());
        if (map.containsKey(CODE_QUERY_KEY)) {
            return endpointInfo.getAddress().contains(UrlUtilities.getStem(uri.getSchemeSpecificPart()));
        }
        return false;
    }
    
    public static void writeUtilsToResponseStream(Class<?> referenceClass, OutputStream outputStream) {
        InputStream utils = referenceClass.getResourceAsStream(JS_UTILS_PATH);
        if (utils == null) {
            throw new RuntimeException("Unable to get stream for " + JS_UTILS_PATH);
        }
        // it's amazing that this still has to be coded up.
        byte buffer[] = new byte[1024];
        int count;
        try {
            while ((count = utils.read(buffer, 0, 1024)) > 0) {
                outputStream.write(buffer, 0, count);
            }
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write javascript utils to HTTP response.", e);
        }
    }
    
    private Endpoint findEndpoint(EndpointInfo endpointInfo) {
        ServerRegistry serverRegistry = bus.getExtension(ServerRegistry.class);
        for (Server server : serverRegistry.getServers()) {
            // Hypothetically, not all destinations have an endpoint.
            // There has to be a better way to do this.
            if (server.getDestination() instanceof DestinationWithEndpoint
                &&
                endpointInfo.
                    equals(((DestinationWithEndpoint)server.getDestination()).getEndpointInfo())) {
                return server.getEndpoint();
            }
        }
        return null;
    }

    public void writeResponse(String fullQueryString, String ctx, EndpointInfo endpoint, OutputStream os) {
        URI uri = URI.create(fullQueryString);
        String query = uri.getQuery();
        Map<String, String> map = UrlUtilities.parseQueryString(query);
        OutputStreamWriter writer = new OutputStreamWriter(os, UTF8);
        if (!map.containsKey(NO_UTILS_QUERY_KEY)) {
            writeUtilsToResponseStream(JavascriptQueryHandler.class, os);
        } 
        if (map.containsKey(CODE_QUERY_KEY)) {
            ServiceInfo serviceInfo = endpoint.getService();
            Collection<SchemaInfo> schemata = serviceInfo.getSchemas();
            Endpoint serverEndpoint = findEndpoint(endpoint);
            // we need to move this to the bus.
            BasicNameManager nameManager = BasicNameManager.newNameManager(serviceInfo, serverEndpoint);
            NamespacePrefixAccumulator prefixManager = new NamespacePrefixAccumulator(serviceInfo
                .getXmlSchemaCollection());
            try {
                for (SchemaInfo schema : schemata) {
                    SchemaJavascriptBuilder builder = new SchemaJavascriptBuilder(serviceInfo
                        .getXmlSchemaCollection(), prefixManager, nameManager);
                    String allThatJavascript = builder.generateCodeForSchema(schema);
                    writer.append(allThatJavascript);
                }

                ServiceJavascriptBuilder serviceBuilder = new ServiceJavascriptBuilder(serviceInfo,
                                                                                       endpoint.getAddress(),
                                                                                       prefixManager,
                                                                                       nameManager);
                serviceBuilder.walk();
                String serviceJavascript = serviceBuilder.getCode();
                writer.append(serviceJavascript);
                writer.flush();
            } catch (IOException e) {
                throw new UncheckedException(e);
            }
        } else {
            throw new RuntimeException("Invalid query " + fullQueryString);
        }
    }

    public boolean isRecognizedQuery(String fullQueryString, String ctx, EndpointInfo endpoint) {
        return isRecognizedQuery(fullQueryString, ctx, endpoint, false);
    }
}
