package com.Job_Queue.JQ_Service.core;

import com.Job_Queue.JQ_Service.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;

    @Transactional
    public Optional<Job> claimNextJob() {
        return jobRepository.findNextJob().map(job -> {
            job.setStatus("RUNNING");
            job.setUpdatedAt(LocalDateTime.now());
            return jobRepository.save(job);
        });
    }

    @Transactional
    public void markDone(Job job) {
        job.setStatus("DONE");
        job.setCompletedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Transactional
    public void markFailed(Job job, String error) {
        job.setAttempts(job.getAttempts() + 1);
        job.setLastError(error);
        job.setUpdatedAt(LocalDateTime.now());

        if (job.getAttempts() >= job.getMaxAttempts()) {
            job.setStatus("DEAD");
            log.warn("Job {} moved to DEAD after {} attempts", job.getId(), job.getAttempts());
        } else {
            job.setStatus("FAILED");
            long delaySeconds = (long) Math.pow(2, job.getAttempts());
            job.setRunAt(LocalDateTime.now().plusSeconds(delaySeconds));
            log.info("Job {} failed, retrying in {}s", job.getId(), delaySeconds);
        }

        jobRepository.save(job);
    }
}
