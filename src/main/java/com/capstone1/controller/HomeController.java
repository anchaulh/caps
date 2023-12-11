package com.capstone1.controller;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.capstone1.model.*;
import com.capstone1.services.*;

import jakarta.annotation.Resource;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.*;

@Controller
public class HomeController {

    @Resource
    TokenUserService tokenUserService;
    @Resource
    TokenAdminService tokenAdminService;
    @Resource
    StaffService staffService;
    @Resource
    UserService userService;
    @Resource
    ProductService productService;
    @Resource
    CategoryService categoryService;
    @Resource
    ManufacturerService manufacturerService;
    @Resource
    OrderService orderService;
    @Resource
    OrderDetailService orderDetailService;
    @Resource
    CartService cartService;
    @Resource
    CartItemService cartItemService;
    @Resource
    AdminService adminService;

    @Autowired
    Encoding encoding;
    @Autowired
    JavaMailSender mailSender;

    @GetMapping({ "/home-page", "/" })
    public String getHome(Model model, HttpSession session) {

        List<Product> listProducts = productService.getAllProducts();
        List<Category> listCategories = categoryService.getAllCategories();
        List<Manufacturer> listManufacturers = manufacturerService.getAllManufacturers();

        model.addAttribute("products", listProducts);
        model.addAttribute("categories", listCategories);
        model.addAttribute("manufacturers", listManufacturers);

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("user", user);
        }

