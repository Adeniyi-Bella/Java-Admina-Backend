package com.admina.api.events.user;

import com.admina.api.model.User;

public record UserCreatedEvent(User user) {}
