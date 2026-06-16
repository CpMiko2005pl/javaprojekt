package pl.pb.monopoly.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.domain.UserWarning;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.repository.UserWarningRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModerationService {

    private final UserRepository userRepository;
    private final UserWarningRepository userWarningRepository;

    public ModerationService(UserRepository userRepository, UserWarningRepository userWarningRepository) {
        this.userRepository = userRepository;
        this.userWarningRepository = userWarningRepository;
    }

    @Transactional
    public boolean isUserSuspended(User user) {
        if (user == null || !user.isSuspended()) {
            return false;
        }
        if (user.getSuspendedUntil() != null && user.getSuspendedUntil().isBefore(LocalDateTime.now())) {
            user.setSuspended(false);
            user.setSuspendedUntil(null);
            user.setSuspensionReason(null);
            userRepository.save(user);
            return false;
        }
        return true;
    }

    public void ensureCanPlay(User user) {
        if (isUserSuspended(user)) {
            String reason = user.getSuspensionReason();
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Twoje konto jest zawieszone — nie mozesz grac.");
            }
            throw new IllegalArgumentException("Twoje konto jest zawieszone: " + reason);
        }
    }

    public boolean canModerate(User moderator, User target) {
        if (moderator == null || target == null) {
            return false;
        }
        if (moderator.getId().equals(target.getId())) {
            return false;
        }
        Role modRole = moderator.getRole();
        Role targetRole = target.getRole();
        if (targetRole == Role.ADMIN) {
            return false;
        }
        if (targetRole == Role.MODERATOR && modRole != Role.ADMIN) {
            return false;
        }
        return modRole == Role.ADMIN || modRole == Role.MODERATOR;
    }

    @Transactional
    public void suspendUser(Long targetId, String moderatorUsername, String reason, Integer days) {
        User moderator = userRepository.findByUsername(moderatorUsername)
                .orElseThrow(() -> new IllegalArgumentException("Brak moderatora."));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono uzytkownika."));
        if (!canModerate(moderator, target)) {
            throw new IllegalArgumentException("Nie mozesz zawiesic tego uzytkownika.");
        }
        target.setSuspended(true);
        target.setSuspensionReason(reason == null || reason.isBlank()
                ? "Zawieszenie przez moderatora." : reason.trim());
        target.setSuspendedUntil(days != null && days > 0
                ? LocalDateTime.now().plusDays(days) : null);
        userRepository.save(target);
    }

    @Transactional
    public void unsuspendUser(Long targetId, String moderatorUsername) {
        User moderator = userRepository.findByUsername(moderatorUsername)
                .orElseThrow(() -> new IllegalArgumentException("Brak moderatora."));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono uzytkownika."));
        if (!canModerate(moderator, target)) {
            throw new IllegalArgumentException("Nie mozesz odwiesic tego uzytkownika.");
        }
        target.setSuspended(false);
        target.setSuspendedUntil(null);
        target.setSuspensionReason(null);
        userRepository.save(target);
    }

    @Transactional
    public UserWarning issueWarning(Long targetId, String moderatorUsername, String message) {
        User moderator = userRepository.findByUsername(moderatorUsername)
                .orElseThrow(() -> new IllegalArgumentException("Brak moderatora."));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono uzytkownika."));
        if (!canModerate(moderator, target)) {
            throw new IllegalArgumentException("Nie mozesz ostrzec tego uzytkownika.");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Podaj tresc ostrzezenia.");
        }
        return userWarningRepository.save(new UserWarning(target, moderator, message.trim()));
    }

    @Transactional(readOnly = true)
    public List<UserWarning> warningsForUser(Long userId) {
        return userWarningRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long warningCount(Long userId) {
        return userWarningRepository.countByUserId(userId);
    }
}
