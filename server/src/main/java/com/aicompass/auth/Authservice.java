package com.aicompass.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;

  public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
  }

  @Transactional
  public Map<String, Object> signup(String email, String password, String username) {
    if (email == null || email.isBlank() || !email.contains("@")) {
      throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "유효한 이메일을 입력해 주세요.");
    }
    if (password == null || password.length() < 4) {
      throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "비밀번호는 4자 이상이어야 합니다.");
    }
    if (username == null || username.isBlank() || username.trim().length() > 50) {
      throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_USERNAME", "유효한 사용자 이름을 입력해 주세요.");
    }

    String normalizedEmail = email.trim().toLowerCase();
    String trimmedUsername = username.trim();

    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new AuthException(HttpStatus.CONFLICT, "EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다.");
    }
    if (userRepository.existsByUsername(trimmedUsername)) {
      throw new AuthException(HttpStatus.CONFLICT, "USERNAME_DUPLICATED", "이미 사용 중인 이름입니다.");
    }

    String hashedPassword = passwordHasher.hash(password);
    User saved = userRepository.save(new User(trimmedUsername, normalizedEmail, hashedPassword));
    return toUserDto(saved);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> login(String email, String password) {
    if (email == null || email.isBlank() || password == null || password.isBlank()) {
      throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "이메일과 비밀번호를 모두 입력해 주세요.");
    }

    String normalizedEmail = email.trim().toLowerCase();
    User user = userRepository.findByEmail(normalizedEmail)
      .orElseThrow(() -> new AuthException(
        HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."));

    if (!passwordHasher.matches(password, user.getPassword())) {
      throw new AuthException(
        HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    return toUserDto(user);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> checkDuplicate(String email, String username) {
    boolean hasEmail = email != null && !email.isBlank();
    boolean hasUsername = username != null && !username.isBlank();

    if (!hasEmail && !hasUsername) {
      throw new AuthException(
        HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "email 또는 username 파라미터가 필요합니다.");
    }

    Map<String, Object> result = new LinkedHashMap<>();
    if (hasEmail) {
      boolean exists = userRepository.existsByEmail(email.trim().toLowerCase());
      result.put("field", "email");
      result.put("value", email.trim());
      result.put("exists", exists);
      result.put("available", !exists);
    } else {
      boolean exists = userRepository.existsByUsername(username.trim());
      result.put("field", "username");
      result.put("value", username.trim());
      result.put("exists", exists);
      result.put("available", !exists);
    }
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getMe(Integer userId) {
    if (userId == null) {
      throw new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
    }
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "로그인이 필요합니다."));
    return toUserDto(user);
  }

  private Map<String, Object> toUserDto(User user) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("userId", user.getUserId());
    dto.put("username", user.getUsername());
    dto.put("email", user.getEmail());
    dto.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
    return dto;
  }
}
