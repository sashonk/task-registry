package ru.asocial.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.asocial.audit.dto.AuditEventDto;
import ru.asocial.audit.dto.PageResponse;
import ru.asocial.audit.model.AuditEvent;
import ru.asocial.audit.model.AuditEventType;
import ru.asocial.audit.repository.AuditEventRepository;
import ru.asocial.audit.service.AuditService;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository repository;

    @InjectMocks
    private AuditService service;

    @Test
    void getEventsPassesEventTypeEnumToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findFiltered(AuditEventType.TASK_CREATED, null, null, null, pageable))
                .thenReturn(Page.empty(pageable));

        PageResponse<AuditEventDto> result = service.getEvents("TASK_CREATED", null, 1, 20);

        assertThat(result.content()).isEmpty();
        verify(repository).findFiltered(eq(AuditEventType.TASK_CREATED), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void getEventsWithNullEventTypeQueriesWithoutFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findFiltered(null, null, null, null, pageable))
                .thenReturn(Page.empty(pageable));

        service.getEvents(null, null, 1, 20);

        verify(repository).findFiltered(isNull(), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void getEventsWithBlankEventTypeQueriesWithoutFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findFiltered(null, "admin", null, null, pageable))
                .thenReturn(Page.empty(pageable));

        service.getEvents("   ", "admin", 1, 20);

        verify(repository).findFiltered(isNull(), eq("admin"), isNull(), isNull(), eq(pageable));
    }

    @Test
    void getEventsWithUnknownEventTypeReturnsEmptyResult() {
        PageResponse<AuditEventDto> result = service.getEvents("NOT_A_REAL_TYPE", null, 1, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verify(repository, never()).findFiltered(any(), any(), any(), any(), any());
    }

    @Test
    void getAllEventsWithUnknownEventTypeReturnsEmptyList() {
        List<AuditEventDto> result = service.getEvents("NOT_A_REAL_TYPE", null);

        assertThat(result).isEmpty();
        verify(repository, never()).findFiltered(any(AuditEventType.class), any());
    }

    @Test
    void getAllEventsPassesEventTypeEnumToRepository() {
        when(repository.findFiltered(AuditEventType.LOGIN_SUCCESSFUL, "admin"))
                .thenReturn(List.<AuditEvent>of());

        service.getEvents("LOGIN_SUCCESSFUL", "admin");

        verify(repository).findFiltered(AuditEventType.LOGIN_SUCCESSFUL, "admin");
    }
}
