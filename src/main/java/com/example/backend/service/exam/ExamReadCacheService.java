package com.example.backend.service.exam;

import com.example.backend.dto.exam.result.ExamTakingRawContent;
import com.example.backend.dto.student.StudentExamCardDTO;
import com.example.backend.dto.student.dashboard.StudentUpcomingExamDTO;
import com.example.backend.entity.exam.Exam;
import com.example.backend.entity.exam.ExamChoice;
import com.example.backend.entity.exam.ExamQuestion;
import com.example.backend.repository.cache.ClassEnrollmentCacheRepository;
import com.example.backend.repository.core.StudentExamRepository;
import com.example.backend.repository.exam.ExamAssignmentRepository;
import com.example.backend.repository.exam.ExamChoiceRepository;
import com.example.backend.repository.exam.ExamQuestionRepository;
import com.example.backend.repository.exam.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamReadCacheService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamChoiceRepository choiceRepository;
    private final StudentExamRepository studentExamRepository;
    private final ClassEnrollmentCacheRepository classEnrollmentCacheRepository;

    // =========================
    // EXAM TAKING RAW CONTENT
    // =========================

    @Cacheable(value = "examTakingRawContent", key = "#examId")
    public ExamTakingRawContent getExamTakingRawContent(Long examId) {
        return loadExamTakingRawContent(examId);
    }

    @CachePut(value = "examTakingRawContent", key = "#examId")
    public ExamTakingRawContent warmExamTakingRawContent(Long examId) {
        return loadExamTakingRawContent(examId);
    }

    @CacheEvict(value = "examTakingRawContent", key = "#examId")
    public void evictExamTakingRawContent(Long examId) {
    }

    // =========================
    // STUDENT EXAM PAGE
    // =========================

    @Cacheable(value = "studentExamsRaw", key = "#studentId")
    public List<StudentExamCardDTO> getStudentExamsRaw(String studentId) {
        return studentExamRepository.findStudentExamCards(studentId);
    }

    @CachePut(value = "studentExamsRaw", key = "#studentId")
    public List<StudentExamCardDTO> warmStudentExamsRaw(String studentId) {
        return studentExamRepository.findStudentExamCards(studentId);
    }

    @CacheEvict(value = "studentExamsRaw", key = "#studentId")
    public void evictStudentExamsRaw(String studentId) {
    }

    // =========================
    // STUDENT DASHBOARD UPCOMING
    // =========================

    @Cacheable(value = "studentDashboardUpcomingExams", key = "#studentId")
    public List<StudentUpcomingExamDTO> getStudentDashboardUpcomingExams(String studentId) {
        return examRepository.findPublishedAssignedExamsForStudent(studentId);
    }

    @CachePut(value = "studentDashboardUpcomingExams", key = "#studentId")
    public List<StudentUpcomingExamDTO> warmStudentDashboardUpcomingExams(String studentId) {
        return examRepository.findPublishedAssignedExamsForStudent(studentId);
    }

    @CacheEvict(value = "studentDashboardUpcomingExams", key = "#studentId")
    public void evictStudentDashboardUpcomingExams(String studentId) {
    }

    // =========================
    //  PUBLISHED EXAM
    // =========================

    public void refreshStudentExamCachesAfterExamChange(List<String> classOfferingIds, Long examId) {
        evictExamTakingRawContent(examId);


        if (classOfferingIds == null || classOfferingIds.isEmpty()) {
            return;
        }

        List<String> affectedStudentIds =
                classEnrollmentCacheRepository.findStudentIdsByClassOfferingIds(classOfferingIds);

        for (String studentId : affectedStudentIds) {
            evictStudentExamsRaw(studentId);
            warmStudentExamsRaw(studentId);

            evictStudentDashboardUpcomingExams(studentId);
            warmStudentDashboardUpcomingExams(studentId);
        }

        warmExamTakingRawContent(examId);
    }

    // =========================
    // INTERNAL LOADER
    // =========================

    private ExamTakingRawContent loadExamTakingRawContent(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found."));

        List<ExamQuestion> questions =
                questionRepository.findByExamExamIdOrderByQuestionOrderAsc(examId);

        List<Long> questionIds = questions.stream()
                .map(ExamQuestion::getQuestionId)
                .toList();

        List<ExamChoice> allChoices = questionIds.isEmpty()
                ? new ArrayList<>()
                : choiceRepository.findByQuestionQuestionIdInOrderByQuestionQuestionIdAscChoiceOrderAsc(questionIds);

        Map<Long, List<ExamChoice>> choiceMap = allChoices.stream()
                .collect(Collectors.groupingBy(
                        choice -> choice.getQuestion().getQuestionId()
                ));

        return new ExamTakingRawContent(exam, questions, choiceMap);
    }
}