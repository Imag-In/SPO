package org.icroco.picture.views.ext_import;

import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.icroco.picture.util.I18N;

@RequiredArgsConstructor
class StrategyStringConverter extends StringConverter<IRenameFilesStrategy> {
    final private I18N i18N;

    @Override
    public String toString(IRenameFilesStrategy object) {
        return object == null ? null : i18N.get(object.getI18NId());
    }

    @Override
    public IRenameFilesStrategy fromString(String name) {
        throw new RuntimeException("Not Implemented");
    }
}
