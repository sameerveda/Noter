import static sam.myutils.MyUtilsArgs.isVersion;

import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;

import javafx.application.Application;
import sam.apps.jbook_reader.App;
import sam.apps.jbook_reader.FilesLookup;

public class Main {

	public static void main( String[] args ) throws CmdLineException, IOException {
		System.out.println(FilesLookup.class);
		System.exit(0);
		
		if(args.length != 0 && isVersion(args[0]))
			System.out.println("1.023");
		else
			Application.launch(App.class, args);
	}

}
