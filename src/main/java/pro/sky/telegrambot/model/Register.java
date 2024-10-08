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
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_nick")
    private String userNick;

    @Column(name = "count_of_pretty")
    private Integer countOfPretty;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "already_roll")
    private boolean alreadyRoll;

    @Column(name = "percent_roll")
    private Integer percentRoll;

    @Column(name = "user_real_name")
    private String userRealName;

    public Register(Long userId, String usersNick, Long chatId, String userRealName) {
        this.userId = userId;
        this.userNick = usersNick;
        this.countOfPretty = 0;
        this.chatId = chatId;
        this.alreadyRoll = false;
        this.percentRoll = null;
        this.userRealName = userRealName;
    }

    public Register() {

    }
}
