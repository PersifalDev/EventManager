package ru.haritonenko.eventmanager.user.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;
import ru.haritonenko.eventmanager.event.registration.db.entity.EventRegistrationEntity;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"password", "ownEvents", "registrations"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @NotBlank(message = "User login can not be blank")
    @Size(min = 4, message = "Min login size is 4")
    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private String login;

    @NotBlank(message = "User password can not be blank")
    @Size(min = 4, message = "Min password size is 4")
    @Column(length = 100, nullable = false)
    @EqualsAndHashCode.Include
    private String password;

    @Min(value = 18, message = "Min age size is 18")
    @EqualsAndHashCode.Include
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @EqualsAndHashCode.Include
    private UserRole userRole;

    @NotNull(message = "User own events can not be null")
    @OneToMany(mappedBy = "owner", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<EventEntity> ownEvents = new ArrayList<>();

    @NotNull(message = "User registrations  can not be null")
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventRegistrationEntity> registrations = new ArrayList<>();

    public void addOwnEvent(EventEntity e) {
        ownEvents.add(e);
        e.setOwner(this);
    }

}
