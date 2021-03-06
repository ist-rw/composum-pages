<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineObjects/>
<cpp:element var="teaser" type="com.composum.pages.components.model.teaser.Teaser"
             cssAdd="@{teaserCSS}_variation_bg-image"
             style="background-image:url(@{teaser.imageUrl})">
    <cpp:dropZone property="image/imageRef" filter="asset:image">
        <cpn:link test="${teaser.hasLink}" body="true" class="${teaserCSS}_link"
                  href="${teaser.linkUrl}" target="${teaser.linkTarget}" title="${teaser.linkTitle}">
            <cpn:div test="${teaser.hasIcon}" class="${teaserCSS}_icon"><i
                    class="fa fa-${teaser.icon}"></i></cpn:div>
            <div class="${teaserCSS}_content">
                <cpp:include replaceSelectors="${teaser.textSelector}"/>
            </div>
        </cpn:link>
    </cpp:dropZone>
</cpp:element>
