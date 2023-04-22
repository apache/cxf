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
import java.util.Collection;
import java.util.Map;

import org.apache.cxf.binding.soap.interceptor.EndpointSelectionInterceptor;
import org.apache.cxf.common.i18n.UncheckedException;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.javascript.service.ServiceJavascriptBuilder;
import org.apache.cxf.javascript.types.SchemaJavascriptBuilder;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class JavascriptGetInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final Interceptor<? extends Message> INSTANCE = new JavascriptGetInterceptor();

    private static final String JS_UTILS_PATH = "/org/apache/cxf/javascript/cxf-utils.js";
    private static final String NO_UTILS_QUERY_KEY = "nojsutils";
    private static final String CODE_QUERY_KEY = "js";

    public JavascriptGetInterceptor() {
        super(Phase.READ);
        getAfter().add(EndpointSelectionInterceptor.class.getName());
    }


    public void handleMessage(Message message) throws Fault {
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        String query = (String)message.get(Message.QUERY_STRING);
        if (!"GET".equals(method) || StringUtils.isEmpty(query)) {
            return;
        }
        String baseUri = (String)message.get(Message.REQUEST_URL);
        final URI uri;

        try {
            uri = URI.create(baseUri);
        } catch (IllegalArgumentException iae) {
            //invalid URI, ignore and continue
            return;
        }
        Map<String, String> map = UrlUtils.parseQueryString(query);
        if (isRecognizedQuery(map, uri, message.getExchange().getEndpoint().getEndpointInfo())) {
            try {
                Conduit c = message.getExchange().getDestination().getBackChannel(message);
                Message mout = new MessageImpl();
                mout.setExchange(message.getExchange());
                message.getExchange().setOutMessage(mout);
                mout.put(Message.CONTENT_TYPE, "application/javascript;charset=UTF-8");
                c.prepare(mout);
                OutputStream os = mout.getContent(OutputStream.class);
                writeResponse(uri, map, os, message.getExchange().getEndpoint());
                message.getInterceptorChain().pause();                
            } catch (IOException ioe) {
                throw new Fault(ioe);
            }
        }
    }

    private boolean isRecognizedQuery(Map<String, String> map, URI uri, EndpointInfo endpointInfo) {
        if (uri == null) {
            return false;
        }
        return map.containsKey(CODE_QUERY_KEY);
    }

    public static void writeUtilsToResponseStream(Class<?> referenceClass, OutputStream outputStream) {
        try (InputStream utils = referenceClass.getResourceAsStream(JS_UTILS_PATH)) {
            if (utils == null) {
                throw new RuntimeException("Unable to get stream for " + JS_UTILS_PATH);
            }
            IOUtils.copy(utils, outputStream, 4096);
            //outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write javascript utils to HTTP response.", e);
        }
    }

    private void writeResponse(URI uri, Map<String, String> map, OutputStream os, Endpoint serverEndpoint) {
        if (!map.containsKey(NO_UTILS_QUERY_KEY)) {
            writeUtilsToResponseStream(JavascriptGetInterceptor.class, os);
        }
        if (map.containsKey(CODE_QUERY_KEY)) {
            ServiceInfo serviceInfo = serverEndpoint.getService().getServiceInfos().get(0);
            Collection<SchemaInfo> schemata = serviceInfo.getSchemas();
            // we need to move this to the bus.
            BasicNameManager nameManager = BasicNameManager.newNameManager(serviceInfo, serverEndpoint);
            NamespacePrefixAccumulator prefixManager = new NamespacePrefixAccumulator(serviceInfo
                .getXmlSchemaCollection());
            try {
                OutputStreamWriter writer = new OutputStreamWriter(os, UTF_8);
                for (SchemaInfo schema : schemata) {
                    SchemaJavascriptBuilder builder = new SchemaJavascriptBuilder(serviceInfo
                        .getXmlSchemaCollection(), prefixManager, nameManager);
                    String allThatJavascript = builder.generateCodeForSchema(schema.getSchema());
                    writer.append(allThatJavascript);
                }

                ServiceJavascriptBuilder serviceBuilder
                    = new ServiceJavascriptBuilder(serviceInfo,
                                                   serverEndpoint.getEndpointInfo().getAddress(),
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
            throw new RuntimeException("Invalid query " + uri.toString());
        }
    }

}
