package com.example.backend.dto.profile;

import java.io.Serializable;

public record ProfileActivityDTO(
        String title,
        String subtitle
) implements Serializable {}