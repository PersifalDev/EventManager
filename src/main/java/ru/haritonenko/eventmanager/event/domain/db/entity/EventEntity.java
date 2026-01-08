package ru.haritonenko.eventmanager.event.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import ru.haritonenko.eventmanager.event.registration.db.entity.EventRegistrationEntity;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"owner", "location", "registrations"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "events")
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @NotBlank(message = "Event name can not be blank")
    @Size(min = 1, max = 50, message = "Min name size is 1, max is 50")
    private String name;

    @NotNull(message = "Event owner can not be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @NotNull(message = "Event location can not be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private EventLocationEntity location;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventRegistrationEntity> registrations = new ArrayList<>();

    @NotNull(message = "Event maxPlaces can not be null")
    @Min(value = 0, message = "Min count of maxPlaces is 0")
    private Integer maxPlaces;

    @NotNull(message = "Event occupiedPlaces can not be null")
    @Min(value = 0, message = "Min count of occupiedPlaces is 0")
    private Integer occupiedPlaces;

    @NotBlank(message = "Event date can not be blank")
    private String date;

    @NotNull(message = "Event cost can not be null")
    @Positive(message = "Event cost can not be negative or zero")
    private BigDecimal cost;

    @NotNull(message = "Event duration can not be null")
    @Min(value = 30, message = "Min duration is 30")
    private Integer duration;

    @NotNull(message = "Event status can not be null")
    @Enumerated(EnumType.STRING)
    private EventStatus status;

}
