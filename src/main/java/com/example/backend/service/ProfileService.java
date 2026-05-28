package com.example.backend.service;

import com.example.backend.dto.core.CurrentTermDTO;
import com.example.backend.dto.exam.response.ImageUploadResponse;
import com.example.backend.dto.profile.ProfileActivityDTO;
import com.example.backend.dto.profile.ProfileClassDTO;
import com.example.backend.dto.profile.ProfileResponseDTO;
import com.example.backend.entity.cache.ClassOfferingCache;
import com.example.backend.entity.cache.FacultyProfileCache;
import com.example.backend.entity.cache.StudentProfileCache;
import com.example.backend.entity.core.AdminProfile;
import com.example.backend.entity.core.SystemActivityLog;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.repository.cache.ClassEnrollmentCacheRepository;
import com.example.backend.repository.cache.ClassOfferingCacheRepository;
import com.example.backend.repository.cache.FacultyLoadCacheRepository;
import com.example.backend.repository.cache.FacultyProfileCacheRepository;
import com.example.backend.repository.cache.StudentProfileCacheRepository;
import com.example.backend.repository.core.AdminProfileRepository;
import com.example.backend.repository.core.SystemActivityLogRepository;
import com.example.backend.repository.core.UserAccessRepository;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserAccessRepository userAccessRepository;
    private final StudentProfileCacheRepository studentProfileCacheRepository;
    private final FacultyProfileCacheRepository facultyProfileCacheRepository;
    private final AdminProfileRepository adminProfileRepository;

    private final ClassEnrollmentCacheRepository classEnrollmentCacheRepository;
    private final FacultyLoadCacheRepository facultyLoadCacheRepository;
    private final ClassOfferingCacheRepository classOfferingCacheRepository;
    private final SystemActivityLogRepository systemActivityLogRepository;

    private static final Path PROFILE_UPLOAD_DIR = Path.of("uploads", "profiles");

    private static final ZoneId MANILA_ZONE = ZoneId.of("Asia/Manila");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);

    private static final DateTimeFormatter ACTIVITY_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH);

    public ProfileResponseDTO getMyProfile(
            String userId,
            String role
    ) {
        UserAccess user = findUser(userId);

        String normalizedRole = normalizeRole(role);
        String schoolId = user.getSchoolId();

        CurrentTermDTO currentTerm = getCurrentTerm();

        List<ProfileClassDTO> classes = buildClasses(normalizedRole, schoolId);

        List<ProfileActivityDTO> activities =
                buildRecentActivities(schoolId);

        if ("STUDENT".equals(normalizedRole)) {
            StudentProfileCache student = studentProfileCacheRepository
                    .findByStudentId(schoolId)
                    .orElseThrow(() -> new RuntimeException("Student profile not found for " + schoolId));

            return new ProfileResponseDTO(
                    "STUDENT",
                    fullName(student.getFirstName(), student.getLastName()),
                    firstNonBlank(student.getEmailAddress(), user.getEmail()),
                    student.getStudentId(),
                    firstNonBlank(student.getCollegeName(), student.getCollegeCode()),
                    student.getProgramName(),
                    user.getUsername(),
                    buildAccountStatus(user),
                    formatDate(user.getCreatedAt()),
                    buildTenureDuration(user.getCreatedAt()),
                    buildPasswordStatus(user),
                    formatDate(user.getUpdatedAt()),
                    user.getProfileImageUrl(),
                    currentTerm.getAcademicYear(),
                    currentTerm.getTerm(),
                    classes,
                    activities
            );
        }

        if ("FACULTY".equals(normalizedRole)) {
            FacultyProfileCache faculty = facultyProfileCacheRepository
                    .findByEmployeeId(schoolId)
                    .orElseThrow(() -> new RuntimeException("Faculty profile not found for " + schoolId));

            return new ProfileResponseDTO(
                    "FACULTY",
                    fullName(faculty.getFirstName(), faculty.getLastName()),
                    firstNonBlank(faculty.getEmailAddress(), user.getEmail()),
                    faculty.getEmployeeId(),
                    "Faculty",
                    firstNonBlank(faculty.getStatus(), "Faculty Member"),
                    user.getUsername(),
                    buildAccountStatus(user),
                    formatDate(user.getCreatedAt()),
                    buildTenureDuration(user.getCreatedAt()),
                    buildPasswordStatus(user),
                    formatDate(user.getUpdatedAt()),
                    user.getProfileImageUrl(),
                    currentTerm.getAcademicYear(),
                    currentTerm.getTerm(),
                    classes,
                    activities
            );
        }

        if ("ADMIN".equals(normalizedRole)) {
            AdminProfile admin = adminProfileRepository
                    .findByEmployeeId(schoolId)
                    .orElseThrow(() -> new RuntimeException("Admin profile not found for " + schoolId));

            return new ProfileResponseDTO(
                    "ADMIN",
                    fullName(admin.getFirstName(), admin.getLastName()),
                    firstNonBlank(admin.getEmail(), user.getEmail()),
                    admin.getEmployeeId(),
                    "System Administration",
                    "Administrator",
                    user.getUsername(),
                    buildAccountStatus(user),
                    formatDate(user.getCreatedAt()),
                    buildTenureDuration(user.getCreatedAt()),
                    buildPasswordStatus(user),
                    formatDate(user.getUpdatedAt()),
                    user.getProfileImageUrl(),
                    currentTerm.getAcademicYear(),
                    currentTerm.getTerm(),
                    classes,
                    activities
            );
        }

        throw new RuntimeException("Unsupported role: " + role);
    }

    public ImageUploadResponse uploadProfilePhoto(
            String userId,
            MultipartFile file
    ) {

        final long MAX_IMAGE_SIZE = 1_000_000; // 1MB

        if (file == null || file.isEmpty()) {
            return new ImageUploadResponse(false, "Image file is empty.", null);
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            return new ImageUploadResponse(false, "Only image files are allowed.", null);
        }

        try {

            UserAccess user = findUser(userId);

            String uploadDir = System.getProperty("user.dir") + "/uploads/profiles/";

            File directory = new File(uploadDir);

            if (!directory.exists()) {
                boolean created = directory.mkdirs();

                if (!created) {
                    return new ImageUploadResponse(false, "Failed to create upload directory.", null);
                }
            }

            String safeSchoolId = user.getSchoolId().replaceAll("[^a-zA-Z0-9_-]", "");

            String filename = safeSchoolId + "-" + UUID.randomUUID() + ".jpg";

            File destination = new File(directory, filename);

            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            if (originalImage == null) {
                return new ImageUploadResponse(false, "Invalid image file.", null);
            }

            double quality = 0.90;
            boolean compressedSuccessfully = false;

            while (quality >= 0.10) {

                Thumbnails.of(originalImage)
                        .size(1000, 1000)
                        .outputFormat("jpg")
                        .outputQuality(quality)
                        .toFile(destination);

                if (destination.length() <= MAX_IMAGE_SIZE) {
                    compressedSuccessfully = true;
                    break;
                }

                quality -= 0.10;
            }

            if (!compressedSuccessfully) {
                if (destination.exists()) {
                    destination.delete();
                }

                return new ImageUploadResponse(
                        false,
                        "Unable to compress image below 1MB.",
                        null
                );
            }

            String imageUrl = "/uploads/profiles/" + filename;

            String oldImageUrl = user.getProfileImageUrl();

            user.setProfileImageUrl(imageUrl);

            userAccessRepository.save(user);

            if (oldImageUrl != null && !oldImageUrl.isBlank()) {

                String oldFilename = oldImageUrl.replace("/uploads/profiles/", "");

                File oldFile = new File(directory, oldFilename);

                if (oldFile.exists()) {oldFile.delete();}
            }

            return new ImageUploadResponse(
                    true,
                    "Profile photo uploaded successfully.",
                    imageUrl
            );

        } catch (Exception e) {

            e.printStackTrace();

            return new ImageUploadResponse(
                    false,
                    "Failed to save profile photo.",
                    null
            );
        }
    }

    private List<ProfileClassDTO> buildClasses(
            String role,
            String schoolId
    ) {
        if ("STUDENT".equals(role)) {
            return classEnrollmentCacheRepository
                    .findActiveOfferingsByStudentId(schoolId)
                    .stream()
                    .sorted(Comparator.comparing(ClassOfferingCache::getCourseCode))
                    .map(this::toClassDTO)
                    .toList();
        }

        if ("FACULTY".equals(role)) {
            return facultyLoadCacheRepository
                    .findActiveOfferingsByEmployeeId(schoolId)
                    .stream()
                    .sorted(Comparator.comparing(ClassOfferingCache::getCourseCode))
                    .map(this::toClassDTO)
                    .toList();
        }

        return classOfferingCacheRepository
                .findAllActive()
                .stream()
                .sorted(Comparator.comparing(ClassOfferingCache::getCourseCode))
                .map(this::toClassDTO)
                .toList();
    }

    private ProfileClassDTO toClassDTO(ClassOfferingCache offering) {
        String title = firstNonBlank(offering.getCourseCode(), "No course code")
                + " · "
                + firstNonBlank(offering.getCourseDescription(), "No course description");

        String subtitle =
                firstNonBlank(offering.getProgramCode(), "No section")
                        + " · "
                        + firstNonBlank(offering.getTerm(), "No term")
                        + " · "
                        + firstNonBlank(offering.getAcademicYear(), "No academic year");

        return new ProfileClassDTO(
                title,
                subtitle
        );
    }

    private List<ProfileActivityDTO> buildRecentActivities(String schoolId) {
        return systemActivityLogRepository
                .findTop5ByActorIdOrderByOccurredAtDesc(schoolId)
                .stream()
                .map(this::toActivityDTO)
                .toList();
    }

    private ProfileActivityDTO toActivityDTO(SystemActivityLog log) {
        String title =
                firstNonBlank(log.getModule(), "System")
                        + " · "
                        + firstNonBlank(log.getAction(), "Activity");

        String subtitle =
                firstNonBlank(log.getMessage(), firstNonBlank(log.getStatus(), "Recorded activity"))
                        + " • "
                        + formatDateTime(log.getOccurredAt());

        return new ProfileActivityDTO(
                title,
                subtitle
        );
    }

    private CurrentTermDTO getCurrentTerm() {
        return classOfferingCacheRepository
                .findCurrentTerm()
                .stream()
                .findFirst()
                .orElse(new CurrentTermDTO(
                        "Not available",
                        "Not available"
                ));
    }

    private UserAccess findUser(String userId) {
        return userAccessRepository
                .findByUsername(userId)
                .or(() -> userAccessRepository.findBySchoolId(userId))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new RuntimeException("Role header is required.");
        }

        return role.trim().toUpperCase();
    }

    private String fullName(
            String firstName,
            String lastName
    ) {
        String name = (safe(firstName) + " " + safe(lastName)).trim();

        return name.isBlank()
                ? "Not available"
                : name;
    }

    private String buildAccountStatus(UserAccess user) {
        if (user.isBlocked()) {
            return "Blocked";
        }

        if (!user.isActive()) {
            return "Inactive";
        }

        if (user.isMustChangePassword()) {
            return "Password Change Required";
        }

        return "Active";
    }

    private String buildPasswordStatus(UserAccess user) {
        if (user.isMustChangePassword()) {
            return "Password change required";
        }

        if (user.getTempPasswordExpiry() != null) {
            return "Temporary password issued";
        }

        return "Password is active";
    }

    private String buildTenureDuration(OffsetDateTime createdAt) {
        if (createdAt == null) {
            return "Not available";
        }

        Period period = Period.between(
                createdAt.atZoneSameInstant(MANILA_ZONE).toLocalDate(),
                OffsetDateTime.now(MANILA_ZONE).toLocalDate()
        );

        if (period.getYears() > 0) {
            return period.getYears() + " year" + plural(period.getYears())
                    + ", "
                    + period.getMonths() + " month" + plural(period.getMonths());
        }

        if (period.getMonths() > 0) {
            return period.getMonths() + " month" + plural(period.getMonths());
        }

        return period.getDays() + " day" + plural(period.getDays());
    }

    private String formatDate(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "Not available";
        }

        return dateTime
                .atZoneSameInstant(MANILA_ZONE)
                .format(DATE_FORMATTER);
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "Date not available";
        }

        return dateTime
                .atZoneSameInstant(MANILA_ZONE)
                .format(ACTIVITY_FORMATTER);
    }

    private String firstNonBlank(
            String first,
            String second
    ) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return "Not available";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String plural(int count) {
        return count == 1 ? "" : "s";
    }
}