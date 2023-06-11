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

package org.apache.cxf.staxutils.validation;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchemaCollection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class Stax2ValidationUtilsTest {

    private static final String VALID_MESSAGE_ECHO = "<echo xmlns=\"http://www.echo.org\">"
            + "<echo>Testing echo</echo>" + "</echo>";

    private static final String INVALID_MESSAGE_ECHO = "<wrongEcho xmlns=\"http://www.echo.org\">"
            + "<echo>Testing echo</echo>" + "</wrongEcho>";

    private static final String VALID_MESSAGE_LOG = "<log xmlns=\"http://www.log.org\">"
            + "<message>Testing Log</message>" + "</log>";

    private static final String INVALID_MESSAGE_LOG = "<wrongLog xmlns=\"http://www.log.org\">"
            + "<message>Testing Log</message>" + "</wrongLog>";

    private static final String ECHO_ERROR_MESSAGE = "tag name \"wrongEcho\" is not allowed.";

    private static final String LOG_ERROR_MESSAGE = "tag name \"wrongLog\" is not allowed.";

    private static final String ECHO_SCHEMA = "schemas/echoSchema.xsd";

    private static final String LOG_SCHEMA = "schemas/logSchema.xsd";

    private static final String MULTI_IMPORT_SCHEMA = "schemas/schemaWithImports.xsd";

    private Stax2ValidationUtils utils;
    private XMLStreamReader xmlReader;
    private final Endpoint endpoint = mock(Endpoint.class);
    private final ServiceInfo serviceInfo = new ServiceInfo();
    private final SchemaInfo schemaInfo = new SchemaInfo("testUri");

    private String validMessage;

    private String invalidMessage;

    private String errorMessage;

    private String schemaPath;

    public Stax2ValidationUtilsTest(String validMessage, String invalidMessage, String errorMessage,
                                    String schemaPath) throws ClassNotFoundException {
        this.validMessage = validMessage;
        this.invalidMessage = invalidMessage;
        this.errorMessage = errorMessage;
        this.schemaPath = schemaPath;
        utils = new Stax2ValidationUtils();
    }

    @Parameterized.Parameters
    public static Collection<String[]> data() {
        List<String[]> parameters = new ArrayList<>();
        parameters.add(new String[]{VALID_MESSAGE_ECHO, INVALID_MESSAGE_ECHO, ECHO_ERROR_MESSAGE, MULTI_IMPORT_SCHEMA});
        parameters.add(new String[]{VALID_MESSAGE_LOG, INVALID_MESSAGE_LOG, LOG_ERROR_MESSAGE, MULTI_IMPORT_SCHEMA});
        parameters.add(new String[]{VALID_MESSAGE_ECHO, INVALID_MESSAGE_ECHO, ECHO_ERROR_MESSAGE, ECHO_SCHEMA});
        parameters.add(new String[]{VALID_MESSAGE_LOG, INVALID_MESSAGE_LOG, LOG_ERROR_MESSAGE, LOG_SCHEMA});
        return parameters;
    }

    @Before
    public void setUp() throws Exception {
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();

        InputStream io = getClass().getClassLoader().getResourceAsStream(schemaPath);
        String sysId = getClass().getClassLoader().getResource(schemaPath).toString();
        schemaCol.setBaseUri(getTestBaseURI());
        schemaCol.read(new StreamSource(io, sysId));
        serviceInfo.addSchema(schemaInfo);
        schemaInfo.setSchema(schemaCol.getXmlSchema(sysId)[0]);
        when(endpoint.get(any())).thenReturn(null);
        when(endpoint.containsKey(any())).thenReturn(false);
        when(endpoint.put(anyString(), any())).thenReturn(null);
    }

    @Test
    public void testValidMessage() throws Exception {
        Throwable exception = null;
        xmlReader = createReader(validMessage);
        utils.setupValidation(xmlReader, endpoint, serviceInfo);
        try {
            while (xmlReader.hasNext()) {
                xmlReader.next();
            }
        } catch (Throwable e) {
            exception = e;
        }

        assertThat(exception, is(nullValue()));
    }

    @Test
    public void testInvalidMessage() throws Exception {
        Throwable exception = null;
        xmlReader = createReader(invalidMessage);
        utils.setupValidation(xmlReader, endpoint, serviceInfo);
        try {
            while (xmlReader.hasNext()) {
                xmlReader.next();
            }
        } catch (Throwable e) {
            exception = e;
        }

        assertThat(exception, is(notNullValue()));
        assertThat(exception.getMessage(), containsString(errorMessage));
    }

    private String getTestBaseURI() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(schemaPath).getFile());
        return file.getAbsolutePath();
    }

    private XMLStreamReader createReader(String message) throws XMLStreamException {
        Reader reader = new StringReader(message);
        XMLInputFactory factory = XMLInputFactory.newInstance();
        return factory.createXMLStreamReader(reader);
    }

}
