import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class BlackFriday {

    private static final Scanner SC = new Scanner(System.in);

    /* ========================= REGISTER ========================= */
    public static void register(Connection con) {

        try {
            System.out.print("Enter ID: ");
            int id = Integer.parseInt(SC.nextLine());

            System.out.print("Enter username: ");
            String username = SC.nextLine();

            System.out.print("Enter password: ");
            String password = SC.nextLine();

            System.out.print("Enter email: ");
            String email = SC.nextLine();

            String checkSql = "SELECT 1 FROM users WHERE userEmail = ?";
            try (PreparedStatement check = con.prepareStatement(checkSql)) {
                check.setString(1, email);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {
                    System.out.println("User already exists.");
                    return;
                }
            }

            String insertSql = "INSERT INTO users VALUES (?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setInt(1, id);
                ps.setString(2, username);
                ps.setString(3, password);
                ps.setString(4, email);
                ps.executeUpdate();
            }

            System.out.println("User created successfully.");

        } catch (Exception e) {
            System.out.println("Register error: " + e.getMessage());
        }
    }

    /* ========================= LOGIN ========================= */
    public static String[] login(Connection con) {

        try {
            System.out.print("Username: ");
            String username = SC.nextLine();

            System.out.print("Password: ");
            String password = SC.nextLine();

            System.out.print("Email: ");
            String email = SC.nextLine();

            String sql = """
                    SELECT id FROM users
                    WHERE userName = ? AND userPass = ? AND userEmail = ?
                    """;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, email);

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    System.out.println("Login successful.");
                    return new String[]{username, password, email, rs.getString("id")};
                }
            }

            System.out.println("Invalid credentials.");

        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
        }

        return null;
    }

    /* ========================= MENU ========================= */
    public static void menu(Connection con, String[] user) throws Exception {

        int option = 0;
        while (option != 6) {

            System.out.println("""
                        1. Search product
                        2. Add product
                        3. Show cart
                        4. Remove product
                        5. Generate file
                        6. Exit
                    """);

            option = Integer.parseInt(SC.nextLine());

            switch (option) {
                case 1 -> searchProduct(con, user[0]);
                case 2 -> addProduct(con, user[0]);
                case 3 -> showCart(con, user[0]);
                case 4 -> removeProduct(con, user[0]);
                case 5 -> generateFile(user[0]);
                case 6 -> System.out.println("Exiting...");
            }
        }
    }

    /* ========================= OPERATIONS ========================= */
    private static void searchProduct(Connection con, String user) throws Exception {

        System.out.print("Enter search text: ");
        String product = "%" + SC.nextLine() + "%";

        String sql = """
                    SELECT p.id, p.description, p.price
                    FROM products p
                    JOIN carts c ON c.id_product = p.id
                    WHERE c.user = ? AND p.description LIKE ?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, product);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println(
                        rs.getInt(1) + " - " +
                                rs.getString(2) + " - " +
                                rs.getDouble(3)
                );
            }
        }
    }

    private static void addProduct(Connection con, String user) throws Exception {

        System.out.print("Cart ID: ");
        int id = Integer.parseInt(SC.nextLine());

        System.out.print("Product ID: ");
        int productId = Integer.parseInt(SC.nextLine());

        String sql = "INSERT INTO carts VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, user);
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
    }

    private static void showCart(Connection con, String user) throws Exception {

        String sql = """
                    SELECT c.id, p.description, p.price
                    FROM carts c
                    JOIN products p ON c.id_product = p.id
                    WHERE c.user = ?
                """;

        int total = 0;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();

            System.out.println("ORDER SUMMARY (user: " + user + ")");
            System.out.println("Date: " +
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));

            while (rs.next()) {
                System.out.println(rs.getInt(1) + " - " + rs.getString(2));
                total += rs.getInt(3);
            }
        }

        System.out.println("TOTAL: " + total);
        System.out.println("TOTAL incl. VAT (21%): " + (total * 1.21));
    }

    private static void removeProduct(Connection con, String user) throws Exception {

        System.out.print("Enter ID to remove: ");
        int id = Integer.parseInt(SC.nextLine());

        String sql = "DELETE FROM carts WHERE id = ? AND user = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, user);
            ps.executeUpdate();
        }
    }

    private static void generateFile(String user) throws Exception {

        String fileName = "order_" + user +
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";

        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write("Order generated for user: " + user);
        }

        System.out.println("File generated: " + fileName);
    }

    /* ========================= MAIN ========================= */
    public static void main(String[] args) {

        try (Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/blackDB", "root", "")) {

            System.out.println("Connection established.");

            System.out.print("1 Register | 2 Login: ");
            int option = Integer.parseInt(SC.nextLine());

            if (option == 1) {
                register(con);
            } else if (option == 2) {
                String[] user = login(con);
                if (user != null) {
                    menu(con, user);
                }
            }

        } catch (Exception e) {
            System.out.println("General error: " + e.getMessage());
        }
    }
}
