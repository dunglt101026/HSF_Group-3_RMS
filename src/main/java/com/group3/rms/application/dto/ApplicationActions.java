package com.group3.rms.application.dto;












public record ApplicationActions(
        boolean hrViewer,
        boolean jobOwner,
        boolean canApprove,
        boolean canReject,
        boolean canHire,
        boolean canSchedule
) {
}
