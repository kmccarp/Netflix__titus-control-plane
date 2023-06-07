/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.titus.api.jobmanager.service;

import java.util.List;

import com.netflix.titus.api.jobmanager.model.job.Capacity;
import com.netflix.titus.api.jobmanager.model.job.Job;
import com.netflix.titus.api.jobmanager.model.job.JobDescriptor;
import com.netflix.titus.api.jobmanager.model.job.JobState;
import com.netflix.titus.api.jobmanager.model.job.ServiceJobProcesses;
import com.netflix.titus.api.jobmanager.model.job.Task;
import com.netflix.titus.api.jobmanager.model.job.TaskState;
import com.netflix.titus.api.jobmanager.model.job.ebs.EbsVolume;
import com.netflix.titus.api.jobmanager.model.job.ext.ServiceJobExt;
import com.netflix.titus.api.model.ResourceDimension;
import com.netflix.titus.api.model.Tier;

public class JobManagerException extends RuntimeException {

    public enum ErrorCode {
        JobCreateLimited,
        JobNotFound,
        NotServiceJobDescriptor,
        NotServiceJob,
        NotBatchJobDescriptor,
        NotBatchJob,
        UnexpectedJobState,
        UnexpectedTaskState,
        TaskNotFound,
        JobTerminating,
        TaskTerminating,
        InvalidContainerResources,
        InvalidDesiredCapacity,
        InvalidMaxCapacity,
        InvalidSequenceId,
        BelowMinCapacity,
        AboveMaxCapacity,
        TerminateAndShrinkNotAllowed,
        SameJobIds,
        TaskJobMismatch,
        NotEnabled,
        JobsNotCompatible
    }

    private final ErrorCode errorCode;

    private JobManagerException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    private JobManagerException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Returns true, if the argument holds a {@link JobManagerException} instance with an error that may happen during
     * normal execution (for example 'JobNotFound').
     */
    public static boolean isExpected(Throwable error) {
        if (!(error instanceof JobManagerException)) {
            return false;
        }
        switch (((JobManagerException) error).getErrorCode()) {
            case JobCreateLimited:
            case JobNotFound:
            case TaskNotFound:
            case NotServiceJobDescriptor:
            case NotServiceJob:
            case NotBatchJobDescriptor:
            case NotBatchJob:
            case JobTerminating:
            case TaskTerminating:
            case InvalidContainerResources:
            case InvalidDesiredCapacity:
            case InvalidMaxCapacity:
            case InvalidSequenceId:
            case BelowMinCapacity:
            case AboveMaxCapacity:
            case TerminateAndShrinkNotAllowed:
            case SameJobIds:
            case TaskJobMismatch:
            case NotEnabled:
                return true;
            case UnexpectedJobState:
            case UnexpectedTaskState:
                return false;
        }
        return false;
    }

    public static boolean hasErrorCode(Throwable error, ErrorCode errorCode) {
        return (error instanceof JobManagerException jme) && jme.getErrorCode() == errorCode;
    }

    public static JobManagerException jobCreateLimited(String violation) {
        return new JobManagerException(ErrorCode.JobCreateLimited, violation);
    }

    public static JobManagerException invalidSequenceId(String violation) {
        return new JobManagerException(ErrorCode.InvalidSequenceId, violation);
    }

    public static JobManagerException jobNotFound(String jobId) {
        return new JobManagerException(ErrorCode.JobNotFound, "Job with id %s does not exist".formatted(jobId));
    }

    public static JobManagerException v3JobNotFound(String jobId) {
        return new JobManagerException(ErrorCode.JobNotFound, "Job with id %s does not exist, or is running on the V2 engine".formatted(jobId));
    }

    public static JobManagerException unexpectedJobState(Job job, JobState expectedState) {
        return new JobManagerException(
                ErrorCode.UnexpectedJobState,
                "Job %s is not in the expected state %s (expected) != %s (actual)".formatted(job.getId(), expectedState, job.getStatus().getState())
        );
    }

    public static JobManagerException taskNotFound(String taskId) {
        return new JobManagerException(ErrorCode.TaskNotFound, "Task with id %s does not exist".formatted(taskId));
    }

    public static JobManagerException notServiceJobDescriptor(JobDescriptor<?> jobDescriptor) {
        return new JobManagerException(ErrorCode.NotServiceJobDescriptor, "Operation restricted to service job descriptors, but got: %s".formatted(jobDescriptor));
    }

    public static JobManagerException notServiceJob(String jobId) {
        return new JobManagerException(ErrorCode.NotServiceJob, "Operation restricted to service jobs, and %s is not a service job".formatted(jobId));
    }

    public static JobManagerException notBatchJobDescriptor(JobDescriptor<?> jobDescriptor) {
        return new JobManagerException(ErrorCode.NotBatchJobDescriptor, "Operation restricted to batch job descriptors, but got: %s".formatted(jobDescriptor));
    }

    public static JobManagerException notBatchJob(String jobId) {
        return new JobManagerException(ErrorCode.NotBatchJob, "Operation restricted to batch jobs, and %s is not a batch job".formatted(jobId));
    }

