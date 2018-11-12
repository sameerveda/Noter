import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JOptionPane;

import javafx.application.Application;
import sam.myutils.MyUtilsPath;
import sam.noter.App;
import sam.noter.dao.Entry;
import sam.noter.dao.RootEntry;
import sam.noter.dao.dom.RootDOMEntryFactory;
import sam.noter.dao.zip.RootEntryZFactory;

public class Main {

	public static void main( String[] args ) throws Exception {
		try {
			FileChannel c = FileChannel.open(Paths.get("noter.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			c.tryLock();
			c.write(ByteBuffer.wrap(new byte[]{1}));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Only one instanceof program is allowed", "No Two instance allowed", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		
		Application.launch(App.class, args);
	}
	
	@SuppressWarnings("unused")
	private static void convertFolder() throws IOException {
		Path root = Paths.get("C:\\Users\\Sameer\\Documents\\MEGAsync\\Mega");
		Path jbook = root.resolve("jbooks");
		Path zips = root.resolve("jbooks.zip");
		
		Files.walk(jbook)
		.filter(Files::isRegularFile)
		.forEach(s -> {
			Path t = zips.resolve(MyUtilsPath.subpath(s, jbook));
			try {
				Files.createDirectories(t.getParent());
				convert(s, t);
				check(s, t);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		System.out.println("DONE");
	} 

	@SuppressWarnings("unused")
	private static void check(Path xml, Path zip) throws Exception {
		RootDOMEntryFactory domF = new RootDOMEntryFactory();
		RootEntryZFactory zF = RootEntryZFactory.getInstance();
		
		RootEntry  dom = domF.load(xml);
		RootEntry z = zF.load(zip);
		
		ArrayList<Entry> domList = new ArrayList<>(); 
		((Entry)dom).walk(w -> domList.add(w));
		
		ArrayList<Entry> zList = new ArrayList<>(); 
		((Entry)z).walk(w -> zList.add(w));
		
		List<String> out = new ArrayList<>();
		
		int n = 0;
		for (Entry e : domList) {
			Entry f = zList.get(n++);
			if(!Objects.equals(e.getTitle(), f.getTitle()) || !Objects.equals(e.getContent(), f.getContent()))
				out.add(e+"  "+f+"  title: "+(Objects.equals(e.getTitle(), f.getTitle()) +",  content:"+ Objects.equals(e.getContent(), f.getContent())));//+"  "+e.getContent() +"\n"+f.getContent());
		}
		
		if(!out.isEmpty()) {
			System.out.println("\n");
			System.out.println(String.join("\n", out));
		}
		System.out.println("DONE");
	}
	@SuppressWarnings("unused")
	private static void convert(Path xml, Path zip) throws Exception {
		RootDOMEntryFactory factory = new RootDOMEntryFactory();
		RootEntryZFactory factory2 = RootEntryZFactory.getInstance();
		
		RootEntry root = factory.load(xml);
		RootEntry root2 = factory2.convert(root);
		
		root2.save(zip);
		System.out.println("DONE");
	}
}
