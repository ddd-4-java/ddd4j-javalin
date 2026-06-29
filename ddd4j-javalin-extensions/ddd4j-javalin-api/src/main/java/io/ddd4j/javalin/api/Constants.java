/**
 * Copyright (C) 2018 Hiwepy (http://hiwepy.io).
 * All Rights Reserved.
 */
package io.ddd4j.javalin.api;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Constants {

    public static final String RT_SUCCESS = "success";
    public static final String RT_FAIL = "fail";
    public static final String RT_ERROR = "error";
    public static final String THEME_PARAM_NAME = "theme";
    public static final String THEME_PARAM_DEFAULT = "default";
    public static final String THEME_SOURCE_CLASSPATH = "classpath:/static/assets/css/themes/";
    public static final String LANG_PARAM_NAME = "lang";
    public static Marker accessMarker = MarkerFactory.getMarker("io.javalin.access");
    public static Marker authzMarker = MarkerFactory.getMarker("io.javalin.authz");
    public static Marker bizMarker = MarkerFactory.getMarker("io.javalin.biz");

}

