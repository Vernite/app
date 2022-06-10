package com.workflow.workflow.user;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
    User findByEmail(String email);
    User findByUsername(String username);
    List<User> findByDeletedPermanentlyFalseAndDeletedLessThan(Date date);
    List<User> findByEmailInOrUsernameIn(List<String> emails, List<String> usernames);
    @Transactional
    void deleteAllByEmailNot(String email);
}
