package com.turnosodontologia.schedule;

import com.turnosodontologia.common.AuditableEntity;
import com.turnosodontologia.dentist.Dentist;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "schedule_configurations",
        uniqueConstraints = @UniqueConstraint(name = "uk_schedule_configurations_dentist", columnNames = "dentist_id"))
public class ScheduleConfiguration extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "dentist_id", nullable = false, unique = true)
    private Dentist dentist;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "schedule_windows",
            joinColumns = @JoinColumn(name = "schedule_configuration_id"))
    private List<ScheduleWindow> windows = new ArrayList<>();

    protected ScheduleConfiguration() {}

    public ScheduleConfiguration(Dentist dentist, List<ScheduleWindow> windows) {
        if (dentist == null) throw new IllegalArgumentException("Dentista requerido");
        this.dentist = dentist;
        replaceWindows(windows);
    }

    public void replaceWindows(List<ScheduleWindow> windows) {
        if (windows == null) throw new IllegalArgumentException("Franjas requeridas");
        this.windows = new ArrayList<>(windows);
        this.windows.sort(Comparator.comparing(ScheduleWindow::getDayOfWeek)
                .thenComparing(ScheduleWindow::getStartTime));
    }

    public UUID getId() { return id; }
    public Dentist getDentist() { return dentist; }
    public List<ScheduleWindow> getWindows() { return List.copyOf(windows); }
}
