package com.devpilot.security;

import com.devpilot.entity.User;
import com.devpilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user: ", ex);
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String githubId = String.valueOf(attributes.get("id"));
        String login = (String) attributes.get("login");
        String name = (String) attributes.get("name");
        String email = (String) attributes.get("email");
        String avatarUrl = (String) attributes.get("avatar_url");
        String htmlUrl = (String) attributes.get("html_url");
        String accessToken = userRequest.getAccessToken().getTokenValue();

        User user = userRepository.findByGithubId(githubId)
                .map(existingUser -> {
                    existingUser.setUsername(login);
                    existingUser.setName(name != null ? name : login);
                    if (email != null) existingUser.setEmail(email);
                    existingUser.setAvatarUrl(avatarUrl);
                    existingUser.setGithubProfileUrl(htmlUrl);
                    existingUser.setAccessToken(accessToken);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .githubId(githubId)
                        .username(login)
                        .name(name != null ? name : login)
                        .email(email)
                        .avatarUrl(avatarUrl)
                        .githubProfileUrl(htmlUrl)
                        .accessToken(accessToken)
                        .build()));

        return new UserPrincipal(user, attributes);
    }
}
