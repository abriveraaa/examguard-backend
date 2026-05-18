package com.example.backend.service.admin;

import com.example.backend.dto.admin.monitoring.*;
import com.example.backend.repository.admin.AdminMonitoringRepository;
import com.example.backend.repository.admin.AdminViolationMonitoringRepository;
import com.example.backend.repository.core.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMonitoringService {

    private final AdminMonitoringRepository monitoringRepository;
    private final AdminViolationMonitoringRepository violationRepository;
    private final UserSessionLogRepository sessionLogRepository;
    private final UserAccessLogRepository accessLogRepository;
    private final AccountStatusLogRepository accountStatusLogRepository;
    private final RegistrarSyncLogRepository registrarSyncLogRepository;
    private final ReactivationLogRepository reactivationLogRepository;

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    public MonitoringOverviewResponse getOverview(MonitoringFilterRequest filter) {

        ResolvedFilter resolved = resolveFilter(filter);

        MonitoringOverviewResponse response = new MonitoringOverviewResponse();

        List<MetricCardDto> cards = new ArrayList<>();
        cards.add(monitoringRepository.countActivities(resolved.startDate(), resolved.endDate()));
        cards.add(violationRepository.countViolations(resolved.startDate(), resolved.endDate()));
        cards.add(monitoringRepository.countCriticalEvents(resolved.startDate(), resolved.endDate()));

        response.setSummaryCards(cards);

        List<Object[]> activityRows = switch (resolved.dateFormat()) {
            case "YYYY" -> monitoringRepository.activityVolumeByYear(
                    resolved.startDate(),
                    resolved.endDate(),
                    normalizeRole(filter.getRole())
            );

            case "YYYY-MM-DD" -> monitoringRepository.activityVolumeByDay(
                    resolved.startDate(),
                    resolved.endDate(),
                    normalizeRole(filter.getRole())
            );

            case "YYYY-MM-DD HH24:00", "HH24:00" -> monitoringRepository.activityVolumeByHour(
                    resolved.startDate(),
                    resolved.endDate(),
                    normalizeRole(filter.getRole())
            );

            case "YYYY-MM" -> monitoringRepository.activityVolumeByMonth(
                    resolved.startDate(),
                    resolved.endDate(),
                    normalizeRole(filter.getRole())
            );

            default -> monitoringRepository.activityVolumeByMonth(
                    resolved.startDate(),
                    resolved.endDate(),
                    normalizeRole(filter.getRole())
            );
        };

        List<ChartPointDto> activityVolume = activityRows.stream()
                .map(row -> new ChartPointDto(
                        String.valueOf(row[0]),
                        String.valueOf(row[1]),
                        ((Number) row[2]).longValue()
                ))
                .toList();

        response.setActivityVolume(activityVolume);

        List<ChartPointDto> violationsByType = violationRepository
                .violationsByTypeRaw(
                        resolved.startDate(),
                        resolved.endDate()
                )
                .stream()
                .map(row -> new ChartPointDto(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .toList();

        response.setViolationsByType(violationsByType);

        List<ChartPointDto> violationsByProgram = violationRepository
                .violationsByProgramRaw(
                        resolved.startDate(),
                        resolved.endDate(),
                        filter.getProgramCode(),
                        filter.getCollegeCode()
                )
                .stream()
                .map(row -> new ChartPointDto(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .toList();

        response.setViolationsByProgram(violationsByProgram);

        response.setRecentCriticalEvents(
                monitoringRepository.recentCriticalEvents(
                        resolved.startDate(),
                        resolved.endDate(),
                        PageRequest.of(0, 10)
                )
        );

        return response;
    }

    private ResolvedFilter resolveFilter(MonitoringFilterRequest filter) {

        OffsetDateTime start = OffsetDateTime.of(
                2000,
                1,
                1,
                0,
                0,
                0,
                0,
                ZoneOffset.UTC
        );

        OffsetDateTime end = OffsetDateTime.now(ZoneOffset.UTC);

        String range = filter.getRange() == null ? "Entire Period" : filter.getRange();

        ZonedDateTime now = ZonedDateTime.now(MANILA);

        switch (range) {
            case "Today" -> {
                start = now.toLocalDate().atStartOfDay(MANILA).toOffsetDateTime();
                end = now.toLocalDate().atTime(LocalTime.MAX).atZone(MANILA).toOffsetDateTime();
            }

            case "This Month" -> {
                LocalDate firstDay = now.toLocalDate().withDayOfMonth(1);
                LocalDate lastDay = now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth());

                start = firstDay.atStartOfDay(MANILA).toOffsetDateTime();
                end = lastDay.atTime(LocalTime.MAX).atZone(MANILA).toOffsetDateTime();
            }

            case "Custom Range" -> {
                start = filter.getStartDate();
                end = filter.getEndDate();
            }

            case "Entire Term", "Entire Period" -> {
                if (filter.getStartDate() != null) {
                    start = filter.getStartDate();
                }

                if (filter.getEndDate() != null) {
                    end = filter.getEndDate();
                }
            }

            default -> {
                if (filter.getStartDate() != null) {
                    start = filter.getStartDate();
                }

                if (filter.getEndDate() != null) {
                    end = filter.getEndDate();
                }
            }
        }

        String dateFormat = resolveDateFormat(filter.getGroupBy(), start, end);

        return new ResolvedFilter(start, end, dateFormat);
    }

    public AdminMonitoringLogsResponse getLogs(AdminMonitoringLogsRequest request) {

        ResolvedFilter resolved = resolveFilterFromLogsRequest(request);

        String source = normalizeSource(request.getSource());
        String role = normalizeRole(request.getRole());
        String severity = normalizeSeverity(request.getSeverity());
        String search = normalizeSearch(request.getSearch());

        List<AdminLogRowDto> merged = new ArrayList<>();

        if (source.equals("ALL") || source.equals("SYSTEM")) {
            merged.addAll(
                    monitoringRepository.findSystemLogsForMonitoring(
                            resolved.startDate(),
                            resolved.endDate(),
                            role,
                            search
                    )
            );
        }

        if (source.equals("ALL") || source.equals("VIOLATION")) {
            merged.addAll(
                    violationRepository.findViolationLogsForMonitoringRaw(
                                    resolved.startDate(),
                                    resolved.endDate(),
                                    severity,
                                    search
                            )
                            .stream()
                            .map(this::mapViolationLogRow)
                            .toList()
            );
        }

        if (source.equals("ALL") || source.equals("SESSION")) {
            merged.addAll(
                    sessionLogRepository.findSessionLogsForMonitoring(
                            resolved.startDate(),
                            resolved.endDate(),
                            search
                    )
            );
        }

        if (source.equals("ALL") || source.equals("ACCESS")) {
            merged.addAll(
                    accessLogRepository.findAccessLogsForMonitoring(
                            resolved.startDate(),
                            resolved.endDate(),
                            search
                    )
            );
        }


        if (source.equals("ALL") || source.equals("REGISTRAR")) {
            merged.addAll(
                    registrarSyncLogRepository.findRegistrarLogsForMonitoring(
                            resolved.startDate(),
                            resolved.endDate(),
                            search
                    )
            );
        }

        if (source.equals("ALL") || source.equals("ACCOUNT")) {
            merged.addAll(
                    accountStatusLogRepository.findAccountLogsForMonitoring(
                            resolved.startDate(),
                            resolved.endDate(),
                            search
                    )
            );

            merged.addAll(
                    reactivationLogRepository.findReactivationLogsForMonitoring(
                            resolved.startDate(),
                            resolved.endDate(),
                            search
                    )
            );
        }

        if (source.equals("REACTIVATION")) {
            merged.addAll(
                    reactivationLogRepository.findReactivationLogsForMonitoring(
                            resolved.startDate(),
                            resolved.endDate(),
                            search
                    )
            );
        }

        merged.sort((a, b) -> {
            if (a.getStartedAt() == null && b.getStartedAt() == null) return 0;
            if (a.getStartedAt() == null) return 1;
            if (b.getStartedAt() == null) return -1;

            return b.getStartedAt().compareTo(a.getStartedAt());
        });

        int page = Math.max(request.getPage(), 0);
        int size = request.getSize() <= 0 ? 20 : Math.min(request.getSize(), 100);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, merged.size());

        List<AdminLogRowDto> content =
                fromIndex >= merged.size()
                        ? List.of()
                        : merged.subList(fromIndex, toIndex);

        long totalElements = merged.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page + 1 < totalPages;

        return new AdminMonitoringLogsResponse(
                content,
                totalElements,
                totalPages,
                page,
                size,
                hasNext
        );
    }

    private String resolveDateFormat(String groupBy, OffsetDateTime start, OffsetDateTime end) {

        if (groupBy == null || groupBy.isBlank() || groupBy.equals("Auto")) {

            if (start == null || end == null) {
                return "YYYY-MM";
            }

            long days = Duration.between(start, end).toDays();

            if (days <= 1) {
                return "HH24:00";
            }

            if (days <= 60) {
                return "YYYY-MM-DD";
            }

            if (days <= 730) {
                return "YYYY-MM";
            }

            return "YYYY";
        }

        return switch (groupBy) {
            case "Hour" -> "YYYY-MM-DD HH24:00";
            case "Day" -> "YYYY-MM-DD";
            case "Month" -> "YYYY-MM";
            case "Year" -> "YYYY";
            default -> "YYYY-MM";
        };
    }

    private record ResolvedFilter(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String dateFormat
    ) {}

    private String normalizeRole(String role) {
        return role == null || role.isBlank() ? "All Roles" : role;
    }

    private ResolvedFilter resolveFilterFromLogsRequest(AdminMonitoringLogsRequest request) {
        MonitoringFilterRequest filter = new MonitoringFilterRequest();

        filter.setRange(request.getRange());
        filter.setStartDate(request.getStartDate());
        filter.setEndDate(request.getEndDate());

        return resolveFilter(filter);
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "ALL";
        }
        return source.toUpperCase();
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "All Severities";
        }
        return severity;
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return search.trim();
    }

    private AdminLogRowDto mapViolationLogRow(Object[] row) {
        return new AdminLogRowDto(
                string(row[0]),              // source
                offsetDateTime(row[1]),      // startedAt
                offsetDateTime(row[2]),      // endedAt
                string(row[3]),              // actorId
                string(row[4]),              // actorRole
                string(row[5]),              // targetUserId
                string(row[6]),              // targetRole
                string(row[7]),              // module
                string(row[8]),              // action
                string(row[9]),              // status
                string(row[10]),             // message
                longValue(row[11]),          // examId
                longValue(row[12]),          // attemptId
                longValue(row[13]),          // questionId
                longValue(row[14]),          // durationMs
                string(row[15]),             // programCode
                string(row[16]),             // programName
                string(row[17]),             // section
                string(row[18]),             // severity
                string(row[19]),             // courseCode
                string(row[20]),             // examTitle
                intValue(row[21])            // questionNumber
        );
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        return ((Number) value).longValue();
    }

    private Integer intValue(Object value) {
        if (value == null) return null;
        return ((Number) value).intValue();
    }

    private OffsetDateTime offsetDateTime(Object value) {
        if (value == null) return null;

        if (value instanceof OffsetDateTime odt) {
            return odt;
        }

        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atOffset(ZoneOffset.UTC);
        }

        if (value instanceof java.time.LocalDateTime ldt) {
            return ldt.atOffset(ZoneOffset.UTC);
        }

        return OffsetDateTime.parse(String.valueOf(value));
    }
}