package sam.noter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import javafx.scene.control.TreeItem;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import sam.config.Session;
import sam.io.fileutils.FilesUtilsIO;
import sam.io.serilizers.StringReader2;
import sam.io.serilizers.StringWriter2;
import sam.logging.MyLoggerFactory;
import sam.myutils.System2;
import sam.noter.dao.Entry;

public class Utils {
	private static final Logger LOGGER = MyLoggerFactory.logger(Utils.class.getSimpleName());

	public static final Path APP_DATA = Optional.ofNullable(System2.lookup("app_data")).map(Paths::get).orElse(Paths.get("app_data"));
	private static final Path BACKUP_DIR = Utils.APP_DATA.resolve("book_backup/"+LocalDate.now().toString()); 
	static {
		BACKUP_DIR.toFile().mkdirs();
	}

	private Utils() {}

	public static String treeToString(TreeItem<String> item) {
		return treeToString(item, new StringBuilder()).toString();
	}
	private static final char[][] separator = Stream.of(" ( ", " > ", " )\n").map(String::toCharArray).toArray(char[][]::new);
	public static StringBuilder treeToString(TreeItem<String> item, StringBuilder sb) {
		if(item == null)
			return sb;

		TreeItem<String> t = item;
		sb.append(t.getValue());

		if(t.getParent().getParent() != null) {
			sb.append(separator[0]);
			List<String> list = new ArrayList<>();
			list.add(t.getValue());
			while((t = t.getParent()) != null) list.add(t.getValue());

			for (int i = list.size() - 2; i >= 0 ; i--)
				sb.append(list.get(i)).append(separator[1]);

			sb.setLength(sb.length() - 3);
			sb.append(separator[2]);
		}
		else
			sb.append('\n');

		return sb;
	}

	public enum FileChooserType {
		OPEN, SAVE
	}

	public static File chooseFile(String title, File expectedDir, String expectedFilename, FileChooserType type) {
		Objects.requireNonNull(type);

		FileChooser chooser = new FileChooser();
		chooser.setTitle(title);
		chooser.getExtensionFilters().add(new ExtensionFilter("jbook file", "*.jbook"));
		Window parent = Session.get(Stage.class);

		if(expectedDir == null || !expectedDir.isDirectory()){
			final Path p = Utils.APP_DATA.resolve("last-visited-folder.txt");
			try {
				expectedDir = Files.exists(p) ? new File(StringReader2.getText(p)) : null;
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "failed to read: "+p, e);
				expectedDir = null;
			}
		}

		if(expectedDir != null && expectedDir.isDirectory())
			chooser.setInitialDirectory(expectedDir);

		if(expectedFilename != null)
			chooser.setInitialFileName(expectedFilename);

		File file = type == FileChooserType.OPEN ? chooser.showOpenDialog(parent) : chooser.showSaveDialog(parent);

		if(file != null) {
			final Path p = Utils.APP_DATA.resolve("last-visited-folder.txt");
			try {
				StringWriter2.setText(p, file.getParent().toString().replace('\\', '/'));
			} catch (IOException e) {}
		}
		return file;
	}
	public static Entry castEntry(TreeItem<String> parent) {
		return (Entry)parent;
	}

	public static void createBackup(File target) {
		if(target == null || !target.exists())
			return;
		
		try(FileInputStream is = new FileInputStream(target);
				OutputStream os = Files.newOutputStream(BACKUP_DIR.resolve(target.getName()+"_SAVED_ON_"+LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)).replace(':', '_')+".gzip"));
				GZIPOutputStream gos = new GZIPOutputStream(os)) {
			FilesUtilsIO.pipe(is, gos);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "failed to backup: "+target, e);
		}
	}

	public static void stop() {
		backupClean();
	}

	private static void backupClean() {
		File backup = BACKUP_DIR.getParent().toFile();
		if(!backup.exists()) return;
		
		LocalDateTime now = LocalDateTime.now();
		
		for (String s : backup.list()) {
			LocalDate date = LocalDate.parse(s);
			if(Duration.between(date.atStartOfDay(), now).toDays() > 10){
				FilesUtilsIO.delete(new File(backup, s));
				LOGGER.info("DELETE backup(s): "+s);
			}
		}
	}
}
