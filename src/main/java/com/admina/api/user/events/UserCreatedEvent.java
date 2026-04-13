package com.admina.api.user.events;

import com.admina.api.user.model.User;

public record UserCreatedEvent(User user) {}
