package pl.pb.monopoly.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.ProfileComment;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.ProfileCommentRepository;
import pl.pb.monopoly.repository.UserRepository;

import java.time.LocalDateTime;

/**
 * Komentarze pod profilem gracza. Zasady:
 *  - jeden autor moze miec maks. 1 komentarz pod danym profilem,
 *  - autor moze edytowac i usunac swoj komentarz (po usunieciu moze dodac nowy),
 *  - nie mozna komentowac wlasnego profilu,
 *  - admin/moderator moga usuwac dowolne komentarze (moderacja).
 */
@Controller
public class ProfileCommentController {

    private final ProfileCommentRepository commentRepository;
    private final UserRepository userRepository;

    public ProfileCommentController(ProfileCommentRepository commentRepository,
                                    UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/u/{username}/comment")
    @Transactional
    public String add(@PathVariable String username,
                      @RequestParam String content,
                      Authentication auth,
                      RedirectAttributes ra) {
        String redirect = "redirect:/u/" + username;
        User author = currentUser(auth);
        if (author == null) {
            return "redirect:/login";
        }
        User target = userRepository.findByUsername(username).orElse(null);
        if (target == null) {
            ra.addFlashAttribute("commentError", "Nie znaleziono profilu.");
            return redirect;
        }
        if (target.getId().equals(author.getId())) {
            ra.addFlashAttribute("commentError", "Nie mozesz komentowac wlasnego profilu.");
            return redirect;
        }
        if (content == null || content.isBlank()) {
            ra.addFlashAttribute("commentError", "Komentarz nie moze byc pusty.");
            return redirect;
        }
        if (commentRepository.findByAuthorIdAndTargetId(author.getId(), target.getId()).isPresent()) {
            ra.addFlashAttribute("commentError", "Masz juz komentarz pod tym profilem — edytuj go lub usun.");
            return redirect;
        }
        commentRepository.save(new ProfileComment(author, target, content.strip().substring(0, Math.min(500, content.strip().length()))));
        ra.addFlashAttribute("commentMessage", "Dodano komentarz.");
        return redirect;
    }

    @PostMapping("/comments/{id}/edit")
    @Transactional
    public String edit(@PathVariable Long id,
                       @RequestParam String content,
                       Authentication auth,
                       RedirectAttributes ra) {
        User me = currentUser(auth);
        ProfileComment c = commentRepository.findById(id).orElse(null);
        if (me == null || c == null) {
            return "redirect:/";
        }
        String redirect = "redirect:/u/" + c.getTarget().getUsername();
        if (!c.getAuthor().getId().equals(me.getId())) {
            ra.addFlashAttribute("commentError", "To nie Twoj komentarz.");
            return redirect;
        }
        if (content == null || content.isBlank()) {
            ra.addFlashAttribute("commentError", "Komentarz nie moze byc pusty.");
            return redirect;
        }
        c.setContent(content.strip().substring(0, Math.min(500, content.strip().length())));
        c.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(c);
        ra.addFlashAttribute("commentMessage", "Zaktualizowano komentarz.");
        return redirect;
    }

    @PostMapping("/comments/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id,
                         Authentication auth,
                         RedirectAttributes ra) {
        User me = currentUser(auth);
        ProfileComment c = commentRepository.findById(id).orElse(null);
        if (me == null || c == null) {
            return "redirect:/";
        }
        String redirect = "redirect:/u/" + c.getTarget().getUsername();
        boolean isAuthor = c.getAuthor().getId().equals(me.getId());
        boolean canModerate = me.getRole() == Role.ADMIN || me.getRole() == Role.MODERATOR;
        if (!isAuthor && !canModerate) {
            ra.addFlashAttribute("commentError", "Brak uprawnien do usuniecia.");
            return redirect;
        }
        commentRepository.delete(c);
        ra.addFlashAttribute("commentMessage", "Usunieto komentarz — mozesz dodac nowy.");
        return redirect;
    }

    private User currentUser(Authentication auth) {
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
