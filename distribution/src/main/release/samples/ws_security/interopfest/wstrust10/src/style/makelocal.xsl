<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    xmlns:soap12="http://schemas.xmlsoap.org/wsdl/soap12/"
    xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
    xmlns:sp="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy"
    xmlns:t="http://schemas.xmlsoap.org/ws/2005/02/trust"
    xmlns:wsa="http://www.w3.org/2005/08/addressing">
    <xsl:output method="xml"/>

    <xsl:param name="target.host">131.107.72.15</xsl:param>
    <xsl:param name="wsdl.host">131.107.72.15</xsl:param>

    <xsl:template match="wsdl:import">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="@location='http://$wsdl.host/Security_Federation_FederatedService_Indigo/Symmetric.svc?wsdl'">
                    <xsl:attribute name="location">WsTrustSym.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:when test="@location='http://$wsdl.host/Security_Federation_FederatedService_Indigo/Symmetric.svc?wsdl=wsdl0'">
                    <xsl:attribute name="location">WsTrustSym_policy.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:when test="@location='http://$wsdl.host/Security_Federation_FederatedService_Indigo/Asymmetric.svc?wsdl'">
                    <xsl:attribute name="location">WsTrustAsym.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:when test="@location='http://$wsdl.host/Security_Federation_FederatedService_Indigo/Asymmetric.svc?wsdl=wsdl0'">
                    <xsl:attribute name="location">WsTrustAsym_policy.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="location">WsTrustSym_policy.wsdl</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates select="@namespace"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="soap12:address|soap:address">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="starts-with(@location,'http://ndgo-introp-s24/')">
                    <xsl:attribute name="location">http://<xsl:value-of select="$target.host"/>/<xsl:value-of select="substring(@location,22)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,'https://ndgo-introp-s24/')">
                    <xsl:attribute name="location">https://<xsl:value-of select="$target.host"/>/<xsl:value-of select="substring(@location,23)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,'https://ndgo-introp-s24:8443/')">
                    <xsl:attribute name="location">https://<xsl:value-of select="$target.host"/>:8443/<xsl:value-of select="substring(@location,38)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,'https://kirillgdev04')">
                    <xsl:attribute name="location">https://<xsl:value-of select="$target.host"/><xsl:value-of select="substring(@location,21)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,'http://kirillgdev04')">
                    <xsl:attribute name="location">http://<xsl:value-of select="$target.host"/><xsl:value-of select="substring(@location,20)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,concat('https://',$wsdl.host))">
                    <xsl:attribute name="location">https://<xsl:value-of select="$target.host"/><xsl:value-of select="substring(@location,string-length($wsdl.host)+9)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,concat('http://',$wsdl.host))">
                    <xsl:attribute name="location">http://<xsl:value-of select="$target.host"/><xsl:value-of select="substring(@location,string-length($wsdl.host)+8)"/></xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="@location"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="wsdl:port/wsa:EndpointReference"/>

    <xsl:template match="xs:element[@ref='xs:schema']"/>

    <xsl:template match="@*">
        <xsl:copy/>
    </xsl:template>
    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
