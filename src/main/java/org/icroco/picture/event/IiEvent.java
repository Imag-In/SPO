package org.icroco.picture.event;

import lombok.Builder;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@ToString
public class IiEvent extends ApplicationEvent {
    @Builder
    public IiEvent(Object source, Clock clock) {
        super(source, clock == null ? Clock.systemDefaultZone() : clock);
    }

    protected IiEvent(IiEventBuilder<?, ?> b) {
        this(b.source, b.clock);
    }

    public static IiEventBuilder<?, ?> builder() {
        return new IiEventBuilderImpl();
    }

    public static abstract class IiEventBuilder<C extends IiEvent, B extends IiEventBuilder<C, B>> {
        protected abstract B self();

        public abstract C build();

        public String toString() {
            return STR."IiEvent.IiEventBuilder(super=\{super.toString()})";
        }
    }

    private static final class IiEventBuilderImpl extends IiEventBuilder<IiEvent, IiEventBuilderImpl> {
        private IiEventBuilderImpl() {
        }

        protected IiEventBuilderImpl self() {
            return this;
        }

        public IiEvent build() {
            return new IiEvent(this);
        }
    }

    public ZonedDateTime getDateTime() {
        return Instant.ofEpochMilli(getTimestamp()).atZone(ZoneId.systemDefault());
    }
}
