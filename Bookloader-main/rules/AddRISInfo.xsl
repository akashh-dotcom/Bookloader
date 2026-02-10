<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:exsl="http://exslt.org/common" exclude-result-prefixes="exsl saxon" version="1.1"
    xmlns:saxon="http://icl.com/saxon">
    <!-- Import common xslt-->
    <xsl:import href="common.xsl"/>
    <xsl:output method="xml" encoding="UTF-8" indent="yes" 
        doctype-public="-//RIS Dev//DTD DocBook V4.3 -Based Variant V1.1//EN"
        doctype-system="C:/RittenhouseRepos/Bookloader/test/dtd/v1.1/RittDocBook.dtd"
        />
    <xsl:strip-space elements="*"/>
    
    <!--Variable definition -->
    <xsl:variable name="risbooktitle" select="book/bookinfo/title | book/bookinfo/title/emphasis"/>
     <xsl:variable name="risisbn"><!--Stip dashes -->
        <xsl:call-template name="search-and-replace">
            <xsl:with-param name="input" select="/book/bookinfo/isbn | /book/bookinfo/isbn/emphasis"/>
            <xsl:with-param name="search-string">-</xsl:with-param>
            <xsl:with-param name="replace-string"></xsl:with-param>
        </xsl:call-template>
     </xsl:variable>
    
    <xsl:variable name="primaryauthor">
        <primaryauthor>
		<xsl:choose >
			<xsl:when test="/book/bookinfo/authorgroup/author[1]/personname/node() != ''"><xsl:copy-of select="/book/bookinfo/authorgroup/author[1]/node()"/></xsl:when>
			<xsl:when test="/book/bookinfo/authorgroup/author[1]/node() != ''">
				<personname>
					<xsl:copy-of select="/book/bookinfo/authorgroup/author[1]/honorific"/>
					<xsl:copy-of select="/book/bookinfo/authorgroup/author[1]/firstname"/>
					<xsl:copy-of select="/book/bookinfo/authorgroup/author[1]/othername"/>
					<xsl:copy-of select="/book/bookinfo/authorgroup/author[1]/surname"/>
					<xsl:copy-of select="/book/bookinfo/authorgroup/author[1]/lineage"/>
					<xsl:copy-of select="/book/bookinfo/authorgroup/author[1]/degree"/>
				</personname></xsl:when>
			<xsl:when test="/book/bookinfo/author[1]/personname/node() != ''"><xsl:copy-of select="/book/bookinfo/author[1]/node()"/></xsl:when>
			<xsl:when test="/book/bookinfo/author[1]/node() != ''">
				<personname>
					<xsl:copy-of select="/book/bookinfo/author[1]/honorific"/>
					<xsl:copy-of select="/book/bookinfo/author[1]/firstname"/>
					<xsl:copy-of select="/book/bookinfo/author[1]/othername"/>
					<xsl:copy-of select="/book/bookinfo/author[1]/surname"/>
					<xsl:copy-of select="/book/bookinfo/author[1]/lineage"/>
					<xsl:copy-of select="/book/bookinfo/author[1]/degree"/>
				</personname></xsl:when>

			<xsl:when test="/book/bookinfo/authorgroup/editor[1]/personname/node() != ''"><xsl:copy-of select="/book/bookinfo/authorgroup/editor[1]/node()"/></xsl:when>
			<xsl:when test="/book/bookinfo/authorgroup/editor[1]/node() != ''">
				<personname>
					<xsl:copy-of select="/book/bookinfo/authorgroup/editor[1]/honorific"/>
					<xsl:copy-of select="/book/bookinfo/authorgroup/editor[1]/firstname"/>
					<xsl:copy-of select="/book/bookinfo/authorgroup/editor[1]/othername"/>
					<xsl:copy-of select="/book/bookinfo/authorgroup/editor[1]/surname"/>
					<xsl:copy-of select="/book/bookinfo/authorgroup/editor[1]/lineage"/>
					<xsl:copy-of select="/book/bookinfo/authorgroup/editor[1]/degree"/>
				</personname></xsl:when>
			<xsl:when test="/book/bookinfo/editor[1]/personname/node() != ''"><xsl:copy-of select="/book/bookinfo/editor[1]/node()"/></xsl:when>
			<xsl:when test="/book/bookinfo/editor[1]/node() != ''">
			<personname>
					<xsl:copy-of select="/book/bookinfo/editor[1]/honorific"/>
					<xsl:copy-of select="/book/bookinfo/editor[1]/firstname"/>
					<xsl:copy-of select="/book/bookinfo/editor[1]/othername"/>
					<xsl:copy-of select="/book/bookinfo/editor[1]/surname"/>
					<xsl:copy-of select="/book/bookinfo/editor[1]/lineage"/>
					<xsl:copy-of select="/book/bookinfo/editor[1]/degree"/>
				</personname></xsl:when>			<xsl:when test="/chapter">
				<!-- Chapter documents don't have bookinfo, use empty author -->
				<personname><firstname></firstname><surname></surname></personname>		</xsl:when>
		<xsl:when test="/dedication or /preface or /acknowledgements or /colophon or /appendix or /glossary or /bibliography or /index">
			<!-- Other document types don't have bookinfo, use empty author -->
			<personname><firstname></firstname><surname></surname></personname>			</xsl:when>			<xsl:otherwise><xsl:message terminate="yes" >No Author Found</xsl:message></xsl:otherwise>	
		</xsl:choose>	
        </primaryauthor>
    </xsl:variable>
    
    <xsl:variable name="pubdate">
        <pubdate><xsl:choose>
					<xsl:when test="/book/bookinfo//copyright/year[last()] != '' "><xsl:value-of select="/book/bookinfo//copyright/year[last()]"/></xsl:when>
					<xsl:when test="/book/bookinfo//pubdate != '' "><xsl:value-of select="/book/bookinfo//pubdate"/></xsl:when>
				</xsl:choose></pubdate>
    </xsl:variable>
    
    <xsl:variable name="publisher">
        <xsl:copy-of select="/book/bookinfo//publisher"/>
    </xsl:variable>
    
    <!--copy everything by default, override copy in templates below-->
    <xsl:template match="node() | @*">
         <xsl:copy>
             <xsl:apply-templates select="@* | node()"/>
         </xsl:copy>
    </xsl:template>
    
    <xsl:template match="/book/bookinfo/title">
        <xsl:copy-of select="."/>
        <xsl:copy-of select="$primaryauthor"/>
    </xsl:template>
    
    <xsl:template match="/book/bookinfo/isbn">
        <isbn>
            <xsl:value-of select="$risisbn"/>
        </isbn>
    </xsl:template>
    
    <xsl:variable name="authors">
        <xsl:copy-of select="book/bookinfo/authorgroup"/>
    </xsl:variable>
    
    <xsl:variable name="bookimage">
        <xsl:copy-of select="book/bookinfo/mediaobject"/>
    </xsl:variable>
    
    <xsl:template match="book//appendix | book//preface">
        <xsl:variable name="infoTagName" select="concat(name(),'info')"/>
        <xsl:variable name="count" select="count(saxon:evaluate($infoTagName))"/>
        <xsl:choose>
            <xsl:when test="$count=0">
                <xsl:copy>
                    <xsl:for-each select="@*">
                        <xsl:copy/>
                    </xsl:for-each>
                    <xsl:element name="{$infoTagName}">
                        <xsl:call-template name="Add-RIS-Info-No-Chapter"/>
                    </xsl:element>
                    <xsl:apply-templates select="node()"/>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select="@* | node()"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
 <xsl:template match="book//dedication">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:copy/>
            </xsl:for-each>
            <xsl:call-template name="Add-RIS-Info-No-Chapter-No-PrimaryAuthor"/>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="chapter">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="sect1">
        <xsl:variable name="sect1infocount" select="count(sect1info)"/>
        <xsl:choose>
            <xsl:when test="$sect1infocount=0">
                <xsl:copy>
                    <xsl:for-each select="@*">
                        <xsl:copy/>
                    </xsl:for-each>
                    <xsl:element name="sect1info">
                        <xsl:call-template name="Add-RIS-Info"/>
						<xsl:call-template name="chapter-Authors" ><xsl:with-param name="sect1Node"	select="."	/></xsl:call-template>
                    </xsl:element>
                    <xsl:apply-templates select="node()"/>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select="@* | node()"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="sect1info">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:copy/>
            </xsl:for-each>
            <xsl:call-template name="Add-RIS-Info"/>
            <xsl:apply-templates select="node()"/>
            <xsl:call-template name="chapter-Authors" ><xsl:with-param name="sect1Node"	select=".."	/></xsl:call-template>
        </xsl:copy>
    </xsl:template>
    
