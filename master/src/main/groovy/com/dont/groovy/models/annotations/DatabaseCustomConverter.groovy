package com.dont.groovy.models.annotations

import com.google.gson.TypeAdapter

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface DatabaseCustomConverter {

    Class target()

    Class<? extends TypeAdapter> converter()

}