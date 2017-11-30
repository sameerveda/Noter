package sam.apps.jbook_reader;

import static javafx.scene.input.KeyCombination.ALT_DOWN;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;
import static sam.apps.jbook_reader.Utils.button;
import static sam.apps.jbook_reader.Utils.combination;
import static sam.apps.jbook_reader.Utils.menuitem;
import static sam.apps.jbook_reader.Utils.radioMenuitem;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.When;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import sam.apps.jbook_reader.editor.Editor;
import sam.apps.jbook_reader.tabs.Tab;
import sam.apps.jbook_reader.tabs.TabContainer;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.properties.session.Session;

public class Viewer extends Application {
	public static void main( String[] args ) {
		launch(args);
	}
	private final TreeView<String> bookmarks = new TreeView<>();
	private BooleanBinding selectedItemNull = bookmarks.getSelectionModel().selectedItemProperty().isNull();

	private ReadOnlyObjectWrapper<Tab> currentTab = new ReadOnlyObjectWrapper<>();
	private  BooleanBinding currentTabNull = currentTab.isNull();
	private final Actions actions = Actions.getInstance();

	private SimpleObjectProperty<Path> currentFile = new SimpleObjectProperty<>();
	private SimpleBooleanProperty searchActive = new SimpleBooleanProperty();

	private final TabContainer tabsContainer = TabContainer.getInstance();
	private final Editor editor = Editor.getInstance(currentTab.getReadOnlyProperty(), bookmarks.getSelectionModel().selectedItemProperty());
	private static Stage stage;
	public static final ColorAdjust GRAYSCALE_EFFECT = new ColorAdjust();

	static {
		GRAYSCALE_EFFECT.setSaturation(-1);
	}
	public static Stage getStage() {
		return stage;
	}
	@Override
	public void start(Stage stage) throws Exception {
		Viewer.stage = stage;
		FxAlert.setParent(stage);
		FxPopupShop.setParent(stage);

		readRecents();

		SplitPane splitPane = getSplitPane();
		prepareTabsContainer();

		BorderPane root = new BorderPane(splitPane);
		root.setTop(getMenubar());

		Scene scene = new Scene(root);
		stage.setScene(scene);

		showStage(stage);
		loadFirstFile();

		scene.getStylesheets().add("style.css");
		// TODO FxUtils.liveReloadCss(getHostServices(), "resources/style.css", scene);
	}

	private void readRecents() throws IOException, URISyntaxException {
		Stream.of(Session.get("recents", "").split(";"))
		.map(String::trim)
		.filter(s -> !s.isEmpty())
		.map(Paths::get)
		.filter(Files::exists)
		.map(this::recentsMenuItem)
		.forEach(recentsMenu.getItems()::add);
	}
	private void showStage(Stage stage2) {
		Rectangle2D screen = Screen.getPrimary().getBounds();

		Function<String, Double> parser = Double::parseDouble;

		stage.setWidth(Session.get("stage.width", screen.getWidth()/2, parser));
		stage.setHeight(Session.get("stage.height", screen.getHeight(), parser));
		stage.setX(Session.get("stage.x", 0d, parser));
		stage.setY(Session.get("stage.y", 0d, parser));

		try {
			stage.getIcons().add(new Image("notebook.png"));
		} catch (Exception e2) {}

		stage.show();
		stage.setOnCloseRequest(e -> {
			exit();
			e.consume();
		});
	}
	private void prepareTabsContainer() {
		tabsContainer.setOnTabSwitch(this::switchTab);
		tabsContainer.setOnTabClosing(editor::finish);
		tabsContainer.setOnTabClosed(tab -> {
			Path p = tab.getJbookPath();
			if(p == null)
				return;

			List<MenuItem> list = recentsMenu.getItems(); 

			list.add(0, recentsMenuItem(p));
			if(list.size() > 10)
				list.subList(10, list.size()).clear();
			actions.tabClosed(tab);
		});
	}
	private MenuItem recentsMenuItem(Path path) {
		MenuItem mi =  menuitem(path.toString(), e -> actions.open(tabsContainer, path, recentsMenu));
		mi.getStyleClass().add("recent-mi");
		mi.setUserData(path);
		return mi;
	}

