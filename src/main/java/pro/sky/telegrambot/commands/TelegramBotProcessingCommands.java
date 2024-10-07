package pro.sky.telegrambot.commands;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.Register;
import pro.sky.telegrambot.model.TelegramBotModel;
import pro.sky.telegrambot.repository.RegisterRepository;
import pro.sky.telegrambot.repository.TelegramBotRepository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramBotProcessingCommands {

    private final TelegramBot bot;
    private final RegisterRepository registerRepository;
    private final TelegramBotRepository telegramBotRepository;

    private LocalDateTime lastChosenDate = null; // Флаг, был ли сегодня запущен метод /findPretty

    public void startProcessing(Long chatId) {
        String welcomeMessage = "Добро пожаловать в нашего бота. Я могу:" +
                "\n1. Сохранить для вас напоминание. Для этого отправьте мне сообщение в следующем формате: 14.09.2024 16:33 Текст напоминания" +
                "\n2. Зарегистрировать в игру 'красавчик дня' (команда /reg)" +
                "\n3. Определить красавчика дня (команда /findPretty)" +
                "\n4. Определить процент твоей натуральности (команда /roll)" +
                "\n5. Посмотреть статистику (команда /stat)" +
                "\n6. Поддержать проект (команда /donate)" +
                "\n7. Очистить статистику (команда /clear)";
        // Отправляем приветственное сообщение
        // ChatID нужен для того, чтобы понимать в какой чат будет отправлено сообщение
        bot.execute(new SendMessage(chatId, welcomeMessage));
    }
    /**
     * Зарегистрироваться в игру
     * сохраняются следующие данные в базу: @никтелеграм, ID чата, реальное имя пользователя.
     **/
    public void regProcess(Update update) {
        Long chatId = update.message().chat().id();
        if (chatId < 0) {
            boolean findUserIdAndChatId = registerRepository.findUserIdAndChatId(update.message().from().id(), chatId);
            if (findUserIdAndChatId) {
                bot.execute(new SendMessage(chatId, "Уже в игре."));
            } else {
                registerRepository.save(new Register(update.message().from().id(),
                                                     update.message().from().username(),
                                                     chatId,
                                                     update.message().from().firstName()));
                bot.execute(new SendMessage(chatId, update.message().from().username() + " зарегистрировался в игру"));
            }
        } else {
            bot.execute(new SendMessage(chatId, "Бот работает только для групповых чатов"));
        }
    }

    /**
     * Посылка в чат с ID = -1001861391530 выбора красавчика дня в 20:00 ежедневно
     **/
    @Scheduled(cron = "0 0 20 * * *")
    public void findPrettyProcess() {
        Long chatId = -1001861391530L;
        Register randomUser = registerRepository.findRandomUserId(chatId);
        log.info(randomUser.getUserNick() + " выбран. ID юзера " + randomUser.getUserId());
        registerRepository.updateCountOfPrettyRandomUser(randomUser.getId());
        bot.execute(new SendMessage(chatId, "Поздравляем @" + randomUser.getUserNick() + " сегодня ты красавчик!"));
    }

    public void rollProcess(Update update) {
        Long chatId = update.message().chat().id();
        Long userId = update.message().from().id();
        if (!registerRepository.existsRoll(userId, chatId)) {
            registerRepository.updateAlreadyRoll(userId, chatId);
            Random random = new Random();
            int randomNumber = random.nextInt(101);
            registerRepository.updateRollPercent(randomNumber, userId, chatId);
            if (randomNumber == 100) {
                bot.execute(new SendMessage(chatId, "Билли Харрингтон хочет пожать твою могучую руку " + randomNumber + "%"));
            } else if (randomNumber >= 75) {
                bot.execute(new SendMessage(chatId, "Отличный результат " + randomNumber + "%"));
            } else if (randomNumber >= 50) {
                bot.execute(new SendMessage(chatId, "Нормально " + randomNumber + "%"));
            } else if (randomNumber >= 25) {
                bot.execute(new SendMessage(chatId, "Не забывай поддевать свой кожаный костюм " + randomNumber + "%"));
            } else if (randomNumber >= 1) {
                bot.execute(new SendMessage(chatId, "Поговаривают тебя давно не было видно в Gym " + randomNumber + "%"));
            } else {
                bot.execute(new SendMessage(chatId, "Скоро у тебя совсем не останется друзей " + randomNumber + "%"));
            }
        } else {
            bot.execute(new SendMessage(chatId, "Дружок, ты сегодня уже прошел проверку, приходи завтра."));
        }
    }

    public void statRollProcess(Update update) {
        Long chatId = update.message().chat().id();
        if (chatId < 0) {
            List<Object[]> topsRoller = registerRepository.findTopRollUsers(chatId);
            log.info("Все топы" + topsRoller);
            StringBuilder topRollMessage = new StringBuilder("Статистика по процентам на сегодня:\n\n");
            int i = 1;
            for (Object[] row : topsRoller) {
                String userNick = (String) row[0];
                Integer percentRoll = (Integer) row[1];
                String userRealName = (String) row[2];
                log.info("Пользователь: " + userNick + ", Процент ролла: " + percentRoll + "%");
                topRollMessage.append(i).append(". ").append(userNick).append(" (").append(userRealName).append("): ").append(percentRoll).append("%\n");
                i++;
            }
            bot.execute(new SendMessage(chatId, topRollMessage.toString()));
        } else {
            bot.execute(new SendMessage(chatId, "Бот работает только для групповых чатов"));
        }
    }

    public void clearRollProcess(Update update) {
        Long chatId = update.message().chat().id();;
        registerRepository.clearRollColumn(chatId);
        bot.execute(new SendMessage(chatId, "Очистка завершена, но так лучше не делать."));
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void clearRolling() {
        Long chatId = -1001861391530L;
        registerRepository.clearRollColumn(chatId);
        bot.execute(new SendMessage(chatId, "Дружок-пирожок, ты можешь узнать свой % вновь."));
    }

    public void statProcess(Update update) {
        Long chatId = update.message().chat().id();
        if (chatId < 0) {
            List<Object[]> topPretty = registerRepository.findTopPrettyUsers(chatId);
            StringBuilder prettyMessage = new StringBuilder("Статистика пользователей:\n\n");
            int i = 1;
            for (Object[] row : topPretty) {
                String userNick = (String) row[0];
                BigInteger countOfPretty = (BigInteger) row[1];
                String userRealName = (String) row[2];
                log.info("Пользователь: " + userNick + ", count_of_pretty: " + countOfPretty);
                prettyMessage.append(i).append(". ").append(userNick).append(" (").append(userRealName).append("): ").append(countOfPretty).append("\n");
                i++;
            }
            bot.execute(new SendMessage(chatId, prettyMessage.toString()));
        } else {
            bot.execute(new SendMessage(chatId, "Бот работает только для групповых чатов"));
        }
    }

    /**
     * Установка напоминания
     **/
    public void scheduledProcess(Update update, Matcher matcher) {
        Long chatId = update.message().chat().id();
        String date = matcher.group(1);
        String time = matcher.group(2);
        String text = matcher.group(3);

        log.info("Установлено напоминание: Дата: " + date + ". Время: " + time + ". Сообщение: " + text + ".");

        SendMessage message = new SendMessage(chatId, "Установлено напоминание!" +
                "\nДата: " + date +
                "\nВремя: " + time +
                "\nСообщение: " + text);
        // отправляем сообщение в чат, что напоминание установлено
        bot.execute(message);
        // приводим дату и время к типу LocalDateTime
        LocalDateTime localDateTime = LocalDateTime.parse(date + " " + time, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        // сохранение напоминания в базу данных
        telegramBotRepository.save(new TelegramBotModel(chatId, localDateTime, text));


    }
}
