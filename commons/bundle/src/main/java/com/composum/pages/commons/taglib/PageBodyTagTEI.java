/*
 * Copyright (c) 2013 IST GmbH Dresden
 * Eisenstuckstraße 10, 01069 Dresden, Germany
 * All rights reserved.
 *
 * Name: ComponentTagTEI.java
 * Autor: Mirko Zeibig
 * Datum: 25.01.2013 22:10:43
 */

package com.composum.pages.commons.taglib;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

public class PageBodyTagTEI extends TagExtraInfo {

    public PageBodyTagTEI() {
    }

    @Override
    public VariableInfo[] getVariableInfo(TagData data) {
        String type = PageBodyTag.class.getName();
        VariableInfo variableInfo = new VariableInfo(PageBodyTag.PAGE_BODY, type, true, VariableInfo.NESTED);
        return new VariableInfo[]{variableInfo};
    }
}
