package com.bluewave.apexbank.account;

import com.bluewave.apexbank.users.Users;
import com.bluewave.apexbank.utils.common.AccountType;
import com.bluewave.apexbank.utils.common.UsersAccountStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "accounts",
        indexes = {
                // Index on accountNo for fast lookup in transfer & details queries
                @Index(name = "idx_accounts_account_no", columnList = "accountNo", unique = true),
                // Index on user_id foreign key for fast user account lookups
                @Index(name = "idx_accounts_user_id", columnList = "user_id")
        }
)
@ToString(exclude = {"users", "transactionList"})
@EqualsAndHashCode(exclude = {"users", "transactionList"})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false, length = 12)
    private String accountNo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsersAccountStatus usersAccountStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users users;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactionList = new ArrayList<>();
}