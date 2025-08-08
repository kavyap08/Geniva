package com.test.geniva;

import java.sql.*;
import java.util.*;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class Homecontroller {

    static final String DB_URL = "jdbc:mysql://localhost/geniva?useSSL=false";
    static final String USER = "root";
    static final String PASS = "admin";
    static final String QUERY = "SELECT name, image, description, href FROM collection";

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request) {
        List<Collection> collections = new ArrayList<>();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(QUERY);

            while (rs.next()) {
                Collection item = new Collection();
                item.setName(rs.getString("name"));
                item.setImage(rs.getString("image"));
                item.setDescription(rs.getString("description"));
                item.setHref(rs.getString("href"));
                collections.add(item);
            }
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("Collections", collections);
        HttpSession session = request.getSession(false);
        if (session != null) {
            model.addAttribute("username", session.getAttribute("username"));
        }

        return "home";
    }

    @GetMapping("/product")
    public String product(Model model, HttpServletRequest request) {
        String name_str = request.getParameter("type");
        String query = "SELECT id, image, name, cost FROM product WHERE type = '" + name_str + "'";
        List<Product> products = new ArrayList<>();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Product product = new Product();
                product.setId(rs.getString("id"));
                product.setImage(rs.getString("image"));
                product.setName(rs.getString("name"));
                product.setCost(rs.getString("cost"));
                products.add(product);
            }
            model.addAttribute("products", products);
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "product";
    }

    @GetMapping("/customize")
    public String customize(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            return "redirect:/login";
        }
        return "customize";
    }

    @PostMapping("/customize")
    public String customizePost(Model model, HttpServletRequest request) {
        String name = request.getParameter("name");
        String type = request.getParameter("type");
        String budget = request.getParameter("budget");
        String notes = request.getParameter("notes");
        String image = request.getParameter("image");

        String query = "INSERT INTO CUSTOMIZE (name, type, budget, notes, image) VALUES (?, ?, ?, ?, ?)";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, name);
            pstmt.setString(2, type);
            pstmt.setString(3, budget);
            pstmt.setString(4, notes);
            pstmt.setString(5, image);
            pstmt.executeUpdate();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "thank";
    }

    @GetMapping("/review")
    public String review() {
        return "review";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @PostMapping("/contact")
    public String formContact(Model model, HttpServletRequest request) {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String queryType = request.getParameter("query");
        String message = request.getParameter("message");

        String query = "INSERT INTO CONTACT (name, email, query, message) VALUES (?, ?, ?, ?)";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, queryType);
            pstmt.setString(4, message);
            pstmt.executeUpdate();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "thank";
    }

    @GetMapping("/login")
    public String form() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(HttpServletRequest request, Model model) {
        String inputUsername = request.getParameter("username");
        String inputPassword = request.getParameter("password");
        String query = "SELECT * FROM LOGIN WHERE username = ? AND password = ?";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, inputUsername);
            pstmt.setString(2, inputPassword);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                HttpSession session = request.getSession();
                session.setAttribute("username", inputUsername);
                conn.close();
                return "redirect:/";
            } else {
                model.addAttribute("error", "Invalid username or password");
                conn.close();
                return "login";
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Something went wrong!");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String register(Model model, HttpServletRequest request) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String dob = request.getParameter("dob");
        String contact = request.getParameter("contact");
        String gender = request.getParameter("gender");

        String query = "INSERT INTO login (username, password, name, dob, contact, email, gender) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, name);
            pstmt.setString(4, dob);
            pstmt.setString(5, contact);
            pstmt.setString(6, email);
            pstmt.setString(7, gender);
            pstmt.executeUpdate();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "login";
    }
    @GetMapping("/profile")
    public String showProfile(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            return "redirect:/login";
        }

        String username = (String) session.getAttribute("username");
        String query = "SELECT * FROM login WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Map<String, String> user = new HashMap<>();
                user.put("username", rs.getString("username"));
                user.put("name", rs.getString("name"));
                user.put("email", rs.getString("email"));
                user.put("contact", rs.getString("contact"));
                user.put("gender", rs.getString("gender"));
                user.put("dob", rs.getString("dob"));

                model.addAttribute("user", user);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "profile";
    }
   
  

}
