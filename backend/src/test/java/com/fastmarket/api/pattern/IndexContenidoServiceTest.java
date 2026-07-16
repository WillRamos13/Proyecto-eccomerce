package com.fastmarket.api.pattern;

import com.fastmarket.api.repository.IndexContenidoRepository;
import com.fastmarket.api.service.IndexContenidoService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class IndexContenidoServiceTest {
    @Test
    void aplicaActivoAunqueNoSeEnvieTipo() {
        IndexContenidoRepository repository = mock(IndexContenidoRepository.class);
        when(repository.findByActivoTrueOrderByOrdenAscIdAsc()).thenReturn(List.of());
        IndexContenidoService service = new IndexContenidoService(repository);

        assertTrue(service.listar(null, true).isEmpty());
        verify(repository).findByActivoTrueOrderByOrdenAscIdAsc();
        verify(repository, never()).findAllByOrderByOrdenAscIdAsc();
    }
}
