package org.icroco.picture.ui.model.mapper;

import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.persistence.DbCatalog;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(uses = {CatalogEntryMapper.class, MediaFileMapper.class})
public interface CatalogMapper {
//    @Mapping(target = "manufacturer", source = "make")
    Catalog map(DbCatalog catalog);

    @InheritInverseConfiguration
    DbCatalog map(Catalog catalog);
}
