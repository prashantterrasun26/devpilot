package com.devpilot.controller;

import com.devpilot.dto.ApiResponse;
import com.devpilot.dto.UserDto;
import com.devpilot.entity.User;
import com.devpilot.repository.UserRepository;
import com.devpilot.security.JwtTokenProvider;
import com.devpilot.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated", "UNAUTHORIZED"));
        }

        User user = userPrincipal.getUser();
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .githubId(user.getGithubId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .githubProfileUrl(user.getGithubProfileUrl())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(userDto));
    }

    /**
     * Demo login helper for local testing when GitHub OAuth client keys are in local dev mode.
     */
    @PostMapping("/demo-login")
    public ResponseEntity<ApiResponse<UserDto>> demoLogin(@RequestParam(defaultValue = "demo-developer") String username) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(User.builder()
                        .githubId("demo-" + System.currentTimeMillis())
                        .username(username)
                        .name("Demo Developer")
                        .email("developer@devpilot.local")
                        .avatarUrl("https://avatars.githubusercontent.com/u/583231?v=4")
                        .githubProfileUrl("https://github.com/octocat")
                        .accessToken("demo-access-token")
                        .build()));

        String token = tokenProvider.generateToken(user.getId(), user.getUsername());

        UserDto dto = UserDto.builder()
                .id(user.getId())
                .githubId(user.getGithubId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .githubProfileUrl(user.getGithubProfileUrl())
                .token(token)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Demo login successful", dto));
    }
}
