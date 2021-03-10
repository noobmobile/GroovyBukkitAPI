package com.dont.groovy.models.annotations

import org.bukkit.event.EventPriority

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@interface EventSetting {

    EventPriority priority() default EventPriority.NORMAL;

    boolean ignoreCancelled() default false;
}

