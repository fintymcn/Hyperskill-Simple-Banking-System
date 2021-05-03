package banking;

import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.*;

public class Bank {
    private final SQLiteDataSource dataSource;
    private boolean dbStatusOK;

    public Bank(String url) {
        this.dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);
        dbStatusOK = false;
    }

    public void initialiseDB() {
        String sql = "CREATE TABLE IF NOT EXISTS card (\n"
                + "	id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "	number TEXT,\n"
                + "	pin TEXT,\n"
                + " balance INTEGER DEFAULT 0\n"
                + ");";
        try (Connection con = this.dataSource.getConnection();
             Statement s = con.createStatement()) {
            s.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("Database initialisation error...");
            return;
        }
        dbStatusOK = true;
    }

    public String[] createNewAccount() {
        Random rand = new Random();
        String PIN = String.format("%04d", rand.nextInt(10000));
        String cardNum;

        do {
            StringBuilder sb = new StringBuilder("400000");
            for (int i = 0; i < 9; i++) {
                sb.append(rand.nextInt(10));
            }
            sb.append(generateCheckSum(sb.toString()));
            cardNum = sb.toString();
        } while (!checkCardNumberAvailable(cardNum));

        addAccountToDb(cardNum, PIN);

        return new String[]{cardNum, PIN};
    }

    private void addAccountToDb(String cardNum, String PIN) {
        String sql = String.format("INSERT INTO CARD (number, pin) VALUES ('%S', '%S')", cardNum, PIN);
        try (Connection con = this.dataSource.getConnection();
             Statement s = con.createStatement()) {
            s.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("Error!");
            e.printStackTrace();
        }
    }

    public int generateCheckSum(String cardNum) {
        int sum = 0;
        for (int i = 0; i < cardNum.length(); i++) {
            int currentDigit = cardNum.charAt(i) - '0';
            if (i % 2 == 0) {
                currentDigit *= 2;
                currentDigit = (currentDigit > 9) ? currentDigit - 9 : currentDigit;
            }
            sum += currentDigit;
        }

        return (sum % 10 == 0) ? 0 : 10 - (sum % 10);
    }

    public boolean validateLoginCredentials(String cardNum, String PIN) {
        if (checkCardNumberAvailable(cardNum)) {
            return false;
        }
        String sql = String.format("SELECT * FROM CARD where number = %S", cardNum);
        try (Connection con = this.dataSource.getConnection();
             Statement s = con.createStatement();
             ResultSet matchedAccount = s.executeQuery(sql)) {
            return matchedAccount.getString("pin").equals(PIN);
        } catch (SQLException e) {
            System.out.println("Error!");
            e.printStackTrace();
        }
        return false;
    }


    public boolean checkCardNumberAvailable(String candidate) {
        String sql = String.format("SELECT * FROM card where number = '%S'", candidate);
        try (Connection con = this.dataSource.getConnection();
             Statement s = con.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return !rs.next();
        } catch (SQLException e) {
            System.out.println("Error!");
            e.printStackTrace();
        }
        return false;
    }

    public int getBalance(String cardNum) {
        String sql = String.format("SELECT * FROM card where number = %S", cardNum);
        try (Connection con = this.dataSource.getConnection();
             Statement s = con.createStatement();
             ResultSet matchedAccount = s.executeQuery(sql)) {
            return matchedAccount.getInt("balance");
        } catch (SQLException e) {
            System.out.println("Error!");
            e.printStackTrace();
        }
        return -999999999;
    }

    public int deleteAccount(String cardNum) {
        String deleteSQL = "DELETE FROM card WHERE number = ?";
        int changes = 0;
        try (Connection con = this.dataSource.getConnection();
             PreparedStatement delete = con.prepareStatement(deleteSQL)) {
            delete.setString(1, cardNum);
            changes = delete.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error!");
            e.printStackTrace();
        }
        return changes;
    }

    public int deposit(String cardNum, int amount) {
        String depositSQL = "UPDATE card SET balance = balance + ? WHERE number = ?";
        int changes = 0;
        try (Connection con = this.dataSource.getConnection();
             PreparedStatement deposit = con.prepareStatement(depositSQL)) {
            deposit.setInt(1, amount);
            deposit.setString(2, cardNum);
            changes = deposit.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error!");
            e.printStackTrace();
        }
        return changes;
    }

    public int transfer(String cardNum, String recipient, int amount) {
        String withdrawSQL = "UPDATE card SET balance = balance - ? WHERE number = ?";
        String depositSQL = "UPDATE card SET balance = balance + ? WHERE number = ?";
        int changes = 0;
        try (Connection con = this.dataSource.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement withdraw = con.prepareStatement(withdrawSQL);
                 PreparedStatement deposit = con.prepareStatement(depositSQL)) {

                withdraw.setInt(1, amount);
                withdraw.setString(2, cardNum);
                int first = withdraw.executeUpdate();

                deposit.setInt(1, amount);
                deposit.setString(2, recipient);
                int second = deposit.executeUpdate();

                con.commit();
                changes = first + second;
            } catch (SQLException e) {
                if (con != null) {
                    try {
                        System.err.println("Error! Transaction is being rolled back");
                        con.rollback();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return changes;
    }

    public boolean isDbStatusOK() {
        return dbStatusOK;
    }
}
