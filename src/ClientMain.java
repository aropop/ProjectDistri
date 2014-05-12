import java.util.ArrayList;
import java.util.Scanner;

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
					// check to print err message
					if (scl.hasNext()) {
						// read files
						while (scl.hasNext()) {
							cr.addFile(scl.next());
						}
					} else {
						System.out
								.println("No files given, usage: add <file> (<otherfiles>)");
					}

				} else if (command.equals("checkout")) {

					cr.checkout(scl.next(), scl.nextInt());

				} else if (command.equals("status")) {

					System.out.println(cr.status());

				} else if (command.equals("add-remote")) {

					String ip;
					int port;
					String err = "Wrong use of add-remote, correct use: add-remote <ip> <port>";

					if (scl.hasNext()) {
						ip = scl.next();
						if (scl.hasNext()) {
							port = scl.nextInt();
							cr.addRemote(ip, port);
							System.out.println("Remote was added (ip: " + ip
									+ ", port: " + port + ")");
						} else
							System.err.println(err);
					} else
						System.err.println(err);

				} else if (command.equals("commit")) {

					// check for message option
					String message = "";
					ArrayList<String> files = new ArrayList<String>();
					String possibleCommand = "";
					if (scl.hasNext()) {
						possibleCommand = scl.next();
						if (possibleCommand.equals("-m")) {
							scl.useDelimiter("\"");
							scl.next(); // reads first half away
							message = scl.next();
							scl.useDelimiter(" ");
							scl.next(); // reads spaces away
						}
					}

					// possible command could've been a file so let's check
					if (!possibleCommand.equals("-m")) {
						files.add(possibleCommand);
					}

					// read files
					while (scl.hasNext()) {
						files.add(scl.next());
					}

					cr.addCommit(message, files);

				} else if (command.equals("test")) {
					// TODO: remove debug code

				}else if (command.equals("list-commits")) {
					// Return a list of all the commits with it's files and date
					
					System.out.println(cr.listCommits());

				} else {
					// else print help text
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
