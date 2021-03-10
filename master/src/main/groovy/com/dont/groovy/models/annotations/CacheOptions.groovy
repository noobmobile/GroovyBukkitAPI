package com.dont.groovy.models.annotations

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface CacheOptions {

    boolean loadAllOnEnable() default false

    boolean saveAllOnDisable() default true

    boolean autoSave() default true

    int autoSaveDelay() default 30

    boolean loadOnJoin() default false

    boolean saveOnQuit() default false

    boolean uncacheOnQuit() default true

}
