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
package org.apache.cxf.ws.rm;

import org.junit.Assert;
import org.junit.Test;

public class AbstractSequenceTest extends Assert {

    @Test
    public void testIdentifierEquals() {
        Identifier id1 = null;
        Identifier id2 = null;   
        assertTrue(AbstractSequence.identifierEquals(id1, id2));
        
        ObjectFactory factory = new ObjectFactory();
        id1 = factory.createIdentifier();
        id1.setValue("seq1"); 
        assertTrue(!AbstractSequence.identifierEquals(id1, id2));
        
        id2 = factory.createIdentifier();
        id2.setValue("seq2"); 
        assertTrue(!AbstractSequence.identifierEquals(id1, id2));
        
        id2.setValue("seq1");
        assertTrue(AbstractSequence.identifierEquals(id1, id2));
    }

}
