import java.io.File;
import java.util.Date;
import java.util.Scanner;
import java.util.UUID;

import javax.swing.text.Position;

public class ClientMain {
	private static final String helpText = "Wrong command! \nUse: ClientMain [command] {options} (args)\n"
			+ "* add (-m) message files";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ClientRepository cr = null;
		// check if path is given as argument
		try {
			if (args.length != 0) {
				cr = new ClientRepository(args[0]);
			} else {
				cr = new ClientRepository(System.getProperty("user.dir") + "/");
			}
		} catch (Exception e) {
			System.err.println("Could not initialize a repository: "
					+ e.getMessage());
			return;
		}

		String input;
		System.out.print("> ");
		Scanner sc = new Scanner(System.in);
		input = sc.nextLine();
		while (!input.equals("exit")) {
			// command loop
			Scanner scl = new Scanner(input);
			if (scl.hasNext()) {
				// has more otherwise it could block
				String command = scl.next();
				if (command.equals("add")) {
					
					//read files
					while(scl.hasNext()){
						cr.addFile(new File(System.getProperty("user.dir") + "/" +scl.next()));
					}
					
					
				} else if (command.equals("checkout")) {
					
					cr.checkout(scl.next(), scl.nextInt());
					
				} else if (command.equals("commit")) {
					
					//check for message option
					String message = "";
					String possibleCommand = "";
					if (scl.hasNext()){
						possibleCommand = scl.next();
						if (possibleCommand.equals("-m")){
							scl.useDelimiter("\"");
							scl.next(); //reads first half away
							message = scl.next();
							scl.useDelimiter(" ");
							scl.next(); //reads spaces away
						}
					}
					
					//make commit object
					Commit com = new Commit(message, new Date(),
							UUID.randomUUID());
				
					//possible command could've been a file so let's check
					if(!possibleCommand.equals("-m")){
						com.addFile(new File(System.getProperty("user.dir") + "/" + possibleCommand));
					}
					
					
					//read files
					while(scl.hasNext()){
						com.addFile(new File(System.getProperty("user.dir") + "/" +scl.next()));
					}
					
					
					//send commit to repo
					cr.addCommit(com);
					
				} else if (command.equals("test")) {
					String test = "dit is \" een test \" nl";
					Scanner slc = new Scanner(test);
					slc.useDelimiter("");
					slc.next();
					System.out.println(slc.next());
				
				} else {
					//else print help text
					printHelp();
				}
			} else {
				printHelp();
			}

			// next command
			System.out.print("> ");
			input = sc.nextLine();
		}

	}

	private static void printHelp() {
		System.out.println(helpText);
	}

}
