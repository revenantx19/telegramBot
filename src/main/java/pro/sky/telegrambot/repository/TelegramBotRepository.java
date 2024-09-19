package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pro.sky.telegrambot.model.Register;
import pro.sky.telegrambot.model.TelegramBotModel;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelegramBotRepository extends JpaRepository<TelegramBotModel, Long> {

    //List<LocalDateTime> findDateAndTime(LocalDateTime localDateTime);

    // Запрос, который проверяет, имеется ли искомое время в базе напоминаний
    @Query(value = "SELECT EXISTS (SELECT 1 FROM telegram_bot_model WHERE date_and_time = :dateTime)", nativeQuery = true)
    boolean existsDateTime(LocalDateTime dateTime);

    // Запрос, который возвращает объект из базы, если найдено время с напоминанием
    @Query(value = "SELECT * FROM telegram_bot_model WHERE date_and_time = :value", nativeQuery = true)
    TelegramBotModel findEqualTimeAndDateNotification(LocalDateTime value);

}
