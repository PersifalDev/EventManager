package ru.haritonenko.eventmanager.user.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;
import ru.haritonenko.eventmanager.event.registration.domain.db.entity.EventRegistrationEntity;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "users")
@Builder
@Getter
@Setter
@ToString(exclude = {"password", "ownEvents", "registrations"})
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "User login can not be blank")
    @Size(min = 4, message = "Min login size is 4")
    @Column(name = "login", unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private String login;

    @NotBlank(message = "User password can not be blank")
    @Size(min = 4, message = "Min password size is 4")
    @Column(name = "password", length = 100, nullable = false)
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

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void addOwnEvent(EventEntity e) {
        ownEvents.add(e);
        e.setOwner(this);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> objectEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != objectEffectiveClass) {
            return false;
        }
        UserEntity that = (UserEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
