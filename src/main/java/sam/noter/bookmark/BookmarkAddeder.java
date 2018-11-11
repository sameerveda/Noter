package sam.noter.bookmark;

import static sam.noter.bookmark.BookmarkType.RELATIVE;
import static sam.noter.bookmark.BookmarkType.RELATIVE_TO_PARENT;

import java.util.Collection;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.fx.helpers.FxCell;
import sam.fx.helpers.FxFxml;
import sam.fx.popup.FxPopupShop;
import sam.myutils.MyUtilsCheck;
import sam.myutils.MyUtilsException;
import sam.noter.InitFinalized;
import sam.noter.dao.Entry;
import sam.noter.tabs.Tab;

class BookmarkAddeder extends Stage implements InitFinalized, ChangeListener<String> {
	
	@FXML private Label header;
	@FXML private VBox center;
	@FXML private TextField titleTf;
	@FXML private ListView<Entry> similar;
	@FXML private TextArea entryPath;
	
	private final TitleSearch search = new TitleSearch();
	
	private Tab tab;
	private final WeakChangeListener<Entry> similarSelect = new WeakChangeListener<>((p, o, n) -> entryPath.setText(n == null ? null : n.toTreeString()));
	private BookmarkType bookMarkType;
	private Entry item;

	public BookmarkAddeder() {
		super(StageStyle.UTILITY);
		MyUtilsException.hideError(() -> FxFxml.load(this, true));

		similar.setCellFactory(FxCell.listCell(Entry::getTitle));
		similar.getSelectionModel()
		.selectedItemProperty()
		.addListener(similarSelect);
		
		init();  
	}
	
	private Entry result;
	
	@FXML
	private void cancelAction(ActionEvent e) {
		super.hide();
	}
	@FXML
	private void okAction(ActionEvent e) {
		result = null;
		
		String s = titleTf.getText();
		if(MyUtilsCheck.isEmptyTrimmed(s)){
			FxPopupShop.showHidePopup("Invalid title", 1500);
			return;
		}

		String title = titleTf.getText();
		if(item == null)
			tab.addChild( title);
		else {
			switch (bookMarkType) {
				case RELATIVE:
					result =  tab.addChild(title, item.parent(), item);
					break;
				case CHILD: 
					result =  tab.addChild(title, item);
					break;
				case RELATIVE_TO_PARENT:
					result =  tab.addChild(title,item.parent().parent(), item.parent());
					break;
			}
		}
		super.hide();
	}
	
	@Override
	public void hide() {
		super.hide();
		similar.getItems().clear();
		titleTf.textProperty().removeListener(this);
		titleTf.clear();
		search.stop();
	}
	
	public Entry showDialog(BookmarkType bookMarkType, MultipleSelectionModel<TreeItem<String>> selectionModel, TreeView<String> tree, Tab tab) {
		this.item = (Entry)selectionModel.getSelectedItem();
		this.tab = tab;

		this.bookMarkType = bookMarkType == RELATIVE_TO_PARENT && item.getParent() == tree.getRoot() ? RELATIVE : bookMarkType;
		header.setText(header());

		Platform.runLater(() -> titleTf.requestFocus());
		titleTf.clear();
		titleTf.textProperty().addListener(this);
		
		Collection<Entry> list = tab.getAllEntries();
		similar.getItems().setAll(list);
		search.start(list);
		search.setOnChange(() -> Platform.runLater(()-> search.process(similar.getItems())));
		
		showAndWait();
		
		if(result != null){
			selectionModel.clearSelection();
			selectionModel.select(result);
		}
		return result;
	}
	private String header() {
		String header = "Add new Bookmark";
		if(item != null) {
			switch (bookMarkType) {
				case RELATIVE:
					header += "\nRelative To: "+item.getValue();
					break;
				case CHILD:
					header += "\nChild To: "+item.getValue();
					break;
				case RELATIVE_TO_PARENT:
					header += "\nRelative To: "+item.getParent().getValue();
					break;
			}	
		}
		return header;
	}
	@Override
	public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
		search.search(newValue == null ? newValue : newValue.toLowerCase());
	}

	@Override
	protected void finalize() throws Throwable {
		search.completeStop();
		finalized();
		super.finalize();
	}
}
