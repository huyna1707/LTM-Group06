package uth.edu.appchat.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Models.Friend;
import uth.edu.appchat.Models.User;
import uth.edu.appchat.Repositories.FriendRepository;
import uth.edu.appchat.Repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // DTO classes
    public static class FriendRequestDto {
        public String username;
    }

    public static class FriendDto {
        public Long id;
        public String username;
        public String fullName;
        public String status;
        public LocalDateTime createdAt;
    }

    // Gửi lời mời kết bạn
    @PostMapping("/send-request")
    public ResponseEntity<?> sendFriendRequest(
            @RequestBody FriendRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Tìm user hiện tại
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElse(null);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy người dùng hiện tại");
            }

            // Tìm user được mời
            User targetUser = userRepository.findByUsername(request.username)
                    .orElse(null);
            if (targetUser == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy người dùng: " + request.username);
            }

            // Không thể kết bạn với chính mình
            if (currentUser.getId().equals(targetUser.getId())) {
                return ResponseEntity.badRequest().body("Không thể kết bạn với chính mình");
            }

            // Kiểm tra xem đã có quan hệ bạn bè chưa
            Optional<Friend> existingFriendship = friendRepository.findFriendshipBetween(currentUser, targetUser);
            if (existingFriendship.isPresent()) {
                Friend friendship = existingFriendship.get();
                if (friendship.getStatus() == Friend.FriendStatus.ACCEPTED) {
                    return ResponseEntity.badRequest().body("Đã là bạn bè rồi");
                } else if (friendship.getStatus() == Friend.FriendStatus.PENDING) {
                    return ResponseEntity.badRequest().body("Lời mời kết bạn đã được gửi trước đó");
                }
            }

            // Tạo lời mời kết bạn mới
            Friend friendRequest = new Friend();
            friendRequest.setUser(currentUser);
            friendRequest.setFriend(targetUser);
            friendRequest.setStatus(Friend.FriendStatus.PENDING);
            friendRequest.setCreatedAt(LocalDateTime.now());

            friendRepository.save(friendRequest);

            // Gửi thông báo real-time tới người được mời
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "FRIEND_REQUEST_RECEIVED");
            notification.put("fromUser", currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername());
            notification.put("fromUserId", currentUser.getId());
            notification.put("message", "Bạn có lời mời kết bạn mới");

            messagingTemplate.convertAndSendToUser(
                    targetUser.getUsername(),
                    "/friend-request",
                    notification
            );

            return ResponseEntity.ok("Đã gửi lời mời kết bạn thành công");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Có lỗi xảy ra: " + e.getMessage());
        }
    }

    // Chấp nhận lời mời kết bạn
    @PostMapping("/accept/{userId}")
    public ResponseEntity<?> acceptFriendRequest(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElse(null);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy người dùng hiện tại");
            }

            User requestUser = userRepository.findById(userId).orElse(null);
            if (requestUser == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy người dùng");
            }

            // Tìm lời mời kết bạn
            Optional<Friend> friendRequestOpt = friendRepository.findFriendshipBetween(requestUser, currentUser);
            if (friendRequestOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Không tìm thấy lời mời kết bạn");
            }

            Friend friendRequest = friendRequestOpt.get();
            if (friendRequest.getStatus() != Friend.FriendStatus.PENDING) {
                return ResponseEntity.badRequest().body("Lời mời kết bạn không hợp lệ");
            }

            // Chấp nhận lời mời
            friendRequest.setStatus(Friend.FriendStatus.ACCEPTED);
            friendRepository.save(friendRequest);

            // Gửi thông báo tới người gửi lời mời
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "FRIEND_REQUEST_ACCEPTED");
            notification.put("fromUser", currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername());
            notification.put("message", "Lời mời kết bạn đã được chấp nhận");

            messagingTemplate.convertAndSendToUser(
                    requestUser.getUsername(),
                    "/friend-request",
                    notification
            );

            // Gửi thông báo cập nhật danh sách bạn bè cho cả hai người
            Map<String, Object> friendListUpdate = new HashMap<>();
            friendListUpdate.put("type", "FRIEND_LIST_UPDATE");
            friendListUpdate.put("message", "Danh sách bạn bè đã được cập nhật");

            // Gửi cho người chấp nhận (currentUser)
            messagingTemplate.convertAndSendToUser(
                    currentUser.getUsername(),
                    "/friend-request",
                    friendListUpdate
            );

            // Gửi cho người gửi lời mời (requestUser)
            messagingTemplate.convertAndSendToUser(
                    requestUser.getUsername(),
                    "/friend-request",
                    friendListUpdate
            );

            return ResponseEntity.ok("Đã chấp nhận lời mời kết bạn");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Có lỗi xảy ra: " + e.getMessage());
        }
    }

    // Lấy danh sách bạn bè
    @GetMapping("/list")
    public ResponseEntity<List<FriendDto>> getFriendsList(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElse(null);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            List<Friend> friendships = friendRepository.findAcceptedFriends(currentUser);
            List<FriendDto> friends = new ArrayList<>();

            for (Friend friendship : friendships) {
                User friendUser = friendship.getUser().getId().equals(currentUser.getId())
                        ? friendship.getFriend()
                        : friendship.getUser();

                FriendDto friendDto = new FriendDto();
                friendDto.id = friendUser.getId();
                friendDto.username = friendUser.getUsername();
                friendDto.fullName = friendUser.getFullName();
                friendDto.status = friendUser.getStatus() != null ? friendUser.getStatus().toString() : "OFFLINE";
                friendDto.createdAt = friendship.getCreatedAt();

                friends.add(friendDto);
            }

            return ResponseEntity.ok(friends);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Lấy danh sách lời mời kết bạn đang chờ
    @GetMapping("/pending-requests")
    public ResponseEntity<List<FriendDto>> getPendingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElse(null);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            List<Friend> pendingRequests = friendRepository.findPendingFriendRequests(currentUser);
            List<FriendDto> requests = new ArrayList<>();

            for (Friend request : pendingRequests) {
                FriendDto requestDto = new FriendDto();
                requestDto.id = request.getUser().getId();
                requestDto.username = request.getUser().getUsername();
                requestDto.fullName = request.getUser().getFullName();
                requestDto.status = request.getUser().getStatus() != null ? request.getUser().getStatus().toString() : "OFFLINE";
                requestDto.createdAt = request.getCreatedAt();

                requests.add(requestDto);
            }

            return ResponseEntity.ok(requests);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Từ chối lời mời kết bạn
    @PostMapping("/reject/{userId}")
    public ResponseEntity<?> rejectFriendRequest(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElse(null);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy người dùng hiện tại");
            }

            User requestUser = userRepository.findById(userId).orElse(null);
            if (requestUser == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy người dùng");
            }

            // Tìm và xóa lời mời kết bạn
            Optional<Friend> friendRequestOpt = friendRepository.findFriendshipBetween(requestUser, currentUser);
            if (friendRequestOpt.isPresent()) {
                Friend friendRequest = friendRequestOpt.get();
                if (friendRequest.getStatus() == Friend.FriendStatus.PENDING) {
                    friendRepository.delete(friendRequest);
                    return ResponseEntity.ok("Đã từ chối lời mời kết bạn");
                }
            }

            return ResponseEntity.badRequest().body("Không tìm thấy lời mời kết bạn hợp lệ");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Có lỗi xảy ra: " + e.getMessage());
        }
    }
}