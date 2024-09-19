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
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
@Slf4j
public class TelegramBotUpdatesListener implements UpdatesListener {

    @Autowired
    private TelegramBot telegramBot;

    private final TelegramBotRepository telegramBotRepository;
    private final RegisterRepository registerRepository;
    private LocalDateTime lastChosenDate = null;
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
                    if (messageText.equalsIgnoreCase("/start")) {
                        // Создаем приветственное сообщение
                        String welcomeMessage = "Добро пожаловать в нашего бота. Я могу:" +
                                "\n1. Сохранить для вас напоминание. Для этого отправьте мне сообщение в следующем формате: 14.09.2024 16:33 Текст напоминания" +
                                "\n2. Зарегистрировать в игру 'красавчик дня' (команда /reg)" +
                                "\n3. Определить красавчика дня (команда /findPretty)" +
                                "\n4. Определить процент твоей натуральности (команда /roll)" +
                                "\n5. Посмотреть статистику (команда /stat)" +
                                "\n6. Поддержать проект (команда /donate)";
                        // Отправляем приветственное сообщение
                        // ChatID нужен для того, чтобы понимать в какой чат будет отправлено сообщение
                        telegramBot.execute(new SendMessage(chatId, welcomeMessage));
                    }
                    // регистрация в игру
                    if (messageText.equalsIgnoreCase("/reg")) {
                        boolean findUserId = registerRepository.findUserId(update.message().from().id());
                        if (findUserId) {
                            telegramBot.execute(new SendMessage(chatId, "Уже в игре."));
                        } else {
                            registerRepository.save(new Register(update.message().from().id(), update.message().from().username()));
                            telegramBot.execute(new SendMessage(chatId, update.message().from().username() + " зарегистрировался в игру"));
                        }
                    }
                    // поиск по зарегистрировавшимся красавчика дня
                    if (messageText.equalsIgnoreCase("/findPretty") && registerRepository.existsDataBase()) {
                        LocalDateTime today = LocalDateTime.now();
                        if (lastChosenDate == null || today.isEqual(lastChosenDate)) {
                            lastChosenDate = today;
                            Register randomUser = registerRepository.findRandomUserId();
                            log.info(randomUser.getUserNick() + " выбран." + randomUser.getUserId());
                            registerRepository.updateCountOfPrettyRandomUser(randomUser.getId());
                            telegramBot.execute(new SendMessage(chatId, "Поздравляем @" + randomUser.getUserNick() + " сегодня ты красавчик!"));
                        } else {
                            telegramBot.execute(new SendMessage(chatId, "Красавчик сегодня уже был выбран!"));
                        }
                    }

                    // определяем процент натуральности
                    if (messageText.equalsIgnoreCase("/roll")) {
                        Random random = new Random();
                        int randomNumber = random.nextInt(101);
                        if (randomNumber == 100) {
                            telegramBot.execute(new SendMessage(chatId, "Билли Харрингтон хочет пожать твою могучую руку " + randomNumber + "%"));
                        } else if (randomNumber >= 75) {
                            telegramBot.execute(new SendMessage(chatId, "Отличный результат " + randomNumber + "%"));
                        } else if (randomNumber >= 50) {
                            telegramBot.execute(new SendMessage(chatId, "Нормально " + randomNumber + "%"));
                        } else if (randomNumber >= 25) {
                            telegramBot.execute(new SendMessage(chatId, "Не забывай поддевать свой кожаный костюм " + randomNumber + "%"));
                        } else if (randomNumber >= 1) {
                            telegramBot.execute(new SendMessage(chatId, "Поговаривают тебя давно не было видно в Gym " + randomNumber + "%"));
                        } else if (randomNumber == 0) {
                            telegramBot.execute(new SendMessage(chatId, "Скоро у тебя совсем не останется друзей " + randomNumber + "%"));
                        }
                    }

                    // добавление показа статистики
                    if (messageText.equalsIgnoreCase("/stat") && registerRepository.existsDataBase()) {

                        List<Object[]> topPretty = registerRepository.findTopPrettyUsers();
                        StringBuilder prettyMessage = new StringBuilder("Статистика пользователей:\n\n");
                        int i = 1;
                        for (Object[] row : topPretty) {
                            String userNick = (String) row[0];
                            int countOfPretty = (Integer) row[1];
                            log.info("Пользователь: " + userNick + ", count_of_pretty: " + countOfPretty);
                            prettyMessage.append(i).append(". ").append(userNick).append(": ").append(countOfPretty).append("\n");
                            i++;
                        }
                        telegramBot.execute(new SendMessage(chatId, prettyMessage.toString()));
                    }

                    // поддержка разработчика
                    if (messageText.equalsIgnoreCase("/donate")) {
                        telegramBot.execute(new SendMessage(chatId, "Поддержать проект можно на сбер по номеру +79998544568"));
                    }

                    // очистка значений столбца count_of_pretty
                    if (messageText.equalsIgnoreCase("/clear") && registerRepository.existsDataBase()) {
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
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
