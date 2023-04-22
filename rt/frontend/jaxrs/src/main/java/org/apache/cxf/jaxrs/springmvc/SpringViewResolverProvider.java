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
package org.apache.cxf.jaxrs.springmvc;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.provider.AbstractResponseViewProvider;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * CXF view provider that delegates view rendering to Spring MVC Views.
 *
 * Sample usage in a spring application:
 * <pre>
 @Bean
 public SpringViewResolverProvider springViewProvider(ViewResolver viewResolver) {
     SpringViewResolverProvider viewProvider = new SpringViewResolverProvider(viewResolver,
            new AcceptHeaderLocaleResolver());
     viewProvider.setUseClassNames(true);
     viewProvider.setBeanName("model");
     viewProvider.setResourcePaths(Collections.singletonMap("/remove", "registeredClients"));
     return viewProvider;
 }
 * </pre>
 */
public class SpringViewResolverProvider extends AbstractResponseViewProvider {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(SpringViewResolverProvider.class);

    private static final Logger LOG = LogUtils.getL7dLogger(SpringViewResolverProvider.class);

    private final ViewResolver viewResolver;

    private LocaleResolver localeResolver;

    public SpringViewResolverProvider(ViewResolver viewResolver, LocaleResolver localeResolver) {
        if (viewResolver == null) {
            throw new IllegalArgumentException("Argument viewResolver is required");
        }
        if (localeResolver == null) {
            throw new IllegalArgumentException("Argument localeResolver is required");
        }
        this.viewResolver = viewResolver;
        this.localeResolver = localeResolver;
    }

    
    private Locale getLocale() {
        return localeResolver.resolveLocale(getMessageContext().getHttpServletRequest());
    }

    public void writeTo(Object o, Class<?> clazz, Type genericType, Annotation[] annotations, MediaType type,
            MultivaluedMap<String, Object> headers, OutputStream os) throws IOException {

        View view = getView(clazz, o);
        String attributeName = getBeanName(o);
        Map<String, Object> model = Collections.singletonMap(attributeName, o);

        try {
            getMessageContext().put(AbstractHTTPDestination.REQUEST_REDIRECTED, Boolean.TRUE);
            logRedirection(view, attributeName, o);
            view.render(model, getMessageContext().getHttpServletRequest(),
                        getMessageContext().getHttpServletResponse());
        } catch (Throwable ex) {
            handleViewRenderingException(view.toString(), ex);
        }
    }
    

    private void logRedirection(View view, String attributeName, Object o) {
        Level level = isLogRedirects() ? Level.INFO : Level.FINE;
        if (LOG.isLoggable(level)) {
            String message = new org.apache.cxf.common.i18n.Message("RESPONSE_REDIRECTED_TO",
                    BUNDLE, o.getClass().getName(),
                    attributeName, view).toString();
            LOG.log(level, message);
        }
    }

    View getView(Class<?> cls, Object o) {
        String path = getResourcePath(cls, o);
        if (path != null) {
            return resolveView(path);
        } else {
            return null;
        }
    }

    private View resolveView(String viewName) {
        try {
            return viewResolver.resolveViewName(viewName, getLocale());
        } catch (Exception ex) {
            LOG.warning(ExceptionUtils.getStackTrace(ex));
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        }
    }

    @Override
    protected boolean resourceAvailable(String resourceName) {
        return resolveView(resourceName) != null;
    }
}