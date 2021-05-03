package banking;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2 || !"-fileName".equals(args[0])) {
            System.out.println("Error! Please rerun the program and specify a database e.g \"-fileName db.s3db\"");
            System.exit(1);
        }

        String url = String.format("jdbc:sqlite:%S", args[1]);
        Bank bank = new Bank(url);
        int initialiseAttempts = 0;
        while (!bank.isDbStatusOK() && initialiseAttempts < 5) {
            bank.initialiseDB();
            initialiseAttempts++;
        }

        if (bank.isDbStatusOK()) {
            UserInterface UI = new UserInterface(bank);
            UI.run();
        } else {
            System.out.println("Database failed to initialise. Please rerun the program and try again.");
        }
    }
}