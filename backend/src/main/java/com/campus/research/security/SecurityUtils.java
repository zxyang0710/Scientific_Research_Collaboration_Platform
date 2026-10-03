package com.campus.research.security;

import com.campus.research.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(401, "请先登录");
        }
        return loginUser;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(currentUser().getRole());
    }
}
