package pl.pb.monopoly.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.ProfileComment;
import pl.pb.monopoly.repository.AchievementRepository;
import pl.pb.monopoly.repository.FriendshipRepository;
import pl.pb.monopoly.repository.GameInviteRepository;
import pl.pb.monopoly.repository.GamePlayerRepository;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.OwnedItemRepository;
import pl.pb.monopoly.repository.ProfileCommentLikeRepository;
import pl.pb.monopoly.repository.ProfileCommentRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.repository.UserWarningRepository;

import java.util.List;

@Service
public class UserAccountDeletionService {

    private final UserRepository userRepository;
    private final AchievementRepository achievementRepository;
    private final OwnedItemRepository ownedItemRepository;
    private final MatchHistoryRepository matchHistoryRepository;
    private final FriendshipRepository friendshipRepository;
    private final GameInviteRepository gameInviteRepository;
    private final UserWarningRepository userWarningRepository;
    private final ProfileCommentRepository profileCommentRepository;
    private final ProfileCommentLikeRepository profileCommentLikeRepository;
    private final GamePlayerRepository gamePlayerRepository;

    public UserAccountDeletionService(UserRepository userRepository,
                                      AchievementRepository achievementRepository,
                                      OwnedItemRepository ownedItemRepository,
                                      MatchHistoryRepository matchHistoryRepository,
                                      FriendshipRepository friendshipRepository,
                                      GameInviteRepository gameInviteRepository,
                                      UserWarningRepository userWarningRepository,
                                      ProfileCommentRepository profileCommentRepository,
                                      ProfileCommentLikeRepository profileCommentLikeRepository,
                                      GamePlayerRepository gamePlayerRepository) {
        this.userRepository = userRepository;
        this.achievementRepository = achievementRepository;
        this.ownedItemRepository = ownedItemRepository;
        this.matchHistoryRepository = matchHistoryRepository;
        this.friendshipRepository = friendshipRepository;
        this.gameInviteRepository = gameInviteRepository;
        this.userWarningRepository = userWarningRepository;
        this.profileCommentRepository = profileCommentRepository;
        this.profileCommentLikeRepository = profileCommentLikeRepository;
        this.gamePlayerRepository = gamePlayerRepository;
    }

    @Transactional
    public void purgeAndDelete(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Nie ma uzytkownika o id " + userId);
        }

        List<ProfileComment> comments = profileCommentRepository.findByAuthorIdOrTargetId(userId, userId);
        if (!comments.isEmpty()) {
            List<Long> commentIds = comments.stream().map(ProfileComment::getId).toList();
            profileCommentLikeRepository.deleteByCommentIdIn(commentIds);
            profileCommentRepository.deleteAll(comments);
        }
        profileCommentLikeRepository.deleteByUserId(userId);

        achievementRepository.deleteByUserId(userId);
        ownedItemRepository.deleteByUserId(userId);
        matchHistoryRepository.deleteByUserId(userId);
        friendshipRepository.deleteAllForUser(userId);
        gameInviteRepository.deleteAllForUser(userId);
        userWarningRepository.deleteByUserId(userId);
        userWarningRepository.deleteByModeratorId(userId);

        gamePlayerRepository.findByUser_Id(userId).forEach(player -> player.setUser(null));

        userRepository.deleteById(userId);
    }
}
