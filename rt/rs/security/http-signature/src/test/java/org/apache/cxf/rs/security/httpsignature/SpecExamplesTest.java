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
package org.apache.cxf.rs.security.httpsignature;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.rs.security.httpsignature.filters.CreateSignatureInterceptor;
import org.apache.cxf.rs.security.httpsignature.filters.VerifySignatureFilter;
import org.apache.cxf.rs.security.httpsignature.provider.KeyProvider;
import org.apache.cxf.rs.security.httpsignature.provider.MockAlgorithmProvider;
import org.apache.cxf.rs.security.httpsignature.provider.MockSecurityProvider;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * Some examples from the Appendix C of the spec.
 */
public class SpecExamplesTest {

    private static final String MESSAGE_BODY = "Hello";

    private static KeyProvider keyProvider;
    private static PublicKey publicKey;
    private Bus bus;
    
    @BeforeClass
    public static void setUp() throws IOException, InvalidKeySpecException {
        try {
            // Load keys
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = new File(".").getCanonicalPath();
            }

            Path privateKeyPath = FileSystems.getDefault().getPath(basedir, "/src/test/resources/private_key.der");
            byte[] keyBytes = Files.readAllBytes(privateKeyPath);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);
            keyProvider = keyId -> privateKey;

            Path publicKeyPath = FileSystems.getDefault().getPath(basedir, "/src/test/resources/public_key.der");
            byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath);

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            publicKey = KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    
    @Before
    public void setUpBus() {
        bus = BusFactory.getDefaultBus();
    }

    @Test
    public void defaultTest() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();

        MessageSigner messageSigner = new MessageSigner(keyProvider, "Test", Collections.singletonList("Date"));
        messageSigner.sign(headers, "/foo?param=value&pet=dog", "POST");
        String signatureHeader = headers.get("Signature").get(0);

        String expectedHeader = "keyId=\"Test\",algorithm=\"rsa-sha256\","
            + "signature=\"SjWJWbWN7i0wzBvtPl8rbASWz5xQW6mcJmn+ibttBqtifLN7Sazz"
            + "6m79cNfwwb8DMJ5cou1s7uEGKKCs+FLEEaDV5lp7q25WqS+lavg7T8hc0GppauB"
            + "6hbgEKTwblDHYGEtbGmtdHgVCk9SuS13F0hZ8FD0k/5OxEPXe5WozsbM=\"";

        // CXF adds all headers by default, so above we explicitly only add Date just to simulate the expected header
        assertEquals(signatureHeader.replaceAll("headers=\"date\",", ""), expectedHeader);

        // Now check we validate the Date header as expected on an empty header list
        headers.put("Signature", Collections.singletonList(expectedHeader));
        MessageVerifier messageVerifier = new MessageVerifier(keyId -> publicKey);
        messageVerifier.setAddDefaultRequiredHeaders(false);
        messageVerifier.setSecurityProvider(new MockSecurityProvider());
        messageVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
        messageVerifier.verifyMessage(headers, "POST", "/foo?param=value&pet=dog",
                new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test
    public void interceptorDefaultTest() {
        URI uri = URI.create("https://www.example.com/foo?param=value&pet=dog");
        String method = "POST";
        MessageSigner messageSigner = new MessageSigner(keyProvider, "Test", Collections.singletonList("Date"));

        CreateSignatureInterceptor interceptor = new CreateSignatureInterceptor();
        interceptor.setMessageSigner(messageSigner);

        Map<String, List<String>> headers = createMockHeaders();
        MultivaluedMap<String, Object> requestHeaders = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> requestStringHeaders = new MultivaluedHashMap<>();
        headers.forEach((header, value) -> {
            requestHeaders.add(header, value.get(0));
            requestStringHeaders.add(header, value.get(0));
        });

        final Message message = new MessageImpl();
        doInPhaseInterceptor(message, () -> {
            ClientRequestContext requestContext = getClientRequestContextMock(uri, method, requestHeaders);
            interceptor.filter(requestContext);

            String signatureHeader = (String) requestHeaders.get("Signature").get(0);
            requestStringHeaders.add("Signature", signatureHeader);
    
            String expectedHeader = "keyId=\"Test\",algorithm=\"rsa-sha256\","
                + "signature=\"SjWJWbWN7i0wzBvtPl8rbASWz5xQW6mcJmn+ibttBqtifLN7Sazz"
                + "6m79cNfwwb8DMJ5cou1s7uEGKKCs+FLEEaDV5lp7q25WqS+lavg7T8hc0GppauB"
                + "6hbgEKTwblDHYGEtbGmtdHgVCk9SuS13F0hZ8FD0k/5OxEPXe5WozsbM=\"";
    
            assertEquals(signatureHeader.replaceAll("headers=\"date\",", ""), expectedHeader);
    
            // Verify that the request signature can be verified by the filter
            MessageVerifier messageVerifier = new MessageVerifier(keyId -> publicKey);
            messageVerifier.setAddDefaultRequiredHeaders(false);
            messageVerifier.setSecurityProvider(new MockSecurityProvider());
            messageVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
    
            VerifySignatureFilter verifySignatureFilter = new VerifySignatureFilter();
            verifySignatureFilter.setMessageVerifier(messageVerifier);
    
            ContainerRequestContext containerRequestContext =
                getContainerRequestContextMock(uri, method, requestStringHeaders);
    
            verifySignatureFilter.filter(containerRequestContext);
        });
    }


    @Test
    public void basicTest() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();

        MessageSigner messageSigner = new MessageSigner(keyProvider, "Test",
            Arrays.asList("(request-target)", "host", "Date"));
        messageSigner.sign(headers, "/foo?param=value&pet=dog", "POST");
        String signatureHeader = headers.get("Signature").get(0);

        String expectedHeader = "keyId=\"Test\",algorithm=\"rsa-sha256\","
            + "headers=\"(request-target) host date\",signature=\"qdx+H7PHHDZgy4"
            + "y/Ahn9Tny9V3GP6YgBPyUXMmoxWtLbHpUnXS2mg2+SbrQDMCJypxBLSPQR2aAjn"
            + "7ndmw2iicw3HMbe8VfEdKFYRqzic+efkb3nndiv/x1xSHDJWeSWkx3ButlYSuBs"
            + "kLu6kd9Fswtemr3lgdDEmn04swr2Os0=\"";

        assertEquals(signatureHeader, expectedHeader);

        MessageVerifier messageVerifier = new MessageVerifier(keyId -> publicKey);
        messageVerifier.setAddDefaultRequiredHeaders(false);
        messageVerifier.setSecurityProvider(new MockSecurityProvider());
        messageVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
        messageVerifier.verifyMessage(headers, "POST", "/foo?param=value&pet=dog",
                new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test
    public void interceptorBasicTest() {
        URI uri = URI.create("https://www.example.com/foo?param=value&pet=dog");
        String method = "POST";
        MessageSigner messageSigner = new MessageSigner(keyProvider, "Test",
            Arrays.asList("(request-target)", "host", "Date"));

        CreateSignatureInterceptor interceptor = new CreateSignatureInterceptor();
        interceptor.setMessageSigner(messageSigner);

        Map<String, List<String>> headers = createMockHeaders();
        MultivaluedMap<String, Object> requestHeaders = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> requestStringHeaders = new MultivaluedHashMap<>();
        headers.forEach((header, value) -> {
            requestHeaders.add(header, value.get(0));
            requestStringHeaders.add(header, value.get(0));
        });

        final Message message = new MessageImpl();
        doInPhaseInterceptor(message, () -> {
            ClientRequestContext requestContext = getClientRequestContextMock(uri, method, requestHeaders);
    
            interceptor.filter(requestContext);
    
            String signatureHeader = (String) requestHeaders.get("Signature").get(0);
            requestStringHeaders.add("Signature", signatureHeader);
    
            String expectedHeader = "keyId=\"Test\",algorithm=\"rsa-sha256\","
                + "headers=\"(request-target) host date\",signature=\"qdx+H7PHHDZgy4"
                + "y/Ahn9Tny9V3GP6YgBPyUXMmoxWtLbHpUnXS2mg2+SbrQDMCJypxBLSPQR2aAjn"
                + "7ndmw2iicw3HMbe8VfEdKFYRqzic+efkb3nndiv/x1xSHDJWeSWkx3ButlYSuBs"
                + "kLu6kd9Fswtemr3lgdDEmn04swr2Os0=\"";
    
            assertEquals(signatureHeader, expectedHeader);
    
            // Verify that the request signature can be verified by the filter
            MessageVerifier messageVerifier = new MessageVerifier(keyId -> publicKey);
            messageVerifier.setAddDefaultRequiredHeaders(false);
            messageVerifier.setSecurityProvider(new MockSecurityProvider());
            messageVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
    
            VerifySignatureFilter verifySignatureFilter = new VerifySignatureFilter();
            verifySignatureFilter.setMessageVerifier(messageVerifier);
    
            ContainerRequestContext containerRequestContext =
                getContainerRequestContextMock(uri, method, requestStringHeaders);
    
            verifySignatureFilter.filter(containerRequestContext);
        });
    }

    @Test
    public void allHeadersTest() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();

        MessageSigner messageSigner = new MessageSigner(keyProvider, "Test",
            Arrays.asList("(request-target)", "host", "date",
                "content-type", "digest", "content-length"));
        messageSigner.sign(headers, "/foo?param=value&pet=dog", "POST");
        String signatureHeader = headers.get("Signature").get(0);

        String expectedHeader = "keyId=\"Test\",algorithm=\"rsa-sha256\","
            + "headers=\"(request-target) host date content-type digest content-length\","
            + "signature=\"vSdrb+dS3EceC9bcwHSo4MlyKS59iFIrhgYkz8+oVLEEzmYZZvRs"
            + "8rgOp+63LEM3v+MFHB32NfpB2bEKBIvB1q52LaEUHFv120V01IL+TAD48XaERZF"
            + "ukWgHoBTLMhYS2Gb51gWxpeIq8knRmPnYePbF5MOkR0Zkly4zKH7s1dE=\"";

        assertEquals(signatureHeader, expectedHeader);

        MessageVerifier messageVerifier = new MessageVerifier(keyId -> publicKey);
        messageVerifier.setSecurityProvider(new MockSecurityProvider());
        messageVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
        messageVerifier.verifyMessage(headers, "POST", "/foo?param=value&pet=dog",
                new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test
    public void interceptorAllHeadersTest() {
        URI uri = URI.create("https://www.example.com/foo?param=value&pet=dog");
        String method = "POST";
        MessageSigner messageSigner = new MessageSigner(keyProvider, "Test",
            Arrays.asList("(request-target)", "host", "date",
                "content-type", "digest", "content-length"));

        CreateSignatureInterceptor interceptor = new CreateSignatureInterceptor();

        interceptor.setMessageSigner(messageSigner);

        Map<String, List<String>> headers = createMockHeaders();
        MultivaluedMap<String, Object> requestHeaders = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> requestStringHeaders = new MultivaluedHashMap<>();
        headers.forEach((header, value) -> {
            requestHeaders.add(header, value.get(0));
            requestStringHeaders.add(header, value.get(0));
        });

        final Message message = new MessageImpl();
        doInPhaseInterceptor(message, () -> {
            ClientRequestContext requestContext = getClientRequestContextMock(uri, method, requestHeaders);
            interceptor.filter(requestContext);
    
            String signatureHeader = (String) requestHeaders.get("Signature").get(0);
            requestStringHeaders.add("Signature", signatureHeader);
    
            String expectedHeader = "keyId=\"Test\",algorithm=\"rsa-sha256\","
                + "headers=\"(request-target) host date content-type digest content-length\","
                + "signature=\"vSdrb+dS3EceC9bcwHSo4MlyKS59iFIrhgYkz8+oVLEEzmYZZvRs"
                + "8rgOp+63LEM3v+MFHB32NfpB2bEKBIvB1q52LaEUHFv120V01IL+TAD48XaERZF"
                + "ukWgHoBTLMhYS2Gb51gWxpeIq8knRmPnYePbF5MOkR0Zkly4zKH7s1dE=\"";
    
            assertEquals(signatureHeader, expectedHeader);
    
            // Verify that the request signature can be verified by the filter
            MessageVerifier messageVerifier = new MessageVerifier(keyId -> publicKey);
            messageVerifier.setAddDefaultRequiredHeaders(false);
            messageVerifier.setSecurityProvider(new MockSecurityProvider());
            messageVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
    
            VerifySignatureFilter verifySignatureFilter = new VerifySignatureFilter();
            verifySignatureFilter.setMessageVerifier(messageVerifier);
    
            ContainerRequestContext containerRequestContext =
                getContainerRequestContextMock(uri, method, requestStringHeaders);
            InputStream stream = new ByteArrayInputStream("{\"hello\": \"world\"}".getBytes(StandardCharsets.UTF_8));
            Mockito.when(containerRequestContext.getEntityStream()).thenReturn(stream);
    
            verifySignatureFilter.filter(containerRequestContext);
        });
    }

    private ClientRequestContext getClientRequestContextMock(URI uri, String method,
                                                             MultivaluedMap<String, Object> requestHeaders) {
        ClientRequestContext requestContext = Mockito.mock(ClientRequestContext.class);
        Mockito.when(requestContext.getEntity()).thenReturn(null);
        Mockito.when(requestContext.getMethod()).thenReturn(method);
        Mockito.when(requestContext.getHeaders()).thenReturn(requestHeaders);
        Mockito.when(requestContext.getUri()).thenReturn(uri);
        return requestContext;
    }

    private ContainerRequestContext getContainerRequestContextMock(URI uri, String method,
                                                                   MultivaluedMap<String, String>
                                                                       requestStringHeaders) {
        ContainerRequestContext containerRequestContext = Mockito.mock(ContainerRequestContext.class);
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getRequestUri()).thenReturn(uri);
        Mockito.when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        Mockito.when(containerRequestContext.getMethod()).thenReturn(method);
        Mockito.when(containerRequestContext.getHeaders()).thenReturn(requestStringHeaders);
        return containerRequestContext;
    }

    private static Map<String, List<String>> createMockHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Host", Collections.singletonList("example.com"));
        headers.put("Date", Collections.singletonList("Sun, 05 Jan 2014 21:31:40 GMT"));
        headers.put("Content-Type", Collections.singletonList("application/json"));
        headers.put("Digest", Collections.singletonList("SHA-256=X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE="));
        headers.put("Content-Length", Collections.singletonList("18"));
        return headers;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doInPhaseInterceptor(Message message, Runnable r) {
        final PhaseManager phaseManager = bus.getExtension(PhaseManager.class);
        final PhaseInterceptorChain chain = new PhaseInterceptorChain(phaseManager.getInPhases());
        
        chain.add(new PhaseInterceptor<Message>() {
            @Override
            public void handleMessage(Message message) throws Fault {
                r.run();
            }

            @Override
            public void handleFault(Message message) {
            }

            @Override
            public Set getAfter() {
                return Collections.emptySet();
            }

            @Override
            public Set getBefore() {
                return Collections.emptySet();
            }

            @Override
            public String getId() {
                return "test-signature-verify";
            }

            @Override
            public String getPhase() {
                return Phase.INVOKE;
            }

            @Override
            public Collection getAdditionalInterceptors() {
                return Collections.emptyList();
            }
        });
        
        message.setInterceptorChain(chain);
        chain.doIntercept(message);
    }
}
