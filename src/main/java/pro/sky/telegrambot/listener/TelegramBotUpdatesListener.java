package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Local;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.commands.ScheduledClass;
import pro.sky.telegrambot.commands.TelegramBotProcessingCommands;
import pro.sky.telegrambot.model.Register;
import pro.sky.telegrambot.model.TelegramBotModel;
import pro.sky.telegrambot.repository.RegisterRepository;
import pro.sky.telegrambot.repository.TelegramBotRepository;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
@Slf4j
public class TelegramBotUpdatesListener implements UpdatesListener {

    //@Autowired
    private final TelegramBot telegramBot;
    private final TelegramBotRepository telegramBotRepository;
    private final RegisterRepository registerRepository;
    private final TelegramBotProcessingCommands telegramBotProcessingCommands;
    private LocalDateTime lastChosenDate = null; // Флаг, был ли сегодня запущен метод /findPretty
    private List<Long> chatIdCheck = new ArrayList<>(); // Флаг, был ли сегодня запущен метод /findPretty для конкретного чата
    private boolean isClearConfirmationPending = false; // Флаг, подтверждает ли пользователь очистку

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {

        updates.forEach(update -> {
            try {
                log.info("Processing update: {}", update);
                if (update.message() != null) {
                    // ловим отправленное сообщение
                    String messageText = update.message().text();
                    // достаём ID чата с пользователем
                    Long chatId = update.message().chat().id();
                    /** Создаем приветственное сообщение **/
                    if (messageText.equalsIgnoreCase("/start")) {
                        telegramBotProcessingCommands.startProcessing(chatId);
                    }
                    /** Регистрация в игру **/
                    if (messageText.equalsIgnoreCase("/reg")) {
                        telegramBotProcessingCommands.regProcess(update);
                    }
                    // поиск по зарегистрировавшимся красавчика дня
                    if (messageText.equalsIgnoreCase("/findPretty") && registerRepository.existsDataBase(chatId)) {
                        if (chatId < 0) {
                            LocalDateTime today = LocalDateTime.now();
                            if (lastChosenDate == null || today.isEqual(lastChosenDate)) {
                                //chatIdCheck.add(chatId);
                                lastChosenDate = today;
                                Register randomUser = registerRepository.findRandomUserId(chatId);
                                log.info(randomUser.getUserNick() + " выбран. ID юзера " + randomUser.getUserId());
                                registerRepository.updateCountOfPrettyRandomUser(randomUser.getId());
                                telegramBot.execute(new SendMessage(chatId, "Поздравляем @" + randomUser.getUserNick() + " сегодня ты красавчик!"));
                            } else {
                                telegramBot.execute(new SendMessage(chatId, "Красавчик сегодня уже был выбран!"));
                            }
                        } else {
                            telegramBot.execute(new SendMessage(chatId, "Бот работает только для групповых чатов"));
                        }
                    }
                    // добавление показа статистики красавчика
                    if (messageText.equalsIgnoreCase("/stat") && registerRepository.existsDataBase(chatId)) {
                        telegramBotProcessingCommands.statProcess(update);
                    }

                    // определяем процент
                    if (messageText.equalsIgnoreCase("/roll")) {
                        telegramBotProcessingCommands.rollProcess(update);
                    }

                    // добавление показа статистики ролла
                    if (messageText.equalsIgnoreCase("/statroll")) {
                        telegramBotProcessingCommands.statRollProcess(update);
                    }

                    // очистка статистики
                    if (messageText.equalsIgnoreCase("/clearroll")) {
                        telegramBotProcessingCommands.clearRollProcess(update);
                    }

                    // поддержка разработчика
                    if (messageText.equalsIgnoreCase("/donate")) {
                        telegramBot.execute(new SendMessage(chatId, "Поддержать проект можно на сбер по номеру +79998544568"));
                    }

                    // очистка значений столбца count_of_pretty
                    if (messageText.equalsIgnoreCase("/clear") && registerRepository.existsDataBase(chatId)) {
                        isClearConfirmationPending = true;
                        telegramBot.execute(new SendMessage(chatId, "Вы уверены? Если да введите: yes"));
                    }
                    System.out.println(isClearConfirmationPending + messageText);
                    if (isClearConfirmationPending == true && messageText.equalsIgnoreCase("yes")) {
                        isClearConfirmationPending = false;
                        registerRepository.resetCountOfPretty();
                        telegramBot.execute(new SendMessage(chatId, "Данные были очищены."));
                    }

                    // Создаем регулярное вырежение
                    Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})\\s+(.*)");
                    // Обрабатываем входящее сообщение. Ищем совпадения в строке и дальнейшего применения метода .find
                    Matcher matcher = pattern.matcher(messageText);
                    // Проверяем совпадения
                    if (matcher.find()) {
                        telegramBotProcessingCommands.scheduledProcess(update, matcher);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
