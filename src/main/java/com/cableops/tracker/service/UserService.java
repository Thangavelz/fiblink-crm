package com.cableops.tracker.service;

import com.cableops.tracker.dto.*;
import com.cableops.tracker.entity.*;
import com.cableops.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

	private static final List<String> ALLOWED_TYPES =
			List.of("ADMIN", "Manager", "Field Engineer");

	private final UserRepository       repo;
	private final UserTeamRepository   userTeamRepo;
	private final UserRoleRepository   userRoleRepo;
	private final UserDocumentRepository userDocRepo;
	private final PasswordEncoder      passwordEncoder;

	// ── CREATE ────────────────────────────────────────────────────────────────
	public UserResponse create(UserRequest req) {

		if (req.getPassword() == null || !req.getPassword().equals(req.getPasswordConfirm())) {
			throw new RuntimeException("Password mismatch");
		}
		if (repo.existsByUserName(req.getUserName())) {
			throw new RuntimeException("Username '" + req.getUserName() + "' already exists");
		}

		validateType(req.getType());
		validateMandatoryFields(req);

		User u = buildUser(new User(), req);
		u.setId(UUID.randomUUID().toString());
		u.setPassword(passwordEncoder.encode(req.getPassword()));
		u.setCreatedAt(LocalDateTime.now());
		u.setModifiedAt(LocalDateTime.now());

		repo.save(u);
		saveJoinTables(u.getId(), req);

		return mapResponse(u, req);
	}

	// ── GET ───────────────────────────────────────────────────────────────────
	public UserResponse get(String id) {
		User u = repo.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found: " + id));
		return mapResponseFromEntity(u);
	}

	// ── LIST ──────────────────────────────────────────────────────────────────
	public List<UserResponse> list() {
		return repo.findAll().stream().map(this::mapResponseFromEntity).toList();
	}

	// ── UPDATE ────────────────────────────────────────────────────────────────
	public UserResponse update(String id, UserRequest req) {

		User u = repo.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found: " + id));

		validateType(req.getType());
		validateMandatoryFields(req);

		buildUser(u, req);
		u.setModifiedAt(LocalDateTime.now());
		repo.save(u);

		if (req.getTeamsIds() != null || req.getCProofDocumentsIds() != null) {
			saveJoinTables(id, req);
		}

		return mapResponseFromEntity(u);
	}

	// ── DELETE ────────────────────────────────────────────────────────────────
	public void delete(String id) {
		if (!repo.existsById(id)) throw new RuntimeException("User not found: " + id);
		repo.deleteById(id);
	}

	// ── PRIVATE HELPERS ───────────────────────────────────────────────────────

	private void validateType(String type) {
		if (type != null && !ALLOWED_TYPES.contains(type.trim())) {
			throw new RuntimeException(
					"Invalid type '" + type + "'. Allowed: " + ALLOWED_TYPES);
		}
	}

	/**
	 * Validates fields that are mandatory from a business perspective.
	 * Called on both create and update so neither path can bypass them.
	 */
	private void validateMandatoryFields(UserRequest req) {
		List<String> missing = new ArrayList<>();

		if (isBlank(req.getCTelegramUsername()))       missing.add("Telegram Username");
		if (isBlank(req.getCFathername()))             missing.add("Father / Guardian Name");
		if (isBlank(req.getCAadharNumber()))           missing.add("Aadhar Number");
		else if (!req.getCAadharNumber().matches("\\d{12}"))
			throw new RuntimeException("Aadhar Number must be exactly 12 digits");
		if (isBlank(req.getCEmergencyContactNumber())) missing.add("Emergency Contact Number");

		// On create, proof documents must be supplied
		if (req.getCProofDocumentsIds() == null || req.getCProofDocumentsIds().isEmpty()) {
			missing.add("Proof Documents (at least one required)");
		}

		if (!missing.isEmpty()) {
			throw new RuntimeException("Missing required fields: " + String.join(", ", missing));
		}
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private User buildUser(User u, UserRequest req) {
		if (req.getUserName()               != null) u.setUserName(req.getUserName());
		if (req.getSalutationName()         != null) u.setSalutationName(req.getSalutationName());
		if (req.getFirstName()              != null) u.setFirstName(req.getFirstName());
		if (req.getLastName()               != null) u.setLastName(req.getLastName());
		if (req.getName()                   != null) {
			u.setName(req.getName());
		} else {
			String sal   = coalesce(req.getSalutationName(),          u.getSalutationName());
			String first = coalesce(req.getFirstName(),               u.getFirstName());
			String last  = coalesce(req.getLastName(),                u.getLastName());
			String computed = (sal + " " + first + " " + last).trim();
			if (!computed.isEmpty()) u.setName(computed);
		}
		if (req.getType()                   != null) u.setType(req.getType().trim());
		if (req.getIsActive()               != null) u.setIsActive(req.getIsActive());
		else if (u.getIsActive()            == null) u.setIsActive(true);
		if (req.getTitle()                  != null) u.setTitle(req.getTitle());
		if (req.getGender()                 != null) u.setGender(req.getGender());
		if (req.getCTelegramUsername()      != null) u.setTelegramUsername(req.getCTelegramUsername());
		if (req.getCFathername()            != null) u.setFatherName(req.getCFathername());
		if (req.getCAadharNumber()          != null) u.setAadharNumber(req.getCAadharNumber());
		if (req.getCEmergencyContactNumber()!= null) u.setEmergencyContactNumber(req.getCEmergencyContactNumber());
		if (req.getCDateofjoining()         != null) u.setDateOfJoining(req.getCDateofjoining());
		if (req.getAvatarId()               != null) u.setAvatarId(req.getAvatarId());
		if (req.getAvatarName()             != null) u.setAvatarName(req.getAvatarName());
		return u;
	}

	private static String coalesce(String a, String b) {
		return a != null ? a : (b != null ? b : "");
	}

	/**
	 * Saves UserTeam and UserDocument join rows.
	 * Roles are intentionally omitted — the "type" field covers access control.
	 * On update: existing rows for the user are replaced when the list is supplied.
	 */
	private void saveJoinTables(String userId, UserRequest req) {

		if (req.getTeamsIds() != null) {
			// Delete old team memberships for this user, then re-insert
			userTeamRepo.findByUserId(userId)
					.forEach(ut -> userTeamRepo.deleteById(ut.getId()));

			for (String teamId : req.getTeamsIds()) {
				String teamName = req.getTeamsNames() != null
						? req.getTeamsNames().get(teamId) : null;
				userTeamRepo.save(new UserTeam(null, userId, teamId, teamName));
			}
		}

		if (req.getCProofDocumentsIds() != null) {
			for (String docId : req.getCProofDocumentsIds()) {
				String docName = req.getCProofDocumentsNames() != null
						? req.getCProofDocumentsNames().get(docId) : null;
				String docType = req.getCProofDocumentsTypes() != null
						? req.getCProofDocumentsTypes().get(docId) : null;
				// saveJoinTables uses merge-by-id so duplicate uploads don't create duplicate rows
				if (!userDocRepo.existsById(docId)) {
					userDocRepo.save(new UserDocument(docId, userId, docName, docType));
				}
			}
		}
	}

	/** Used after CREATE — req already has teams/docs in memory. */
	private UserResponse mapResponse(User u, UserRequest req) {
		UserResponse res = baseFields(u);
		res.setTeamsIds(req.getTeamsIds());
		res.setTeamsNames(req.getTeamsNames());
		res.setCProofDocumentsIds(req.getCProofDocumentsIds());
		res.setCProofDocumentsNames(req.getCProofDocumentsNames());
		res.setCProofDocumentsTypes(req.getCProofDocumentsTypes());
		return res;
	}

	/** Used for GET / LIST — reads join tables from DB. */
	private UserResponse mapResponseFromEntity(User u) {
		UserResponse res = baseFields(u);

		List<UserTeam> teams = userTeamRepo.findByUserId(u.getId());
		res.setTeamsIds(teams.stream().map(UserTeam::getTeamId).toList());
		res.setTeamsNames(teams.stream().filter(t -> t.getTeamName() != null)
				.collect(Collectors.toMap(UserTeam::getTeamId, UserTeam::getTeamName)));

		List<UserDocument> docs = userDocRepo.findByUserId(u.getId());
		res.setCProofDocumentsIds(docs.stream().map(UserDocument::getId).toList());
		res.setCProofDocumentsNames(docs.stream().filter(d -> d.getFileName() != null)
				.collect(Collectors.toMap(UserDocument::getId, UserDocument::getFileName)));
		res.setCProofDocumentsTypes(docs.stream().filter(d -> d.getFileType() != null)
				.collect(Collectors.toMap(UserDocument::getId, UserDocument::getFileType)));

		return res;
	}

	private UserResponse baseFields(User u) {
		UserResponse res = new UserResponse();
		res.setId(u.getId());
		res.setUserName(u.getUserName());
		res.setSalutationName(u.getSalutationName());
		res.setFirstName(u.getFirstName());
		res.setLastName(u.getLastName());
		res.setName(u.getName());
		res.setType(u.getType());
		res.setIsActive(u.getIsActive());
		res.setTitle(u.getTitle());
		res.setGender(u.getGender());
		res.setCTelegramUsername(u.getTelegramUsername());
		res.setCFathername(u.getFatherName());
		res.setCAadharNumber(u.getAadharNumber());
		res.setCEmergencyContactNumber(u.getEmergencyContactNumber());
		res.setCDateofjoining(u.getDateOfJoining());
		res.setAvatarId(u.getAvatarId());
		res.setAvatarName(u.getAvatarName());
		res.setCreatedAt(u.getCreatedAt());
		res.setModifiedAt(u.getModifiedAt());
		return res;
	}
}