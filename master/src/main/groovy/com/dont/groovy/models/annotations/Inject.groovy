package com.dont.groovy.models.annotations

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * instanciar o objeto e injetar as dependências
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Inject {

    /**
     * realizar instância do objeto?
     * @return
     */
    boolean initialize() default true

    int priority() default 100
}
