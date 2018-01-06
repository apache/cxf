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

package org.apache.cxf.ws.transfer.unit;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.manager.MemoryResourceManager;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.shared.faults.UnknownResource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MemoryResourceManagerTest {

    public static final String ELEMENT_NAMESPACE = "test";

    public static final String ELEMENT_NAME = "name1";

    public static final String ELEMENT_VALUE = "value1";

    public static final String ELEMENT_VALUE_NEW = "value2";

    private static Document document;

    private ResourceManager resourceManager;

    @BeforeClass
    public static void beforeClass() throws ParserConfigurationException {
        document = DOMUtils.createDocument();
    }

    @AfterClass
    public static void afterClass() {
        document = null;
    }

    @Before
    public void before() {
        resourceManager = new MemoryResourceManager();
    }

    @After
    public void after() {
        resourceManager = null;
    }

    @Test(expected = UnknownResource.class)
    public void getEmptyReferenceParamsTest() {
        resourceManager.get(new ReferenceParametersType());
    }

    @Test(expected = UnknownResource.class)
    public void getUnknownReferenceParamsTest() {
        ReferenceParametersType refParams = new ReferenceParametersType();
        Element uuid = DOMUtils.getEmptyDocument().createElementNS(
                MemoryResourceManager.REF_NAMESPACE, MemoryResourceManager.REF_LOCAL_NAME);
        uuid.setTextContent("123456");
        refParams.getAny().add(uuid);
        resourceManager.get(refParams);
    }

    @Test(expected = UnknownResource.class)
    public void putEmptyReferenceParamsTest() {
        resourceManager.put(new ReferenceParametersType(), new Representation());
    }

    @Test(expected = UnknownResource.class)
    public void putUnknownReferenceParamsTest() {
        ReferenceParametersType refParams = new ReferenceParametersType();
        Element uuid = DOMUtils.getEmptyDocument().createElementNS(
                MemoryResourceManager.REF_NAMESPACE, MemoryResourceManager.REF_LOCAL_NAME);
        uuid.setTextContent("123456");
        refParams.getAny().add(uuid);
        resourceManager.put(refParams, new Representation());
    }

    @Test(expected = UnknownResource.class)
    public void deleteEmptyReferenceParamsTest() {
        resourceManager.delete(new ReferenceParametersType());
    }

    @Test(expected = UnknownResource.class)
    public void deleteUnknownReferenceParamsTest() {
        ReferenceParametersType refParams = new ReferenceParametersType();
        Element uuid = DOMUtils.getEmptyDocument().createElementNS(
                MemoryResourceManager.REF_NAMESPACE, MemoryResourceManager.REF_LOCAL_NAME);
        uuid.setTextContent("123456");
        refParams.getAny().add(uuid);
        resourceManager.delete(refParams);
    }

    @Test
    public void createTest() {
        Element representationEl = document.createElementNS(ELEMENT_NAMESPACE, ELEMENT_NAME);
        representationEl.setTextContent(ELEMENT_VALUE);
        Representation representation = new Representation();
        representation.setAny(representationEl);

        ReferenceParametersType refParams = resourceManager.create(representation);
        Assert.assertTrue("ResourceManager returned unexpected count of reference elements.",
                refParams.getAny().size() == 1);
    }

    @Test
    public void getTest() {
        Element representationEl = document.createElementNS(ELEMENT_NAMESPACE, ELEMENT_NAME);
        representationEl.setTextContent(ELEMENT_VALUE);
        Representation representation = new Representation();
        representation.setAny(representationEl);

        ReferenceParametersType refParams = resourceManager.create(representation);
        Representation returnedRepresentation = resourceManager.get(refParams);

        Element returnedEl = (Element) returnedRepresentation.getAny();
        Assert.assertEquals("Namespace is other than expected.",
                ELEMENT_NAMESPACE, returnedEl.getNamespaceURI());
        Assert.assertEquals("Element name is other than expected",
                ELEMENT_NAME, returnedEl.getLocalName());
        Assert.assertEquals("Value is other than expected.",
                ELEMENT_VALUE, returnedEl.getTextContent());
    }

    @Test
    public void putTest() {
        Element representationEl = document.createElementNS(ELEMENT_NAMESPACE, ELEMENT_NAME);
        representationEl.setTextContent(ELEMENT_VALUE);
        Representation representation = new Representation();
        representation.setAny(representationEl);

        Element representationElNew = document.createElementNS(ELEMENT_NAMESPACE, ELEMENT_NAME);
        representationElNew.setTextContent(ELEMENT_VALUE_NEW);
        Representation representationNew = new Representation();
        representationNew.setAny(representationElNew);

        ReferenceParametersType refParams = resourceManager.create(representation);
        resourceManager.put(refParams, representationNew);
        Representation returnedRepresentation = resourceManager.get(refParams);

        Element returnedEl = (Element) returnedRepresentation.getAny();
        Assert.assertEquals("Namespace is other than expected.",
                ELEMENT_NAMESPACE, returnedEl.getNamespaceURI());
        Assert.assertEquals("Element name is other than expected",
                ELEMENT_NAME, returnedEl.getLocalName());
        Assert.assertEquals("Value is other than expected.",
                ELEMENT_VALUE_NEW, returnedEl.getTextContent());
    }

    @Test(expected = UnknownResource.class)
    public void deleteTest() {
        ReferenceParametersType refParams = resourceManager.create(new Representation());
        resourceManager.delete(refParams);
        resourceManager.get(refParams);
    }

    @Test
    public void createEmptyRepresentationTest() {
        ReferenceParametersType refParams = resourceManager.create(new Representation());
        Assert.assertTrue("ResourceManager returned unexpected count of reference elements.",
                refParams.getAny().size() == 1);
    }

    @Test
    public void putEmptyRepresentationTest() {
        Element representationEl = document.createElementNS(ELEMENT_NAMESPACE, ELEMENT_NAME);
        representationEl.setTextContent(ELEMENT_VALUE);
        Representation representation = new Representation();
        representation.setAny(representationEl);

        ReferenceParametersType refParams = resourceManager.create(representation);
        resourceManager.put(refParams, new Representation());
    }

    @Test
    public void getEmptyRepresentationTest() {
        ReferenceParametersType refParams = resourceManager.create(new Representation());
        Representation returnedRepresentation = resourceManager.get(refParams);
        Assert.assertNull(returnedRepresentation.getAny());
    }
}
