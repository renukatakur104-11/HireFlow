package com.hireflow.modules.drive;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "drive_rounds",
       uniqueConstraints = @UniqueConstraint(columnNames = {"drive_id", "round_order"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriveRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drive_id", nullable = false)
    private Drive drive;

    @Column(name = "round_order", nullable = false)
    private Integer roundOrder;

    @Column(name = "round_name", nullable = false, length = 100)
    private String roundName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 30)
    private RoundStatusCode statusCode;

    @Column(length = 255)
    private String description;
}
