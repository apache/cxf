<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:soap12="http://schemas.xmlsoap.org/wsdl/soap12/"
    xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
    xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" >
    <xsl:output method="xml"/>

    <xsl:template match="xs:import">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WSSecureConversation.svc?xsd=xsd0'">
                    <xsl:attribute name="schemaLocation">WSSecureConversation_0.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WSSecureConversationSign.svc?xsd=xsd0'">
                    <xsl:attribute name="schemaLocation">WSSecureConversation_0.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WSSecureConversation.svc?xsd=xsd1'">
                    <xsl:attribute name="schemaLocation">WSSecureConversation_1.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WSSecureConversationSign.svc?xsd=xsd1'">
                    <xsl:attribute name="schemaLocation">WSSecureConversation_1.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WSSecureConversation.svc?xsd=xsd2'">
                    <xsl:attribute name="schemaLocation">WSSecureConversation_2.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WSSecureConversationSign.svc?xsd=xsd2'">
                    <xsl:attribute name="schemaLocation">WSSecureConversation_2.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WSSecureConversation.svc?xsd=xsd3'">
                    <xsl:attribute name="schemaLocation">WSSecureConversation_3.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WSSecureConversationSign.svc?xsd=xsd3'">
                    <xsl:attribute name="schemaLocation">WSSecureConversation_3.xsd</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="schemaLocation">
                        <xsl:value-of select="@schemaLocation"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates select="@namespace"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="wsdl:import">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="@location='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WSSecureConversation.svc?wsdl'">
                    <xsl:attribute name="location">WSSecureConversation.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:when test="@location='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WSSecureConversationSign.svc?wsdl'">
                    <xsl:attribute name="location">WSSecureConversation.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="location">WSSecureConversation_policy.wsdl</xsl:attribute>
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
                    <xsl:attribute name="location">http://131.107.153.205/<xsl:value-of select="substring(@location,24)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,'https://ndgo-introp-s24/')">
                    <xsl:attribute name="location">https://131.107.153.205/<xsl:value-of select="substring(@location,25)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,'https://ndgo-introp-s24:8443/')">
                    <xsl:attribute name="location">https://131.107.153.205:8443/<xsl:value-of select="substring(@location,30)"/></xsl:attribute>
                </xsl:when>
                <!--xsl:when test="starts-with(@location,'http://131.107.153.205/')">
                    <xsl:attribute name="location">http://131.107.72.15/<xsl:value-of select="substring(@location,24)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,'https://131.107.153.205/')">
                    <xsl:attribute name="location">https://131.107.72.15/<xsl:value-of select="substring(@location,25)"/></xsl:attribute>
                </xsl:when-->
                <xsl:otherwise>
                    <xsl:apply-templates select="@location"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="wsdl:portType[@name='IPingServiceSign']">
        <xsl:copy>
            <xsl:attribute name="name">IPingService</xsl:attribute>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="wsdl:service[@name='PingServiceSign']">
        <xsl:copy>
            <xsl:attribute name="name">PingService</xsl:attribute>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="wsdl:binding">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="substring(@name, string-length(@name) - 3) = 'Sign'">
                    <xsl:attribute name="name"><xsl:value-of select="substring(@name,1,string-length(@name) - 4)"/></xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="@name"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="substring(@type, string-length(@type) - 3) = 'Sign'">
                    <xsl:attribute name="type"><xsl:value-of select="substring(@type,1,string-length(@type) - 4)"/></xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="@type"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="wsdl:port">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="substring(@binding, string-length(@binding) - 3) = 'Sign'">
                    <xsl:attribute name="binding"><xsl:value-of select="substring(@binding,1,string-length(@binding) - 4)"/></xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="@binding"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="@name='SecureConversation_MutualCertificate10Sign_IPingServiceSign'"><xsl:attribute name="name">SecureConversation_MutualCertificate10SignEncrypt_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='XSC_IPingServiceSign'"><xsl:attribute name="name">XC_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='XSDC_IPingServiceSign'"><xsl:attribute name="name">XDC_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='XSDC_IPingServiceSign1'"><xsl:attribute name="name">XDC_IPingService1</xsl:attribute></xsl:when>
                <xsl:when test="@name='_XS_IPingServiceSign'"><xsl:attribute name="name">_X_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='_XSD_IPingServiceSign'"><xsl:attribute name="name">_XD_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='KSC_IPingServiceSign'"><xsl:attribute name="name">KC_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='KSC10_IPingServiceSign'"><xsl:attribute name="name">KC10_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='KSDC10_IPingServiceSign'"><xsl:attribute name="name">KDC10_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='KSDC_IPingServiceSign'"><xsl:attribute name="name">KDC_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='_KS_IPingServiceSign'"><xsl:attribute name="name">_K_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='_KS10_IPingServiceSign'"><xsl:attribute name="name">_K10_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='_KSD_IPingServiceSign'"><xsl:attribute name="name">_KD_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='_KSD10_IPingServiceSign'"><xsl:attribute name="name">_KD10_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='ASC_IPingServiceSign'"><xsl:attribute name="name">AC_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='ASDC_IPingServiceSign'"><xsl:attribute name="name">ADC_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='_AS_IPingServiceSign'"><xsl:attribute name="name">_A_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='_ASD_IPingServiceSign'"><xsl:attribute name="name">_AD_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='UXSC_IPingServiceSign'"><xsl:attribute name="name">UXC_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='UXSDC_IPingServiceSign'"><xsl:attribute name="name">UXDC_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='_UXS_IPingServiceSign'"><xsl:attribute name="name">_UX_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="@name='_UXSD_IPingServiceSign'"><xsl:attribute name="name">_UXD_IPingService</xsl:attribute></xsl:when>
                <xsl:when test="substring(@name, string-length(@name) - 3) = 'Sign'">
                    <xsl:attribute name="name">
                        <xsl:value-of select="substring(@name,1,string-length(@name) - 4)"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:when test="substring(@name, string-length(@name) - 4) = 'Sign1'">
                    <xsl:attribute name="name"><xsl:value-of select="substring(@name,1,string-length(@name) - 5)"/>1</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="@name"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="xs:element[@ref='xs:schema']">
    </xsl:template>
    <xsl:template match="wsdl:port/wsa:EndpointReference">
    </xsl:template>
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
