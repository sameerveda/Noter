package sam.noter;

import java.io.IOException;
import java.nio.file.Path;

import javafx.application.Platform;
import sam.fx.alert.FxAlert;
import sam.fx.clipboard.FxClipboard;
import sam.fx.popup.FxPopupShop;
import sam.io.serilizers.StringWriter2;
import sam.noter.dao.api.IEntry;

public interface Utils {

	public static void copyToClipboard(String s) {
		FxClipboard.setString(s);
		FxPopupShop.showHidePopup(s, 2000);
	}
	public static void fx(Runnable runnable) {
		Platform.runLater(runnable);
	}
	
	public static void toTreeString(IEntry n, StringBuilder  sb) {
		toTreeString(n.getParent(), sb);
		
		if(n != null)
			sb.append(n.getTitle()).append(" > ");
	}
	public static String toTreeString(EntryTreeItem e, boolean b) {
		StringBuilder sb = new StringBuilder(); //TODO
		toTreeString(e.getEntry(), sb);
		return sb.toString();
	}
	public static void writeTextHandled(String text, Path path) {
		try {
			new StringWriter2().write(text, path);	
		} catch (IOException e2) {
			FxAlert.showErrorDialog(path, "failed to save", e2);
		}
	}
}
