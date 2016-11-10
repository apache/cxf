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
package org.apache.cxf.maven.invoke.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.feature.LoggingFeature;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static org.apache.cxf.maven.invoke.plugin.XmlUtil.document;

/**
 * Maven MOJO to invoke SOAP service in Maven execution. Not tied to any Maven lifecycle phase so configure your own
 * executions.
 */
@Mojo(name = "invoke-soap", defaultPhase = LifecyclePhase.NONE)
public final class InvokeSoap extends AbstractMojo {

    /** URL for the service where the request will be sent */
    @Parameter(property = "cxf.invoke.endpoint", required = false)
    String endpoint;

    /** SOAP headers to add in the request */
    @Parameter(property = "cxf.invoke.headers", required = false)
    Node[] headers;

    /** {@link MojoExecution} needed to get execution id */
    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    MojoExecution mojoExecution;

    /** Target namespace of the SOAP service */
    @Parameter(property = "cxf.invoke.namespace", required = true)
    String namespace;

    /** Operation to invoke on the service */
    @Parameter(property = "cxf.invoke.operation", required = true)
    String operation;

    /** Port name of the SOAP service */
    @Parameter(property = "cxf.invoke.port", required = false)
    String portName;

    /** Needed to set any extracted properties */
    @Parameter(readonly = true, defaultValue = "${project}")
    MavenProject project;

    /** Properties to extract from the SOAP response */
    @Parameter(property = "cxf.invoke.properties")
    final Map<String, String> properties = new HashMap<>();

    /** If repeating, how long to wait before next invocation of the service, default 5 seconds */
    @Parameter(property = "cxf.invoke.repeatInterval", required = false, defaultValue = "5000")
    int repeatInterval;

    /** XPath expression to determine if the request should be repeated */
    @Parameter(property = "cxf.invoke.repeatUntil", required = false)
    String repeatUntil;

    /** Compiled XPath expression for repetition */
    XPathExpression repeatUntilExpression;

    /** SOAP request, Maven parameter conversion forces us to use array even if only has one element */
    @Parameter(property = "cxf.invoke.request", required = true)
    Node[] request;

    /** Path in which to store SOAP request and response XMLs */
    @Parameter(property = "cxf.invoke.request.path", required = true, defaultValue = "${project.build.directory}")
    File requestPath;

    /** Name of the SOAP service to invoke */
    @Parameter(property = "cxf.invoke.service", required = true)
    String serviceName;

    final Transformer transformer;

    /** URL for the WSDL document of the SOAP service */
    @Parameter(property = "cxf.invoke.wsdl", required = true)
    URI wsdl;

    public InvokeSoap() {
        this(XmlUtil.transformer());
    }

    InvokeSoap(final Transformer transformer) {
        this.transformer = transformer;
    }

    /**
     * Given a request, return {@link Source}.
     *
     * @param request
     * @return the source
     */
    static Source createRequest(final Node request) {
        return new DOMSource(request);
    }

    /**
     * Main MOJO entry point, invokes the SOAP service, repeats if needed, and extracts the properties in the end.
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (repeatUntil != null) {
            try {
                repeatUntilExpression = XmlUtil.xpathExpression(repeatUntil);
            } catch (final XPathExpressionException e) {
                throw new MojoExecutionException("Unable to compile XPath expression `" + repeatUntil + "`", e);
            }
        }

        boolean first = true;
        Document response;
        do {
            if (!first) {
                try {
                    Thread.sleep(repeatInterval);
                } catch (final InterruptedException e) {
                    return;
                }
            }
            first = false;

            response = invokeService();

        } while (shouldRepeat(response));

        extractProperties(response);
    }

    /**
     * Creates {@link Service} using the {@link InvokeSoap#wsdl},{@link InvokeSoap#namespace} and
     * {@link InvokeSoap#serviceName}, attaching any {@link InvokeSoap#headers} via {@link HeadersHandlerResolver}.
     *
     * @return created service
     * @throws MojoExecutionException
     *             if WSDL URL is malformed
     */
    Service createService() throws MojoExecutionException {
        final Service service;
        try {
            if (getLog().isDebugEnabled()) {
                service = Service.create(wsdl.toURL(), new QName(namespace, serviceName), new LoggingFeature());
            } else {
                service = Service.create(wsdl.toURL(), new QName(namespace, serviceName));
            }
        } catch (final MalformedURLException e) {
            throw new MojoExecutionException("Unable to convert `" + wsdl + "` to URL", e);
        }

        if ((headers != null) && (headers.length != 0)) {
            service.setHandlerResolver(new HeadersHandlerResolver(headers));
        }
        return service;
    }