        return "homePage";
    }

    @GetMapping("/list-products")
    public String getProductsForUser(Model model, HttpSession session) {
        List<Product> listProducts = productService.getAllProducts();
        List<Category> listCategories = categoryService.getAllCategories();
        List<Manufacturer> listManufacturers = manufacturerService.getAllManufacturers();

        model.addAttribute("products", listProducts);
        model.addAttribute("categories", listCategories);
        model.addAttribute("manufacturers", listManufacturers);

        return "listProducts";
    }

    @GetMapping("/products-by-category")
    public String getProductsByCategory(Model model, @RequestParam("categoryName") String categoryName) {

        if (categoryName != null) {
            List<Product> listProducts = productService.findByCategoryName(categoryName);
            model.addAttribute("products", listProducts);
        }

        List<Category> listCategories = categoryService.getAllCategories();
        List<Manufacturer> listManufacturers = manufacturerService.getAllManufacturers();

        model.addAttribute("categories", listCategories);
        model.addAttribute("manufacturers", listManufacturers);

        return "listProducts";
    }

    @GetMapping("/products-by-manufacturer")
    public String getProductsByManufacturer(Model model, @RequestParam("manufacturerName") String manufacturerName) {

        if (manufacturerName != null) {
            List<Product> listProducts = productService.findByManufacturerName(manufacturerName);
            model.addAttribute("products", listProducts);
        }

        List<Category> listCategories = categoryService.getAllCategories();
        List<Manufacturer> listManufacturers = manufacturerService.getAllManufacturers();

        model.addAttribute("categories", listCategories);
        model.addAttribute("manufacturers", listManufacturers);

        return "listProducts";
    }

    @GetMapping("/dashBoard")
    public String getDashBoardPage(Model model, HttpSession session) {
        List<Product> listProducts = productService.getAllProducts();
        List<Category> listCategories = categoryService.getAllCategories();
        List<Manufacturer> listManufacturers = manufacturerService.getAllManufacturers();
        List<User> listUsers = userService.getAllUsers();
        List<Staff> listStaffs = staffService.getAllStaffs();

        model.addAttribute("products", listProducts);
        model.addAttribute("categories", listCategories);
        model.addAttribute("manufacturers", listManufacturers);
        model.addAttribute("users", listUsers);
        model.addAttribute("staffs", listStaffs);

        Staff staff = (Staff) session.getAttribute("staff");
        if (staff != null) {
            model.addAttribute("staff", staff);
        }
        return "admin/dashBoard";

    }

    @GetMapping("/login-admin")
    public String getLoginPage(Model model) {
        return "loginAdmin_Staff";
    }

    @PostMapping("/login-admin")
    public String getLogin(Model model, HttpSession session, @RequestParam("username") String username,
            @RequestParam("password") String password) {

        String passEncoding = encoding.toSHA1(password);
        Staff checkStaff = staffService.findByUsernameAndPassword(username, passEncoding);

        if (username.equals("admin") && password.equals("admin")) {
            Admin admin = adminService.findByUsernameAndPassword("admin", "admin");

            session.setAttribute("admin", admin);
            model.addAttribute("alert", "success");

            return getDashBoardPage(model, session);

        } else if (checkStaff != null && checkStaff.getStatus() == 0) {
            // model.addAttribute("mode", mode);
            session.setAttribute("staff", checkStaff);
            model.addAttribute("alert", "success");
            return getDashBoardPage(model, session);
        } else {
            model.addAttribute("alert", "error");
            return getLoginPage(model);
        }

    }

    public boolean checkLogin(Model model, HttpSession session) {
        Staff staff = (Staff) session.getAttribute("staff");
        Admin admin = (Admin) session.getAttribute("admin");

        if (admin != null) {
            model.addAttribute("admin", admin);
        } else if (staff != null) {
            model.addAttribute("staff", staff);
        } else if (admin == null && staff == null) {
            return false;
        }
        return true;
    }

    @GetMapping("/logout")
    public String getLogout(Model model, HttpSession session) {

        session.removeAttribute("admin");
        session.removeAttribute("staff");

        return getLoginPage(model);
    }

    @GetMapping("/orders")
    public String listOrders(Model model, HttpSession session) {
        List<Order> listOrders = orderService.getAllOrders();
        Boolean flag = checkLogin(model, session);
        if (flag == false) {
            return getLoginPage(model);
        }

        model.addAttribute("orders", listOrders);
        model.addAttribute("mode", "staff");

        return "admin/orders";
    }

    @GetMapping("/orders/get-order-detail/{orderId}")
    @ResponseBody
    public List<OrderDetail> getOrderDetails(@PathVariable Long orderId, Model model) {
        List<OrderDetail> listOrderDetails = orderDetailService.findByOrderId(orderId);
        model.addAttribute("orderDetails", listOrderDetails);
        return listOrderDetails;
    }

    @GetMapping("/orders/change-status/{id}")
    public String changeStatus(@PathVariable Long id, Model model, @ModelAttribute("order") Order order,
            HttpSession session) {
        Order existOrder = orderService.getOrderById(id);
        if (existOrder.getStatus() == 0) {
            existOrder.setStatus(1);
        } else {
            existOrder.setStatus(0);
        }
        model.addAttribute("alert", "success");
        orderService.changeStatusOrder(existOrder);
        return listOrders(model, session);
    }

    /* Order user */
    @GetMapping("/users/add-to-cart/{productId}")
    public String addToCart(Model model, @PathVariable long productId, HttpSession session,
            @RequestParam(name = "quantity", defaultValue = "1") int quantity) {
        Long userId = (Long) session.getAttribute("userId");
        Cart cart = (Cart) session.getAttribute("cart");
        Product temp = productService.getProductById(productId);

        if (userId == null) {
            addToCartWithoutUser(session, cart, temp);
        } else {
            User user = userService.getUserById(userId);
            addToCartWithUser(session, quantity, cart, temp, user);
        }

        return "redirect:/home-page";

    }

    private void addToCartWithUser(HttpSession session, int quantity, Cart cart, Product product, User user) {
        if (cart == null) {
            cart = cartService.saveCart(new Cart(user));
        }

        CartItem cartItem = cartItemService.findByProductId(product.getId());

        if (cartItem == null) {
            cartItem = cartItemService.save(new CartItem(cart, product, quantity));
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
            cartItemService.save(cartItem);
        }

        cart.addProduct(product, cart);
        session.setAttribute("cart", cart);
    }

    private void addToCartWithoutUser(HttpSession session, Cart cart, Product product) {
        if (cart == null) {
            cart = new Cart();
            cart.addProduct(product, null);
        } else {
            CartItem cartItem = cart.checkProductExist(product.getId());
            if (cartItem == null) {
                cartItem = new CartItem(cart, product, 1);
            }
            cart.addProduct(product, cart);
        }
        session.setAttribute("cart", cart);
    }

    @PostMapping("/users/delete-product-in-cart")
    public String deleteToCart(Model model, @RequestParam long productId, HttpSession session) {
        Cart cart = (Cart) session.getAttribute("cart");
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) { // withLogin
            cart.deleteByProductId(productId);
        } else { // without login
            cartItemService.deleteByProductId(productId);
            cart.removeItem(productId);
            if (cart.getListItem().size() == 0) {
                cartService.deleteByUserId(userId);
                cart = null;
            }
        }
        session.setAttribute("cart", cart);
        return "redirect:/home-page";

    }

    @GetMapping("/users/add-order")
    public String addOrder(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        User userLogin = userService.getUserById(userId);
        Cart cart = (Cart) session.getAttribute("cart");
        LocalDateTime now = LocalDateTime.now();
        Order order = null;
        double total = 0;

        if (cart != null && userLogin != null) {
            order = orderService.saveOrder(new Order(0, userLogin, now));
            for (int i = 0; i < cart.getListItem().size(); i++) {
                Product product = cart.getListItem().get(i).getProduct();
                double subtotal = product.getPrice();
                OrderDetail tempOrderDetail = orderDetailService
                        .saveOrderDetail(new OrderDetail(order, product,
                                cartItemService.findByProductId(product.getId()).getQuantity(), subtotal));
                cartItemService.deleteByProductId(product.getId());
                total += tempOrderDetail.getFinalPrice();

            }
            order.setTotal(total);
            cartService.deleteByUserId(userLogin.getId());
            cart.getListItem().clear();

        }
        session.removeAttribute("cart");
        orderService.saveOrder(order);
        return "redirect:/home-page";

    }

    @PostMapping("/login-user")
    public String getLoginUser(Model model, @RequestParam("username") String username,
            @RequestParam("password") String password, HttpSession session) {
        // List<User> listUsers = userService.getAllUsers();
        String passEncoding = encoding.toSHA1(password);
        User user = userService.findByUsernameAndPassword(username, passEncoding);

        if (user != null && user.getStatus() == 0) {
            session.setAttribute("userId", user.getId());
            session.setAttribute("user", user);
            model.addAttribute("alert", "success");

            Cart cart = cartService.findByUserId(user.getId());
            session.setAttribute("cart", cart);
            return getHome(model, session);
        }

        model.addAttribute("alert", "error");
        return getHome(model, session);
    }

    @GetMapping("/users/logout")
    public String logOutUser(Model model, HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId != null) {
            session.removeAttribute("userId");
            session.removeAttribute("user");
            session.removeAttribute("cart");
        }

        return "redirect:/home-page";
    }

    /* Forgot password admin and staff */

    @GetMapping("/reset-password")
    public String toResetPasswordPage(@RequestParam("token") String tokenString, Model model) {
        TokenAdmin token = tokenAdminService.findByToken(tokenString);
        if (token == null)
            model.addAttribute("tokenExprired", true);

        model.addAttribute("token", tokenString);
        return "recover_password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(Model model, @RequestParam("token") String tokenString,
            @RequestParam("password") String password, HttpSession session) {

        System.out.println("----------------------" + password);
        TokenAdmin token = tokenAdminService.findByToken(tokenString);
        if (token == null) {
            model.addAttribute("flag", false);
            model.addAttribute("message", "Your token link is invalid!");
            return getLoginPage(model);
        }
        Staff staff = staffService.getStaffById(token.getStaffId());

        model.addAttribute("flag", true);
        model.addAttribute("message", "You have successfully changed your password.");

        staff.setPassword(encoding.toSHA1(password));

        tokenAdminService.deleteByStaffId(staff.getId());
        staffService.saveStaff(staff);

        session.removeAttribute("staff");
        model.addAttribute("alert", "recoverPass");

        return getLoginPage(model);
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam("email") String email, HttpServletRequest request, Model model) {

        String tokenString = RandomStringUtils.randomAlphanumeric(60);

        Staff staff = staffService.findByEmail(email);
        if (staff == null) {
            model.addAttribute("alert", "sendMailfail");
            return getLoginPage(model);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationTime = now.plusMinutes(30);

        tokenAdminService.save(new TokenAdmin(tokenString, staff.getId(), expirationTime));

        String siteURL = request.getRequestURL().toString();
        siteURL = siteURL.replace(request.getServletPath(), "");

        String resetPasswordLink = siteURL + "/reset-password?token=" + tokenString;
        sendEmail(email, resetPasswordLink);
        model.addAttribute("alert", "sendMailsuccess");

        return getLoginPage(model);

    }

    /* Forgot password user */

    @GetMapping("/users/reset-password")
    public String toResetPasswordPageForUser(@RequestParam("token") String tokenString, Model model) {
        TokenUser token = tokenUserService.findByToken(tokenString);
        if (token == null)
            model.addAttribute("tokenExprired", true);

        model.addAttribute("token", tokenString);
        return "/users/recover_password_user"; // user
    }

    @PostMapping("/users/reset-password")
    public String resetPasswordForUser(Model model, @RequestParam("token") String tokenString,
            @RequestParam("password") String password, HttpSession httpSession) {

        System.out.println("---------user-------------" + password);
        System.out.println("---------user-------------" + encoding.toSHA1(password));

        TokenUser token = tokenUserService.findByToken(tokenString);
        if (token == null) {
            model.addAttribute("flag", false);
            model.addAttribute("message", "Your token link is invalid!");
            return getHome(model, httpSession);
        }
        User user = userService.getUserById(token.getUserId());

        model.addAttribute("flag", true);
        model.addAttribute("message", "You have successfully changed your password.");

        user.setPassword(encoding.toSHA1(password));

        tokenUserService.deleteByUserId(user.getId());
        userService.saveUser(user);

        httpSession.removeAttribute("user");

        return getHome(model, httpSession);
    }

    @PostMapping("/users/forgot-password")
    public String forgotPasswordForUser(@RequestParam("email") String email, HttpServletRequest request, Model model,
            HttpSession session) {

        String tokenString = RandomStringUtils.randomAlphanumeric(60);
        List<User> listUsers = userService.getAllUsers();
        // User existuser = userService.getUserByEmail(email);
        User existuser = userService.findByEmail(email);
        for (User user : listUsers) {
            if (!user.getEmail().equals(email)) {
                model.addAttribute("alert", "sendMailfail");
                return getHome(model, session);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationTime = now.plusMinutes(30);

        tokenUserService.save(new TokenUser(tokenString, existuser.getId(), expirationTime));

        String siteURL = request.getRequestURL().toString();
        siteURL = siteURL.replace(request.getServletPath(), "");

        String resetPasswordLink = siteURL + "/users/reset-password?token=" + tokenString;
        sendEmail(email, resetPasswordLink);
        model.addAttribute("user", existuser);
        model.addAttribute("alert", "sendMailsuccess");

        return getHome(model, session); // user

    }

    /* send email */
    public void sendEmail(String recipientEmail, String link) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        try {
            helper.setTo(recipientEmail);
            helper.setFrom("contact@shopme.com", "NAD Support");

            String subject = "Here's the link to reset your password";
            String content = "<p>Hello,</p>"
                    + "<p>You have requested to reset your password.</p>"
                    + "<p>Click the link below to reset your password:</p>"
                    + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                    + "<br>"
                    + "<p>Ignore this email if you do remember your password, "
                    + "or you have not made the request.</p>";

            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}