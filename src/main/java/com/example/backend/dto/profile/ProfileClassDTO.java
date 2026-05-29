package com.example.backend.dto.profile;

import java.io.Serializable;

public record ProfileClassDTO(
        String title,
        String subtitle
) implements Serializable {}