package org.icroco.picture.ui.model.mapper;

import org.icroco.picture.ui.model.CatalogueEntry;
import org.icroco.picture.ui.persistence.model.DbCatalogEntry;
import org.mapstruct.Builder;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(builder = @Builder(disableBuilder = true))
public interface CatalogEntryMapper {
//    @Mapping(target = "manufacturer", source = "make")
    CatalogueEntry map(DbCatalogEntry entry);

    @InheritInverseConfiguration
    DbCatalogEntry map(CatalogueEntry entry);
}
