package com.example.backend.audit;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ActivityTarget {

    ActivityTargetType value();
}