    /**
     * Returns SOAP port of the service to use. If specific port is specified use that, otherwise use the one port
     * defined in WSDL.
     *
     * @param service
     *            SOAP service
     * @return SOAP port to use
     * @throws MojoExecutionException
     *             if there are none or more than one ports in the service when specific port has not been defined
     */
    QName determinePort(final Service service) throws MojoExecutionException {
        if (portName != null) {
            return new QName(namespace, portName);
        }

        final Iterator<QName> ports = service.getPorts();

        if (ports.hasNext()) {
            final QName port = ports.next();

            if (ports.hasNext()) {
                throw new MojoExecutionException("Found more than one port type defined in specified WSDL `" + wsdl
                        + "`, please specify which one to use with `portName` configuration option");
            }
            return port;
        } else {
            throw new MojoExecutionException(
                    "Given WSDL `" + wsdl + "` does not specify any port types, please specify one to use with "
                            + "`portName` configuration option");
        }
    }

    /**
     * Extracts properties defined by XPath expressions from the SOAP response.
     *
     * @param response
     *            SOAP response
     *
     * @throws IllegalArgumentException
     *             if XPath expression cannot be compiled or there is an error evaluating the expression
     */
    void extractProperties(final Document response) {
        if (properties.isEmpty()) {
            return;
        }

        final Map<String, Object> values = properties.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> {
                    try {
                        final XPathExpression expression = XmlUtil.xpathExpression(e.getValue());

                        return expression.evaluate(response, XPathConstants.STRING);
                    } catch (final XPathExpressionException ex) {
                        throw new IllegalArgumentException("Unable to get property " + e.getKey()
                                + " from XML using XPath expression `" + e.getValue() + "`", ex);
                    }
                }));

        final Properties projectProperties = project.getProperties();
        projectProperties.putAll(values);
    }

    /**
     * Invokes the SOAP service.
     *
     * @return SOAP response
     * @throws MojoExecutionException
     *             if unable to serialize request or response XML
     * @throws javax.xml.ws.WebServiceException
     *             see {@link Dispatch#invoke(Object)}
     */
    Document invokeService() throws MojoExecutionException {
        final Service service = createService();

        final QName port = determinePort(service);

        final File executionDir = new File(requestPath, mojoExecution.getExecutionId());
        if (!executionDir.exists()) {
            executionDir.mkdirs();
        }

        final Source soapRequest = createRequest(request[0]);

        final File requestFile = new File(executionDir, "request.xml");
        try {
            transformer.transform(soapRequest, new StreamResult(requestFile));
        } catch (final TransformerException e) {
            throw new MojoExecutionException("Unable to store request XML to file `" + requestFile + "`", e);
        }

        final Dispatch<Source> dispatch = service.createDispatch(port, Source.class, Service.Mode.PAYLOAD);

        final Map<String, Object> requestContext = dispatch.getRequestContext();
        requestContext.put(MessageContext.WSDL_OPERATION, new QName(namespace, operation));

        if (endpoint != null) {
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        }

        final Source soapResponse = dispatch.invoke(soapRequest);

        final Document soapResponseDocument = document();
        try {
            transformer.transform(soapResponse, new DOMResult(soapResponseDocument));
        } catch (final TransformerException e) {
            throw new MojoExecutionException("Unable to transform response source XML to DOM document", e);
        }

        final File responseFile = new File(executionDir, "response.xml");
        try {
            transformer.transform(new DOMSource(soapResponseDocument), new StreamResult(responseFile));
        } catch (final TransformerException e) {
            throw new MojoExecutionException("Unable to store request XML to file `" + requestFile + "`", e);
        }

        return soapResponseDocument;
    }

    /**
     * Determines if the request should be repeated by evaluating {@link InvokeSoap#repeatUntil} expression.
     *
     * @param response
     *            SOAP response from the last invocation
     * @return true if request should be repeated, false if not
     * @throws MojoExecutionException
     *             if the {@link InvokeSoap#repeatUntil} expression cannot be evaluated
     */
    boolean shouldRepeat(final Document response) throws MojoExecutionException {
        if (repeatUntilExpression == null) {
            return false;
        }

        try {
            return (boolean) repeatUntilExpression.evaluate(response, XPathConstants.BOOLEAN);
        } catch (final XPathExpressionException e) {
            throw new MojoExecutionException("Unable to evaluate repeatUntil XPath expression `" + repeatUntil + "`",
                    e);
        }
    }
}
