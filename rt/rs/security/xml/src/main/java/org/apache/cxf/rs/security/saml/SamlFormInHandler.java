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

package org.apache.cxf.rs.security.saml;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.provider.FormEncodingProvider;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class SamlFormInHandler extends AbstractSamlBase64InHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(SamlFormInHandler.class);
    private static final String SAML_ELEMENT = "SAMLToken";
    private static final String SAML_RELAY_STATE = "RelayState";

    private FormEncodingProvider<Form> provider = new FormEncodingProvider<>(true);
    private List<URI> allowedRelayStateOrigins = Collections.emptyList();

    public SamlFormInHandler() {
    }

    /**
     * Set the origins a RelayState may redirect to in addition to the origin of the service,
     * for example https://app.example.com. Each entry is compared with the RelayState as an
     * exact scheme, host and port, no entries are allowed by default and null resets to that.
     *
     * @throws IllegalArgumentException if an entry is null, has no scheme or host, or has
     *         user info, a path, a query or a fragment
     */
    public void setAllowedRelayStateOrigins(List<String> allowedRelayStateOrigins) {
        this.allowedRelayStateOrigins = allowedRelayStateOrigins == null
            ? Collections.emptyList()
            : allowedRelayStateOrigins.stream().map(SamlFormInHandler::parseOrigin).toList();
    }

    private static URI parseOrigin(String value) {
        URI origin = null;
        if (value != null) {
            try {
                origin = new URI(value);
            } catch (URISyntaxException ex) {
                // reported below
            }
        }
        if (origin == null || origin.getScheme() == null || origin.getHost() == null
            || origin.getRawUserInfo() != null || !origin.getRawPath().isEmpty()
            || origin.getRawQuery() != null || origin.getRawFragment() != null) {
            throw new IllegalArgumentException("Invalid RelayState origin " + value
                + ": it must have a scheme and a host and no user info, path, query or fragment");
        }
        return origin;
    }

    @Override
    public void filter(ContainerRequestContext context) {
        Message message = JAXRSUtils.getCurrentMessage();

        Form form = readFormData(message);
        MultivaluedMap<String, String> formData = form.asMap();
        String assertion = formData.getFirst(SAML_ELEMENT);

        handleToken(message, assertion);

        // redirect if needed
        String samlRequestURI = formData.getFirst(SAML_RELAY_STATE);
        if (samlRequestURI != null) {
            // RelayState may actually represent a reference to a transient local state
            // containing the actual REQUEST URI client was using before being redirected
            // back to IDP - at the moment assume it's URI
            UriInfoImpl ui = new UriInfoImpl(message);
            URI baseUri = ui.getBaseUri();
            if (!samlRequestURI.startsWith(baseUri.toString())) {
                URI location = getRelayStateLocation(samlRequestURI, baseUri);
                if (location != null) {
                    context.abortWith(Response.status(302).location(location).build());
                    return;
                }
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Ignoring RelayState " + samlRequestURI.replaceAll("\\p{Cntrl}", "_")
                        + ": it is not relative and its origin matches neither " + baseUri
                        + " nor the allowed RelayState origins");
                }
            }
        }
        formData.remove(SAML_ELEMENT);
        formData.remove(SAML_RELAY_STATE);

        // restore input stream
        try {
            FormUtils.restoreForm(provider, form, message);
        } catch (Exception ex) {
            throwFault(ex.getMessage(), ex);
        }
    }

    /**
     * Get the location to redirect to, null means the RelayState is ignored and the request
     * continues. RelayState is not covered by the assertion signature, so only a relative location
     * or one on the origin of the service or on an allowed origin is returned. Origins are compared
     * by scheme, host and effective port, a URI whose host java.net.URI can't parse never matches.
     */
    protected URI getRelayStateLocation(String relayState, URI baseUri) {
        try {
            URI location = new URI(relayState);
            if ((location.getScheme() == null && location.getRawAuthority() == null)
                || isSameOrigin(location, baseUri)
                || allowedRelayStateOrigins.stream().anyMatch(origin -> isSameOrigin(location, origin))) {
                return location;
            }
        } catch (URISyntaxException ex) {
            // not a valid URI
        }
        return null;
    }

    private static boolean isSameOrigin(URI location, URI origin) {
        return location.getScheme() != null
            && location.getHost() != null
            && location.getRawUserInfo() == null
            && location.getScheme().equalsIgnoreCase(origin.getScheme())
            && location.getHost().equalsIgnoreCase(origin.getHost())
            && getEffectivePort(location) == getEffectivePort(origin);
    }

    private static int getEffectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        return -1;
    }

    private Form readFormData(Message message) {
        try {
            return FormUtils.readForm(provider, message);
        } catch (Exception ex) {
            throwFault("Error reading the form", ex);
        }
        return null;
    }
}
