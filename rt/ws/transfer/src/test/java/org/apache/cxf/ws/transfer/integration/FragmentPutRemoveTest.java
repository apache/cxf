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
package org.apache.cxf.ws.transfer.integration;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Element;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.PutResponse;
import org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType;
import org.apache.cxf.ws.transfer.dialect.fragment.Fragment;
import org.apache.cxf.ws.transfer.dialect.fragment.FragmentDialectConstants;
import org.apache.cxf.ws.transfer.manager.MemoryResourceManager;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resource.Resource;

import org.junit.Assert;
import org.junit.Test;

public class FragmentPutRemoveTest extends IntegrationBaseTest {

    @Test
    public void removeAttrTest() throws XMLStreamException {
        String content = "<a foo=\"1\"/>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.setMode(FragmentDialectConstants.FRAGMENT_MODE_REMOVE);
        expression.getContent().add("/a/@foo");
        fragment.setExpression(expression);
        request.getAny().add(fragment);

        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Assert.assertNull(rootEl.getAttributeNode("foo"));

        resource.destroy();
    }

    @Test
    public void removeElementTest() throws XMLStreamException {
        String content = "<a><b/></a>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.setMode(FragmentDialectConstants.FRAGMENT_MODE_REMOVE);
        expression.getContent().add("/a/b");
        fragment.setExpression(expression);
        request.getAny().add(fragment);

        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Assert.assertEquals(0, rootEl.getChildNodes().getLength());

        resource.destroy();
    }

    @Test
    public void removeElement2Test() throws XMLStreamException {
        String content = "<a><b/><b/></a>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.setMode(FragmentDialectConstants.FRAGMENT_MODE_REMOVE);
        expression.getContent().add("/a/b[1]");
        fragment.setExpression(expression);
        request.getAny().add(fragment);

        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Assert.assertEquals(1, rootEl.getChildNodes().getLength());

        resource.destroy();
    }
}
