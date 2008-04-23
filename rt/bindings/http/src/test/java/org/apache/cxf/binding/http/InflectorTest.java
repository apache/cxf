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
package org.apache.cxf.binding.http;


import org.apache.cxf.binding.http.strategy.EnglishInflector;
import org.apache.cxf.binding.http.strategy.Inflector;
import org.junit.Assert;
import org.junit.Test;

public class InflectorTest extends Assert {
    
    @Test
    public void testPluralization() {
        Inflector i = new EnglishInflector();
        assertEquals("quizzes", i.pluralize("quiz"));
        assertEquals("QUIZzes", i.pluralize("QUIZ"));
        assertEquals("matrices", i.pluralize("matrix"));
        assertEquals("people", i.pluralize("person"));
        assertEquals("kids", i.pluralize("kid"));
        assertEquals("bashes", i.pluralize("bash"));
    }
    @Test
    public void testSingularization() {
        Inflector i = new EnglishInflector();
        assertEquals("matrix", i.singularlize("matrices"));
        assertEquals("quiz", i.singularlize("quizzes"));
        assertEquals("person", i.singularlize("people"));
        assertEquals("kid", i.singularlize("kids"));
        assertEquals("bash", i.singularlize("bashes"));
    }
}
