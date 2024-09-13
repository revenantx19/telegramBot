package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pro.sky.telegrambot.model.TelegramBotModel;

import java.time.LocalDateTime;
import java.util.List;

public interface TelegramBotRepository extends JpaRepository<TelegramBotModel, Long> {

    //List<LocalDateTime> findDateAndTime(LocalDateTime localDateTime);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM telegram_bot_model WHERE date_and_time = :dateTime)", nativeQuery = true)
    boolean existsDateTime(LocalDateTime dateTime);

}
