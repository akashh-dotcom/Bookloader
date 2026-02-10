<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.1">
    <xsl:import href="common.xsl"/>
    <xsl:param name="basedir" select="'./'"/>
    <xsl:output method="xml" encoding="UTF-8" indent="yes" omit-xml-declaration="no"/>
    <xsl:strip-space elements="*"/>
    
    <xsl:variable name="risisbn">
		<xsl:value-of select="translate(/book/bookinfo/isbn,' -&#10;&#13;','' )"	/>
<!--        <xsl:call-template name="search-and-replace">
            <xsl:with-param name="input" select="/book/bookinfo/isbn"/>
            <xsl:with-param name="search-string">- \n
            </xsl:with-param>
            <xsl:with-param name="replace-string"></xsl:with-param>
        </xsl:call-template> -->
    </xsl:variable>

    <xsl:template name="add-doctype-head">
        <xsl:text disable-output-escaping="yes">&#60;!DOCTYPE book PUBLIC &#34;-//RIS Dev//DTD DocBook V4.3 -Based Variant V1.1//EN&#34; &#34;http://localhost/dtd/v1.1/RittDocBook.dtd&#34; [</xsl:text>
    </xsl:template>
        
    <xsl:template name="add-entity-ref-body">
        <xsl:for-each select="book//chapter/sect1 ">
            <xsl:variable name="outFile" select="concat(name(),'.',$risisbn,'.',translate(@id,' ','_'))"/>
            <xsl:text disable-output-escaping="yes">&#60;!ENTITY </xsl:text><xsl:value-of select="$outFile"/><xsl:text disable-output-escaping="yes"> SYSTEM &#34;</xsl:text><xsl:value-of select="$outFile"/><xsl:text disable-output-escaping="yes">.xml&#34;&#62;</xsl:text>
        </xsl:for-each>
        <xsl:for-each select="book/appendix | book/toc | book/dedication | book/preface | book/bibliography | book/part/appendix | book/part/toc | book/part/dedication | book/part/preface | book/part/bibliography | book//chapter/bibliography">
            <xsl:variable name="outFileAppendix">
                <xsl:choose>
                    <xsl:when test="name()='bibliography' and parent::chapter">sect1.<xsl:value-of select="$risisbn"/>.<xsl:value-of select="translate(@id,' ','_')"/></xsl:when>
                    <xsl:otherwise><xsl:value-of select="concat(name(),'.',$risisbn,'.',translate(@id,' ','_'))"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:text disable-output-escaping="yes">&#60;!ENTITY </xsl:text><xsl:value-of select="$outFileAppendix"/><xsl:text disable-output-escaping="yes"> SYSTEM &#34;</xsl:text><xsl:value-of select="$outFileAppendix"/><xsl:text disable-output-escaping="yes">.xml&#34;&#62;</xsl:text>
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template name="add-entity-ref-foot">
        <xsl:text disable-output-escaping="yes">]&#62;</xsl:text>
    </xsl:template>
    
    <xsl:template match="/">
        <xsl:call-template name="add-doctype-head"/>
        <xsl:call-template name="add-entity-ref-body"/>
        <xsl:call-template name="add-entity-ref-foot"/>
        <xsl:apply-templates/>
    </xsl:template>
    
    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="book//chapter/sect1 ">
        <xsl:variable name="outFile" select="concat(name(),'.',$risisbn,'.',translate(@id,' ','_'))"/>
        <xsl:variable name="outFileName" select="concat($basedir,$outFile,'.xml')"/>
        <xsl:variable name="prevOutFileName" select="concat(name(),'.',$risisbn,'.',translate(./preceding-sibling::sect1[1]/@id,' ','_'))"/>
        <xsl:variable name="nextOutFileName" select="concat(name(),'.',$risisbn,'.',translate(./following-sibling::sect1/@id,' ','_'))"/>
        <xsl:variable name="tempRISInfo">
                <xsl:apply-templates select="sect1info/risinfo">
                    <xsl:with-param name="risprev" select="$prevOutFileName"/>
                    <xsl:with-param name="riscurrent" select="$outFile"/>
                    <xsl:with-param name="risnext" select="$nextOutFileName"/>
                </xsl:apply-templates>
        </xsl:variable>
        <xsl:variable name="tempSect1Info">
            <xsl:apply-templates select="sect1info">
                <xsl:with-param name="tempRISInfo" select="$tempRISInfo"/>
            </xsl:apply-templates>
        </xsl:variable>
        
        <!-- Non-standard saxon xsl:document! -->
        <xsl:document method="xml" encoding="UTF-8" href="{$outFileName}">
            <xsl:copy><!--sect1 -->
                <xsl:copy-of select="@*"/>
                <xsl:copy-of select="$tempSect1Info"/>
                <xsl:copy-of select="./sect1info/following-sibling::*"/>
            </xsl:copy>
        </xsl:document>
        <xsl:text disable-output-escaping="yes"> &#38;</xsl:text><xsl:value-of select="$outFile"/>;
    </xsl:template>
    
    <xsl:template match="book/appendix | book/toc | book/dedication | book/preface | book/bibliography | book/part/appendix | book/part/toc | book/part/dedication | book/part/preface | book/part/bibliography | book//chapter/bibliography">
        <xsl:variable name="outFileAppendix">
            <xsl:choose>
                <xsl:when test="name()='bibliography' and parent::chapter">sect1.<xsl:value-of select="$risisbn"/>.<xsl:value-of select="translate(@id,' ','_')"/></xsl:when>
                <xsl:otherwise><xsl:value-of select="concat(name(),'.',$risisbn,'.',translate(@id,' ','_'))"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="outAppendixFileName" select="concat($basedir,$outFileAppendix,'.xml')"/>
        <!-- Non-standard saxon xsl:document! -->
        <xsl:document method="xml" encoding="UTF-8" href="{$outAppendixFileName}">
            <xsl:copy><!--toc -->
                <xsl:copy-of select="@*"/>
                <xsl:copy-of select="*"/>
            </xsl:copy>
        </xsl:document>
        <xsl:text disable-output-escaping="yes"> &#38;</xsl:text><xsl:value-of select="$outFileAppendix"/>;
    </xsl:template>
    
    <xsl:template match="sect1info">
        <xsl:param name="tempRISInfo"/>
        <xsl:copy><!--sect1info -->
            <xsl:copy-of select="@*"/>
            <xsl:copy-of select="./risinfo/preceding-sibling::*"/>
            <xsl:copy-of select="$tempRISInfo"/>
            <xsl:copy-of select="./risinfo/following-sibling::*"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="sect1info/risinfo">
    <xsl:param name="risprev"/>
        <xsl:param name="riscurrent"/>
        <xsl:param name="risnext"/>
         
             <xsl:copy><!--risinfo -->
                 <xsl:copy-of select="@*"/>
             <xsl:element name="risprev">
            <xsl:value-of select="$risprev"/>
        </xsl:element>
        <xsl:element name="riscurrent">
            <xsl:value-of select="$riscurrent"/>
        </xsl:element>
        <xsl:element name="risnext">
            <xsl:value-of select="$risnext"/>
        </xsl:element>
                    <xsl:copy-of select="*"/>
             </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
