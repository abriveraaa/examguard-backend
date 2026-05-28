package com.example.backend.service.core;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendActivationEmail(String toEmail, String username, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, "ExamGuard System");
            helper.setTo(toEmail);
            helper.setSubject("ExamGuard Account Activation");

            String html = buildActivationHtml(username, tempPassword);
            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
        e.printStackTrace();

        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        System.out.println("EMAIL ROOT ERROR: " + root.getClass().getName());
        System.out.println("EMAIL ROOT MESSAGE: " + root.getMessage());

        throw new RuntimeException(
                "Failed to send activation email. Root cause: " + root.getMessage(),
                e
        );
    }
    }

    public void sendResetPasswordEmail(String toEmail, String username, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, "ExamGuard Security");
            helper.setTo(toEmail);
            helper.setSubject("ExamGuard Password Reset");

            String html = buildResetPasswordHtml(username, tempPassword);
            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send reset password email.", e);
        }
    }

    @Async
    public void sendExamPublishedEmail(String toEmail,
                                       String studentName,
                                       String examTitle,
                                       String courseCode,
                                       String startDateTime,
                                       String endDateTime,
                                       int durationMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, "ExamGuard System");
            helper.setTo(toEmail);
            helper.setSubject("New Exam Available | " + examTitle + " | " + courseCode);

            String html = buildExamPublishedHtml(
                    studentName,
                    examTitle,
                    courseCode,
                    startDateTime,
                    endDateTime,
                    durationMinutes
            );

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send exam published email.", e);
        }
    }

    @Async
    public void sendExamCancelledEmail(String toEmail,
                                       String studentName,
                                       String examTitle,
                                       String courseCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, "ExamGuard System");
            helper.setTo(toEmail);
            helper.setSubject("Exam Cancelled | " + examTitle + " | " + courseCode);

            String html = buildExamCancelledHtml(studentName, examTitle, courseCode);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send exam cancelled email.", e);
        }
    }

    @Async
    public void sendExamRescheduledEmail(String toEmail,
                                         String studentName,
                                         String examTitle,
                                         String courseCode,
                                         String newStartDateTime,
                                         String newEndDateTime) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, "ExamGuard System");
            helper.setTo(toEmail);
            helper.setSubject("Exam Rescheduled | " + examTitle + " | " + courseCode);

            String html = buildExamRescheduledHtml(
                    studentName,
                    examTitle,
                    courseCode,
                    newStartDateTime,
                    newEndDateTime
            );

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send exam rescheduled email.", e);
        }
    }

    @Async
    public void sendExamResultsReleasedEmail(String toEmail,
                                             String studentName,
                                             String examTitle,
                                             String courseCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, "ExamGuard System");
            helper.setTo(toEmail);
            helper.setSubject("Exam Results Released | " + examTitle + " | " + courseCode);

            String html = buildExamResultsReleasedHtml(studentName, examTitle, courseCode);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send exam results released email.", e);
        }
    }

    @Async
    public void sendBulkExamPublishedEmail(List<String> emails,
                                           String examTitle,
                                           String courseCode,
                                           String start,
                                           String end,
                                           int duration) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, "ExamGuard System");

            // primary recipient (required)
            helper.setTo(fromEmail);

            // BCC all students
            helper.setBcc(emails.toArray(new String[0]));

            helper.setSubject(
                    "New Exam Available | " + examTitle + " | " + courseCode
            );

            String html = """
                <h2>New Exam Available</h2>
                <p>A new exam has been published.</p>

                <b>Exam:</b> %s<br>
                <b>Course:</b> %s<br>
                <b>Start:</b> %s<br>
                <b>End:</b> %s<br>
                <b>Duration:</b> %d minutes<br><br>

                <p>Please login to ExamGuard.</p>
                """.formatted(examTitle, courseCode, start, end, duration);

            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send bulk email", e);
        }
    }

    private String buildExamPublishedHtml(String studentName,
                                          String examTitle,
                                          String courseCode,
                                          String startDateTime,
                                          String endDateTime,
                                          int durationMinutes) {
        return buildExamNotificationTemplate(
                "New Exam Available",
                "A new exam has been published and is now available in ExamGuard.",
                "#650000",
                "#FFF8E1",
                "Exam Details",
                studentName,
                examTitle,
                courseCode,
                """
                <p style="margin:0 0 10px 0; font-size:15px;">
                    <strong style="color:#650000;">Start:</strong>
                    <span style="color:#333333;">%s</span>
                </p>
                <p style="margin:0 0 10px 0; font-size:15px;">
                    <strong style="color:#650000;">End:</strong>
                    <span style="color:#333333;">%s</span>
                </p>
                <p style="margin:0; font-size:15px;">
                    <strong style="color:#650000;">Duration:</strong>
                    <span style="color:#333333;">%d minutes</span>
                </p>
                """.formatted(startDateTime, endDateTime, durationMinutes),
                "Please login to ExamGuard to view and take the exam."
        );
    }

    private String buildExamCancelledHtml(String studentName,
                                          String examTitle,
                                          String courseCode) {
        return buildExamNotificationTemplate(
                "Exam Cancelled",
                "An exam assigned to your class has been cancelled.",
                "#b91c1c",
                "#FEF2F2",
                "Cancelled Exam",
                studentName,
                examTitle,
                courseCode,
                """
                <p style="margin:0; font-size:15px; color:#b91c1c; font-weight:bold;">
                    This exam is no longer available for taking.
                </p>
                """,
                "Please check ExamGuard for further updates from your faculty."
        );
    }

    private String buildExamRescheduledHtml(String studentName,
                                            String examTitle,
                                            String courseCode,
                                            String newStartDateTime,
                                            String newEndDateTime) {
        return buildExamNotificationTemplate(
                "Exam Rescheduled",
                "The schedule of your exam has been updated.",
                "#C9A227",
                "#FFF8E1",
                "Updated Schedule",
                studentName,
                examTitle,
                courseCode,
                """
                <p style="margin:0 0 10px 0; font-size:15px;">
                    <strong style="color:#650000;">New Start:</strong>
                    <span style="color:#333333;">%s</span>
                </p>
                <p style="margin:0; font-size:15px;">
                    <strong style="color:#650000;">New End:</strong>
                    <span style="color:#333333;">%s</span>
                </p>
                """.formatted(newStartDateTime, newEndDateTime),
                "Please login to ExamGuard to view the updated exam schedule."
        );
    }

    private String buildExamResultsReleasedHtml(String studentName,
                                                String examTitle,
                                                String courseCode) {
        return buildExamNotificationTemplate(
                "Exam Results Released",
                "Your exam result is now available.",
                "#15803d",
                "#F0FDF4",
                "Released Result",
                studentName,
                examTitle,
                courseCode,
                """
                <p style="margin:0; font-size:15px; color:#15803d; font-weight:bold;">
                    You may now view your score, result summary, and feedback in ExamGuard.
                </p>
                """,
                "Please login to ExamGuard to review your exam result."
        );
    }

    private String buildExamNotificationTemplate(String title,
                                                 String message,
                                                 String accentColor,
                                                 String detailBackground,
                                                 String detailTitle,
                                                 String studentName,
                                                 String examTitle,
                                                 String courseCode,
                                                 String extraDetailsHtml,
                                                 String closingMessage) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#f4f4f4; font-family:Arial, sans-serif; color:#333333;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4; padding:30px 0;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff; border-radius:14px; overflow:hidden; box-shadow:0 4px 14px rgba(0,0,0,0.08);">
                
                                    <tr>
                                        <td style="background-color:#650000; padding:28px 36px; text-align:center;">
                                            <h1 style="margin:0; color:#ffffff; font-size:28px;">ExamGuard</h1>
                                            <p style="margin:8px 0 0 0; color:#f5e6e6; font-size:14px;">
                                                Exam Notification
                                            </p>
                                        </td>
                                    </tr>
                
                                    <tr>
                                        <td style="padding:36px;">
                                            <h2 style="margin-top:0; color:%s; font-size:22px;">%s</h2>
                
                                            <p style="font-size:15px; line-height:1.7; margin-bottom:12px;">
                                                Hello %s,
                                            </p>
                
                                            <p style="font-size:15px; line-height:1.7; margin-bottom:20px;">
                                                %s
                                            </p>
                
                                            <table width="100%%" cellpadding="0" cellspacing="0"
                                                   style="background-color:%s; border:1px solid #f0e0a0; border-radius:10px; margin:24px 0;">
                                                <tr>
                                                    <td style="padding:18px 22px;">
                                                        <p style="margin:0 0 14px 0; font-size:14px; font-weight:bold; color:%s;">
                                                            %s
                                                        </p>
                                                        <p style="margin:0 0 10px 0; font-size:15px;">
                                                            <strong style="color:#650000;">Exam:</strong>
                                                            <span style="color:#333333;">%s</span>
                                                        </p>
                                                        <p style="margin:0 0 10px 0; font-size:15px;">
                                                            <strong style="color:#650000;">Course Code:</strong>
                                                            <span style="color:#333333;">%s</span>
                                                        </p>
                                                        %s
                                                    </td>
                                                </tr>
                                            </table>
                
                                            <p style="font-size:14px; line-height:1.7; margin:0 0 12px 0;">
                                                %s
                                            </p>
                
                                            <p style="font-size:13px; line-height:1.6; color:#777777; margin:0;">
                                                This is an automated message. Please do not reply to this email.
                                            </p>
                                        </td>
                                    </tr>
                
                                    <tr>
                                        <td style="background-color:#fafafa; border-top:1px solid #eeeeee; padding:18px 36px; text-align:center;">
                                            <p style="margin:0; font-size:12px; color:#777777;">
                                                ExamGuard System • BSITOUMN 2-2
                                            </p>
                                        </td>
                                    </tr>
                
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                accentColor,
                title,
                safe(studentName),
                message,
                detailBackground,
                accentColor,
                detailTitle,
                safe(examTitle),
                safe(courseCode),
                extraDetailsHtml,
                closingMessage
        );
    }

    private String buildActivationHtml(String username, String tempPassword) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#f4f4f4; font-family:Arial, sans-serif; color:#333333;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4; padding:30px 0;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff; border-radius:14px; overflow:hidden; box-shadow:0 4px 14px rgba(0,0,0,0.08);">
                
                                    <tr>
                                        <td style="background-color:#650000; padding:28px 36px; text-align:center;">
                                            <h1 style="margin:0; color:#ffffff; font-size:28px;">ExamGuard</h1>
                                        </td>
                                    </tr>
                
                                    <tr>
                                        <td style="padding:36px;">
                                            <h2 style="margin-top:0; color:#650000; font-size:22px;">Account Activation Successful</h2>
                
                                            <p style="font-size:15px; line-height:1.7; margin-bottom:20px;">
                                                Your ExamGuard account has been successfully activated.
                                                Please use the credentials below to log in.
                                            </p>
                
                                            <table width="100%%" cellpadding="0" cellspacing="0"
                                                   style="background-color:#FFF8E1; border:1px solid #f0e0a0; border-radius:10px; margin:24px 0;">
                                                <tr>
                                                    <td style="padding:18px 22px;">
                                                        <p style="margin:0 0 12px 0; font-size:15px;">
                                                            <strong style="color:#650000;">Username:</strong>
                                                            <span style="color:#333333;">%s</span>
                                                        </p>
                                                        <p style="margin:0; font-size:15px;">
                                                            <strong style="color:#650000;">Temporary Password:</strong>
                                                            <span style="color:#333333;">%s</span>
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                
                                            <p style="font-size:14px; line-height:1.7; margin:0 0 12px 0;">
                                                    <strong style="color:#C9A227;">Important:</strong>
                                                    This is a temporary password and will expire in <strong>5 minutes</strong>.
                                                    Please log in and change your password immediately.
                                             </p>
                
                                            <p style="font-size:14px; line-height:1.7; margin:0;">
                                                If you did not request this activation, please ignore this email.
                                            </p>
                                        </td>
                                    </tr>
                
                                    <tr>
                                        <td style="background-color:#fafafa; border-top:1px solid #eeeeee; padding:18px 36px; text-align:center;">
                                            <p style="margin:0; font-size:12px; color:#777777;">
                                                ExamGuard System • BSITOUMN 2-2
                                            </p>
                                        </td>
                                    </tr>
                
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(safe(username), safe(tempPassword));
    }

    private String buildResetPasswordHtml(String username, String tempPassword) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#f4f4f4; font-family:Arial, sans-serif; color:#333333;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4; padding:30px 0;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff; border-radius:14px; overflow:hidden; box-shadow:0 4px 14px rgba(0,0,0,0.08);">
                
                                    <tr>
                                        <td style="background-color:#650000; padding:28px 36px; text-align:center;">
                                            <h1 style="margin:0; color:#ffffff; font-size:28px;">ExamGuard</h1>
                                            <p style="margin:8px 0 0 0; color:#f5e6e6; font-size:14px;">
                                                Security Notification
                                            </p>
                                        </td>
                                    </tr>
                
                                    <tr>
                                        <td style="padding:36px;">
                                            <h2 style="margin-top:0; color:#650000; font-size:22px;">Password Reset Request</h2>
                
                                            <p style="font-size:15px; line-height:1.7; margin-bottom:20px;">
                                                We received a request to reset your ExamGuard account password.
                                            </p>
                
                                            <table width="100%%" cellpadding="0" cellspacing="0"
                                                   style="background-color:#FFF8E1; border:1px solid #f0e0a0; border-radius:10px; margin:24px 0;">
                                                <tr>
                                                    <td style="padding:18px 22px;">
                                                        <p style="margin:0 0 12px 0; font-size:15px;">
                                                            <strong style="color:#650000;">Username:</strong>
                                                            <span style="color:#333333;">%s</span>
                                                        </p>
                                                        <p style="margin:0; font-size:15px;">
                                                            <strong style="color:#650000;">Temporary Password:</strong>
                                                            <span style="color:#333333;">%s</span>
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                
                                            <p style="font-size:14px; line-height:1.7; margin:0 0 12px 0; color:#b91c1c; font-weight:bold;">
                                                    ⚠ This temporary password will expire in <strong>5 minutes</strong>.
                                            </p>
                
                                            <p style="font-size:14px; line-height:1.7; margin:0 0 12px 0;">
                                                    Please log in immediately and change your password.
                                                </p>
                
                                                <p style="font-size:14px; line-height:1.7; margin:0;">
                                                    If you did NOT request this reset, ignore this email.
                                                </p>
                                        </td>
                                    </tr>
                
                                    <tr>
                                        <td style="background-color:#fafafa; border-top:1px solid #eeeeee; padding:18px 36px; text-align:center;">
                                            <p style="margin:0; font-size:12px; color:#777777;">
                                                ExamGuard Security • BSITOUMN 2-2
                                            </p>
                                        </td>
                                    </tr>
                
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(safe(username), safe(tempPassword));
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}