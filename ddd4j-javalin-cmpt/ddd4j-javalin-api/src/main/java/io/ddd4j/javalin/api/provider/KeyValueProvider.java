/**
 * Copyright (C) 2018 Hiwepy (http://hiwepy.io).
 * All Rights Reserved.
 */
package io.ddd4j.javalin.api.provider;

public interface KeyValueProvider<T> {

    public T get(String key);

}
