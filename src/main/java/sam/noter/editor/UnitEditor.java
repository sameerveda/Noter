package sam.noter.editor;

import static sam.fx.helpers.FxClassHelper.addClass;

import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import sam.fxml.Button2;
import sam.noter.Utils;
import sam.noter.datamaneger.Entry;

class UnitEditor extends BorderPane {
	protected final Label title = new Label();
	protected final TextArea content = new TextArea();
	protected volatile Entry item;

	public UnitEditor(Consumer<Entry> onExpanded) {
		updateFont();
		setCenter(content);

		if(onExpanded != null) {
			Button expandButton = new Button2("edit", null, e -> onExpanded.accept(this.item));
			addClass(expandButton, "expand-button");

			Pane p = new Pane();
			HBox titleContainer = new HBox(title, p, expandButton);
			titleContainer.setPadding(new Insets(5, 10, 5, 10));

			HBox.setHgrow(p, Priority.ALWAYS);

			addClass(titleContainer, "title-box");
			setTop(titleContainer);
		}
		else {
			setTop(title);
			addClass(title, "title");
			title.setMaxWidth(Double.MAX_VALUE);
		}

		addClass(this, "unit-editor");
		addClass(content, "content");

		content.textProperty().addListener((prop, old, _new) -> {
			if(item != null)
				item.setContent(_new);
		});
	}

	public void setItem(Entry item) {
		this.item = null;
		title.setText(item.getTitle());
		content.setText(item.getContent());
	
		String text = content.getText();
		long count = text == null || text.isEmpty() ? 0 : text.chars().filter(s -> s == '\n').count();
		content.setPrefRowCount(count  < 5 ? 5 : (count > 40 ? 40 : (int)count));
		this.item = item;
	}
	public void updateFont() {
		title.setFont(Editor.getFont());
		content.setFont(Editor.getFont());
	}
	public String getItemTitle() {
		return item.getTitle();
	}
	public Entry getItem() {
		return item;
	}
	public void updateTitle() {
		if(item == null)
			return;
		
		title.setTooltip(new Tooltip(Utils.treeToString(item)));
		title.setText(item.getTitle());
	}
	public void setWordWrap(boolean wrap) {
		content.setWrapText(wrap);
	}
	public String getContent() {
		return content.getText();
	}
}