    public static JobManagerException unexpectedTaskState(Task task, TaskState expectedState) {
        return new JobManagerException(
                ErrorCode.UnexpectedTaskState,
                "Task %s is not in the expected state %s (expected) != %s (actual)".formatted(task.getId(), expectedState, task.getStatus().getState())
        );
    }

    public static Throwable jobTerminating(Job<?> job) {
        if (job.getStatus().getState() == JobState.Finished) {
            return new JobManagerException(ErrorCode.JobTerminating, "Job %s is terminated".formatted(job.getId()));
        }
        return new JobManagerException(ErrorCode.JobTerminating, "Job %s is in the termination process".formatted(job.getId()));
    }

    public static Throwable taskTerminating(Task task) {
        if (task.getStatus().getState() == TaskState.Finished) {
            return new JobManagerException(ErrorCode.TaskTerminating, "Task %s is terminated".formatted(task.getId()));
        }
        return new JobManagerException(ErrorCode.TaskTerminating, "Task %s is in the termination process".formatted(task.getId()));
    }

    public static JobManagerException invalidContainerResources(Tier tier, ResourceDimension requestedResources, List<ResourceDimension> tierResourceLimits) {
        return new JobManagerException(
                ErrorCode.InvalidContainerResources,
                "Job too large to run in the %s tier: requested=%s, limits=%s".formatted(tier, requestedResources, tierResourceLimits)
        );
    }

    public static JobManagerException invalidContainerResources(EbsVolume ebsVolume, String message) {
        return new JobManagerException(
                ErrorCode.InvalidContainerResources,
                "Job has invalid EBS volume: volume id=%s, reason=%s".formatted(ebsVolume.getVolumeId(), message)
        );
    }

    public static JobManagerException invalidDesiredCapacity(String jobId, int targetDesired, ServiceJobProcesses serviceJobProcesses) {
        return new JobManagerException(
                ErrorCode.InvalidDesiredCapacity,
                "Job %s can not be updated to desired capacity of %s, disableIncreaseDesired %s, disableDecreaseDesired %s".formatted(
                        jobId, targetDesired, serviceJobProcesses.isDisableIncreaseDesired(), serviceJobProcesses.isDisableDecreaseDesired())
        );
    }

    public static JobManagerException invalidMaxCapacity(String jobId, int targetMax, int ipAllocations) {
        return new JobManagerException(
                ErrorCode.InvalidMaxCapacity,
                "Job %s can not be updated to max capacity of %d due to only %d IP allocations".formatted(
                        jobId, targetMax, ipAllocations)
        );
    }

    public static JobManagerException belowMinCapacity(Job<ServiceJobExt> job, int decrement) {
        Capacity capacity = job.getJobDescriptor().getExtensions().getCapacity();
        return new JobManagerException(
                ErrorCode.BelowMinCapacity,
                "Cannot decrement job %s desired size by %s, as it violates the minimum job size constraint: min=%s, desired=%d, max=%d".formatted(
                        job.getId(), decrement, capacity.getMin(), capacity.getDesired(), capacity.getMax()
                )
        );
    }

    public static JobManagerException aboveMaxCapacity(Job<ServiceJobExt> job, int increment) {
        Capacity capacity = job.getJobDescriptor().getExtensions().getCapacity();
        return new JobManagerException(
                ErrorCode.AboveMaxCapacity,
                "Cannot increment job %s desired size by %s, as it violates the maximum job size constraint: min=%s, desired=%d, max=%d".formatted(
                        job.getId(), increment, capacity.getMin(), capacity.getDesired(), capacity.getMax()
                )
        );
    }

    public static JobManagerException terminateAndShrinkNotAllowed(Job<ServiceJobExt> job, Task task) {
        Capacity capacity = job.getJobDescriptor().getExtensions().getCapacity();
        return new JobManagerException(
                ErrorCode.TerminateAndShrinkNotAllowed,
                "Terminate and shrink would make desired job size go below the configured minimum, which is not allowed for this request: jobId=%s, taskId=%s, min=%s, desired=%d, max=%d".formatted(
                        job.getId(), task.getId(), capacity.getMin(), capacity.getDesired(), capacity.getMax()
                )
        );
    }

    public static JobManagerException sameJobs(String jobId) {
        return new JobManagerException(
                ErrorCode.SameJobIds,
                "Operation requires two different job, but the same job was provided as the source and target: %s".formatted(jobId)
        );
    }

    public static JobManagerException taskJobMismatch(String jobId, String taskId) {
        return new JobManagerException(
                ErrorCode.TaskJobMismatch,
                "Operation requires task id to belong to the source job id. Task with id %s does not belong to job with id %s".formatted(taskId, jobId)
        );
    }

    public static JobManagerException notCompatible(Job<ServiceJobExt> jobFrom, Job<ServiceJobExt> jobTo, String details) {
        return new JobManagerException(
                ErrorCode.JobsNotCompatible,
                "Operation requires jobs to be compatible: %s -> %s\n%s".formatted(jobFrom.getId(), jobTo.getId(), details)
        );
    }

    public static JobManagerException notEnabled(String taskAction) {
        return new JobManagerException(
                ErrorCode.NotEnabled,
                "%s not enabled".formatted(taskAction)
        );
    }
}
