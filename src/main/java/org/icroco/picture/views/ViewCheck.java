package org.icroco.picture.views;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.views.util.FxView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ViewCheck {

    private final List<FxView<?>> views;

    @PostConstruct
    void checkViews() {
        for (FxView<?> view : views) {
            if (view.getRootContent().getId() == null || view.getRootContent().getId().isBlank()) {
                log.error("FxView: '{}'. rootContent do node have Id (Node.setId(...))", view.getClass().getSimpleName());
            }
        }
    }
}
