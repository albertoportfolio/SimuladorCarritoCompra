import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class BlackFriday {

    private static final Scanner SC = new Scanner(System.in);

    /* ========================= REGISTRO ========================= */
    public static void registro(Connection con) {

        try {
            System.out.print("Introduce ID: ");
            int id = Integer.parseInt(SC.nextLine());

            System.out.print("Introduce username: ");
            String username = SC.nextLine();

            System.out.print("Introduce password: ");
            String password = SC.nextLine();

            System.out.print("Introduce email: ");
            String email = SC.nextLine();

            String checkSql = "SELECT 1 FROM users WHERE userEmail = ?";
            try (PreparedStatement check = con.prepareStatement(checkSql)) {
                check.setString(1, email);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {
                    System.out.println("El usuario ya existe.");
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

            System.out.println("Usuario creado correctamente.");

        } catch (Exception e) {
            System.out.println("Error en registro: " + e.getMessage());
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
                    WHERE userName = ? AND userPass = ? AND userEmail = ?""";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, email);

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    System.out.println("Login correcto.");
                    return new String[]{username, password, email, rs.getString("id")};
                }
            }

            System.out.println("Credenciales incorrectas.");

        } catch (Exception e) {
            System.out.println("Error en login: " + e.getMessage());
        }

        return null;
    }

    /* ========================= MENÚ ========================= */
    public static void menu(Connection con, String[] user) throws Exception {

        int opc = 0;
        while (opc != 6) {

            System.out.println("""
                        1. Buscar producto
                        2. Añadir producto
                        3. Mostrar carrito
                        4. Eliminar producto
                        5. Generar fichero
                        6. Salir
                    """);

            opc = Integer.parseInt(SC.nextLine());

            switch (opc) {

                case 1 -> buscarProducto(con, user[0]);
                case 2 -> anadirProducto(con, user[0]);
                case 3 -> mostrarCarrito(con, user[0]);
                case 4 -> eliminarProducto(con, user[0]);
                case 5 -> generarFichero(user[0]);
                case 6 -> System.out.println("Saliendo...");
            }
        }
    }

    /* ========================= OPERACIONES ========================= */
    private static void buscarProducto(Connection con, String user) throws Exception {

        System.out.print("Introduce texto a buscar: ");
        String articulo = "%" + SC.nextLine() + "%";

        String sql = """
                    SELECT p.id, p.description, p.price
                    FROM products p
                    JOIN carts c ON c.id_product = p.id
                    WHERE c.user = ? AND p.description LIKE ?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, articulo);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getInt(1) + " - " +
                        rs.getString(2) + " - " +
                        rs.getDouble(3));
            }
        }
    }

    private static void anadirProducto(Connection con, String user) throws Exception {

        System.out.print("ID carrito: ");
        int id = Integer.parseInt(SC.nextLine());

        System.out.print("ID producto: ");
        int productId = Integer.parseInt(SC.nextLine());

        String sql = "INSERT INTO carts VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, user);
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
    }

    private static void mostrarCarrito(Connection con, String user) throws Exception {

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
            System.out.println("Date: " + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));

            while (rs.next()) {
                System.out.println(rs.getInt(1) + " - " + rs.getString(2));
                total += rs.getInt(3);
            }
        }

        System.out.println("TOTAL: " + total);
        System.out.println("TOTAL con IVA (21%): " + (total * 1.21));
    }

    private static void eliminarProducto(Connection con, String user) throws Exception {

        System.out.print("ID a borrar: ");
        int id = Integer.parseInt(SC.nextLine());

        String sql = "DELETE FROM carts WHERE id = ? AND user = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, user);
            ps.executeUpdate();
        }
    }

    private static void generarFichero(String user) throws Exception {

        String name = "order_" + user +
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";

        try (FileWriter fw = new FileWriter(name)) {
            fw.write("Pedido generado para: " + user);
        }

        System.out.println("Fichero generado: " + name);
    }

    /* ========================= MAIN ========================= */
    public static void main(String[] args) {

        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/blackDB", "root", "")) {

            System.out.println("Conexión creada");

            System.out.print("1 Registrar | 2 Login: ");
            int opc = Integer.parseInt(SC.nextLine());

            if (opc == 1) {
                registro(con);
            } else if (opc == 2) {
                String[] user = login(con);
                if (user != null) {
                    menu(con, user);
                }
            }

        } catch (Exception e) {
            System.out.println("Error general: " + e.getMessage());
        }
    }
}
