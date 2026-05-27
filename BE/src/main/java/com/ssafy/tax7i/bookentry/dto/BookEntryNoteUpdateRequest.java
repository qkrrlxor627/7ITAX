package com.ssafy.tax7i.bookentry.dto;

import jakarta.validation.constraints.Size;

public record BookEntryNoteUpdateRequest(
        @Size(max = 200, message = "메모는 200자 이내로 입력해주세요.")
        String note
) {
}
