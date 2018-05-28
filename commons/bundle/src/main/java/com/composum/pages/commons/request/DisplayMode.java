package com.composum.pages.commons.request;

import com.composum.sling.core.BeanContext;

import java.util.Stack;

public class DisplayMode extends Stack<DisplayMode.Value> {

    public enum Value {NONE, PREVIEW, BROWSE, EDIT, DEVELOP}

    public static boolean isEditMode(BeanContext context) {
        return isEditMode(current(context));
    }

    public static boolean isEditMode(Value value) {
        return value == Value.EDIT || value == Value.DEVELOP;
    }

    public static boolean isPreviewMode(BeanContext context) {
        return isPreviewMode(current(context));
    }

    public static boolean isPreviewMode(Value value) {
        return value == Value.PREVIEW || value == Value.BROWSE;
    }

    public static boolean isLiveMode(BeanContext context) {
        return isLiveMode(current(context));
    }

    public static boolean isLiveMode(Value value) {
        return value == Value.NONE;
    }

    /**
     * returns the current (topmost) value of the stack instance in the given context
     */
    public static Value current(BeanContext context) {
        return get(context).peek();
    }

    /**
     * returns the initial value of the stack instance in the given context
     */
    public static Value requested(BeanContext context) {
        return get(context).firstElement();
    }

    public static DisplayMode get(BeanContext context) {
        return context.getRequest().adaptTo(DisplayMode.class);
    }

    DisplayMode(Value requestedValue) {
        push(requestedValue);
    }

    public void reset(Value value) {
        clear();
        push(value);
    }
}
