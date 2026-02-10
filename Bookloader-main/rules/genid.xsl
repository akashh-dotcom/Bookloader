<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:template match="book">
    <xsl:for-each select="node()">
        <xsl:message>id = <xsl:value-of select="generate-id(.)"/></xsl:message>
    </xsl:for-each>
</xsl:template>
</xsl:stylesheet>
