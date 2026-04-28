package com.cableops.tracker.service;

import com.cableops.tracker.entity.Task;
import com.cableops.tracker.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AcceptanceSlaScheduler {

    private final TaskRepository     taskRepo;
    private final TelegramService    telegramService;
    private final com.cableops.tracker.repository.UserRepository    userRepo;
    private final com.cableops.tracker.repository.AccountRepository accountRepo;

    /**
     * Runs every minute.
     * Finds tasks whose acceptance SLA window has expired and sends a one-time alert.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void checkAcceptanceSla() {
        List<Task> tasks = taskRepo.findPendingAcceptanceCheck();
        if (tasks.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();

        for (Task t : tasks) {
            if (t.getCreatedAt() == null || t.getAcceptanceTimeMins() == null) continue;

            LocalDateTime deadline = t.getCreatedAt()
                .plusMinutes(t.getAcceptanceTimeMins());

            if (now.isAfter(deadline)) {
                long overdueMins = java.time.Duration.between(deadline, now).toMinutes();
                sendOverdueAlert(t, overdueMins);

                // Mark alerted so we don't send again
                t.setAcceptanceAlerted(true);
                taskRepo.save(t);

                log.info("Acceptance SLA overdue alert sent for task {} ({})",
                    t.getId(), t.getCSRNumber());
            }
        }
    }

    private void sendOverdueAlert(Task t, long overdueMins) {
        String msg = "<b>⏰ Acceptance SLA Overdue</b>\n\n"
            + "<b>SR No:</b> "      + coalesce(t.getCSRNumber(), "—")      + "\n"
            + "<b>Task:</b> "       + coalesce(t.getName(), "—")           + "\n"
            + "<b>Account:</b> "    + coalesce(t.getAccountName(), "—")    + "\n"
            + "<b>Circuit:</b> "    + coalesce(t.getParentName(), "—")     + "\n"
            + "<b>SLA Window:</b> " + t.getAcceptanceTimeMins() + " mins"  + "\n"
            + "<b>Overdue by:</b> " + overdueMins + " min(s)\n"
            + "\n<i>Please accept or escalate this task immediately.</i>";

        // Notify assigned user
        if (t.getAssignedUserId() != null) {
            userRepo.findById(t.getAssignedUserId()).ifPresent(u -> {
                if (u.getTelegramUsername() != null && !u.getTelegramUsername().isBlank()) {
                    telegramService.sendMessage(u.getTelegramUsername(), msg);
                }
            });
        }

        // Also notify account group chat
        if (t.getAccountId() != null) {
            accountRepo.findById(t.getAccountId()).ifPresent(acc -> {
                if (acc.getCTelegramgroupchatid() != null
                        && !acc.getCTelegramgroupchatid().isBlank()) {
                    telegramService.sendMessage(acc.getCTelegramgroupchatid(), msg);
                }
            });
        }
    }

    private static String coalesce(String a, String b) {
        return a != null ? a : b;
    }
}