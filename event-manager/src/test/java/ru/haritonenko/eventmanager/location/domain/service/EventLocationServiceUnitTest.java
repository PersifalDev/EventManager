package ru.haritonenko.eventmanager.location.domain.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.haritonenko.eventmanager.location.domain.db.repository.EventLocationRepository;
import ru.haritonenko.eventmanager.location.domain.mapper.EventLocationEntityMapper;

@ExtendWith(MockitoExtension.class)
public class EventLocationServiceUnitTest {

    @Mock
    private EventLocationRepository locationRepository;

    @Mock
    private EventLocationEntityMapper mapper;

    @InjectMocks
    private EventLocationService eventLocationService;



}
