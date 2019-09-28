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
package org.apache.cxf.rs.security.oauth.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthValidator;
import net.oauth.server.OAuthServlet;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.oauth.data.Client;
import org.apache.cxf.rs.security.oauth.data.RequestToken;
import org.apache.cxf.rs.security.oauth.data.Token;
import org.apache.cxf.rs.security.oauth.provider.DefaultOAuthValidator;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;

/**
 * Various utility methods
 */
public final class OAuthUtils {
    public static final String REPORT_FAILURE_DETAILS = "report.failure.details";
    public static final String REPORT_FAILURE_DETAILS_AS_HEADER = "report.failure.details.as.header";

    private OAuthUtils() {
    }

    public static boolean checkRequestURI(String servletPath, String uri) {
        boolean wildcard = uri.endsWith("*");
        String theURI = wildcard ? uri.substring(0, uri.length() - 1) : uri;
        try {
            URITemplate template = new URITemplate(theURI);
            MultivaluedMap<String, String> map = new MetadataMap<>();
            if (template.match(servletPath, map)) {
                String finalGroup = map.getFirst(URITemplate.FINAL_MATCH_GROUP);
                if (wildcard || StringUtils.isEmpty(finalGroup) || "/".equals(finalGroup)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            // ignore
        }
        return false;
    }

    public static void validateMessage(OAuthMessage oAuthMessage,
                                       Client client,
                                       Token token,
                                       OAuthDataProvider provider,
                                       OAuthValidator validator)
        throws Exception {
        OAuthConsumer consumer = new OAuthConsumer(null, client.getConsumerKey(),
            client.getSecretKey(), null);
        OAuthAccessor accessor = new OAuthAccessor(consumer);
        if (token != null) {
            if (token instanceof RequestToken) {
                accessor.requestToken = token.getTokenKey();
            } else {
                accessor.accessToken = token.getTokenKey();
            }
            accessor.tokenSecret = token.getTokenSecret();
        }
        try {
            validator.validateMessage(oAuthMessage, accessor);
        } catch (Exception ex) {
            if (token != null) {
                provider.removeToken(token);
            }
            throw ex;
        }
        if (token != null && validator instanceof DefaultOAuthValidator) {
            ((DefaultOAuthValidator)validator).validateToken(token, provider);
        }
    }

    public static OAuthMessage getOAuthMessage(MessageContext mc,
                                               HttpServletRequest request,
                                               String[] requiredParams) throws Exception {
        OAuthMessage oAuthMessage = OAuthServlet.getMessage(request, request.getRequestURL().toString());
        OAuthUtils.addParametersIfNeeded(mc, request, oAuthMessage);
        oAuthMessage.requireParameters(requiredParams);
        return oAuthMessage;
    }

    public static void addParametersIfNeeded(MessageContext mc,
                                             HttpServletRequest request,
                                             OAuthMessage oAuthMessage) throws IOException {
        List<Entry<String, String>> params = oAuthMessage.getParameters();
        String enc = oAuthMessage.getBodyEncoding();
        enc = enc == null ? StandardCharsets.UTF_8.name() : enc;

        if (params.isEmpty()
            && MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(
                MediaType.valueOf(oAuthMessage.getBodyType()))) {
            InputStream stream = mc != null
                ? mc.getContent(InputStream.class) : oAuthMessage.getBodyAsStream();
            String body = FormUtils.readBody(stream, enc);
            MultivaluedMap<String, String> map = new MetadataMap<>();
            FormUtils.populateMapFromString(map, PhaseInterceptorChain.getCurrentMessage(), body, enc, true,
                                            request);
            for (String key : map.keySet()) {
                oAuthMessage.addParameter(key, map.getFirst(key));
            }
        } else {
            // This path will most likely work only for the AuthorizationRequestService
            // when processing a user confirmation with only 3 parameters expected
            String ct = request.getContentType();
            if (MediaType.APPLICATION_FORM_URLENCODED.equals(ct)) {
                Map<String, List<String>> map = new HashMap<>();
                for (Entry<String, String> param : params) {
                    map.put(param.getKey(), Collections.singletonList(param.getValue()));
                }
                FormUtils.logRequestParametersIfNeeded(map, enc);
            }
        }
    }


    public static Response handleException(MessageContext mc,
                                           Exception e,
                                           int status) {
        ResponseBuilder builder = Response.status(status);
        if (PropertyUtils.isTrue(mc.getContextualProperty(REPORT_FAILURE_DETAILS))) {
            boolean asHeader = PropertyUtils.isTrue(
                mc.getContextualProperty(REPORT_FAILURE_DETAILS_AS_HEADER));
            String text = null;
            if (e instanceof OAuthProblemException) {
                OAuthProblemException problem = (OAuthProblemException)e;
                if (asHeader && problem.getProblem() != null) {
                    text = problem.getProblem();
                }
            }
            if (text == null) {
                text = e.getMessage();
            }
            if (asHeader) {
                builder.header("oauth_problem", text);
            } else {
                builder.entity(e.getMessage());
            }
        }
        return builder.build();
    }

    public static List<String> parseParamValue(String paramValue, String defaultValue)
        throws IOException {

        List<String> scopeList = new ArrayList<>();

        if (!StringUtils.isEmpty(paramValue)) {
            StringTokenizer tokenizer = new StringTokenizer(paramValue, " ");

            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                scopeList.add(token);
            }
        }
        if (defaultValue != null && !scopeList.contains(defaultValue)) {
            scopeList.add(defaultValue);
        }
        return scopeList;
    }


    public static RequestToken handleTokenRejectedException() throws OAuthProblemException {
        OAuthProblemException problemEx = new OAuthProblemException(
                OAuth.Problems.TOKEN_REJECTED);
        problemEx
                .setParameter(OAuthProblemException.HTTP_STATUS_CODE, HttpServletResponse.SC_UNAUTHORIZED);
        throw problemEx;
    }

    public static Object instantiateClass(String className) throws Exception {
        Class<?> clazz = ClassLoaderUtils.loadClass(className, OAuthUtils.class);
        return clazz.newInstance();
    }

    public static synchronized OAuthDataProvider getOAuthDataProvider(
            OAuthDataProvider provider,
            ServletContext servletContext) {
        if (provider != null) {
            return provider;
        }
        return getOAuthDataProvider(servletContext);
    }

    public static synchronized OAuthDataProvider getOAuthDataProvider(
            ServletContext servletContext) {
        OAuthDataProvider dataProvider = (OAuthDataProvider) servletContext
                .getAttribute(OAuthConstants.OAUTH_DATA_PROVIDER_INSTANCE_KEY);

        if (dataProvider == null) {
            String dataProviderClassName = servletContext
                    .getInitParameter(OAuthConstants.OAUTH_DATA_PROVIDER_CLASS);

            if (StringUtils.isEmpty(dataProviderClassName)) {
                throw new RuntimeException(
                        "There should be provided [ " + OAuthConstants.OAUTH_DATA_PROVIDER_CLASS
                                + " ] context init param in web.xml");
            }

            try {
                dataProvider = (OAuthDataProvider) OAuthUtils
                        .instantiateClass(dataProviderClassName);

                servletContext
                        .setAttribute(OAuthConstants.OAUTH_DATA_PROVIDER_INSTANCE_KEY, dataProvider);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Cannot instantiate OAuth Data Provider class: " + dataProviderClassName, e);
            }
        }

        return dataProvider;
    }

    public static synchronized OAuthValidator getOAuthValidator(ServletContext servletContext) {

        OAuthValidator dataProvider = (OAuthValidator) servletContext
              .getAttribute(OAuthConstants.OAUTH_VALIDATOR_INSTANCE_KEY);

        if (dataProvider == null) {
            String dataProviderClassName = servletContext
                .getInitParameter(OAuthConstants.OAUTH_VALIDATOR_CLASS);

            if (!StringUtils.isEmpty(dataProviderClassName)) {

                try {
                    dataProvider = (OAuthValidator) OAuthUtils
                        .instantiateClass(dataProviderClassName);

                    servletContext
                        .setAttribute(OAuthConstants.OAUTH_VALIDATOR_INSTANCE_KEY, dataProvider);
                } catch (Exception e) {
                    throw new RuntimeException(
                        "Cannot instantiate OAuthValidator class: " + dataProviderClassName, e);
                }
            }
        }

        return dataProvider == null ? new DefaultOAuthValidator() : dataProvider;
    }

}
