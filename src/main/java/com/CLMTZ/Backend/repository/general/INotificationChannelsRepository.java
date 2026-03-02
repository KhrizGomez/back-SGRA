package com.CLMTZ.Backend.repository.general;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.general.NotificationChannels;

public interface INotificationChannelsRepository extends JpaRepository<NotificationChannels, Integer> {

}
