package pro.sky.telegrambot.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;

@Entity
@Table(name = "register")
@Getter
@Setter
public class Register {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_nick")
    private String userNick;

    @Column(name = "count_of_pretty")
    private Integer countOfPretty;

    public Register(Long userId, String usersNick) {
        this.userId = userId;
        this.userNick = usersNick;
        this.countOfPretty = 0;
    }

    public Register() {

    }
}
