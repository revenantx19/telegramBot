package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pro.sky.telegrambot.model.Register;

import java.time.LocalDateTime;

@Repository
public interface RegisterRepository extends JpaRepository<Register, Long> {

    // Запрос, который проверяет, имеется ли искомое время в базе напоминаний
    @Query(value = "SELECT EXISTS (SELECT 1 FROM register WHERE user_id = :userId)", nativeQuery = true)
    boolean findUserId(Long userId);

    @Query(value = "SELECT * FROM register ORDER BY RANDOM() LIMIT 1;", nativeQuery = true)
    Register findRandomUserId();

    @Query(value = "UPDATE register\n" +
            "SET count_of_pretty = count_of_pretty + 1\n" +
            "WHERE id = (SELECT id FROM register ORDER BY RANDOM() LIMIT 1)", nativeQuery = true)
    void updateCountOfPretty();

}
