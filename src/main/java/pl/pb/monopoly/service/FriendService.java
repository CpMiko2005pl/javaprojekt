package pl.pb.monopoly.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.FriendStatus;
import pl.pb.monopoly.domain.Friendship;
import pl.pb.monopoly.domain.PlayerStatistics;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.dto.FriendDto;
import pl.pb.monopoly.dto.FriendRequestDto;
import pl.pb.monopoly.repository.FriendshipRepository;
import pl.pb.monopoly.repository.UserRepository;

import java.util.List;

@Service
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserPresenceService presenceService;

    public FriendService(FriendshipRepository friendshipRepository,
                         UserRepository userRepository,
                         UserPresenceService presenceService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }

    private User user(String username) {
        return userRepository.findByUsername(username).orElseThrow();
    }

    private int levelOf(User u) {
        PlayerStatistics s = u.getStatistics();
        return s != null ? s.getLevel() : 1;
    }

    private int eloOf(User u) {
        PlayerStatistics s = u.getStatistics();
        return s != null ? s.getEloPoints() : 0;
    }

    @Transactional(readOnly = true)
    public List<FriendDto> friendsOf(String username) {
        User me = user(username);
        List<Friendship> rels = friendshipRepository
                .findByStatusAndRequesterIdOrStatusAndAddresseeId(
                        FriendStatus.ACCEPTED, me.getId(),
                        FriendStatus.ACCEPTED, me.getId());
        return rels.stream().map(f -> {
            User other = f.getRequester().getId().equals(me.getId()) ? f.getAddressee() : f.getRequester();
            return new FriendDto(f.getId(), other.getId(), other.getUsername(),
                    levelOf(other), eloOf(other), presenceService.isOnline(other.getUsername()));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDto> pendingFor(String username) {
        User me = user(username);
        return friendshipRepository.findByAddresseeIdAndStatus(me.getId(), FriendStatus.PENDING)
                .stream()
                .map(f -> new FriendRequestDto(f.getId(), f.getRequester().getId(),
                        f.getRequester().getUsername(), levelOf(f.getRequester())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendDto> search(String fragment, String username) {
        if (fragment == null || fragment.isBlank()) {
            return List.of();
        }
        return userRepository.findTop10ByUsernameContainingIgnoreCase(fragment.trim()).stream()
                .filter(u -> !u.getUsername().equals(username))
                .map(u -> new FriendDto(null, u.getId(), u.getUsername(),
                        levelOf(u), eloOf(u), presenceService.isOnline(u.getUsername())))
                .toList();
    }

    @Transactional
    public void sendRequest(String fromUsername, String toUsername) {
        User from = user(fromUsername);
        User to = userRepository.findByUsername(toUsername)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma gracza: " + toUsername));
        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Nie mozesz dodac samego siebie");
        }

        boolean exists = friendshipRepository.existsByRequesterIdAndAddresseeId(from.getId(), to.getId())
                || friendshipRepository.existsByRequesterIdAndAddresseeId(to.getId(), from.getId());
        if (exists) {
            throw new IllegalArgumentException("Zaproszenie lub znajomosc juz istnieje");
        }
        friendshipRepository.save(new Friendship(from, to, FriendStatus.PENDING));
    }

    @Transactional
    public void accept(Long friendshipId, String username) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Brak zaproszenia"));
        if (!f.getAddressee().getUsername().equals(username)) {
            throw new IllegalArgumentException("To nie Twoje zaproszenie");
        }
        f.setStatus(FriendStatus.ACCEPTED);
    }

    @Transactional(readOnly = true)
    public boolean isFriend(String username, Long otherUserId) {
        if (otherUserId == null) return false;
        return friendsOf(username).stream().anyMatch(f -> otherUserId.equals(f.userId()));
    }

    @Transactional
    public void removeOrReject(Long friendshipId, String username) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Brak relacji"));
        boolean involved = f.getRequester().getUsername().equals(username)
                || f.getAddressee().getUsername().equals(username);
        if (!involved) {
            throw new IllegalArgumentException("Brak uprawnien");
        }
        friendshipRepository.delete(f);
    }
}
