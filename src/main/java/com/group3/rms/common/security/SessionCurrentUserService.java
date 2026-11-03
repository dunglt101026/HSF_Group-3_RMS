package com.group3.rms.common.security;

import com.group3.rms.common.exception.ForbiddenException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionCurrentUserService implements CurrentUserService {

    public static final String SESSION_KEY = "CURRENT_USER";
    private final HttpSession httpSession;

    @Override
    public Optional<CurrentUser> get() {
        Object user = httpSession.getAttribute(SESSION_KEY);
        if (user instanceof CurrentUser currentUser) {
            return Optional.of(currentUser);
        }
        return Optional.empty();
    }

    @Override
    public CurrentUser require() {
        return get().orElseThrow(() -> new ForbiddenException("Bạn cần đăng nhập để thực hiện thao tác này."));
    }

    @Override
    public void set(CurrentUser currentUser) {
        httpSession.setAttribute(SESSION_KEY, currentUser);
    }

    @Override
    public void clear() {
        httpSession.removeAttribute(SESSION_KEY);
    }
}
