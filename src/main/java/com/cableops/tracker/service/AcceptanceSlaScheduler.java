package com.cableops.tracker.service;
 
import com.cableops.tracker.entity.Task;
import com.cableops.tracker.repository.TaskRepository;
import com.cableops.tracker.repository.UserRepository;
import com.cableops.tracker.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
 
@Slf4j
@Component
@RequiredArgsConstructor
public class AcceptanceSlaScheduler {
 
    private final TaskRepository    taskRepo;
    private final TelegramService   telegramService;
    private final UserRepository    userRepo;
    private final AccountRepository accountRepo;
 
    /**
     * Runs every minute. Sends ONE alert per task when SLA expires.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void checkAcceptanceSla() {
        List<Task> tasks = taskRepo.findPendingAcceptanceCheck();
        log.debug("AcceptanceSlaScheduler: scanning {} pending task(s)", tasks.size());
        if (tasks.isEmpty()) return;
 
        LocalDateTime now = LocalDateTime.now();
 
        for (Task t : tasks) {
            if (t.getCreatedAt() == null) {
                log.warn("Task {} has acceptanceTimeMins set but createdAt is null — skipping",
                        t.getId());
                continue;
            }
            if (t.getAcceptanceTimeMins() == null) {
                continue;  // can't happen given the query, but be safe
            }
 
            LocalDateTime deadline = t.getCreatedAt().plusMinutes(t.getAcceptanceTimeMins());
 
            if (now.isAfter(deadline)) {
                long overdueMins = Duration.between(deadline, now).toMinutes();
                try {
                    sendOverdueAlert(t, overdueMins);
                    t.setAcceptanceAlerted(true);
                    taskRepo.save(t);
                    log.info("Acceptance SLA overdue alert sent  taskId={} sr={} overdue={}min",
                            t.getId(), t.getCSRNumber(), overdueMins);
                } catch (Exception e) {
                    // Important: don't mark alerted if send failed —
                    // the next run will retry.
                    log.error("Failed to send acceptance SLA alert taskId={}: {}",
                            t.getId(), e.getMessage(), e);
                }
            } else {
                log.trace("Task {} not yet overdue (deadline={})", t.getId(), deadline);
            }
        }
    }
 
    private void sendOverdueAlert(Task t, long overdueMins) {
 
        StringBuilder sb = new StringBuilder();
        sb.append("<b>⏰ Acceptance SLA Overdue</b>\n\n");
        sb.append("<b>Circuit Name:</b> ").append(coalesce(t.getParentName(), "—")).append("\n");
        sb.append("<b>Account:</b> ")     .append(coalesce(t.getAccountName(), "—")).append("\n");
        sb.append("<b>SR Number:</b> ")   .append(coalesce(t.getCSRNumber(), "—")).append("\n");
        sb.append("<b>Status:</b> ")      .append(coalesce(t.getStatus(), "—")).append("\n");
        sb.append("<b>Priority:</b> ")    .append(coalesce(t.getPriority(), "Normal")).append("\n");
        sb.append("<b>Work Type:</b> ")   .append(coalesce(t.getCWorkType(), "—")).append("\n");
        if (t.getAssignedUserName() != null && !t.getAssignedUserName().isBlank())
            sb.append("<b>Assigned User:</b> ").append(t.getAssignedUserName()).append("\n");
        sb.append("<b>SLA Window:</b> ").append(t.getAcceptanceTimeMins()).append(" min(s)\n");
        sb.append("<b>Overdue by:</b> ").append(overdueMins).append(" min(s)\n");
        sb.append("\n<i>Please accept or escalate this task immediately.</i>");
 
        String msg = sb.toString();
 
        // 1) Primary assigned user
        notifyUser(t.getAssignedUserId(), msg);
 
        // 2) Secondary users (comma-separated id list)
        if (t.getCSecondaryUserIds() != null && !t.getCSecondaryUserIds().isBlank()) {
            for (String id : t.getCSecondaryUserIds().split(",")) {
                notifyUser(id.trim(), msg);
            }
        } else if (t.getCSecondaryAssignedUserId() != null) {
            notifyUser(t.getCSecondaryAssignedUserId(), msg);
        }
 
        // 3) Account group chat
        if (t.getAccountId() != null) {
            accountRepo.findById(t.getAccountId()).ifPresent(acc -> {
                if (acc.getCTelegramgroupchatid() != null
                        && !acc.getCTelegramgroupchatid().isBlank()) {
                    telegramService.sendMessage(acc.getCTelegramgroupchatid(), msg);
                }
            });
        }
    }
 
    private void notifyUser(String userId, String message) {
        if (userId == null || userId.isBlank()) return;
        userRepo.findById(userId).ifPresent(u -> {
            String chatId = u.getTelegramUsername();
            if (chatId != null && !chatId.isBlank()) {
                telegramService.sendMessage(chatId, message);
            }
        });
    }
 
    private static String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}