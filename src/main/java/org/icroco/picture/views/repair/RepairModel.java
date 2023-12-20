package org.icroco.picture.views.repair;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TreeItem;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.util.List;
import java.util.Objects;

@Slf4j
public class RepairModel {

    private final ReadOnlyObjectWrapper<RepairTool> selectedPage = new ReadOnlyObjectWrapper<>();

    public ReadOnlyObjectProperty<RepairTool> selectedPageProperty() {
        return selectedPage.getReadOnlyProperty();
    }

    private final ReadOnlyObjectWrapper<NavTree.Item> navTree = new ReadOnlyObjectWrapper<>();

    public ReadOnlyObjectProperty<NavTree.Item> navTreeProperty() {
        return navTree.getReadOnlyProperty();
    }

    public void navigate(RepairTool tool) {
        log.info("Navigate to: {}", tool.getName());
        selectedPage.set(Objects.requireNonNull(tool));
//        currentSubLayer.set(PAGE);
    }

    public void createTree(List<RepairTool> tools) {
        var root = NavTree.Item.root();

        tools.forEach(tool -> {
            int           idx = root.getChildren().indexOf(tool.getGroup());
            TreeItem<Nav> group;
            if (idx == -1) {
                group = tool.getGroup();
                root.getChildren().add(group);
            } else {
                group = root.getChildren().get(idx);
            }
            NavTree.Item page = NavTree.Item.page(tool.getName(), tool);
            group.getChildren().add(page);
        });
        navTree.set(root);
        root.getChildren().getFirst().setExpanded(true);
    }


    private NavTree.Item createTree() {
        var general = NavTree.Item.group("Duplicate", new FontIcon(MaterialDesignC.CONTENT_DUPLICATE));
        general.getChildren().setAll(
//                NavTree.Item.page("Same hash", DuplicateTool.class)
        );
        general.setExpanded(true);
//
//        var containers = NavTree.Item.group("Containers", new FontIcon(Material2OutlinedMZ.TABLE_CHART));
//        containers.getChildren().setAll(
//                NAV_TREE.get(AccordionPage.class),
//                NAV_TREE.get(CardPage.class),
//                NAV_TREE.get(ContextMenuPage.class),
//                NAV_TREE.get(DeckPanePage.class),
//                NAV_TREE.get(InputGroupPage.class),
//                NAV_TREE.get(ModalPanePage.class),
//                NAV_TREE.get(ScrollPanePage.class),
//                NAV_TREE.get(SeparatorPage.class),
//                NAV_TREE.get(SplitPanePage.class),
//                NAV_TREE.get(PopoverPage.class),
//                NAV_TREE.get(TilePage.class),
//                NAV_TREE.get(TitledPanePage.class),
//                NAV_TREE.get(ToolBarPage.class)
//        );

//        var dataDisplay = NavTree.Item.group("Data Display", new FontIcon(Material2OutlinedAL.LIST_ALT));
//        dataDisplay.getChildren().setAll(
//                NAV_TREE.get(ChartPage.class),
//                NAV_TREE.get(ListViewPage.class),
//                NAV_TREE.get(TableViewPage.class),
//                NAV_TREE.get(TreeTableViewPage.class),
//                NAV_TREE.get(TreeViewPage.class)
//        );
//
//        var feedback = NavTree.Item.group("Feedback", new FontIcon(Material2OutlinedAL.CHAT_BUBBLE_OUTLINE));
//        feedback.getChildren().setAll(
//                NAV_TREE.get(DialogPage.class),
//                NAV_TREE.get(MessagePage.class),
//                NAV_TREE.get(NotificationPage.class),
//                NAV_TREE.get(ProgressIndicatorPage.class),
//                NAV_TREE.get(TooltipPage.class)
//        );
//
//        var inputs = NavTree.Item.group("Inputs & Controls", new FontIcon(Material2OutlinedAL.EDIT));
//        inputs.getChildren().setAll(
//                NAV_TREE.get(ButtonPage.class),
//                NAV_TREE.get(CalendarPage.class),
//                NAV_TREE.get(CheckBoxPage.class),
//                NAV_TREE.get(ChoiceBoxPage.class),
//                NAV_TREE.get(ColorPickerPage.class),
//                NAV_TREE.get(ComboBoxPage.class),
//                NAV_TREE.get(CustomTextFieldPage.class),
//                NAV_TREE.get(DatePickerPage.class),
//                NAV_TREE.get(HtmlEditorPage.class),
//                NAV_TREE.get(MenuButtonPage.class),
//                NAV_TREE.get(RadioButtonPage.class),
//                NAV_TREE.get(SliderPage.class),
//                NAV_TREE.get(SpinnerPage.class),
//                NAV_TREE.get(TextAreaPage.class),
//                NAV_TREE.get(TextFieldPage.class),
//                NAV_TREE.get(ToggleButtonPage.class),
//                NAV_TREE.get(ToggleSwitchPage.class)
//        );
//
//        var navigation = NavTree.Item.group("Navigation", new FontIcon(Material2OutlinedMZ.MENU_OPEN));
//        navigation.getChildren().setAll(
//                NAV_TREE.get(BreadcrumbsPage.class),
//                NAV_TREE.get(MenuBarPage.class),
//                NAV_TREE.get(PaginationPage.class),
//                NAV_TREE.get(TabPanePage.class)
//        );
//
//        var showcases = NavTree.Item.group("Showcase", new FontIcon(Material2OutlinedMZ.VISIBILITY));
//        showcases.getChildren().setAll(
//                NAV_TREE.get(BlueprintsPage.class),
//                NAV_TREE.get(FileManagerPage.class),
//                NAV_TREE.get(MusicPlayerPage.class),
//                NAV_TREE.get(OverviewPage.class)
//        );

        var root = NavTree.Item.root();
        root.getChildren().setAll(
                general
//                containers,
//                dataDisplay,
//                feedback,
//                inputs,
//                navigation,
//                showcases
        );

        return root;
    }
}