<!-- the first sect1 in a chapter should have the chapter authors if they exists -->    
    <xsl:template name="chapter-Authors">
		<xsl:param name="sect1Node"></xsl:param>	
		<xsl:variable name="isFirstSect1Node"><xsl:value-of select="ancestor-or-self::chapter/sect1[position()]/@id"/></xsl:variable>	
		<xsl:if test="$isFirstSect1Node = @id ">
			<xsl:if test="ancestor-or-self::chapter/chapterinfo/authorgroup[1]/node() != ''" ><xsl:copy-of  select="ancestor-or-self::chapter/chapterinfo/authorgroup"	/></xsl:if>
			<xsl:if test="ancestor-or-self::chapter/chapterinfo/author[1]/node() != ''" ><xsl:copy-of  select="ancestor-or-self::chapter/chapterinfo/author"	/></xsl:if>
			<xsl:if test="ancestor-or-self::chapter/chapterinfo/editor[1]/node() != ''"><xsl:copy-of  select="ancestor-or-self::chapter/chapterinfo/editor"	/></xsl:if>
		</xsl:if>
    </xsl:template>		

    <xsl:template match="title" mode="chapter.title.complete"	>
		<xsl:param name="sect1id">test</xsl:param>	
            <xsl:element name="chaptertitle"><xsl:apply-templates     mode="chapter.title.complete"	><xsl:with-param name="sect1id" select="$sect1id" /></xsl:apply-templates></xsl:element>                
    </xsl:template>

    <xsl:template match="subtitle" mode="chapter.title.complete"	>
		<xsl:param name="sect1id">test</xsl:param>	
            <xsl:element name="chaptersubtitle"><xsl:apply-templates     mode="chapter.title.complete"	><xsl:with-param name="sect1id" select="$sect1id" /></xsl:apply-templates></xsl:element>                
    </xsl:template>

    <xsl:template match="footnote" mode="chapter.title.complete"	>		
    <xsl:param name="sect1id">test</xsl:param>	
    <xsl:copy  >
    <xsl:for-each select="@*">
    <xsl:choose>
					<xsl:when test="local-name(.) = 'id' "><xsl:attribute name="id"><xsl:value-of select="."	/><xsl:value-of select="$sect1id"	/></xsl:attribute></xsl:when>
					<xsl:otherwise><xsl:attribute name="{local-name(.)}"><xsl:value-of select="."	/></xsl:attribute></xsl:otherwise>	
				</xsl:choose>	
    </xsl:for-each>	
    
    
    <xsl:copy-of select="*"	/></xsl:copy>
    </xsl:template>	

    <xsl:template match="*" mode="chapter.title.complete"	><xsl:value-of  select="."	/></xsl:template>	

    <xsl:template name="Add-RIS-Info">
            <xsl:element name="risinfo">
                <xsl:element name="booktitle">
                    <xsl:value-of select="$risbooktitle"/>
                </xsl:element>
                <xsl:element name="isbn">
                    <xsl:value-of select="$risisbn"/>
                </xsl:element>
                <xsl:element name="chapternumber">
                    <xsl:value-of select="ancestor-or-self::chapter/@label"/>
                </xsl:element>
                <xsl:element name="chapterid">
                    <xsl:value-of select="ancestor-or-self::chapter/@id"/>
                </xsl:element>
                <xsl:apply-templates select="ancestor-or-self::chapter/title" mode="chapter.title.complete" ><xsl:with-param name="sect1id" select="ancestor-or-self::sect1/@id" /></xsl:apply-templates>					
                <xsl:apply-templates select="ancestor-or-self::chapter/subtitle" mode="chapter.title.complete" ><xsl:with-param name="sect1id" select="ancestor-or-self::sect1/@id" /></xsl:apply-templates>					

                <xsl:copy-of select="$authors"/>
                <xsl:copy-of select="$publisher"/>
                <xsl:copy-of select="$pubdate"/>
            </xsl:element>
            <xsl:copy-of select="$primaryauthor"/>
            <xsl:copy-of select="$bookimage"/>            
    </xsl:template>
    
     <xsl:template name="Add-RIS-Info-No-Chapter">
            <xsl:element name="risinfo">
                <xsl:element name="booktitle">
                    <xsl:value-of select="$risbooktitle"/>
                </xsl:element>
                <xsl:element name="isbn">
                    <xsl:value-of select="$risisbn"/>
                </xsl:element>
                <xsl:copy-of select="$authors"/>
                <xsl:copy-of select="$publisher"/>
                <xsl:copy-of select="$pubdate"/>
            </xsl:element>
            <xsl:copy-of select="$primaryauthor"/>
            <xsl:copy-of select="$bookimage"/>
    </xsl:template>

<xsl:template name="Add-RIS-Info-No-Chapter-No-PrimaryAuthor">
            <xsl:element name="risinfo">
                <xsl:element name="booktitle">
                    <xsl:value-of select="$risbooktitle"/>
                </xsl:element>
                <xsl:element name="isbn">
                    <xsl:value-of select="$risisbn"/>
                </xsl:element>
                <xsl:copy-of select="$authors"/>
                <xsl:copy-of select="$publisher"/>
                <xsl:copy-of select="$pubdate"/>
            </xsl:element>
            <xsl:copy-of select="$bookimage"/>
    </xsl:template>
    
</xsl:stylesheet>
