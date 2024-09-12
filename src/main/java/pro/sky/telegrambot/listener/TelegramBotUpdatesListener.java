package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {

        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            String messageText = update.message().text();
            Long chatId = update.message().chat().id();

            logger.info("Сюда попадает отправляемое сообщение messageText: " + messageText);
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

                String dateTimeMessage = ("Дата: " + date +
                                          "\nВремя: " + time +
                                          "\nСообщение " + text);

                SendMessage message = new SendMessage(chatId, dateTimeMessage);
                SendResponse response = telegramBot.execute(message);
            } else {
                logger.error("Сообщение не соответствует формату");
                String error = "Сообщение не соответствует формату";
                SendResponse response = telegramBot.execute(new SendMessage(chatId, error));
            }

        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
