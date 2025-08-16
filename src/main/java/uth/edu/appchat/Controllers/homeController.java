package uth.edu.appchat.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uth.edu.appchat.Dtos.AuthDtos;

@Controller
public class homeController {

    @GetMapping("/")
    public String indexPage() {
        return "index";
    }
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // trả file templates/login.html
    }

}
