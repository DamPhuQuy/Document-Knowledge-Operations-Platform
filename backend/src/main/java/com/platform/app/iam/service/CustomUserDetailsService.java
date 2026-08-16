package com.platform.app.iam.service;

import com.platform.app.iam.domain.User;
import com.platform.app.iam.repository.UserRepository;
import com.platform.app.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return UserPrincipal.create(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getFullName(),
                user.getRole().name(),
                user.isEnabled()
        );
    }
}
