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

    <xsl:import href="inc_type_test_java_types.xsl"/>

    <xsl:output method="text"/>
    <xsl:strip-space elements="*"/>
    
    <xsl:template match="xsd:simpleType|xsd:complexType|xsd:element|itst:builtIn" mode="test_signature">
        <xsl:variable name="the_name">
            <xsl:value-of select="concat(translate(substring(@name, 1, 1),
                                         'abcdefghijklmnopqrstuvwxyz', 
                                         'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),
                                         substring(@name, 2))"/>
        </xsl:variable>
        <xsl:variable name="operation_name">
            <xsl:value-of select="concat('test', $the_name)"/>
        </xsl:variable>
        <xsl:variable name="class_name">
            <xsl:value-of select="concat('Test', $the_name)"/>
        </xsl:variable>
        <xsl:text>&#10;    public </xsl:text>
        <xsl:apply-templates select="." mode="javaType"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="$operation_name"/>
        <xsl:text>(&#10;            </xsl:text>
        <xsl:apply-templates select="." mode="javaType"/>
        <xsl:text> x,</xsl:text>
        <xsl:apply-templates select="." mode="javaHolderType"/>
        <xsl:text> y,</xsl:text>
        <xsl:apply-templates select="." mode="javaHolderType"/>
        <xsl:text> z)</xsl:text>
    </xsl:template>

</xsl:stylesheet>
