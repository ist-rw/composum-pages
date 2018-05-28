<%@ page session="false" pageEncoding="UTF-8"
         import="com.google.gson.stream.JsonWriter,
                 java.io.OutputStreamWriter" %><%
%><%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %><%
%><cpp:defineObjects/><%
%><cpp:model var="model" type="com.composum.pages.commons.model.Statistics"><%
    slingResponse.setContentType("application/json");
    JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(slingResponse.getOutputStream()));
    model.getDataSet().toJSON(jsonWriter);
    jsonWriter.flush();
%></cpp:model>