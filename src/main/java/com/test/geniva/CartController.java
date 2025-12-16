package com.test.one;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    private static final String DB_URL = "jdbc:mysql://localhost/geniva?useSSL=false";
    private static final String USER = "root";
    private static final String PASS = "admin";

    // Add product to cart
    @PostMapping("/add-to-cart")
    public String addToCart(@RequestParam int productId, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("username");

        if (username == null) {
            return "redirect:/login";
        }

        int userId = getUserIdByUsername(username);
        if (userId == -1) return "redirect:/login";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String checkQuery = "SELECT quantity FROM user_cart WHERE user_id = ? AND product_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, productId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int newQty = rs.getInt("quantity") + 1;
                String updateQuery = "UPDATE user_cart SET quantity = ? WHERE user_id = ? AND product_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                updateStmt.setInt(1, newQty);
                updateStmt.setInt(2, userId);
                updateStmt.setInt(3, productId);
                updateStmt.executeUpdate();
            } else {
                String insertQuery = "INSERT INTO user_cart (user_id, product_id, quantity) VALUES (?, ?, 1)";
                PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, productId);
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "redirect:/cart";
    }

    // Show cart items
    @GetMapping("/cart")
    public String viewCart(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("username");

        if (username == null) return "redirect:/login";

        List<Cart> cartList = loadCartFromDatabase(username);
        double total = 0;
        for (Cart item : cartList) {
            total += item.getCost() * item.getQuantity();
        }

        model.addAttribute("cartItems", cartList);
        model.addAttribute("total", total);
        return "cart"; // cart.html
    }

    // Update quantity (+/-)
    @PostMapping("/update-quantity")
    public String updateQuantity(@RequestParam int productId,
                                 @RequestParam String action,
                                 HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        int userId = getUserIdByUsername(username);
        if (userId == -1) return "redirect:/login";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String getQtyQuery = "SELECT quantity FROM user_cart WHERE user_id = ? AND product_id = ?";
            PreparedStatement getStmt = conn.prepareStatement(getQtyQuery);
            getStmt.setInt(1, userId);
            getStmt.setInt(2, productId);
            ResultSet rs = getStmt.executeQuery();

            if (rs.next()) {
                int quantity = rs.getInt("quantity");
                if ("increase".equals(action)) quantity++;
                else if ("decrease".equals(action) && quantity > 1) quantity--;

                String updateQuery = "UPDATE user_cart SET quantity = ? WHERE user_id = ? AND product_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                updateStmt.setInt(1, quantity);
                updateStmt.setInt(2, userId);
                updateStmt.setInt(3, productId);
                updateStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "redirect:/cart";
    }

    // Remove item from cart
    @PostMapping("/remove-item")
    public String removeItem(@RequestParam int productId, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        int userId = getUserIdByUsername(username);
        if (userId == -1) return "redirect:/login";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String deleteQuery = "DELETE FROM user_cart WHERE user_id = ? AND product_id = ?";
            PreparedStatement stmt = conn.prepareStatement(deleteQuery);
            stmt.setInt(1, userId);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "redirect:/cart";
    }

    // Load cart from DB
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
