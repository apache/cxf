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

package org.apache.cxf.jaxb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;


public class JAXBToStringBuilderTest {

    final String dataV = "someData";    

    @Test
    public void testToString() throws Exception {    
        String res = JAXBToStringBuilder.valueOf(dataV);
        Assert.assertEquals(res, dataV);
    }

    @Test
    public void testToStringArray() throws Exception {  
        String[] data = new String[]{dataV};
        String res = JAXBToStringBuilder.valueOf(data);
        Assert.assertTrue(res.indexOf(dataV) != -1);
    }

    @Test
    public void testToStringCollection() throws Exception {  
        List<String> data = new ArrayList<String>();
        data.add(dataV);
        String res = JAXBToStringBuilder.valueOf(data);
        Assert.assertTrue(res.indexOf(dataV) != -1);
    }

    
    @Test
    public void testToStringMap() throws Exception {  
        Map<String, String> data = new HashMap<String, String>();
        data.put(dataV, dataV);
        
        // no content as it is not a Collection
        String res = JAXBToStringBuilder.valueOf(data);
        Assert.assertTrue(res.indexOf(dataV) == -1);
    }

}
