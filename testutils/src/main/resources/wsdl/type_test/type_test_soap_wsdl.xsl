<?xml version="1.0"?>
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
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:xalan="http://xml.apache.org/xslt"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
    xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
    xmlns:x1="http://apache.org/type_test/types1"
    xmlns:x2="http://apache.org/type_test/types2"
    xmlns:x3="http://apache.org/type_test/types3"
    xmlns:itst="http://tests.iona.com/ittests"
    xmlns:http-conf="http://cxf.apache.org/transports/http/configuration">

  <xsl:output method="xml" indent="yes" xalan:indent-amount="4"/>
  <xsl:strip-space elements="*"/>

  <!-- Parameter: Path to the generated type_test WSDL to include -->
  <xsl:param name="inc_wsdl_path"/>
  <!-- Parameter: Use DOC-literal 'document' or RPC-literal 'rpc' style -->
  <xsl:param name="use_style"/>
  <!-- What port to use -->
  <xsl:param name="port"/>
 
  <!-- copy attributes from any node -->
  <xsl:template match="@*" mode="attribute_copy">
    <xsl:attribute name="{name(.)}">
      <xsl:value-of select="."/> 
    </xsl:attribute>
  </xsl:template>

  <!-- 0 - root schema node -->
  <xsl:template match="/xsd:schema">
    <xsl:if test="$use_style='document'">
      <wsdl:definitions
          xmlns="http://schemas.xmlsoap.org/wsdl/"
          xmlns:tns="http://apache.org/type_test/doc"
          targetNamespace="http://apache.org/type_test/doc"
          name="type_test_soap">
        <xsl:apply-templates select="@*[name(.)!='elementFormDefault']" mode="attribute_copy"/>
        <xsl:apply-templates select="." mode="test_binding"/>
      </wsdl:definitions>
    </xsl:if>
    <xsl:if test="$use_style='rpc'">
      <wsdl:definitions
          xmlns="http://schemas.xmlsoap.org/wsdl/"
          xmlns:tns="http://apache.org/type_test/rpc"
          targetNamespace="http://apache.org/type_test/rpc"
          name="type_test_soap">
        <xsl:apply-templates select="@*[name(.)!='elementFormDefault']" mode="attribute_copy"/>
        <xsl:apply-templates select="." mode="test_binding"/>
      </wsdl:definitions>
    </xsl:if>
  </xsl:template>

  <!-- 1 - test binding and service -->
  <xsl:template match="/xsd:schema" mode="test_binding"
        xmlns="http://schemas.xmlsoap.org/wsdl/">
    <!-- doc literal style -->
    <xsl:if test="$use_style='document'">
      <wsdl:import namespace="http://apache.org/type_test/doc" location="type_test_doclit.wsdl"/>
    </xsl:if>
    <!-- rpc literal style -->
    <xsl:if test="$use_style='rpc'">
      <wsdl:import namespace="http://apache.org/type_test/rpc" location="type_test_rpclit.wsdl"/>
    </xsl:if>
    <wsdl:binding type="tns:TypeTestPortType" name="TypeTestSOAP">
      <soap:binding transport="http://schemas.xmlsoap.org/soap/http">
        <xsl:attribute name="style">
          <xsl:value-of select="$use_style"/>
        </xsl:attribute>
      </soap:binding>
      <xsl:apply-templates select="." mode="hardcoded_operations"/>
      <xsl:apply-templates select="itst:it_test_group" mode="test_operations_group"/>
    </wsdl:binding>
    <wsdl:service name="SOAPService">
      <wsdl:port name="SOAPPort">
        <xsl:attribute name="binding" xmlns="http://schemas.xmlsoap.org/">
            <xsl:value-of select="'tns:TypeTestSOAP'"/>
        </xsl:attribute>
        <soap:address>
            <xsl:attribute name="location">http://localhost:<xsl:value-of select="$port"/>/SOAPService/SOAPPort/</xsl:attribute>
        </soap:address>
        <!-- Set AllowChunking to false to avoid chunking issue. -->
        <http-conf:client SendTimeout="90000" AllowChunking="false"/>
      </wsdl:port>
    </wsdl:service>
  </xsl:template>

  <!-- 1.1 - hardcoded operations -->
  <xsl:template match="/xsd:schema" mode="hardcoded_operations"
        xmlns="http://schemas.xmlsoap.org/wsdl/">
    <wsdl:operation name="testVoid">
      <soap:operation soapAction="">
        <xsl:attribute name="style">
          <xsl:value-of select="$use_style"/>
        </xsl:attribute>
      </soap:operation>
      <wsdl:input>
        <xsl:if test="$use_style='document'">
          <xsl:attribute name="name">
            <xsl:value-of select="'testVoid'"/>
          </xsl:attribute>
        </xsl:if>
        <soap:body use="literal">
          <xsl:if test="$use_style='rpc'">
            <xsl:attribute name="namespace">
              <xsl:value-of select="'http://apache.org/type_test/rpc'"/>
            </xsl:attribute>
          </xsl:if>
        </soap:body>
      </wsdl:input>
    </wsdl:operation>
    <wsdl:operation name="testOneway">
      <soap:operation soapAction="">
        <xsl:attribute name="style">
          <xsl:value-of select="$use_style"/>
        </xsl:attribute>
      </soap:operation>
      <wsdl:input>
        <xsl:if test="$use_style='document'">
          <xsl:attribute name="name">
            <xsl:value-of select="'testOneway'"/>
          </xsl:attribute>
        </xsl:if>
        <soap:body use="literal">
          <xsl:if test="$use_style='rpc'">
            <xsl:attribute name="namespace">
              <xsl:value-of select="'http://apache.org/type_test/rpc'"/>
            </xsl:attribute>
          </xsl:if>
        </soap:body>
      </wsdl:input>
    </wsdl:operation>
  </xsl:template>

  <!-- 1.2 - group of test operations -->
  <xsl:template match="itst:it_test_group" mode="test_operations_group">
    <xsl:apply-templates select="xsd:simpleType[not(@itst:it_no_test='true')]"
        mode="test_operation"/>
    <xsl:apply-templates select="xsd:complexType[not(@itst:it_no_test='true')]"
        mode="test_operation"/>
    <!--
    <xsl:apply-templates select="xsd:element[not(@itst:it_no_test='true')]"
        mode="test_operation"/>
    -->
    <xsl:apply-templates select="itst:builtIn[not(@itst:it_no_test='true')]"
        mode="test_operation"/>
  </xsl:template>
  
  <!-- 1.2.1 - test operations -->
  <xsl:template match="itst:it_test_group/*" mode="test_operation"
        xmlns="http://schemas.xmlsoap.org/wsdl/">
    <xsl:variable name="operation_name">
      <xsl:value-of select="concat('test',
                            concat(translate(substring(@name, 1, 1),    
                                   'abcdefghijklmnopqrstuvwxyz', 
                                   'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),
                                   substring(@name, 2)))"/>
    </xsl:variable>
    <xsl:variable name="operation_input_name">
      <xsl:value-of select="$operation_name"/>
    </xsl:variable>
    <xsl:variable name="operation_output_name">
      <xsl:value-of select="concat($operation_name, 'Response')"/>
    </xsl:variable>
    <wsdl:operation>
      <xsl:attribute name="name">
        <xsl:value-of select="$operation_name"/>
      </xsl:attribute>
      <soap:operation soapAction="">
        <xsl:attribute name="style">
          <xsl:value-of select="$use_style"/>
        </xsl:attribute>
      </soap:operation>
      <wsdl:input>
        <xsl:if test="$use_style='document'">
          <xsl:attribute name="name">
            <xsl:value-of select="$operation_input_name"/>
          </xsl:attribute>
        </xsl:if>
        <soap:body use="literal">
          <xsl:if test="$use_style='rpc'">
            <xsl:attribute name="namespace">
              <xsl:value-of select="'http://apache.org/type_test/rpc'"/>
            </xsl:attribute>
          </xsl:if>
        </soap:body>
      </wsdl:input>
      <wsdl:output>
        <xsl:if test="$use_style='document'">
          <xsl:attribute name="name">
            <xsl:value-of select="$operation_output_name"/>
          </xsl:attribute>
        </xsl:if>
        <soap:body use="literal">
          <xsl:if test="$use_style='rpc'">
            <xsl:attribute name="namespace">
              <xsl:value-of select="'http://apache.org/type_test/rpc'"/>
            </xsl:attribute>
          </xsl:if>
        </soap:body>
      </wsdl:output>
    </wsdl:operation>
  </xsl:template>

</xsl:stylesheet>

