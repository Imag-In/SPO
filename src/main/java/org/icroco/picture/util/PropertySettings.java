package org.icroco.picture.util;

import org.controlsfx.property.editor.PropertyEditor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertySettings {

    String category() default "";

    String displayName() default "";

    String description() default "";

    boolean isEditable() default true;

    boolean isFavorite() default false;

    int groupOrder() default 0;

    int propertyOrder() default 0;

    Class<? extends PropertyEditor<?>>[] editor() default {};
}
