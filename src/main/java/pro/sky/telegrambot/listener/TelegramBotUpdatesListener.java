package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.TelegramBotModel;
import pro.sky.telegrambot.repository.TelegramBotRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    private final TelegramBotRepository telegramBotRepository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {

        updates.forEach(update -> {
            logger.info("Processing update: {}", update);

            String messageText = update.message().text();
            logger.info("Сюда попадает отправляемое сообщение messageText: " + messageText);
            Long chatId = update.message().chat().id();
            logger.info("Сюда попадает ID чата chatId: " + chatId);

            if (messageText.equals("/start")) {
                // Создаем приветственное сообщение
                String welcomeMessage = "Здравствуйте! Добро пожаловать в нашего бота. Как я могу помочь вам сегодня?";
                // Отправляем приветственное сообщение
                // ChatID нужен для того, чтобы понимать куда будет отправлено сообщение
                SendMessage message = new SendMessage(chatId, welcomeMessage);
                SendResponse response = telegramBot.execute(message);
            }
            // Создаем регулярное вырежение
            String regex = "(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})\\s+(.*)";
            Pattern pattern = Pattern.compile(regex);
            // Необходим для поиска совпадений в строке и дальнейшего применения метода .find
            Matcher matcher = pattern.matcher(messageText);
            // Проверяем совпадения в регулярном выражении выделенные скобками, всего их 3 шт.
            if (matcher.find()) {
                String date = matcher.group(1);
                String time = matcher.group(2);
                String text = matcher.group(3);

                logger.info("Дата: " + date);
                logger.info("Время: " + time);
                logger.info("Сообщение: " + text);

                String dateTimeMessage = ("Установлено напоминание!" +
                                          "\nДата: " + date +
                                          "\nВремя: " + time +
                                          "\nСообщение: " + text);

                SendMessage message = new SendMessage(chatId, dateTimeMessage);
                SendResponse response = telegramBot.execute(message);
                // приводим дату и время к типу LocalDateTime
                LocalDateTime localDateTime = LocalDateTime.parse(date + " " + time, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                TelegramBotModel telegramBotModel = new TelegramBotModel(localDateTime, text);
                telegramBotRepository.save(telegramBotModel);
            } else {
                logger.error("Сообщение не соответствует формату");
                String error = "Сообщение не соответствует формату";
                SendResponse response = telegramBot.execute(new SendMessage(chatId, error));
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    //Добавляем выполнение данного метода каждую минуту
    @Scheduled(cron = "0 0/1 * * * *")
    public boolean run() {
        logger.info(String.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));

        boolean dateTimeExists = telegramBotRepository.existsDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));

        if (dateTimeExists) {
            SendResponse response = telegramBot.execute(new SendMessage(325729014, "Всё ок"));
        }

        for (TelegramBotModel telegramBotModel : telegramBotRepository.findAll()) {

            if (telegramBotModel.getDateAndTime().equals(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))) {
                SendResponse response = telegramBot.execute(new SendMessage(325729014, telegramBotModel.getMessage()));
            }
        }
        return false;
    }
}
