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

package org.apache.cxf.management.web.browser.bootstrapping;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.Validate;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.provider.JSONProvider;

@Path("/browser")
public class BootstrapStorage {
    private static final Logger LOGGER = LogUtils.getL7dLogger(BootstrapStorage.class);
    private final SettingsStorage storage;
    
    public BootstrapStorage(final SettingsStorage storage) {
        Validate.notNull(storage, "provider is null");
        
        this.storage = storage;
    }

    @GET
    @Path("/settings")
    @Produces("application/json")
    public Settings getSettings() {

        //TODO Remove username everywhere
        String username = "admin";

        Validate.notNull(username, "username is null");
        Validate.notEmpty(username, "username is empty");

        LOGGER.fine(String.format("Retrieve settings, user='%s'", username));

        return storage.getSettings(username);
    }

    @PUT
    @Path("/settings")
    @Consumes("application/json")
    public Response setSettings(final Settings settings) {

        //TODO Remove username everywhere
        String username = "admin";

        Validate.notNull(username, "username is null");
        Validate.notEmpty(username, "username is empty");
        Validate.notNull(settings, "settings is null");
        
        LOGGER.fine(String.format("Save settings, user='%s'; settings='%s'", username, settings));

        storage.setSettings(username, settings);
        return Response.ok().build();
    }

    @GET
    @Path("{resource:.*}")
    public Response getResource(@Context final MessageContext mc,
                                @PathParam("resource") final String resource) {
        if (isLastModifiedRequest(mc)) {
            return Response.notModified().build();
        }

        try {
            URL url;
            URL jar = getClass().getProtectionDomain().getCodeSource().getLocation();            

            url = new URL(String.format("jar:%s!/static-content/logbrowser/%s", jar, resource));

            JarURLConnection connection = (JarURLConnection) url.openConnection();
            if (connection.getContentLength() == -1 || connection.getJarEntry() == null) {
                return Response.status(Status.NOT_FOUND).build();
            } else if (connection.getJarEntry().isDirectory()) {
                return Response.status(Status.FORBIDDEN).build();
            } else { // correct
                MediaType mime = getMimeType(mc, resource);
                StaticFile staticFile = new StaticFile(url, acceptsGzip(mc), mime);
                
                Response.ResponseBuilder builder = Response.ok(staticFile);
                builder.variant(new Variant(mime , null, staticFile.isGzipEnabled() ? "gzip" : null));

                return builder.build();
            }
        } catch (MalformedURLException e) {
            return Response.status(Status.BAD_REQUEST).build();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occur while retrieve static file", e);
            return Response.serverError().build();
        }
    }

    private boolean isLastModifiedRequest(final MessageContext mc) {
        return mc.getHttpServletRequest().getHeader("Last-Modified") != null;
    }

    private MediaType getMimeType(final MessageContext mc, final String resource) {
        return MediaType.valueOf(mc.getServletContext().getMimeType(resource));
    }

    private boolean acceptsGzip(final MessageContext mc) {
        String ae = mc.getHttpServletRequest().getHeader("Accept-Encoding");
        return ae != null && ae.contains("gzip");
    }

    private final class StaticFile {
        private URL url;
        private boolean isGzipEnabled;

        private StaticFile(URL url, boolean acceptsGzip, MediaType mime) {
            assert url != null;
            assert mime != null;

            this.url = url;
            this.isGzipEnabled = acceptsGzip && "text".equals(mime.getType());
        }

        public URL getUrl() {
            return this.url;
        }

        public boolean isGzipEnabled() {
            return this.isGzipEnabled;
        }
    }

    @Provider
    public static class StaticFileProvider implements MessageBodyWriter<StaticFile> {
        
        public boolean isWriteable(final Class<?> type, final Type genericType,
                                   final Annotation[] annotations, final MediaType mediaType) {
            return StaticFile.class.isAssignableFrom(type);
        }

        public long getSize(final StaticFile staticFile, final Class<?> type, final Type genericType,
                            final Annotation[] annotations, final MediaType mediaType) {
            return -1;
        }

        public void writeTo(final StaticFile staticFile, final Class<?> clazz, final Type genericType,
                            final Annotation[] annotations, final MediaType type,
                            final MultivaluedMap<String, Object> headers, final OutputStream os)
            throws IOException {

            if (staticFile.isGzipEnabled()) {
                GZIPOutputStream gzip = new GZIPOutputStream(os);
                try {
                    IOUtils.copyAndCloseInput(staticFile.getUrl().openStream(), gzip);
                } finally {
                    gzip.finish();
                }
            } else {
                IOUtils.copyAndCloseInput(staticFile.getUrl().openStream(), os);
            }
        }
    }

    @Provider
    public static class SettingsProvider extends JSONProvider {
        private static final String LOGGING_NAMESPACE = "http://cxf.apache.org/log";
        private static final String SUBSCRIPTIONS_ARRAY = "subscriptions";

        public SettingsProvider() {
            setIgnoreNamespaces(true);

            // Solved common JSON's problem with parsing array, which has only one element 
            setSerializeAsArray(true);
            setArrayKeys(Arrays.asList(SUBSCRIPTIONS_ARRAY));

            // Removes namespace from output
            setOutTransformElements(new HashMap<String, String>() {
                {
                    put("{" + LOGGING_NAMESPACE + "}*", "*");
                }
            });

            // Adds namespace to input
            setInTransformElements(new HashMap<String, String>() {
                {
                    put("*", "{" + LOGGING_NAMESPACE + "}*");
                }
            });
        }
    }
}