	private SplitPane getSplitPane() throws IOException {
		SplitPane splitPane = new SplitPane(getBookmarkPane(), getEditor());
		splitPane.setDividerPosition(0, 0.2);
		splitPane.widthProperty().addListener((p, o, n) -> splitPane.setDividerPosition(0, 0.2));
		return splitPane;
	}
	private void loadFirstFile() {
		Optional.ofNullable(getParameters().getRaw())
		.filter(l -> !l.isEmpty())
		.ifPresent(list -> Platform.runLater(() -> list.forEach(s -> tabsContainer.addTab(Paths.get(s)))));
	}
	private Tab getCurrentTab() {
		return currentTab.get();
	}
	private Node getEditor() {
		return new BorderPane(editor, tabsContainer, null, null, null);
	}
	private void exit() {
		if(tabsContainer.closeAll()) {
			Session.put("stage.width", String.valueOf(stage.getWidth()));
			Session.put("stage.height", String.valueOf(stage.getHeight()));
			Session.put("stage.x", String.valueOf(stage.getX()));
			Session.put("stage.y", String.valueOf(stage.getY()));
			Session.put("recents", recentsMenu.getItems().stream()
					.map(MenuItem::getUserData)
					.map(Object::toString)
					.map(s -> s.replace('\\', '/'))
					.collect(Collectors.joining(";")));
			System.exit(0);
		}
	}
	private MenuBar getMenubar() {
		return new MenuBar(getFileMenu(), getBookmarkMenu(), getSearchMenu(), getEditorMenu());
	}
	private Menu getEditorMenu() {
		return new Menu("editor", null,
				radioMenuitem("Text wrap", e -> editor.setWordWrap(((RadioMenuItem)e.getSource()).isSelected())),
				menuitem("Font", e -> editor.setFont())
				);
	}
	private Menu getSearchMenu() {
		Menu menu = new Menu("_Search", null, 
				menuitem("Search", combination(KeyCode.F, SHORTCUT_DOWN), e -> {
					tabsContainer.setDisable(true);
					tabsContainer.setEffect(GRAYSCALE_EFFECT);
					searchActive.set(true);

					new SearchBox(stage, bookmarks, getCurrentTab())
					.setOnHidden(e_e -> {
						tabsContainer.setDisable(false);
						tabsContainer.setEffect(null);
						searchActive.set(false);
					});
				}, currentTabNull)
				);
		return menu;
	}

