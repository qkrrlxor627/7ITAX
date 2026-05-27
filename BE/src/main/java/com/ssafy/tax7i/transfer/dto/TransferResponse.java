package com.ssafy.tax7i.transfer.dto;

import com.ssafy.tax7i.transfer.entity.Transfer;
import com.ssafy.tax7i.transfer.entity.TransferStatus;
import com.ssafy.tax7i.transfer.entity.TransferType;

import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        TransferType transferType,
        Long senderUserId,
        Long receiverUserId,
        Long amount,
        TransferStatus status,
        String description,
        String targetAccountNo,
        LocalDateTime createdAt
) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getTransferType(),
                transfer.getSenderUser().getId(),
                transfer.getReceiverUser() != null ? transfer.getReceiverUser().getId() : null,
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getDescription(),
                transfer.getTargetAccountNo(),
                transfer.getCreatedAt()
        );
    }
}
