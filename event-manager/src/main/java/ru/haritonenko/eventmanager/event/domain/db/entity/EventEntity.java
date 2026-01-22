package ru.haritonenko.eventmanager.event.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import ru.haritonenko.eventmanager.event.registration.domain.db.entity.EventRegistrationEntity;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"owner", "location", "registrations"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Event name can not be blank")
    @Size(min = 1, max = 50, message = "Min name size is 1, max is 50")
    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private EventLocationEntity location;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventRegistrationEntity> registrations = new ArrayList<>();

    @NotNull(message = "Event max places can not be null")
    @Min(value = 0, message = "Min count of max places is 0")
    @Column(name = "max_places")
    private Integer maxPlaces;

    @NotNull(message = "Event occupied places can not be null")
    @Min(value = 0, message = "Min count of occupied places is 0")
    @Column(name = "occupied_places")
    private Integer occupiedPlaces;

    @NotBlank(message = "Event date can not be blank")
    @Column(name = "date")
    private String date;

    @NotNull(message = "Event cost can not be null")
    @Positive(message = "Event cost can not be negative or zero")
    @Column(name = "cost")
    private BigDecimal cost;

    @NotNull(message = "Event duration can not be null")
    @Min(value = 30, message = "Min duration is 30")
    @Column(name = "duration")
    private Integer duration;

    @NotNull(message = "Event status can not be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EventStatus status;

}