	private final Menu recentsMenu = new Menu("_Recents");
	private Menu getFileMenu() {
		recentsMenu.disableProperty().bind(searchActive.or(Bindings.isEmpty(recentsMenu.getItems())));

		BooleanBinding fileNull = currentFile.isNull().or(searchActive);
		BooleanBinding tabNull = currentTabNull.or(searchActive);
		BooleanBinding selectedZero = tabsContainer.tabsCountProperty().isEqualTo(0).or(searchActive);

		Menu closeSpecific = 
				new Menu("Close specific", null,
						menuitem("other tab(s)", e -> tabsContainer.closeExcept(getCurrentTab())),
						menuitem("tab(s) to the right", e -> tabsContainer.closeRightLeft(getCurrentTab(), true)),
						menuitem("tab(s) to the left", e -> tabsContainer.closeRightLeft(getCurrentTab(), false))
						);

		Menu menu = new Menu("_File", null, 
				menuitem("_New", combination(KeyCode.N, SHORTCUT_DOWN, ALT_DOWN), e -> tabsContainer.addBlankTab(), searchActive),
				menuitem("_Open...", combination(KeyCode.O, SHORTCUT_DOWN), e -> actions.open(tabsContainer, null, recentsMenu), searchActive),
				recentsMenu,
				menuitem("Open Containing Folder", e -> actions.open_containing_folder(getHostServices(), getCurrentTab()), fileNull),
				menuitem("Reload From Disk", e -> actions.reload_from_disk(getCurrentTab()), fileNull),
				menuitem("_Save", combination(KeyCode.S, SHORTCUT_DOWN), e -> actions.save(getCurrentTab(), false), fileNull),
				menuitem("Save _As", combination(KeyCode.S, SHORTCUT_DOWN, SHIFT_DOWN), e -> {actions.save_as(getCurrentTab(), false);updateCurrentFile();}, tabNull),
				menuitem("Sav_e All", combination(KeyCode.S, SHORTCUT_DOWN, ALT_DOWN), e -> {tabsContainer.saveAllTabs();updateCurrentFile();}, tabNull),
				menuitem("Rename", e -> {actions.rename(getCurrentTab()); updateCurrentFile();}, fileNull),
				new SeparatorMenuItem(),
				menuitem("_Close",combination(KeyCode.W, SHORTCUT_DOWN), e -> tabsContainer.closeTab(getCurrentTab()), selectedZero),
				menuitem("Close All",combination(KeyCode.W, SHORTCUT_DOWN, SHIFT_DOWN), e -> tabsContainer.closeAll(), selectedZero),
				closeSpecific,
				new SeparatorMenuItem(),
				menuitem("E_xit", combination(KeyCode.F4, ALT_DOWN), e -> exit())
				);
		closeSpecific.disableProperty().bind(tabsContainer.tabsCountProperty().lessThan(2).or(searchActive));
		return menu;
	}
	private void updateCurrentFile() {
		Tab t = getCurrentTab(); 
		if(t == null)
			return;

		currentFile.set(t.getJbookPath());
		stage.setTitle(t.getTabTitle());
	}
	private void switchTab(final Tab newTab) {
		currentTab.set(newTab);
		boolean b = newTab == null;
		stage.setTitle(b ? null : newTab.getTabTitle());
		currentFile.set(b ? null : newTab.getJbookPath());
		bookmarks.setRoot(b ? null : newTab.getRootItem());
		actions.switchTab(newTab);
	}
	private Menu getBookmarkMenu() {
		return new Menu("_Bookmark",
				null,
				menuitem("Add Bookmark", combination(KeyCode.N, SHORTCUT_DOWN), e -> actions.addNewBookmark(bookmarks, getCurrentTab(), false), currentTabNull),
				menuitem("Add Child Bookmark", combination(KeyCode.N, SHORTCUT_DOWN, SHIFT_DOWN), e -> actions.addNewBookmark(bookmarks, getCurrentTab(), true), selectedItemNull),
				menuitem("Remove bookmark", e -> actions.removeBookmarkAction(bookmarks, getCurrentTab()), selectedItemNull),
				menuitem("Undo Remove bookmark", e -> actions.undoRemoveBookmark(getCurrentTab()), actions.undoDeleteSizeProperty().isEqualTo(0))
			//TODO	menuitem("Move bookmark", this::moveBookMark, selectedItemNull)
				);
	}
	private Node getBookmarkPane() throws IOException {
		bookmarks.setShowRoot(false);
		bookmarks.setEditable(true);
		bookmarks.setId("bookmarks");
		bookmarks.setCellFactory(TextFieldTreeCell.forTreeView());
		bookmarks.setOnEditCommit(e -> {
			getCurrentTab().setModified(true);
			Platform.runLater(() -> editor.updateTitle());
		});
		bookmarks.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
			if(e.getClickCount() == 2)
				e.consume();
		});

		BorderPane pane = new BorderPane(bookmarks);

		Button show = button("show","Chevron Right_20px.png", null);
		VBox showBox = new VBox(show);
		showBox.setMaxWidth(20);
		showBox.setPadding(new Insets(5, 0, 0, 0));

		EventHandler<ActionEvent> showHideAction = e -> {
			boolean isShow = e.getSource() == show;
			Node node = isShow ? showBox : pane;
			SplitPane container = (SplitPane)(node.getParent().getParent());
			List<Node> list = container.getItems();
			list.set(list.indexOf(node), isShow ? pane : showBox);
			container.setDividerPositions(0.3);
		};
		show.setOnAction(showHideAction);

		RadioButton expandCollpase = new RadioButton();
		expandCollpase.tooltipProperty().bind(new When(expandCollpase.selectedProperty()).then(new Tooltip("collapse")).otherwise(new Tooltip("expand")));
		expandCollpase.setPrefHeight(24);
		expandCollpase.setPrefWidth(24);
		expandCollpase.setOnAction(e -> getCurrentTab().setExpanded(expandCollpase.isSelected()));

		Pane p;
		Button removeButton, addButton, addChildButton;
		HBox controls = new HBox(3, 
				addButton = button("add", "plus.png", e -> actions.addNewBookmark(bookmarks, getCurrentTab(), false)),
				addChildButton = button("add bookmark child", "bookmarkchild.png", e -> actions.addNewBookmark(bookmarks, getCurrentTab(), true)),
				removeButton = button("remove selected","error.png", e -> actions.removeBookmarkAction(bookmarks, getCurrentTab())),
				expandCollpase,
				p = new Pane(),
				button("hide","Chevron Left_20px.png", showHideAction)
				);

		HBox.setHgrow(p, Priority.ALWAYS);

		removeButton.effectProperty().bind(new When(removeButton.disableProperty()).then(GRAYSCALE_EFFECT).otherwise((ColorAdjust)null));
		removeButton.disableProperty().bind(selectedItemNull);

		addChildButton.effectProperty().bind(removeButton.effectProperty());
		addChildButton.disableProperty().bind(removeButton.disableProperty());

		addButton.effectProperty().bind(new When(addButton.disableProperty()).then(GRAYSCALE_EFFECT).otherwise((ColorAdjust)null));

		addButton.disableProperty().bind(currentTabNull);
		expandCollpase.disableProperty().bind(currentTabNull);

		controls.setPadding(new Insets(5));
		pane.setTop(controls);
		pane.disableProperty().bind(currentTabNull);
		return pane;
	}
}
