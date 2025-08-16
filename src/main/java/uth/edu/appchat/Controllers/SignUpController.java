package uth.edu.appchat.Controllers;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uth.edu.appchat.Dtos.AuthDtos.RegisterRequest;
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
    public String handleSignup(@jakarta.validation.Valid @ModelAttribute("form") RegisterRequest form,
                               org.springframework.validation.BindingResult binding,
                               @org.springframework.web.bind.annotation.RequestParam("confirmPassword") String confirmPassword,
                               org.springframework.ui.Model model) {
        if (binding.hasErrors()) {
            return "signup";
        }
        if (!form.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            return "signup";
        }
        try {
            userService.register(form);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "signup";
        }
        return "redirect:/login?registered=true";
    }
}
