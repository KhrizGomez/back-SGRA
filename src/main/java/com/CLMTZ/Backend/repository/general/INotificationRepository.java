package com.CLMTZ.Backend.repository.general;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.general.Notification;

public interface INotificationRepository extends JpaRepository<Notification, Integer> {

}
