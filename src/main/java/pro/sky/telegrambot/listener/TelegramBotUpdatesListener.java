package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Local;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.commands.ScheduledClass;
import pro.sky.telegrambot.model.Register;
import pro.sky.telegrambot.model.TelegramBotModel;
import pro.sky.telegrambot.repository.RegisterRepository;
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
@Slf4j
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    private final TelegramBotRepository telegramBotRepository;
    private final RegisterRepository registerRepository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {

        updates.forEach(update -> {
            log.info("Processing update: {}", update);
            // ловим отправленное сообщение
            String messageText = update.message().text();
            // достаём ID чата с пользователем
            Long chatId = update.message().chat().id();

            if (messageText.equals("/start")) {
                // Создаем приветственное сообщение
                String welcomeMessage = "Добро пожаловать в нашего бота. Я могу:" +
                        "\n1. Сохранить для вас напоминание. Для этого отправьте мне сообщение в следующем формате: 14.09.2024 16:33 Текст напоминания" +
                        "\n2. Начальник ещё не придумал, что я могу ещё сделать." +
                        "\n3. Зарегистрировать в игру.";
                // Отправляем приветственное сообщение
                // ChatID нужен для того, чтобы понимать в какой чат будет отправлено сообщение
                telegramBot.execute(new SendMessage(chatId, welcomeMessage));
            }
            // регистрация в игру
            if (messageText.equals("/reg")) {
                boolean findUserId = registerRepository.findUserId(update.message().chat().id());
                if (findUserId) {
                    telegramBot.execute(new SendMessage(chatId, "Уже в игре."));
                } else {
                    registerRepository.save(new Register(update.message().chat().id(), update.message().chat().username()));
                    telegramBot.execute(new SendMessage(chatId, update.message().chat().firstName() + " зарегистрировался в игру"));
                }
            }
            // поиск по зарегистрировавшимся красавчика дня
            if (messageText.equals("/findPretty")) {
                Register randomUser = registerRepository.findRandomUserId();
                if (randomUser != null) {
                    registerRepository.updateCountOfPretty();
                    telegramBot.execute(new SendMessage(chatId, randomUser.getUserNick()));
                }

            }

            // Создаем регулярное вырежение
            Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})\\s+(.*)");
            // Обрабатываем входящее сообщение. Ищем совпадения в строке и дальнейшего применения метода .find
            Matcher matcher = pattern.matcher(messageText);
            // Проверяем совпадения в регулярном выражении выделенные скобками, всего их 3 шт.
            if (matcher.find()) {
                String date = matcher.group(1);
                String time = matcher.group(2);
                String text = matcher.group(3);

                log.info("Установлено напоминание: Дата: " + date + ". Время: " + time + ". Сообщение: " + text + ".");

                SendMessage message = new SendMessage(chatId, "Установлено напоминание!" +
                        "\nДата: " + date +
                        "\nВремя: " + time +
                        "\nСообщение: " + text);
                // отправляем сообщение в чат, что напоминание установлено
                telegramBot.execute(message);
                // приводим дату и время к типу LocalDateTime
                LocalDateTime localDateTime = LocalDateTime.parse(date + " " + time, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                // сохранение напоминания в базу данных
                telegramBotRepository.save(new TelegramBotModel(chatId, localDateTime, text));
            } else {
                log.error("Сообщение не соответствует формату");
                if (messageText.equals("/start") && messageText.equals("/reg")) {
                    log.info("Введён /start");
                } else {
                    telegramBot.execute(new SendMessage(chatId, "Сообщение не соответствует формату"));
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
