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
package org.apache.cxf.rs.security.oauth2.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.TokenIntrospection;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

@Provider
@Produces("application/json")
@Consumes("application/json")
public class OAuthJSONProvider implements MessageBodyWriter<Object>,
    MessageBodyReader<Object> {

    public long getSize(Object obj, Class<?> clt, Type t, Annotation[] anns, MediaType mt) {
        return -1;
    }

    public boolean isWriteable(Class<?> cls, Type t, Annotation[] anns, MediaType mt) {
        return cls == ClientAccessToken.class || cls == OAuthError.class || cls == TokenIntrospection.class;
    }

    public void writeTo(Object obj, Class<?> cls, Type t, Annotation[] anns, MediaType mt,
                        MultivaluedMap<String, Object> headers, OutputStream os) throws IOException,
        WebApplicationException {
        if (obj instanceof ClientAccessToken) {
            writeAccessToken((ClientAccessToken)obj, os);
        } else if (obj instanceof TokenIntrospection) {
            writeTokenIntrospection((TokenIntrospection)obj, os);
        } else {
            writeOAuthError((OAuthError)obj, os);
        }
    }

    private void writeTokenIntrospection(TokenIntrospection obj, OutputStream os) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        appendJsonPair(sb, "active", obj.isActive(), false);
        if (obj.isActive()) {
            if (obj.getClientId() != null) {
                sb.append(',');
                appendJsonPair(sb, OAuthConstants.CLIENT_ID, obj.getClientId());
            }
            if (obj.getUsername() != null) {
                sb.append(',');
                appendJsonPair(sb, "username", obj.getUsername());
            }
            if (obj.getTokenType() != null) {
                sb.append(',');
                appendJsonPair(sb, OAuthConstants.ACCESS_TOKEN_TYPE, obj.getTokenType());
            }
            if (obj.getScope() != null) {
                sb.append(',');
                appendJsonPair(sb, OAuthConstants.SCOPE, obj.getScope());
            }
            if (!StringUtils.isEmpty(obj.getAud())) {
                sb.append(',');
                if (obj.getAud().size() == 1) {
                    appendJsonPair(sb, "aud", obj.getAud().get(0));
                } else {
                    StringBuilder arr = new StringBuilder();
                    arr.append('[');
                    List<String> auds = obj.getAud();
                    for (int i = 0; i < auds.size(); i++) {
                        if (i > 0) {
                            arr.append(',');
                        }
                        arr.append('"').append(auds.get(i)).append('"');
                    }
                    arr.append(']');
                    appendJsonPair(sb, "aud", arr.toString(), false);

                }
            }
            if (obj.getIss() != null) {
                sb.append(',');
                appendJsonPair(sb, "iss", obj.getIss());
            }
            sb.append(',');
            appendJsonPair(sb, "iat", obj.getIat(), false);
            if (obj.getExp() != null) {
                sb.append(',');
                appendJsonPair(sb, "exp", obj.getExp(), false);
            }
            if (obj.getNbf() != null) {
                sb.append(',');
                appendJsonPair(sb, "nbf", obj.getNbf(), false);
            }
            if (!obj.getExtensions().isEmpty()) {
                for (Map.Entry<String, String> entry : obj.getExtensions().entrySet()) {
                    sb.append(',');
                    if (JoseConstants.HEADER_X509_THUMBPRINT_SHA256.equals(entry.getKey())) {
                        StringBuilder cnfObj = new StringBuilder();
                        cnfObj.append('{');
                        appendJsonPair(cnfObj, entry.getKey(), entry.getValue());
                        cnfObj.append('}');
                        appendJsonPair(sb, JwtConstants.CLAIM_CONFIRMATION, cnfObj.toString(), false);
                    } else {
                        appendJsonPair(sb, entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        sb.append('}');
        String result = sb.toString();
        os.write(result.getBytes(StandardCharsets.UTF_8));
        os.flush();

    }

    private void writeOAuthError(OAuthError obj, OutputStream os) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        appendJsonPair(sb, OAuthConstants.ERROR_KEY, obj.getError());
        if (obj.getErrorDescription() != null) {
            sb.append(',');
            appendJsonPair(sb, OAuthConstants.ERROR_DESCRIPTION_KEY, obj.getErrorDescription());
        }
        if (obj.getErrorUri() != null) {
            sb.append(',');
            appendJsonPair(sb, OAuthConstants.ERROR_URI_KEY, obj.getErrorUri());
        }

        sb.append('}');
        String result = sb.toString();
        os.write(result.getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    private void writeAccessToken(ClientAccessToken obj, OutputStream os) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        appendJsonPair(sb, OAuthConstants.ACCESS_TOKEN, obj.getTokenKey());
        sb.append(',');
        appendJsonPair(sb, OAuthConstants.ACCESS_TOKEN_TYPE, obj.getTokenType());
        if (obj.getExpiresIn() != -1) {
            sb.append(',');
            appendJsonPair(sb, OAuthConstants.ACCESS_TOKEN_EXPIRES_IN, obj.getExpiresIn(), false);
        }
        if (obj.getApprovedScope() != null) {
            sb.append(',');
            appendJsonPair(sb, OAuthConstants.SCOPE, obj.getApprovedScope());
        }
        if (obj.getRefreshToken() != null) {
            sb.append(',');
            appendJsonPair(sb, OAuthConstants.REFRESH_TOKEN, obj.getRefreshToken());
        }
        Map<String, String> parameters = obj.getParameters();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            sb.append(',');
            appendJsonPair(sb, entry.getKey(), entry.getValue());
        }
        sb.append('}');
        String result = sb.toString();
        os.write(result.getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    private void appendJsonPair(StringBuilder sb, String key, Object value) {
        appendJsonPair(sb, key, value, true);
    }

    private void appendJsonPair(StringBuilder sb, String key, Object value,
                                boolean valueQuote) {
        sb.append('"').append(key).append('"');
        sb.append(':');
        if (valueQuote) {
            sb.append('"');
        }
        sb.append(value);
        if (valueQuote) {
            sb.append('"');
        }
    }

    public boolean isReadable(Class<?> cls, Type t, Annotation[] anns, MediaType mt) {
        return Map.class.isAssignableFrom(cls)
            || ClientAccessToken.class.isAssignableFrom(cls)
            || TokenIntrospection.class.isAssignableFrom(cls);
    }

    public Object readFrom(Class<Object> cls, Type t, Annotation[] anns,
                           MediaType mt, MultivaluedMap<String, String> headers, InputStream is)
        throws IOException, WebApplicationException {
        if (TokenIntrospection.class.isAssignableFrom(cls)) {
            return fromMapToTokenIntrospection(is);
        }
        Map<String, String> params = readJSONResponse(is);
        if (Map.class.isAssignableFrom(cls)) {
            return params;
        }
        ClientAccessToken token = OAuthClientUtils.fromMapToClientToken(params);
        if (token == null) {
            throw new WebApplicationException(500);
        }
        return token;

    }

    private Object fromMapToTokenIntrospection(InputStream is) throws IOException {
        TokenIntrospection resp = new TokenIntrospection();
        Map<String, Object> params = new JsonMapObjectReaderWriter().fromJson(is);
        resp.setActive((Boolean)params.get("active"));
        String clientId = (String)params.get(OAuthConstants.CLIENT_ID);
        if (clientId != null) {
            resp.setClientId(clientId);
        }
        String username = (String)params.get("username");
        if (username != null) {
            resp.setUsername(username);
        }
        String scope = (String)params.get(OAuthConstants.SCOPE);
        if (scope != null) {
            resp.setScope(scope);
        }
        String tokenType = (String)params.get(OAuthConstants.ACCESS_TOKEN_TYPE);
        if (tokenType != null) {
            resp.setTokenType(tokenType);
        }
        Object aud = params.get("aud");
        if (aud != null) {
            if (aud.getClass() == String.class) {
                resp.setAud(Collections.singletonList((String)aud));
            } else {
                @SuppressWarnings("unchecked")
                List<String> auds = (List<String>)aud;
                resp.setAud(auds);
            }
        }
        String iss = (String)params.get("iss");
        if (iss != null) {
            resp.setIss(iss);
        }
        Long iat = (Long)params.get("iat");
        if (iat != null) {
            resp.setIat(iat);
        }
        Long exp = (Long)params.get("exp");
        if (exp != null) {
            resp.setExp(exp);
        }
        Long nbf = (Long)params.get("nbf");
        if (nbf != null) {
            resp.setNbf(nbf);
        }
        Map<String, Object> cnf = CastUtils.cast((Map<?, ?>)params.get(JwtConstants.CLAIM_CONFIRMATION));
        if (cnf != null) {
            String thumbprint = (String)cnf.get(JoseConstants.HEADER_X509_THUMBPRINT_SHA256);
            if (thumbprint != null) {
                resp.getExtensions().put(JoseConstants.HEADER_X509_THUMBPRINT_SHA256, thumbprint);
            }
        }

        return resp;
    }

    public Map<String, String> readJSONResponse(InputStream is) throws IOException  {
        String str = IOUtils.readStringFromStream(is).trim();
        if (str.length() == 0) {
            return Collections.emptyMap();
        }

        Map<String, Object> response = new JsonMapObjectReaderWriter().fromJson(str);

        // Here we're going to assume the values are all Strings
        Map<String, String> forcedStringResponse = new HashMap<>(response.size());
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            forcedStringResponse.put(entry.getKey(), entry.getValue().toString());
        }

        return forcedStringResponse;
    }

}
