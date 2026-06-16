package pl.pb.monopoly.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.ProfileComment;
import pl.pb.monopoly.domain.ProfileCommentLike;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.ProfileCommentLikeRepository;
import pl.pb.monopoly.repository.ProfileCommentRepository;
import pl.pb.monopoly.repository.UserRepository;

import java.time.LocalDateTime;

@Controller
public class ProfileCommentController {

    private final ProfileCommentRepository commentRepository;
    private final ProfileCommentLikeRepository likeRepository;
    private final UserRepository userRepository;

    public ProfileCommentController(ProfileCommentRepository commentRepository,
                                    ProfileCommentLikeRepository likeRepository,
                                    UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/u/{username}/comment")
    @Transactional
    public String add(@PathVariable String username,
                      @RequestParam String content,
                      @RequestParam(required = false) String from,
                      Authentication auth,
                      RedirectAttributes ra) {
        User author = currentUser(auth);
        if (author == null) {
            return "redirect:/login";
        }
        User target = userRepository.findByUsername(username).orElse(null);
        String back = safeBack(from, "/u/" + username);
        if (target == null) {
            ra.addFlashAttribute("commentError", "Nie znaleziono profilu.");
            return "redirect:" + back;
        }
        if (target.getId().equals(author.getId())) {
            ra.addFlashAttribute("commentError", "Nie mozesz komentowac wlasnego profilu.");
            return "redirect:" + back;
        }
        if (content == null || content.isBlank()) {
            ra.addFlashAttribute("commentError", "Komentarz nie moze byc pusty.");
            return "redirect:" + back;
        }
        if (commentRepository.findByAuthorIdAndTargetId(author.getId(), target.getId()).isPresent()) {
            ra.addFlashAttribute("commentError", "Masz juz komentarz pod tym profilem — edytuj go lub usun.");
            return "redirect:" + back;
        }
        commentRepository.save(new ProfileComment(author, target, trim(content)));
        ra.addFlashAttribute("commentMessage", "Dodano komentarz.");
        return "redirect:" + back;
    }

    @PostMapping("/comments/{id}/edit")
    @Transactional
    public String edit(@PathVariable Long id,
                       @RequestParam String content,
                       @RequestParam(required = false) String from,
                       Authentication auth,
                       RedirectAttributes ra) {
        User me = currentUser(auth);
        ProfileComment c = commentRepository.findById(id).orElse(null);
        if (me == null || c == null) {
            return "redirect:/";
        }
        String back = safeBack(from, "/u/" + c.getTarget().getUsername());
        if (!c.getAuthor().getId().equals(me.getId())) {
            ra.addFlashAttribute("commentError", "To nie Twoj komentarz.");
            return "redirect:" + back;
        }
        if (content == null || content.isBlank()) {
            ra.addFlashAttribute("commentError", "Komentarz nie moze byc pusty.");
            return "redirect:" + back;
        }
        c.setContent(trim(content));
        c.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(c);
        ra.addFlashAttribute("commentMessage", "Zaktualizowano komentarz.");
        return "redirect:" + back;
    }

    @PostMapping("/comments/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String from,
                         Authentication auth,
                         RedirectAttributes ra) {
        User me = currentUser(auth);
        ProfileComment c = commentRepository.findById(id).orElse(null);
        if (me == null || c == null) {
            return "redirect:/";
        }
        String back = safeBack(from, "/u/" + c.getTarget().getUsername());
        if (!canManage(me, c)) {
            ra.addFlashAttribute("commentError", "Brak uprawnien do usuniecia.");
            return "redirect:" + back;
        }
        likeRepository.deleteByCommentId(c.getId());
        commentRepository.delete(c);
        ra.addFlashAttribute("commentMessage", "Usunieto komentarz.");
        return "redirect:" + back;
    }

    @PostMapping("/comments/{id}/reply")
    @Transactional
    public String reply(@PathVariable Long id,
                        @RequestParam(required = false) String content,
                        @RequestParam(required = false) String from,
                        Authentication auth,
                        RedirectAttributes ra) {
        User me = currentUser(auth);
        ProfileComment c = commentRepository.findById(id).orElse(null);
        if (me == null || c == null) {
            return "redirect:/";
        }
        String back = safeBack(from, "/u/" + c.getTarget().getUsername());
        if (!c.getTarget().getId().equals(me.getId())) {
            ra.addFlashAttribute("commentError", "Tylko wlasciciel profilu moze odpowiadac.");
            return "redirect:" + back;
        }
        if (content == null || content.isBlank()) {
            c.setReply(null);
            c.setReplyAt(null);
            ra.addFlashAttribute("commentMessage", "Usunieto odpowiedz.");
        } else {
            c.setReply(trim(content));
            c.setReplyAt(LocalDateTime.now());
            ra.addFlashAttribute("commentMessage", "Dodano odpowiedz.");
        }
        commentRepository.save(c);
        return "redirect:" + back;
    }

    @PostMapping("/comments/{id}/like")
    @Transactional
    public String like(@PathVariable Long id,
                       @RequestParam(required = false) String from,
                       Authentication auth,
                       RedirectAttributes ra) {
        User me = currentUser(auth);
        ProfileComment c = commentRepository.findById(id).orElse(null);
        if (me == null || c == null) {
            return "redirect:/login";
        }
        String back = safeBack(from, "/u/" + c.getTarget().getUsername());
        likeRepository.findByCommentIdAndUserId(c.getId(), me.getId())
                .ifPresentOrElse(
                        likeRepository::delete,
                        () -> likeRepository.save(new ProfileCommentLike(c, me)));
        return "redirect:" + back;
    }

    private boolean canManage(User me, ProfileComment c) {
        boolean isAuthor = c.getAuthor().getId().equals(me.getId());
        boolean isOwner = c.getTarget().getId().equals(me.getId());
        boolean canModerate = me.getRole() == Role.ADMIN || me.getRole() == Role.MODERATOR;
        return isAuthor || isOwner || canModerate;
    }

    private static String trim(String s) {
        String t = s.strip();
        return t.length() > 500 ? t.substring(0, 500) : t;
    }

    private static String safeBack(String from, String fallback) {
        if (from != null && from.startsWith("/") && !from.startsWith("//")) {
            return from;
        }
        return fallback;
    }

    private User currentUser(Authentication auth) {
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
