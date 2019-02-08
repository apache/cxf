<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/">
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"><soap:Body>
        <ns2:DoubleItResponse xmlns:ns2="http://www.example.org/schema/DoubleIt">
        <doubledNumber>1<xsl:value-of select="soap:Envelope/soap:Body/ns2:DoubleItResponse/doubledNumber"/></doubledNumber>
        </ns2:DoubleItResponse></soap:Body></soap:Envelope>

    </xsl:template>

</xsl:stylesheet>
