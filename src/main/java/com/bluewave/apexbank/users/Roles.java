package com.bluewave.apexbank.users;

import com.bluewave.apexbank.utils.common.AppRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@ToString(exclude = "users")
@EqualsAndHashCode(exclude = "users")
public class Roles {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private AppRole appRole;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<Users> users = new HashSet<>();
}