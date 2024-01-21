package org.icroco.picture.views.repair;

import javafx.scene.Node;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

public interface RepairTool {
    static NavTree.Item GENERAL   = NavTree.Item.group("General", new FontIcon(MaterialDesignM.MENU_OPEN));
    static NavTree.Item DUPLICATE = NavTree.Item.group("Duplicate", new FontIcon(MaterialDesignC.CONTENT_DUPLICATE));
    static NavTree.Item THUMBNAIL_GENERATE = NavTree.Item.group("Thumbnails", new FontIcon(MaterialDesignI.IMAGE_BROKEN));
    static NavTree.Item OTHERS = NavTree.Item.group("Other", new FontIcon(MaterialDesignT.TOOLS));

    String getName();

    Ikon getIcon();

    Node getView();

    default NavTree.Item getGroup() {
        return GENERAL;
    }
}
