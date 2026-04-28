package com.cableops.tracker.service;

import com.cableops.tracker.dto.TeamRequest;
import com.cableops.tracker.dto.TeamResponse;
import com.cableops.tracker.entity.Team;
import com.cableops.tracker.entity.User;
import com.cableops.tracker.entity.UserTeam;
import com.cableops.tracker.repository.TeamRepository;
import com.cableops.tracker.repository.UserRepository;
import com.cableops.tracker.repository.UserTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepo;
    private final UserTeamRepository userTeamRepo;
    private final UserRepository userRepo;

    // ── CREATE ────────────────────────────────────────────────────────────────
    @Transactional
    public TeamResponse create(TeamRequest req) {
        Team t = new Team();
        t.setId(UUID.randomUUID().toString());
        map(t, req);
        t.setCreatedAt(LocalDateTime.now());
        teamRepo.save(t);

        saveMembers(t.getId(), t.getName(), req.getUserIds());

        return toResponse(t);
    }

    // ── GET ───────────────────────────────────────────────────────────────────
    public TeamResponse get(String id) {
        Team t = teamRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found: " + id));
        return toResponse(t);
    }

    // ── LIST ──────────────────────────────────────────────────────────────────
    public List<TeamResponse> list() {
        return teamRepo.findAll().stream().map(this::toResponse).toList();
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    @Transactional
    public TeamResponse update(String id, TeamRequest req) {
        Team t = teamRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found: " + id));
        map(t, req);
        teamRepo.save(t);

        // Replace members if userIds is provided
        if (req.getUserIds() != null) {
            userTeamRepo.deleteByTeamId(id);
            saveMembers(id, t.getName(), req.getUserIds());
        }

        return toResponse(t);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @Transactional
    public void delete(String id) {
        if (!teamRepo.existsById(id)) throw new RuntimeException("Team not found: " + id);
        userTeamRepo.deleteByTeamId(id);
        teamRepo.deleteById(id);
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private void map(Team t, TeamRequest r) {
        if (r.getName()     != null) t.setName(r.getName());
        if (r.getAreaCode() != null) t.setAreaCode(r.getAreaCode());
        if (r.getIsActive() != null) t.setIsActive(r.getIsActive());
        else if (t.getIsActive() == null) t.setIsActive(true);
    }

    private void saveMembers(String teamId, String teamName, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        for (String userId : userIds) {
            UserTeam ut = new UserTeam();
            ut.setTeamId(teamId);
            ut.setUserId(userId);
            ut.setTeamName(teamName);
            userTeamRepo.save(ut);
        }
    }

    private TeamResponse toResponse(Team t) {
        TeamResponse res = new TeamResponse();
        res.setId(t.getId());
        res.setName(t.getName());
        res.setAreaCode(t.getAreaCode());
        res.setIsActive(t.getIsActive());
        res.setCreatedAt(t.getCreatedAt());

        // Load members
        List<UserTeam> members = userTeamRepo.findByTeamId(t.getId());
        List<TeamResponse.TeamMember> memberList = new ArrayList<>();
        for (UserTeam ut : members) {
            userRepo.findById(ut.getUserId()).ifPresent(u -> {
                TeamResponse.TeamMember m = new TeamResponse.TeamMember();
                m.setUserId(u.getId());
                m.setUserName(u.getUserName());
                m.setName(u.getName());
                m.setAvatarId(u.getAvatarId());
                memberList.add(m);
            });
        }
        res.setUsers(memberList);
        return res;
    }
}