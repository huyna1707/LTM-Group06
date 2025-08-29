package uth.edu.appchat.Controllers;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uth.edu.appchat.Dtos.RegisterRequest;
import uth.edu.appchat.Services.UserService;

@Controller
public class SignUpController {

    private final UserService userService;

    public SignUpController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("form", new RegisterRequest());
        return "signup";
    }

    @PostMapping("/signup")
    public String processSignup(@Valid @ModelAttribute("form") RegisterRequest form,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) return "signup";

        try {
            userService.register(form);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công!");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            result.reject(null, ex.getMessage());
        } catch (Exception ex) {
            result.reject(null, "Đã xảy ra lỗi. Vui lòng thử lại.");
        }

        return "signup";
    }
}
