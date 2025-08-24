package uth.edu.appchat.Controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.UserRepository;

@Controller
public class homeController {

    private final UserRepository userRepository;

    public homeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String indexPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
            if (user != null) {
                model.addAttribute("username", user.getUsername());
                model.addAttribute("fullName", user.getFullName() != null ? user.getFullName() : user.getUsername());
                model.addAttribute("email", user.getEmail());
            } else {
                model.addAttribute("username", userDetails.getUsername());
                model.addAttribute("fullName", userDetails.getUsername());
            }
        }
        return "index";
    }


}
