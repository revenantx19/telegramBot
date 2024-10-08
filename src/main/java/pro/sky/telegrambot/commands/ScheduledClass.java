package pro.sky.telegrambot.commands;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.listener.TelegramBotUpdatesListener;
import pro.sky.telegrambot.model.TelegramBotModel;
import pro.sky.telegrambot.repository.TelegramBotRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class ScheduledClass {


    private final TelegramBotRepository telegramBotRepository;

    private final TelegramBot telegramBot;

    public ScheduledClass(TelegramBotRepository telegramBotRepository, TelegramBot telegramBot) {
        this.telegramBotRepository = telegramBotRepository;
        this.telegramBot = telegramBot;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void run() {
        // Запоминаем текущую дату и время с точностью до минут
        LocalDateTime dateAndTimeNow = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        // Берём объект из базы данных, который совпададает с текущим временем
        TelegramBotModel telegramBotModel = telegramBotRepository.findEqualTimeAndDateNotification(dateAndTimeNow);
        if (telegramBotModel != null) {
            telegramBot.execute(new SendMessage(telegramBotModel.getChatId(), telegramBotModel.getMessage()));
            telegramBotRepository.delete(telegramBotModel);
        }
    }
}
