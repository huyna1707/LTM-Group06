package uth.edu.appchat.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uth.edu.appchat.Dtos.CreateGroupForm;
import uth.edu.appchat.Models.GroupChat;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.UserRepository;
import uth.edu.appchat.Services.GroupChatService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class GroupChatController {
    private final GroupChatService groupChatService;
    private final UserRepository userRepo;

    @GetMapping("/groups/new")
    public String showCreateGroupForm(Model model) {
        model.addAttribute("createGroupForm", new CreateGroupForm());
        return "group_form"; // trỏ tới file group_form.html
    }

    @PostMapping("/groups/create")
    public String handleGroupForm(@ModelAttribute CreateGroupForm form,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        User creator = userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người tạo nhóm"));

        try {
            groupChatService.createGroup(form, creator); // KHÔNG dùng lại creator ở đây
            redirectAttributes.addFlashAttribute("successMessage", "✅ Tạo nhóm thành công!");
            return "redirect:/";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
            return "redirect:/"; // thêm dòng này để tránh lỗi thiếu return
        }
    }
}
