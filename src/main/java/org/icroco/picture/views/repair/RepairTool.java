package org.icroco.picture.views.repair;

import javafx.scene.Node;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;

public interface RepairTool {
    static NavTree.Item GENERAL   = NavTree.Item.group("General", new FontIcon(MaterialDesignM.MENU_OPEN));
    static NavTree.Item DUPLICATE = NavTree.Item.group("Duplicate", new FontIcon(MaterialDesignC.CONTENT_DUPLICATE));

    String getName();

    Ikon getIcon();

    Node getView();

    default NavTree.Item getGroup() {
        return GENERAL;
    }
}
