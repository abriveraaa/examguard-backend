package com.example.backend.utility;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public class TimeUtil {

    private static final ZoneId PH_ZONE = ZoneId.of("Asia/Manila");

    public static OffsetDateTime now() {
        return OffsetDateTime.now(PH_ZONE);
    }
}