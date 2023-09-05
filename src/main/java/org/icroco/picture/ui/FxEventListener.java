package org.icroco.picture.ui;


import org.icroco.picture.ui.config.ImageInConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.lang.annotation.*;


@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Async(ImageInConfiguration.FX_EXECUTOR)
@EventListener()
public @interface FxEventListener {
}
