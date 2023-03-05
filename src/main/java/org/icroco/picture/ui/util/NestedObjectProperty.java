package org.icroco.picture.ui.util;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;

import java.util.function.Function;

public class NestedObjectProperty<T, D> extends SimpleObjectProperty<T> {
    /**
     * Listener to the dependand property
     */
    @SuppressWarnings("FieldCanBeLocal") //mustn't be lokal due to the WeakChangeListener...
    private final ChangeListener<D> listener;
    /**
     * The binding partner of this instance
     */
    private       Property<T>       boundProperty;

    public NestedObjectProperty(ObservableValue<D> dependandProperty, final Function<D, Property<T>> getFunc) {
        this(dependandProperty, getFunc, false);
    }

    /**
     * Creates a new instance
     *
     * @param dependandProperty the property that delivers the property to bind against
     * @param getFunc           a {@link java.util.function.Function} of D and ObjectProperty of T
     * @param bidirectional     boolean flag, if the binding should be bidirectional or unidirectional
     */
    public NestedObjectProperty(ObservableValue<D> dependandProperty, final Function<D, Property<T>> getFunc, boolean bidirectional) {
        bindDependandProperty(dependandProperty.getValue(), getFunc, bidirectional);

        listener = (observable, oldValue, newValue) -> {
            if (boundProperty != null) {
                if (bidirectional) {
                    unbindBidirectional(boundProperty);
                } else {
                    unbind();
                }
                boundProperty = null;
            }
            bindDependandProperty(newValue, getFunc, bidirectional);
        };
        dependandProperty.addListener(new WeakChangeListener<>(listener));
    }

    /**
     * Binds this instance to the dependant property
     *
     * @param newValue D
     * @param getFunc  Function> getFunc, boolean bidirectional) {
     */
    private void bindDependandProperty(D newValue, Function<D, Property<T>> getFunc, boolean bidirectional) {
        if (newValue != null) {
            Property<T> newProp = getFunc.apply(newValue);
            boundProperty = newProp;
            if (newProp != null) {
                if (bidirectional) {
                    bindBidirectional(newProp);
                } else {
                    bind(newProp);
                }
            } else {
                set(null);
            }
        } else {
            set(null);
        }
    }
}
