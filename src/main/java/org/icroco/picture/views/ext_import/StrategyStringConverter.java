package org.icroco.picture.views.ext_import;

import javafx.util.StringConverter;

class StrategyStringConverter extends StringConverter<IRenameFilesStrategy> {
    @Override
    public String toString(IRenameFilesStrategy object) {
        return object.displayName();
    }

    @Override
    public IRenameFilesStrategy fromString(String name) {
        throw new RuntimeException("Not Implemented");
    }
}
