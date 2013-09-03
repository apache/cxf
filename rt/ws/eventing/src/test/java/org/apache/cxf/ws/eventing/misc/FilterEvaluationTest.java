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

package org.apache.cxf.ws.eventing.misc;

import java.io.CharArrayReader;
import java.io.Reader;

import org.w3c.dom.Document;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.eventing.FilterType;
import org.apache.cxf.ws.eventing.shared.utils.FilteringUtil;

import org.junit.Assert;
import org.junit.Test;

public class FilterEvaluationTest {

    @Test
    public void simpleFilterEvaluationPositive() throws Exception {
        Reader reader = new CharArrayReader("<tt><in>1</in></tt>".toCharArray());
        Document doc = StaxUtils.read(reader);
        FilterType filter = new FilterType();
        filter.getContent().add("//tt");
        Assert.assertTrue(FilteringUtil.doesConformToFilter(doc.getDocumentElement(), filter));
    }

    @Test
    public void simpleFilterEvaluationNegative() throws Exception {
        Reader reader = new CharArrayReader("<tt><in>1</in></tt>".toCharArray());
        Document doc = StaxUtils.read(reader);
        FilterType filter = new FilterType();
        filter.getContent().add("//ttx");
        Assert.assertFalse(FilteringUtil.doesConformToFilter(doc.getDocumentElement(), filter));
    }

    @Test
    public void validFilter() throws Exception {
        Assert.assertTrue(FilteringUtil.isValidFilter("//filter"));
    }

    @Test
    public void invalidFilter() throws Exception {
        Assert.assertFalse(FilteringUtil.isValidFilter("@/$"));
    }

}
