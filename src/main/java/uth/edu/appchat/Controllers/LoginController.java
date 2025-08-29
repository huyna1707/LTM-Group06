package uth.edu.appchat.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginPage(Model model,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "registered", required = false) String registered) {

        if (error != null) {
            model.addAttribute("loginError", "Sai tên đăng nhập hoặc mật khẩu.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Đăng xuất thành công.");
        }
        if (registered != null) {
            model.addAttribute("registeredMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
        }

        return "login";
    }
}
