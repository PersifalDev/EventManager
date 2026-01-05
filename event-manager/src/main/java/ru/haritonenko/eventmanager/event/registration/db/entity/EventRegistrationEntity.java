package ru.haritonenko.eventmanager.event.registration.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;
import ru.haritonenko.eventmanager.event.registration.status.EventRegistrationStatus;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;

@Getter
@Setter
@ToString(exclude = {"user", "event"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "event_registrations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_registrations_user_event",
                columnNames = {"user_id", "event_id"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @NotNull(message = "User can not be null for registration")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @NotNull(message = "Event can not be null for registration")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @NotNull(message = "Event registration status can not be null")
    @Enumerated(EnumType.STRING)
    private EventRegistrationStatus status;
}
