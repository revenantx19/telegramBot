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
    @Column(name = "chat_id")
    private Long chatId;

    @Getter
    @Setter
    @Column(name = "date_and_time")
    private LocalDateTime dateAndTime;

    @Getter
    @Setter
    @Column(name = "message")
    private String message;

    public TelegramBotModel(Long chatId, LocalDateTime dateAndTime, String message) {
        this.chatId = chatId;
        this.dateAndTime = dateAndTime;
        this.message = message;
    }

    public TelegramBotModel() {

    }
}
