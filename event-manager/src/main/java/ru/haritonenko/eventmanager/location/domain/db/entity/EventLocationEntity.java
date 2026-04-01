package ru.haritonenko.eventmanager.location.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "locations")
@Builder
@Getter
@Setter
@ToString(exclude = {"events"})
@NoArgsConstructor
@AllArgsConstructor
public class EventLocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Location name can not be blank")
    @Size(min = 1, max = 40, message = "Min name size is 1, max is 40")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Location address can not be blank")
    @Size(min = 5, max = 30, message = "Min address size is 5, max is 30")
    @Column(name = "address", nullable = false)
    private String address;

    @NotNull(message = "Location capacity can not be null")
    @Min(value = 5, message = "Min location capacity is 5")
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @NotBlank(message = "Location description can not be blank")
    @Size(min = 10, max = 100, message = "Min description size is 10, max is 100")
    @Column(name = "description", nullable = false)
    private String description;

    @NotNull(message = "Event events can not be null")
    @OneToMany(mappedBy = "location")
    private List<EventEntity> events = new ArrayList<>();

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

    public void addEvent(EventEntity e) {
        events.add(e);
        e.setLocation(this);
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
        EventLocationEntity that = (EventLocationEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
