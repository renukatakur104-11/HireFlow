package com.hireflow.modules.notification;

import com.hireflow.common.response.ApiResponse;
import com.hireflow.common.response.PagedResponse;
import com.hireflow.modules.notification.dto.BroadcastRequest;
import com.hireflow.modules.notification.dto.NotificationDto;
import com.hireflow.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationDto>>> myNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        var result = notificationService.getMyNotifications(principal,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(new PagedResponse<>(result)));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(principal)));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markRead(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null, "Marked as read"));
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAllRead(principal)));
    }

    @PostMapping("/admin/notifications/preview-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> previewCount(@RequestBody BroadcastRequest req) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.previewCount(req)));
    }

    @PostMapping("/admin/notifications/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> broadcast(
            @Valid @RequestBody BroadcastRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        int sent = notificationService.broadcast(req, principal);
        return ResponseEntity.ok(ApiResponse.success("Sent to " + sent + " recipients"));
    }
}
