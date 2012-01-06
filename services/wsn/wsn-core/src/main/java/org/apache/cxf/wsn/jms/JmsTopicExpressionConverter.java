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
package org.apache.cxf.wsn.jms;

import java.util.Iterator;
import java.util.List;

import javax.jms.Topic;
import javax.xml.namespace.QName;

import org.apache.activemq.command.ActiveMQTopic;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;

public class JmsTopicExpressionConverter {

    public static final String SIMPLE_DIALECT = "http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple";

    public TopicExpressionType toTopicExpression(Topic topic) {
        return toTopicExpression(topic.toString());
    }

    public TopicExpressionType toTopicExpression(ActiveMQTopic topic) {
        return toTopicExpression(topic.getPhysicalName());
    }

    public TopicExpressionType toTopicExpression(String name) {
        TopicExpressionType answer = new TopicExpressionType();
        answer.getContent().add(name);
        answer.setDialect(SIMPLE_DIALECT);
        return answer;
    }

    public ActiveMQTopic toActiveMQTopic(List<TopicExpressionType> topics) throws InvalidTopicException {
        if (topics == null || topics.size() == 0) {
            return null;
        }
        int size = topics.size();
        ActiveMQTopic childrenDestinations[] = new ActiveMQTopic[size];
        for (int i = 0; i < size; i++) {
            childrenDestinations[i] = toActiveMQTopic(topics.get(i));
        }

        ActiveMQTopic topic = new ActiveMQTopic();
        topic.setCompositeDestinations(childrenDestinations);
        return topic;
    }

    public ActiveMQTopic toActiveMQTopic(TopicExpressionType topic) throws InvalidTopicException {
        String dialect = topic.getDialect();
        if (dialect == null || SIMPLE_DIALECT.equals(dialect)) {
            for (Iterator<Object> iter = topic.getContent().iterator(); iter.hasNext();) {
                ActiveMQTopic answer = createActiveMQTopicFromContent(iter.next());
                if (answer != null) {
                    return answer;
                }
            }
            throw new InvalidTopicException("No topic name available topic: " + topic);
        } else {
            throw new InvalidTopicException("Topic dialect: " + dialect + " not supported");
        }
    }

    // Implementation methods
    // -------------------------------------------------------------------------
    protected ActiveMQTopic createActiveMQTopicFromContent(Object contentItem) {
        if (contentItem instanceof String) {
            return new ActiveMQTopic(((String) contentItem).trim());
        }
        if (contentItem instanceof QName) {
            return createActiveMQTopicFromQName((QName) contentItem);
        }
        return null;
    }

    protected ActiveMQTopic createActiveMQTopicFromQName(QName qName) {
        return new ActiveMQTopic(qName.toString());
    }

}
