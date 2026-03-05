package com.zgamelogic.discord.annotations.mappings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for mapping methods to specify a document to respond to an event with.
 * Most mapping fields already have a document field which can also be used in the same way as this annotation.
 * @author Ben Shabowski
 * @since 6.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Document {
    /**
     * The name of the document to respond to this event automatically.
     * If left blank, no document will be sent.
     * @return Document name to respond with
     */
    String value();
}
