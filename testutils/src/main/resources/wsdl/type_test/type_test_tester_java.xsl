<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements. See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership. The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License. You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied. See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<!--
     Stylesheet to convert schema into java file for test implementation.
-->
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns:xalan="http://xml.apache.org/xslt"
        xmlns:itst="http://tests.iona.com/ittests">

    <xsl:output method="text"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="/xsd:schema">
<![CDATA[
package org.apache.cxf.systest.type_test;

/**
 * org.apache.type_test.TypeTestTester
 */
public interface TypeTestTester {

    void setPerformanceTestOnly();
    
    void testVoid() throws Exception;
    
    void testOneway() throws Exception;]]>
        <xsl:apply-templates mode="definitions"/>
        <![CDATA[
}]]>
    </xsl:template>

    <xsl:template match="itst:it_test_group" mode="definitions">
        <xsl:apply-templates select="xsd:simpleType[not(
                @name='SimpleUnionList'
                or @name='AnonUnionList'
                or @name='SimpleUnion'
                or @itst:it_no_test='true')]"
            mode="definition"/>
        <xsl:apply-templates select="xsd:complexType[not(
                @itst:it_no_test='true')]"
            mode="definition"/>
        <!--
        <xsl:apply-templates select="xsd:element[not(
                @itst:it_no_test='true')]"
            mode="definition"/>
        -->
        <xsl:apply-templates select="itst:builtIn[not(
                @itst:it_no_test='true')]"
            mode="definition"/>
    </xsl:template>

    <xsl:template match="itst:it_test_group/*" mode="definition">
        <xsl:text>
    void test</xsl:text>
        <xsl:value-of select="concat(translate(substring(@name, 1, 1),
                                     'abcdefghijklmnopqrstuvwxyz', 
                                     'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),
                                     substring(@name, 2))"/>
        <xsl:text>() throws Exception;</xsl:text>
        <xsl:text>
        </xsl:text>
    </xsl:template>

</xsl:stylesheet>
