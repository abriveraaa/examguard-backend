package com.example.backend.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TrackActivity {

    String module();

    String action();

    String message() default "";

    boolean logArgs() default false;
}