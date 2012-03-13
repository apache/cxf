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

/**
 * XMLStreamReader implementation which can be used to enforce a number of
 * depth-restricting policies. The following properties are currently supported:
 * - total number of elements in the document
 * - the maximum depth of the given element; the root element will be checked by default
 * - the maximum number of immediate child nodes for individual elements
 * 
 * More sophisticated policies can be supported in the future.      
 */
public class DepthRestrictingStreamReader extends DepthXMLStreamReader {
    
    private DocumentDepthProperties props;
    private int totalElementCount;
    private Stack<Integer> stack = new Stack<Integer>();
    
    public DepthRestrictingStreamReader(XMLStreamReader reader,
                                        int elementCountThreshold,
                                        int innerElementLevelThreshold,
                                        int innerElementCountThreshold) {
        super(reader);
        this.props = new DocumentDepthProperties(elementCountThreshold, 
                                            innerElementLevelThreshold,
                                            innerElementCountThreshold);
    }
    
    public DepthRestrictingStreamReader(XMLStreamReader reader,
                                        DocumentDepthProperties props) {
        super(reader);
        this.props = props;
    }
    
    @Override
    public int next() throws XMLStreamException {
        int next = super.next();
        if (next == START_ELEMENT) {
            if (props.getInnerElementLevelThreshold() != -1 
                && getDepth() >= props.getInnerElementLevelThreshold()) {
                throw new DepthExceededStaxException();
            }
            if (props.getElementCountThreshold() != -1 
                && ++totalElementCount >= props.getElementCountThreshold()) {
                throw new DepthExceededStaxException();
            }
            if (props.getInnerElementCountThreshold() != -1) {
                if (!stack.empty()) {
                    int currentCount = stack.pop();
                    if (++currentCount >= props.getInnerElementCountThreshold()) {
                        throw new DepthExceededStaxException();
                    } else {
                        stack.push(currentCount);
                    }
                } 
                stack.push(0);
            }
            
        } else if (next == END_ELEMENT && props.getInnerElementCountThreshold() != -1) {
            stack.pop();
        }
        return next;
    }
}
