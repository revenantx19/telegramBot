package pro.sky.telegrambot.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class TelegramBotModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    private LocalDateTime dateAndTime;

    @Getter
    @Setter
    private String message;

    public TelegramBotModel(LocalDateTime dateAndTime, String message) {
        this.dateAndTime = dateAndTime;
        this.message = message;
    }

    public TelegramBotModel() {

    }
}
