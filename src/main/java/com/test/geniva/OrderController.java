package com.test.one;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    private static final String DB_URL = "jdbc:mysql://localhost/geniva?useSSL=false";
    private static final String USER = "root";
    private static final String PASS = "admin";

    // Show Order Page
    @GetMapping("/order")
    public String showOrderPage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        List<Cart> cartList = loadCartFromDatabase(username);

        if (cartList.isEmpty()) {
            model.addAttribute("message", "Your cart is empty!");
            return "order";
        }

        double total = 0;
        for (Cart item : cartList) {
            total += item.getCost() * item.getQuantity();
        }

        model.addAttribute("cartItems", cartList);
        model.addAttribute("total", total);

        return "order"; // order.html page
    }

    // Place Order
    @PostMapping("/placeOrder")
    public String placeOrder(@RequestParam String name,
                             @RequestParam String address,
                             @RequestParam String contact,
                             HttpSession session,
                             Model model) {

        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        List<Cart> cartList = loadCartFromDatabase(username);
        if (cartList.isEmpty()) {
            model.addAttribute("message", "Your cart is empty!");
            return "order";
        }

        double total = 0;
        for (Cart item : cartList) {
            total += item.getCost() * item.getQuantity();
        }

        try (Connection con = DriverManager.getConnection(DB_URL, USER, PASS)) {

            // Get user_id
            int userId = getUserIdByUsername(username);
            if (userId == -1) return "redirect:/login";

            // Insert into orders table
            String orderQuery = "INSERT INTO orders (user_id, name, address, contact, total_cost) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement orderStmt = con.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS);
            orderStmt.setInt(1, userId);
            orderStmt.setString(2, name);
            orderStmt.setString(3, address);
            orderStmt.setString(4, contact);
            orderStmt.setDouble(5, total);
            orderStmt.executeUpdate();

            // Get generated order_id
            ResultSet rs = orderStmt.getGeneratedKeys();
            int orderId = 0;
            if (rs.next()) orderId = rs.getInt(1);

            // Insert order items
            String itemQuery = "INSERT INTO order_items (order_id, product_id, product_name, quantity, price) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement itemStmt = con.prepareStatement(itemQuery);

            for (Cart item : cartList) {
                itemStmt.setInt(1, orderId);
                itemStmt.setInt(2, item.getProductId());
                itemStmt.setString(3, item.getName());
                itemStmt.setInt(4, item.getQuantity());
                itemStmt.setDouble(5, item.getCost());
                itemStmt.executeUpdate();
            }

            // Clear cart in DB
            String clearCart = "DELETE FROM user_cart WHERE user_id = ?";
            PreparedStatement clearStmt = con.prepareStatement(clearCart);
            clearStmt.setInt(1, userId);
            clearStmt.executeUpdate();

            model.addAttribute("message", "Your order has been placed successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
            model.addAttribute("message", "Error placing order. Please try again.");
        }

        return "order_success"; // order_success.html page
    }

    // Load cart from DB (same as CartController)
    private List<Cart> loadCartFromDatabase(String username) {
        List<Cart> cartList = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String query = """
                    SELECT p.id AS productId, p.name, p.image, p.cost AS cost, uc.quantity
                    FROM user_cart uc
                    JOIN login u ON uc.user_id = u.id
                    JOIN product p ON uc.product_id = p.id
                    WHERE u.username = ?
                    """;
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Cart item = new Cart();
                item.setProductId(rs.getInt("productId"));
                item.setName(rs.getString("name"));
                item.setImage(rs.getString("image"));
                item.setCost(rs.getDouble("cost"));
                item.setQuantity(rs.getInt("quantity"));
                cartList.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cartList;
    }

    // Get user_id by username
    private int getUserIdByUsername(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String query = "SELECT id FROM login WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
