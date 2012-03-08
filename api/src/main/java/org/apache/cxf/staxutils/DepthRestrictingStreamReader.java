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
package org.apache.cxf.staxutils;

import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class DepthRestrictingStreamReader extends DepthXMLStreamReader {
    private int elementCountThreshold = -1;
    private int innerElementLevelThreshold = -1;
    private int innerElementCountThreshold = -1;
    
    private int totalElementCount;
    private Stack<Integer> stack = new Stack<Integer>();
    
    public DepthRestrictingStreamReader(XMLStreamReader reader,
                                        int elementCountThreshold,
                                        int innerElementLevelThreshold,
                                        int innerElementCountThreshold) {
        super(reader);
        this.elementCountThreshold = elementCountThreshold;
        this.innerElementLevelThreshold = innerElementLevelThreshold;
        this.innerElementCountThreshold = innerElementCountThreshold;
    }
    
    @Override
    public int next() throws XMLStreamException {
        int next = super.next();
        if (next == START_ELEMENT) {
            if (innerElementLevelThreshold != -1 && getDepth() >= innerElementLevelThreshold) {
                throw new DepthExceededStaxException();
            }
            if (elementCountThreshold != -1 && ++totalElementCount >= elementCountThreshold) {
                throw new DepthExceededStaxException();
            }
            if (innerElementCountThreshold != -1) {
                if (!stack.empty()) {
                    int currentCount = stack.pop();
                    if (++currentCount >= innerElementCountThreshold) {
                        throw new DepthExceededStaxException();
                    } else {
                        stack.push(currentCount);
                    }
                } 
                stack.push(0);
            }
            
        } else if (next == END_ELEMENT && innerElementCountThreshold != -1) {
            stack.pop();
        }
        return next;
    }

    
}
