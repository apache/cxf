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

package org.apache.cxf.jaxrs.swagger.ui;

import java.lang.annotation.Annotation;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OsgiSwaggerUiResolverTest {

    private String webjarsGroupAndArtifact = "org.webjars/swagger-ui";
    private String webjarsVersion = "4.13.2";
    private final Class<? extends Annotation> annotationBundle = Test.class;
    private Bundle rootBundle;

    @Mock private BundleContext bundleContext;
    @Mock private FrameworkUtilWrapper frameworkUtilWrapper;

    private OsgiSwaggerUiResolver osgiSwaggerUiResolver;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        osgiSwaggerUiResolver = new OsgiSwaggerUiResolver(annotationBundle);
        osgiSwaggerUiResolver.setFrameworkUtilWrapper(frameworkUtilWrapper);
        rootBundle = mockBundle("1.0", "mvn:org.apache.cxf/cxf-rt-rs-service-description-swagger-ui/4.0.0", null);
        when(frameworkUtilWrapper.getBundle(annotationBundle)).thenReturn(rootBundle);
    }

    @Test
    public void testFindSwaggerUiRootInternalWithNullParameters() throws Exception {
        String swaggerEntryFile = "file://index.html";
        String webjarsLocation = "mvn:" + webjarsGroupAndArtifact + "/" + webjarsVersion;
        when(rootBundle.getState()).thenReturn(Bundle.ACTIVE);

        Bundle webjarsBundle = mockBundle(webjarsVersion, webjarsLocation, new URL(swaggerEntryFile));
        Bundle otherBundle = mockBundle("1.0", "mvn:group/artifact/1.0", null);
        Bundle otherBundle2 = mockBundle("2.0", "mvn:group2/artifact2/2.0", null);

        when(bundleContext.getBundles()).thenReturn(new Bundle[] {otherBundle, otherBundle2, webjarsBundle});

        String swaggerUiRoot = osgiSwaggerUiResolver.findSwaggerUiRootInternal(null, null);

        assertEquals("Trailing slash NOT added", swaggerEntryFile + "/", swaggerUiRoot);
        verify(rootBundle, never().description("Bundle not started since it was active")).start();
    }

    @Test
    public void testFindSwaggerUiRootInternalWithMavenIdentifiersProvided() throws Exception {
        when(rootBundle.getState()).thenReturn(Bundle.RESOLVED);
        String swaggerEntryFile = "file://index.html/";
        String webjarsLocation = "mvn:" + webjarsGroupAndArtifact + "/" + webjarsVersion;

        Bundle webjarsBundle = mockBundle(webjarsVersion, webjarsLocation, new URL(swaggerEntryFile));
        Bundle otherBundle = mockBundle("1.0", "mvn:group/artifact/1.0", null);
        Bundle otherBundle2 = mockBundle("2.0", "mvn:group2/artifact2/2.0", null);

        when(bundleContext.getBundles()).thenReturn(new Bundle[] {otherBundle, otherBundle2, webjarsBundle});

        String swaggerUiRoot = osgiSwaggerUiResolver.findSwaggerUiRootInternal(webjarsGroupAndArtifact, webjarsVersion);

        assertEquals("Trailing slash added unnecessarily", swaggerEntryFile, swaggerUiRoot);
        verify(rootBundle, times(1).description("Bundle started since it was not active")).start();
    }

    private Bundle mockBundle(String version, String location, URL entryValue) {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBundleContext()).thenReturn(bundleContext);
        when(bundle.getVersion()).thenReturn(new Version(version));
        when(bundle.getLocation()).thenReturn(location);
        if (entryValue != null) {
            when(bundle.getEntry(SwaggerUiResolver.UI_RESOURCES_ROOT_START + webjarsVersion)).thenReturn(entryValue);
        }
        return bundle;
    }
}
