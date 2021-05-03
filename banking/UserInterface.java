package banking;

import java.util.Scanner;

public class UserInterface {
    private final Scanner scanner;
    private boolean running;
    private final Bank bank;

    public UserInterface(Bank bank) {
        this.scanner = new Scanner(System.in);
        this.bank = bank;
        this.running = true;
    }

    public void run() {
        while (this.running) {
            launchMenu();
        }
    }

    private void launchMenu() {
        System.out.print("1. Create an account\n2. Log into account\n0. Exit\n");
        String input = scanner.nextLine();
        switch (input) {
            case "1":
                createAccount();
                break;
            case "2":
                logIn();
                break;
            case "0":
                System.out.println("Bye!");
                this.running = false;
                break;
            default:
                System.out.println("Unknown command, please try again");
                break;
        }
    }

    private void createAccount() {
        String[] accDetails = this.bank.createNewAccount();

        System.out.printf("\nYour card has been created\n" +
                "Your card number:\n%S\n" +
                "Your card PIN:\n%S\n\n", accDetails[0], accDetails[1]);
    }

    private void logIn() {
        System.out.println("\nEnter your card number:");
        String cardNum = scanner.nextLine();
        System.out.println("Enter your PIN:");
        String PIN = scanner.nextLine();

        if (cardNumIncorrectFormat(cardNum) || !this.bank.validateLoginCredentials(cardNum, PIN)) {
            System.out.println("Wrong card number or PIN!\n");
            return;
        }

        accountMenu(cardNum);
    }

    private boolean cardNumIncorrectFormat(String cardNum) {
        if (cardNum.length() != 16) {
            return true;
        }
        return this.bank.generateCheckSum(cardNum.substring(0, 15)) != cardNum.charAt(15) - '0';
    }

    private void accountMenu(String cardNum) {
        boolean subRunning = true;
        System.out.println("\nYou have successfully logged in!\n");
        while (subRunning) {
            System.out.print("1. Balance\n" +
                    "2. Add income\n" +
                    "3. Do transfer\n" +
                    "4. Close account\n" +
                    "5. Log out\n" +
                    "0. Exit\n");
            String input = scanner.nextLine();

            switch (input) {
                case "1":
                    int balance = this.bank.getBalance(cardNum);
                    if (balance == -999999999) {
                        System.out.println("An error has occurred, please log out and try again.");
                    } else {
                        System.out.printf("\nBalance: %d\n", balance);
                    }
                    break;
                case "2":
                    depositFunds(cardNum);
                    break;
                case "3":
                    transferFunds(cardNum);
                    break;
                case "4":
                    closeAccount(cardNum);
                    subRunning = false;
                    break;
                case "5":
                    System.out.println("\nYou have successfully logged out!\n");
                    subRunning = false;
                    break;
                case "0":
                    subRunning = false;
                    this.running = false;
                    System.out.println("Bye!");
                    break;
                default:
                    System.out.println("Unknown command, please try again");
                    break;
            }
        }
    }

    private void closeAccount(String cardNum) {
        int result = this.bank.deleteAccount(cardNum);

        if (result == 1) {
            System.out.println("\nThe account has been closed!\n");
        } else {
            System.out.println("\nAn error occurred, please try again\n");
        }
    }

    private void transferFunds(String cardNum) {
        int balance = this.bank.getBalance(cardNum);
        System.out.println("\nTransfer\n" +
                "Enter card number:");

        String recipient = scanner.nextLine();

        if (cardNumIncorrectFormat(recipient)) {
            System.out.println("Probably you made a mistake in the card number. Please try again!\n");
            return;
        }

        if (this.bank.checkCardNumberAvailable(recipient)) {
            System.out.println("Such a card does not exist.\n");
            return;
        }

        System.out.println("Enter how much money you want to transfer:");
        int amount = getAmountFromInput();

        if (amount > balance) {
            System.out.println("Not enough money!\n");
            return;
        }

        int result = this.bank.transfer(cardNum, recipient, amount);

        if (result != 2) {
            System.out.println("An error occurred, please try again.\n");
        } else {
            System.out.println("Success!\n");
        }
    }

    private void depositFunds(String cardNum) {
        System.out.println("\nEnter income:");
        int amount = getAmountFromInput();
        int result = this.bank.deposit(cardNum, amount);

        if (result > 0) {
            System.out.println("Income was added!\n");
        } else {
            System.out.println("An error occurred, please try again.\n");
        }
    }

    private int getAmountFromInput() {
        String input = scanner.nextLine();
        while (!input.matches("\\d+")) {
            System.out.println("Please enter a number:");
            input = scanner.nextLine();
        }

        return Integer.parseInt(input);
    }
}
