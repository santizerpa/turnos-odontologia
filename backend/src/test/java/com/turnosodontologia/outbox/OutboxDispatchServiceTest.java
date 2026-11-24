package com.turnosodontologia.outbox;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboxDispatchServiceTest {
    @Test
    void dispatchesPendingMessageEvenWhenTypeIsNull() {
        OutboxMessage message = mock(OutboxMessage.class);
        when(message.getStatus()).thenReturn(OutboxStatus.PENDING);
        when(message.getType()).thenReturn(null);
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        when(repository.findAll()).thenReturn(List.of(message));

        int dispatched = new OutboxDispatchService(repository,
            Clock.fixed(Instant.parse("2040-01-01T00:00:00Z"), ZoneOffset.UTC)).dispatchPending();

        assertThat(dispatched).isEqualTo(1);
        verify(message).markSimulatedSent(any());
    }
}
