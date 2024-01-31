package org.icroco.picture.views;


import org.icroco.picture.config.SpoConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.lang.annotation.*;


@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Async(SpoConfiguration.FX_EXECUTOR)
@EventListener()
public @interface FxEventListener {
}
