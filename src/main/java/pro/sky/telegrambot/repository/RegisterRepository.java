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

    // Запрос, который проверяет, имеется ли искомое время в базе напоминаний
    @Query(value = "SELECT EXISTS (SELECT 1 FROM register WHERE user_id = :userId)", nativeQuery = true)
    boolean findUserId(Long userId);

    @Query(value = "SELECT * FROM register ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Register findRandomUserId();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM register WHERE id = 1)", nativeQuery = true)
    boolean existsDataBase();

    @Modifying
    @Transactional
    @Query(value = "UPDATE register SET count_of_pretty = count_of_pretty + 1 WHERE id = :id", nativeQuery = true)
    void updateCountOfPrettyRandomUser(Long id);

    // Запрос, который достает два столбца и помещает их в лист сортируя по убыванию по стоблцу count_of_pretty
    @Query(value = "SELECT user_nick, count_of_pretty FROM register ORDER BY count_of_pretty DESC", nativeQuery = true)
    List<Object[]> findTopPrettyUsers();

    @Modifying
    @Transactional
    @Query(value = "UPDATE register SET count_of_pretty = 0", nativeQuery = true)
    void resetCountOfPretty();

}
