package com.industrial.safety.course_service.service.impl;

import com.industrial.safety.course_service.dto.PriceChangeRequestDto;
import com.industrial.safety.course_service.exception.ResourceNotFoundException;
import com.industrial.safety.course_service.messaging.PriceChangeEventPublisher;
import com.industrial.safety.course_service.model.PriceChangeRequest;
import com.industrial.safety.course_service.model.PriceChangeRequest.PriceChangeStatus;
import com.industrial.safety.course_service.repository.CourseRepository;
import com.industrial.safety.course_service.repository.PriceChangeRequestRepository;
import com.industrial.safety.course_service.service.PriceChangeRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceChangeRequestServiceImpl implements PriceChangeRequestService {

    private final PriceChangeRequestRepository repository;
    private final CourseRepository courseRepository;
    private final PriceChangeEventPublisher publisher;

    @Override
    public PriceChangeRequestDto.Response create(PriceChangeRequestDto.CreateRequest req) {
        // Validate course exists
        courseRepository.findById(req.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", req.courseId()));

        PriceChangeRequest entity = PriceChangeRequest.builder()
                .courseId(req.courseId())
                .courseTitle(req.courseTitle())
                .currentPrice(req.currentPrice())
                .requestedPrice(req.requestedPrice())
                .justification(req.justification())
                .requesterId(req.requesterId())
                .requesterName(req.requesterName())
                .requesterEmail(req.requesterEmail())
                .createdAt(Instant.now())
                .build();

        PriceChangeRequest saved = repository.save(entity);
        publisher.publishNewRequest(saved);
        log.info("Price change request created: {} for course {}", saved.getId(), saved.getCourseId());
        return toResponse(saved);
    }

    @Override
    public List<PriceChangeRequestDto.Response> getAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Override
    public List<PriceChangeRequestDto.Response> getPending() {
        return repository.findByStatusOrderByCreatedAtDesc(PriceChangeStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<PriceChangeRequestDto.Response> getByRequester(String requesterId) {
        return repository.findByRequesterIdOrderByCreatedAtDesc(requesterId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public PriceChangeRequestDto.Response review(String id, PriceChangeRequestDto.ReviewRequest req) {
        PriceChangeRequest entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PriceChangeRequest", "id", id));

        if (entity.getStatus() != PriceChangeStatus.PENDING) {
            throw new IllegalStateException("Request already reviewed: " + entity.getStatus());
        }

        entity.setStatus(req.approved() ? PriceChangeStatus.APPROVED : PriceChangeStatus.REJECTED);
        entity.setReviewerId(req.reviewerId());
        entity.setReviewerName(req.reviewerName());
        entity.setReviewerComment(req.reviewerComment());
        entity.setReviewedAt(Instant.now());

        if (req.approved()) {
            // Apply price change to the course
            courseRepository.findById(entity.getCourseId()).ifPresent(course -> {
                if (course.getDetails() != null) {
                    var details = course.getDetails();
                    var updated = new com.industrial.safety.course_service.model.record.Details(
                            details.language(), details.level(), details.totalDurationHorus(),
                            details.totalLecture(), entity.getRequestedPrice(), details.lastUpdated()
                    );
                    course.setDetails(updated);
                    courseRepository.save(course);
                    log.info("Price updated for course {}: {} -> {}",
                            course.getId(), entity.getCurrentPrice(), entity.getRequestedPrice());
                }
            });
            publisher.publishApproved(entity);
        } else {
            publisher.publishRejected(entity);
        }

        return toResponse(repository.save(entity));
    }

    private PriceChangeRequestDto.Response toResponse(PriceChangeRequest e) {
        return new PriceChangeRequestDto.Response(
                e.getId(), e.getCourseId(), e.getCourseTitle(),
                e.getCurrentPrice(), e.getRequestedPrice(), e.getJustification(),
                e.getRequesterId(), e.getRequesterName(), e.getRequesterEmail(),
                e.getStatus(), e.getReviewerId(), e.getReviewerName(), e.getReviewerComment(),
                e.getCreatedAt(), e.getReviewedAt()
        );
    }
}
