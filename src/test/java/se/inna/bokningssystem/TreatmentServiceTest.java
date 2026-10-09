// Anger vilket paket testklassen tillhör.
package se.inna.bokningssystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.inna.bokningssystem.dto.TreatmentRequestDto;
import se.inna.bokningssystem.exception.DuplicateTreatmentException;
import se.inna.bokningssystem.mapper.TreatmentMapper;
import se.inna.bokningssystem.repository.TreatmentRepository;
import se.inna.bokningssystem.service.TreatmentService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

// Aktiverar Mockito så att beroenden kan simuleras i testet.
@ExtendWith(MockitoExtension.class)
class TreatmentServiceTest {

    // Skapar en mock av repositoryt så att vi inte behöver använda databasen.
    @Mock
    private TreatmentRepository treatmentRepository;

    // Simulerar mappern som används av servicen.
    @Mock
    private TreatmentMapper treatmentMapper;

    // Skapar en instans av TreatmentService och injicerar mockarna.
    @InjectMocks
    private TreatmentService treatmentService;

    // Markerar metoden som ett test.
    @Test
    void createShouldThrowExceptionWhenTreatmentNameAlreadyExists() {

        // Skapar en DTO med namnet på behandlingen som ska testas.
        TreatmentRequestDto dto = new TreatmentRequestDto();
        dto.setName("Klassisk massage");

        // Simulerar att en behandling med samma namn redan finns.
        when(treatmentRepository.existsByNameIgnoreCase("Klassisk massage"))
                .thenReturn(true);

        // Kontrollerar att servicen kastar rätt exception vid dubblett.
        assertThrows(
                DuplicateTreatmentException.class,
                () -> treatmentService.create(dto)
        );
    }
}