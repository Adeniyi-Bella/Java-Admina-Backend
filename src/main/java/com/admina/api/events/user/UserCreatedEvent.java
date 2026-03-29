package com.admina.api.events.user;

import com.admina.api.model.user.User;

public record UserCreatedEvent(User user) {}
