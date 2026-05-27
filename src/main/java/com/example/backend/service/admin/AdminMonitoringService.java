package com.example.backend.service.admin;

import com.example.backend.dto.admin.monitoring.*;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.repository.admin.AdminMonitoringRepository;
import com.example.backend.repository.admin.AdminViolationMonitoringRepository;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.report.admin.MonitoringLogsExcelExporter;
import com.example.backend.report.admin.MonitoringLogsPdfExporter;
import com.example.backend.repository.core.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final ExamCameraSessionRepository examCameraSessionRepository;
    private final MonitoringLogsPdfExporter monitoringPdfExporter;
    private final MonitoringLogsExcelExporter monitoringExcelExporter;

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    public MonitoringOverviewResponse getOverview(MonitoringFilterRequest filter) {

        ResolvedFilter resolved = resolveFilter(filter);

        MonitoringOverviewResponse response = new MonitoringOverviewResponse();

        List<MetricCardDto> cards = new ArrayList<>();

        OffsetDateTime now = ZonedDateTime.now(MANILA)
                .toOffsetDateTime();

        cards.add(monitoringRepository.countActivities(
                resolved.startDate(),
                resolved.endDate()
        ));

        cards.add(violationRepository.countViolations(
                resolved.startDate(),
                resolved.endDate()
        ));

        cards.add(monitoringRepository.countAttentionEvents(
                resolved.startDate(),
                resolved.endDate()
        ));

        cards.add(sessionLogRepository.countActiveSessions(now));

        cards.add(sessionLogRepository.countSessionVolume(
                resolved.startDate(),
                resolved.endDate()
        ));

        response.setSummaryCards(cards);

        String role =
                normalizeRole(
                        filter.getRole()
                );

        /*
         * LOGIN VOLUME
         * Source: user_access_log
         * event_type = LOGIN
         * event_status = SUCCESS
         */
        List<Object[]> loginRows = switch (resolved.dateFormat()) {

            case "YYYY" -> accessLogRepository.loginVolumeByYear(
                    resolved.startDate(),
                    resolved.endDate(),
                    role
            );

            case "YYYY-MM-DD" -> accessLogRepository.loginVolumeByDay(
                    resolved.startDate(),
                    resolved.endDate(),
                    role
            );

            case "YYYY-MM-DD HH24:00", "HH24:00" -> accessLogRepository.loginVolumeByHour(
                    resolved.startDate(),
                    resolved.endDate(),
                    role
            );

            default -> accessLogRepository.loginVolumeByMonth(
                    resolved.startDate(),
                    resolved.endDate(),
                    role
            );
        };

        List<ChartPointDto> loginVolume =
                loginRows.stream()
                        .map(row -> new ChartPointDto(
                                String.valueOf(row[0]),
                                String.valueOf(row[1]),
                                ((Number) row[2]).longValue()
                        ))
                        .toList();

        if ("HH24:00".equals(resolved.dateFormat())) {
            loginVolume = fillMissingHourlyPoints(
                    loginVolume,
                    resolved.startDate(),
                    resolved.endDate()
            );
        }

        response.setLoginVolume(loginVolume);

        /*
         * CONCURRENT USERS
         * Source: user_session_log
         */
        List<Object[]> concurrentRows = switch (resolved.dateFormat()) {

            case "YYYY" -> sessionLogRepository.concurrentUsersByYear(
                    resolved.startDate(),
                    resolved.endDate(),
                    role
            );

            case "YYYY-MM-DD" -> sessionLogRepository.concurrentUsersByDay(
                    resolved.startDate(),
                    resolved.endDate(),
                    role
            );

            case "YYYY-MM-DD HH24:00", "HH24:00" -> sessionLogRepository.concurrentUsersByHour(
                    resolved.startDate(),
                    resolved.endDate(),
                    role
            );

            default -> sessionLogRepository.concurrentUsersByMonth(
                    resolved.startDate(),
                    resolved.endDate(),
                    role
            );
        };

        List<ChartPointDto> concurrentUsers =
                concurrentRows.stream()
                        .map(row -> new ChartPointDto(
                                String.valueOf(row[0]),
                                String.valueOf(row[1]),
                                ((Number) row[2]).longValue()
                        ))
                        .toList();

        if ("HH24:00".equals(resolved.dateFormat())) {
            concurrentUsers = fillMissingHourlyPoints(
                    concurrentUsers,
                    resolved.startDate(),
                    resolved.endDate()
            );
        }

        response.setConcurrentUsers(concurrentUsers);

        /*
         * Temporary backward compatibility.
         * Remove this later after frontend fully uses loginVolume/concurrentUsers.
         */
        response.setActivityVolume(loginVolume);

        List<ChartPointDto> violationsByType = violationRepository
                .violationsByTypeRaw(
                        resolved.startDate(),
                        resolved.endDate()
                )
                .stream()
                .map(row -> new ChartPointDto(
                        String.valueOf(row[0]),
                        String.valueOf(row[1]),
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
                        String.valueOf(row[0]),
                        String.valueOf(row[1]),
                        ((Number) row[2]).longValue()
                ))
                .toList();

        response.setViolationsByProgram(violationsByProgram);

        response.setRecentCriticalEvents(
                monitoringRepository.recentAttentionSystemEvents(
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


    // ==========
    // REPORT
    // =========


    public byte[] exportLogs(AdminMonitoringLogsRequest request, UserAccess user) {
        request.setPage(0);
        request.setSize(100000);

        AdminMonitoringLogsResponse response = getLogs(request);

        String format = request.getFormat() == null ? "PDF" : request.getFormat().toUpperCase();

        if ("EXCEL".equals(format)) {
            return monitoringExcelExporter.export(response.getContent(), request, user);
        }

        return monitoringPdfExporter.export(response.getContent(), request, user);
    }


    // ==========
    // HELPER
    // ==========

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

        if (source.equals("ALL") || source.equals("CAMERA")) {
            merged.addAll(
                    examCameraSessionRepository.findCameraLogsForMonitoring(
                            resolved.startDate(),
                            resolved.endDate(),
                            search
                    )
            );
        }

        AdminMonitoringFilterOptionsDto filterOptions = buildFilterOptions(merged);

        merged = new ArrayList<>(applyColumnFilters(merged, request));

        merged.sort((a, b) -> {
            OffsetDateTime aTime = a.getStartedAt();
            OffsetDateTime bTime = b.getStartedAt();

            if (aTime == null && bTime == null) return 0;
            if (aTime == null) return 1;
            if (bTime == null) return -1;

            return bTime.compareTo(aTime);
        });

        int page = Math.max(request.getPage(), 0);
        int size = request.getSize() <= 0 ? 20 : Math.min(request.getSize(), 100);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, merged.size());

        List<AdminLogRowDto> content =
                fromIndex >= merged.size()
                        ? new ArrayList<>()
                        : new ArrayList<>(merged.subList(fromIndex, toIndex));

        long totalElements = merged.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page + 1 < totalPages;

        return new AdminMonitoringLogsResponse(
                content,
                filterOptions,
                totalElements,
                totalPages,
                page,
                size,
                hasNext
        );
    }

    private AdminMonitoringFilterOptionsDto buildFilterOptions(List<AdminLogRowDto> rows) {
        AdminMonitoringFilterOptionsDto options = new AdminMonitoringFilterOptionsDto();

        options.setRoles(distinct(rows.stream().map(AdminLogRowDto::getActorRole).toList()));
        options.setStatuses(distinct(rows.stream().map(AdminLogRowDto::getStatus).toList()));
        options.setActions(distinct(rows.stream().map(AdminLogRowDto::getAction).toList()));
        options.setModules(distinct(rows.stream().map(AdminLogRowDto::getModule).toList()));
        options.setSeverities(distinct(rows.stream().map(AdminLogRowDto::getSeverity).toList()));

        options.setViolationTypes(
                distinct(
                        rows.stream()
                                .filter(row -> "VIOLATION".equalsIgnoreCase(row.getSource()))
                                .map(AdminLogRowDto::getAction)
                                .toList()
                )
        );

        options.setCameraStatuses(
                distinct(
                        rows.stream()
                                .filter(row -> "CAMERA".equalsIgnoreCase(row.getSource()))
                                .map(AdminLogRowDto::getStatus)
                                .toList()
                )
        );

        options.setCameraDeviceTypes(
                distinct(
                        rows.stream()
                                .filter(row -> "CAMERA".equalsIgnoreCase(row.getSource()))
                                .map(AdminLogRowDto::getAction)
                                .toList()
                )
        );

        return options;
    }

    private List<String> distinct(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }

    private List<AdminLogRowDto> applyColumnFilters(
            List<AdminLogRowDto> rows,
            AdminMonitoringLogsRequest request
    ) {
        String source = normalizeSource(request.getSource());

        String role = normalizeDropdown(request.getRole());
        String severity = normalizeDropdown(request.getSeverity());
        String status = normalizeDropdown(request.getStatus());
        String module = normalizeDropdown(request.getModule());
        String action = normalizeDropdown(request.getAction());
        String violationType = normalizeDropdown(request.getViolationType());

        return new ArrayList<>(
                rows.stream()
                        .filter(row -> matches(role, row.getActorRole()))
                        .filter(row -> matches(status, row.getStatus()))
                        .filter(row -> {
                            if (!"VIOLATION".equals(source)) return true;
                            return matches(severity, row.getSeverity())
                                    && matches(violationType, row.getAction());
                        })
                        .filter(row -> {
                            if (!"SYSTEM".equals(source)) return true;
                            return matches(module, row.getModule());
                        })
                        .filter(row -> {
                            if (!"ACCESS".equals(source)
                                    && !"ACCOUNT".equals(source)
                                    && !"REGISTRAR".equals(source)) {
                                return true;
                            }
                            return matches(action, row.getAction());
                        })
                        .toList()
        );
    }

    private List<ChartPointDto> fillMissingHourlyPoints(
            List<ChartPointDto> points,
            OffsetDateTime start,
            OffsetDateTime end
    ) {
        List<String> roles = List.of("ADMIN", "FACULTY", "STUDENT");

        Map<String, Long> existing = new java.util.HashMap<>();

        for (ChartPointDto point : points) {
            existing.put(
                    point.getLabel() + "|" + point.getCategory(),
                    point.getValue()
            );
        }

        List<ChartPointDto> filled = new ArrayList<>();

        OffsetDateTime cursor = start
                .atZoneSameInstant(MANILA)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toOffsetDateTime();

        OffsetDateTime endHour = end
                .atZoneSameInstant(MANILA)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toOffsetDateTime();

        while (!cursor.isAfter(endHour)) {
            String label = cursor.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));

            for (String role : roles) {
                Long value = existing.getOrDefault(label + "|" + role, 0L);

                filled.add(new ChartPointDto(
                        label,
                        role,
                        value
                ));
            }

            cursor = cursor.plusHours(1);
        }

        return filled;
    }

    private String resolveDateFormat(String groupBy, OffsetDateTime start, OffsetDateTime end) {

        if (groupBy == null || groupBy.isBlank() || groupBy.equalsIgnoreCase("Auto")) {

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

        return switch (groupBy.trim().toUpperCase()) {
            case "HOUR", "24 HOURS" -> "HH24:00";
            case "DAY" -> "YYYY-MM-DD";
            case "MONTH" -> "YYYY-MM";
            case "YEAR" -> "YYYY";
            default -> "YYYY-MM";
        };
    }

    private record ResolvedFilter(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String dateFormat
    ) {}

    private String normalizeRole(String role) {
        if (role == null || role.isBlank() || role.equalsIgnoreCase("All Roles")) {
            return "All Roles";
        }

        return role.trim().toUpperCase();
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

    private String normalizeDropdown(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.equalsIgnoreCase("All")
                || trimmed.equalsIgnoreCase("All Roles")
                || trimmed.equalsIgnoreCase("All Statuses")
                || trimmed.equalsIgnoreCase("All Actions")
                || trimmed.equalsIgnoreCase("All Modules")
                || trimmed.equalsIgnoreCase("All Severities")
                || trimmed.equalsIgnoreCase("All Violations")
                || trimmed.equalsIgnoreCase("All Devices")
                || trimmed.equalsIgnoreCase("All Stream Roles")
                || trimmed.equalsIgnoreCase("All Events")
                || trimmed.equalsIgnoreCase("All Sync Types")) {
            return null;
        }

        return trimmed.toUpperCase();
    }

    private boolean matches(String selectedValue, String actualValue) {
        if (selectedValue == null) {
            return true;
        }

        if (actualValue == null || actualValue.isBlank()) {
            return false;
        }

        return actualValue.trim().equalsIgnoreCase(selectedValue);
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