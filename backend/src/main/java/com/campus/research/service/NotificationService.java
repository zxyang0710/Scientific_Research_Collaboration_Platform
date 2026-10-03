package com.campus.research.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.research.common.PageResult;
import com.campus.research.entity.Notification;
import com.campus.research.mapper.NotificationMapper;
import com.campus.research.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public void send(Long receiverId, Long senderId, String title, String content,
                     String type, Long businessId, String link) {
        if (receiverId == null || receiverId.equals(senderId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setSenderId(senderId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setBusinessId(businessId);
        notification.setLink(link);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    public PageResult<Notification> list(long page, long size, Boolean unreadOnly) {
        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, size),
                Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getReceiverId, SecurityUtils.currentUserId())
                        .eq(Boolean.TRUE.equals(unreadOnly), Notification::getIsRead, 0)
                        .orderByDesc(Notification::getCreatedAt));
        return PageResult.from(result);
    }

    public long unreadCount() {
        return notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getReceiverId, SecurityUtils.currentUserId())
                .eq(Notification::getIsRead, 0));
    }

    @Transactional
    public void markRead(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification != null && notification.getReceiverId().equals(SecurityUtils.currentUserId())) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }
    }

    @Transactional
    public void markAllRead() {
        List<Notification> notifications = notificationMapper.selectList(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getReceiverId, SecurityUtils.currentUserId())
                .eq(Notification::getIsRead, 0));
        notifications.forEach(notification -> {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        });
    }

    @Transactional
    public void delete(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification != null && notification.getReceiverId().equals(SecurityUtils.currentUserId())) {
            notificationMapper.deleteById(id);
        }
    }
}
