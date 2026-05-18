package com.example.backend.entity.exam;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "essay_rubric")
public class EssayRubric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rubricId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private ExamQuestion question;

    private String criterionName;

    private BigDecimal weightPercentage;

    private String description;

    private Integer displayOrder;
}