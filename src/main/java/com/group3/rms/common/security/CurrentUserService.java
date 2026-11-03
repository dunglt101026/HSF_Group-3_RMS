package com.group3.rms.common.security;

import java.util.Optional;

public interface CurrentUserService {

    Optional<CurrentUser> get();

    CurrentUser require();

    void set(CurrentUser currentUser);

    void clear();
}
