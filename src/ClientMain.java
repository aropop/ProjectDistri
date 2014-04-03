import java.util.Date;
import java.util.Scanner;
import java.util.UUID;

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
			System.out.print("> ");
			Scanner scl = new Scanner(input);
			if (scl.hasNext()) {
				// has more otherwise it could block
				String command = scl.next();
				if (command.equals("add")) {
					String message = "";
					if (scl.hasNext())
						if (scl.next() == "-m")
							message = input.substring(input.indexOf("\""), input.lastIndexOf("\""));
					Commit com = new Commit(message, new Date(),
							UUID.randomUUID());
					cr.addCommit(com);
				} else if (command.equals("checkout")) {

				} else {
					printHelp();
				}
			} else {
				printHelp();
			}

			// next command
			input = sc.nextLine();
		}

	}

	private static void printHelp() {
		System.out.println(helpText);
	}

}
