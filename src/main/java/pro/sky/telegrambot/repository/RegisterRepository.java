package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pro.sky.telegrambot.model.Register;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RegisterRepository extends JpaRepository<Register, Long> {

    /**
     * Запрос для напоминаний, не имеет отношения к ИГРЕ ПОИСКА КРАСАВЧИКА
     * Проверяет, имеется ли искомое время в базе напоминаний
     **/
    @Query(value = "SELECT EXISTS (SELECT 1 FROM register WHERE user_id = :userId AND chat_id = :chatId)", nativeQuery = true)
    boolean findUserIdAndChatId(Long userId, Long chatId);

    /**
     * Рандомный поиск юзера среди зарегистрировавшихся
     * которые принадлежат конкретному ID чата
     **/
    @Query(value = "SELECT * FROM register WHERE chat_id = :chatId ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Register findRandomUserId(Long chatId);

    /**
     * Проверка, есть ли в базе данных данные,
     * которые принадлежат конкретному ID чата
     **/
    @Query(value = "SELECT EXISTS (SELECT 1 FROM register WHERE chat_id = :chatId)", nativeQuery = true)
    boolean existsDataBase(Long chatId);

    /**
     * Добавление +1 в колонку count_of_pretty
     * при рандомном выборе красавчика
     **/
    @Modifying
    @Transactional
    @Query(value = "UPDATE register SET count_of_pretty = count_of_pretty + 1 WHERE id = :id", nativeQuery = true)
    void updateCountOfPrettyRandomUser(Long id);

    /**
     * Установка флага, что ролл сегодня был запущен у конкретного игрока
     **/
    @Modifying
    @Transactional
    @Query(value = "UPDATE register SET already_roll = true WHERE user_id = :userId AND chat_id = :chatId", nativeQuery = true)
    void updateAlreadyRoll(Long userId, Long chatId);

    /**
     * Сохранение рандомного значения после ролл`а в базу
     **/
    @Modifying
    @Transactional
    @Query(value = "UPDATE register SET percent_roll = :percentRoll WHERE user_id = :userId AND chat_id = :chatId", nativeQuery = true)
    void updateRollPercent(int percentRoll, Long userId, Long chatId);

    /**
     * Проверка у конкретного пользователя, был ли сегодня запущен ролл
     **/
    @Query(value = "SELECT already_roll FROM register WHERE user_id = :userId AND chat_id = :chatId", nativeQuery = true)
    boolean existsRoll(Long userId, Long chatId);

    /**
     * Запрос, который достает два столбца и помещает их в лист сортируя по убыванию по стоблцу percent_roll
     **/
    @Query(value = "SELECT user_nick, percent_roll, user_real_name FROM register WHERE chat_id = :chatId ORDER BY percent_roll DESC", nativeQuery = true)
    List<Object[]> findTopRollUsers(Long chatId);

    /**
     * Запрос, который достает два столбца и помещает их в лист сортируя по убыванию по стоблцу count_of_pretty
     **/
    @Query(value = "SELECT user_nick, count_of_pretty, user_real_name FROM register WHERE chat_id = :chatId ORDER BY count_of_pretty DESC", nativeQuery = true)
    List<Object[]> findTopPrettyUsers(Long chatId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE register SET count_of_pretty = 0", nativeQuery = true)
    void resetCountOfPretty();

    /**
     * Запрос, который сбрасывает значения столбцов already_roll и percent_roll, чтобы можно было повторно ролл`ить
     **/
    @Modifying
    @Transactional
    @Query(value = "UPDATE register SET already_roll = false, percent_roll = null WHERE chat_id = :chatId", nativeQuery = true)
    void clearRollColumn(Long chatId);
}
